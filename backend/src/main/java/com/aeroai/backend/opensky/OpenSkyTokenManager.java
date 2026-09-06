package com.aeroai.backend.opensky;

import com.aeroai.backend.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Section 18.3 - OAuth2 client-credentials token acquisition/caching for
 * OpenSky. Since March 18 2026, OpenSky no longer supports Basic Auth for
 * programmatic REST access. Credentials are resolved with the
 * credentials-file taking precedence over OPENSKY_CLIENT_ID/SECRET, per
 * Section 49. The token and client secret are NEVER exposed to callers
 * outside this class - {@link #getBearerToken()} is the only public surface.
 */
@Component
public class OpenSkyTokenManager {

    private static final Logger log = LoggerFactory.getLogger(OpenSkyTokenManager.class);
    /** Refresh this many seconds before actual expiry to avoid a request racing an expiring token. */
    private static final long REFRESH_MARGIN_SECONDS = 30;

    private final RestClient restClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiry = Instant.EPOCH;
    private volatile boolean credentialsResolved;
    private volatile String clientId;
    private volatile String clientSecret;

    public OpenSkyTokenManager(RestClient openSkyRestClient, AppProperties appProperties, ObjectMapper objectMapper) {
        this.restClient = openSkyRestClient;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    /** Returns a valid bearer token, or empty if no credentials are configured (anonymous access applies). */
    public Optional<String> getBearerToken() {
        resolveCredentialsIfNeeded();
        if (clientId == null || clientSecret == null) {
            return Optional.empty();
        }
        if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiry.minusSeconds(REFRESH_MARGIN_SECONDS))) {
            return Optional.of(cachedToken);
        }
        lock.lock();
        try {
            if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiry.minusSeconds(REFRESH_MARGIN_SECONDS))) {
                return Optional.of(cachedToken);
            }
            return refreshToken();
        } finally {
            lock.unlock();
        }
    }

    private Optional<String> refreshToken() {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);

            String rawBody = restClient.post()
                    .uri(appProperties.getOpensky().getOauthTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);

            JsonNode json = objectMapper.readTree(rawBody);
            String accessToken = json.path("access_token").asText(null);
            long expiresInSeconds = json.path("expires_in").asLong(300);
            if (accessToken == null) {
                log.warn("OpenSky token response did not contain access_token.");
                return Optional.empty();
            }
            cachedToken = accessToken;
            cachedTokenExpiry = Instant.now().plusSeconds(expiresInSeconds);
            log.info("Refreshed OpenSky OAuth2 token (expires in {}s).", expiresInSeconds);
            return Optional.of(cachedToken);
        } catch (Exception ex) {
            log.warn("Failed to obtain OpenSky OAuth2 token; falling back to anonymous access: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private void resolveCredentialsIfNeeded() {
        if (credentialsResolved) {
            return;
        }
        synchronized (this) {
            if (credentialsResolved) {
                return;
            }
            String fileClientId = null;
            String fileClientSecret = null;
            String credentialsFile = appProperties.getOpensky().getCredentialsFile();
            if (credentialsFile != null && !credentialsFile.isBlank()) {
                try {
                    Path path = Path.of(credentialsFile);
                    if (Files.exists(path)) {
                        JsonNode json = objectMapper.readTree(Files.readString(path));
                        fileClientId = json.path("clientId").asText(null);
                        fileClientSecret = json.path("clientSecret").asText(null);
                        log.info("Loaded OpenSky OAuth2 credentials from configured credentials file.");
                    } else {
                        log.info("OPENSKY_CREDENTIALS_FILE configured but not found at '{}'; falling back to env vars/anonymous access.", credentialsFile);
                    }
                } catch (IOException ex) {
                    log.warn("Failed to read OpenSky credentials file: {}", ex.getMessage());
                }
            }

            // Credentials-file values take precedence only when explicitly present (Section 18.3);
            // otherwise fall back to OPENSKY_CLIENT_ID / OPENSKY_CLIENT_SECRET env vars.
            this.clientId = fileClientId != null ? fileClientId : blankToNull(appProperties.getOpensky().getClientId());
            this.clientSecret = fileClientSecret != null ? fileClientSecret : blankToNull(appProperties.getOpensky().getClientSecret());

            if (this.clientId == null || this.clientSecret == null) {
                log.warn("No OpenSky OAuth2 credentials configured - using anonymous access at reduced rate limits.");
            }
            credentialsResolved = true;
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
