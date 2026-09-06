package com.aeroai.backend.security;

import com.aeroai.backend.auth.UserRepository;
import com.aeroai.backend.auth.dto.AuthResponse;
import com.aeroai.backend.auth.dto.RegisterRequest;
import com.aeroai.backend.tracking.dto.TrackFlightRequest;
import com.aeroai.backend.tracking.dto.TrackedFlightDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Section 40/78 - a user must never be able to access or modify another
 * user's tracked flights/alerts/account data, even with a valid JWT for a
 * different account.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CrossUserAccessSecurityTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    private String userAEmail;
    private String userBEmail;

    @AfterEach
    void cleanup() {
        if (userAEmail != null) userRepository.findByEmail(userAEmail).ifPresent(userRepository::delete);
        if (userBEmail != null) userRepository.findByEmail(userBEmail).ifPresent(userRepository::delete);
    }

    @Test
    void userCannotUntrackAnotherUsersTrackedFlight() {
        userAEmail = "usera-" + UUID.randomUUID() + "@example.com";
        userBEmail = "userb-" + UUID.randomUUID() + "@example.com";

        String tokenA = register(userAEmail);
        String tokenB = register(userBEmail);

        // User A tracks a flight.
        HttpHeaders headersA = bearerHeaders(tokenA);
        TrackFlightRequest trackRequest = new TrackFlightRequest("abc123", "TEST1");
        ResponseEntity<TrackedFlightDto> trackResponse = restTemplate.postForEntity(
                "/api/tracked-flights", new HttpEntity<>(trackRequest, headersA), TrackedFlightDto.class);
        assertEquals(HttpStatus.CREATED, trackResponse.getStatusCode());
        Long trackedFlightId = trackResponse.getBody().id();

        // User B attempts to delete user A's tracked flight -> must fail, never succeed.
        HttpHeaders headersB = bearerHeaders(tokenB);
        ResponseEntity<String> deleteAttempt = restTemplate.exchange(
                "/api/tracked-flights/" + trackedFlightId, HttpMethod.DELETE, new HttpEntity<>(headersB), String.class);
        assertEquals(HttpStatus.NOT_FOUND, deleteAttempt.getStatusCode(),
                "User B must not be able to delete User A's tracked flight.");

        // User A can still see/delete their own tracked flight.
        ResponseEntity<String> myFlights = restTemplate.exchange(
                "/api/me/flights", HttpMethod.GET, new HttpEntity<>(headersA), String.class);
        assertEquals(HttpStatus.OK, myFlights.getStatusCode());

        restTemplate.exchange("/api/tracked-flights/" + trackedFlightId, HttpMethod.DELETE, new HttpEntity<>(headersA), Void.class);
    }

    private String register(String email) {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/auth/register", new RegisterRequest(email, "password123", "password123"), AuthResponse.class);
        return response.getBody().token();
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
