package com.aeroai.backend.opensky;

import com.aeroai.backend.airspace.Airspace;
import com.aeroai.backend.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Section 18 - the ONLY place in the application that calls OpenSky.
 * Centralized, scheduled, city-bounding-box scoped (Section 18.4). Never
 * triggered by an incoming browser request.
 */
@Component
public class OpenSkyClient {

    private static final Logger log = LoggerFactory.getLogger(OpenSkyClient.class);

    private final RestClient restClient;
    private final OpenSkyTokenManager tokenManager;
    private final AppProperties appProperties;

    public OpenSkyClient(RestClient openSkyRestClient, OpenSkyTokenManager tokenManager, AppProperties appProperties) {
        this.restClient = openSkyRestClient;
        this.tokenManager = tokenManager;
        this.appProperties = appProperties;
    }

    public record FetchResult(boolean success, boolean rateLimited, List<OpenSkyStateVector> states) {
        static FetchResult ok(List<OpenSkyStateVector> states) {
            return new FetchResult(true, false, states);
        }

        static FetchResult failure(boolean rateLimited) {
            return new FetchResult(false, rateLimited, List.of());
        }
    }

    public FetchResult fetchStates(Airspace airspace) {
        String url = appProperties.getOpensky().getApiBaseUrl() + "/states/all"
                + "?lamin=" + airspace.getLamin()
                + "&lomin=" + airspace.getLomin()
                + "&lamax=" + airspace.getLamax()
                + "&lomax=" + airspace.getLomax();

        try {
            Optional<String> token = tokenManager.getBearerToken();
            RestClient.RequestHeadersSpec<?> request = restClient.get().uri(url);
            if (token.isPresent()) {
                request = request.headers(h -> h.setBearerAuth(token.get()));
            }

            JsonNode response = request.retrieve().body(JsonNode.class);
            return FetchResult.ok(parseStates(response));
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests ex) {
            log.warn("OpenSky rate limit (429) hit for airspace '{}'.", airspace.getSlug());
            return FetchResult.failure(true);
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            HttpStatusCode status = ex.getStatusCode();
            log.warn("OpenSky request failed for airspace '{}' with status {}: {}", airspace.getSlug(), status, ex.getMessage());
            return FetchResult.failure(false);
        } catch (Exception ex) {
            log.warn("OpenSky request failed for airspace '{}': {}", airspace.getSlug(), ex.getMessage());
            return FetchResult.failure(false);
        }
    }

    private List<OpenSkyStateVector> parseStates(JsonNode response) {
        List<OpenSkyStateVector> result = new ArrayList<>();
        if (response == null) {
            return result;
        }
        JsonNode states = response.path("states");
        if (!states.isArray()) {
            return result;
        }
        for (JsonNode row : states) {
            try {
                result.add(OpenSkyStateVector.fromJsonArray(row));
            } catch (Exception ex) {
                log.debug("Skipping malformed OpenSky state row: {}", ex.getMessage());
            }
        }
        return result;
    }
}
