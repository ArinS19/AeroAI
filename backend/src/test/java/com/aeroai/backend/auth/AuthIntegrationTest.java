package com.aeroai.backend.auth;

import com.aeroai.backend.auth.dto.AuthResponse;
import com.aeroai.backend.auth.dto.LoginRequest;
import com.aeroai.backend.auth.dto.RegisterRequest;
import com.aeroai.backend.common.dto.ApiError;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Section 75/81 - registration, login, authentication failure, protected
 * endpoints. Runs against the REAL local PostgreSQL (docker container),
 * exercising the actual persistence layer rather than mocks.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private String testEmail;

    @AfterEach
    void cleanup() {
        if (testEmail != null) {
            userRepository.findByEmail(testEmail).ifPresent(userRepository::delete);
        }
    }

    private String uniqueEmail() {
        testEmail = "authtest-" + UUID.randomUUID() + "@example.com";
        return testEmail;
    }

    @Test
    void register_thenLogin_succeeds() {
        String email = uniqueEmail();
        RegisterRequest registerRequest = new RegisterRequest(email, "password123", "password123");

        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity("/api/auth/register", registerRequest, AuthResponse.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());
        assertNotNull(registerResponse.getBody().token());
        assertEquals(email, registerResponse.getBody().user().email());

        LoginRequest loginRequest = new LoginRequest(email, "password123");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity("/api/auth/login", loginRequest, AuthResponse.class);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().token());
    }

    @Test
    void register_duplicateEmail_returns409Conflict() {
        String email = uniqueEmail();
        RegisterRequest registerRequest = new RegisterRequest(email, "password123", "password123");
        restTemplate.postForEntity("/api/auth/register", registerRequest, AuthResponse.class);

        ResponseEntity<ApiError> secondAttempt = restTemplate.postForEntity("/api/auth/register", registerRequest, ApiError.class);
        assertEquals(HttpStatus.CONFLICT, secondAttempt.getStatusCode());
        assertEquals("CONFLICT", secondAttempt.getBody().error());
    }

    @Test
    void register_weakPassword_returns400ValidationError() {
        RegisterRequest registerRequest = new RegisterRequest(uniqueEmail(), "short", "short");
        ResponseEntity<ApiError> response = restTemplate.postForEntity("/api/auth/register", registerRequest, ApiError.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION_ERROR", response.getBody().error());
    }

    @Test
    void login_wrongPassword_returns401WithGenericMessage() {
        String email = uniqueEmail();
        restTemplate.postForEntity("/api/auth/register", new RegisterRequest(email, "password123", "password123"), AuthResponse.class);

        ResponseEntity<ApiError> response = restTemplate.postForEntity("/api/auth/login", new LoginRequest(email, "wrongpassword"), ApiError.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password.", response.getBody().message());
    }

    @Test
    void login_nonexistentUser_returns401WithSameGenericMessage() {
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest("nobody-" + UUID.randomUUID() + "@example.com", "password123"), ApiError.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password.", response.getBody().message());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        ResponseEntity<ApiError> response = restTemplate.getForEntity("/api/me", ApiError.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void protectedEndpoint_withValidToken_succeeds() {
        String email = uniqueEmail();
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/auth/register", new RegisterRequest(email, "password123", "password123"), AuthResponse.class);
        String token = registerResponse.getBody().token();

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange("/api/me", org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains(email));
    }
}
