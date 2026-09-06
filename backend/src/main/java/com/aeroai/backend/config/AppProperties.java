package com.aeroai.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Binds every tunable threshold from IMPLEMENT_v2.md Section 94's
 * configuration reference table to a named property. Nothing in this
 * document's numeric thresholds should ever appear as a magic number
 * embedded directly in a service class - it is read from here instead.
 */
@Component
@ConfigurationProperties(prefix = "aeroai")
public class AppProperties {

    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private Security security = new Security();
    private OpenSky opensky = new OpenSky();
    private Ml ml = new Ml();
    private Airspace airspace = new Airspace();
    private Freshness freshness = new Freshness();
    private Trail trail = new Trail();
    private History history = new History();
    private Prediction prediction = new Prediction();
    private Alert alert = new Alert();
    private Conflict conflict = new Conflict();
    private Analytics analytics = new Analytics();
    private Map map = new Map();
    private Weather weather = new Weather();

    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }
    public OpenSky getOpensky() { return opensky; }
    public void setOpensky(OpenSky opensky) { this.opensky = opensky; }
    public Ml getMl() { return ml; }
    public void setMl(Ml ml) { this.ml = ml; }
    public Airspace getAirspace() { return airspace; }
    public void setAirspace(Airspace airspace) { this.airspace = airspace; }
    public Freshness getFreshness() { return freshness; }
    public void setFreshness(Freshness freshness) { this.freshness = freshness; }
    public Trail getTrail() { return trail; }
    public void setTrail(Trail trail) { this.trail = trail; }
    public History getHistory() { return history; }
    public void setHistory(History history) { this.history = history; }
    public Prediction getPrediction() { return prediction; }
    public void setPrediction(Prediction prediction) { this.prediction = prediction; }
    public Alert getAlert() { return alert; }
    public void setAlert(Alert alert) { this.alert = alert; }
    public Conflict getConflict() { return conflict; }
    public void setConflict(Conflict conflict) { this.conflict = conflict; }
    public Analytics getAnalytics() { return analytics; }
    public void setAnalytics(Analytics analytics) { this.analytics = analytics; }
    public Map getMap() { return map; }
    public void setMap(Map map) { this.map = map; }
    public Weather getWeather() { return weather; }
    public void setWeather(Weather weather) { this.weather = weather; }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Jwt {
        private String secret;
        private int expiryMinutes = 60;
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public int getExpiryMinutes() { return expiryMinutes; }
        public void setExpiryMinutes(int expiryMinutes) { this.expiryMinutes = expiryMinutes; }
    }

    public static class Security {
        private int bcryptStrength = 10;
        public int getBcryptStrength() { return bcryptStrength; }
        public void setBcryptStrength(int bcryptStrength) { this.bcryptStrength = bcryptStrength; }
    }

    public static class OpenSky {
        private String credentialsFile;
        private String clientId;
        private String clientSecret;
        private String oauthTokenUrl = "https://auth.opensky-network.org/auth/realms/opensky-network/protocol/openid-connect/token";
        private String apiBaseUrl = "https://opensky-network.org/api";
        private int pollIntervalSeconds = 15;
        private BoundingBox boundingBox = new BoundingBox();

        public String getCredentialsFile() { return credentialsFile; }
        public void setCredentialsFile(String credentialsFile) { this.credentialsFile = credentialsFile; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getOauthTokenUrl() { return oauthTokenUrl; }
        public void setOauthTokenUrl(String oauthTokenUrl) { this.oauthTokenUrl = oauthTokenUrl; }
        public String getApiBaseUrl() { return apiBaseUrl; }
        public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
        public int getPollIntervalSeconds() { return pollIntervalSeconds; }
        public void setPollIntervalSeconds(int pollIntervalSeconds) { this.pollIntervalSeconds = pollIntervalSeconds; }
        public BoundingBox getBoundingBox() { return boundingBox; }
        public void setBoundingBox(BoundingBox boundingBox) { this.boundingBox = boundingBox; }

        public static class BoundingBox {
            private double lamin = 5;
            private double lomin = 60;
            private double lamax = 38;
            private double lomax = 100;
            public double getLamin() { return lamin; }
            public void setLamin(double lamin) { this.lamin = lamin; }
            public double getLomin() { return lomin; }
            public void setLomin(double lomin) { this.lomin = lomin; }
            public double getLamax() { return lamax; }
            public void setLamax(double lamax) { this.lamax = lamax; }
            public double getLomax() { return lomax; }
            public void setLomax(double lomax) { this.lomax = lomax; }
        }
    }

    public static class Ml {
        private String serviceUrl = "http://127.0.0.1:8000";
        public String getServiceUrl() { return serviceUrl; }
        public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }
    }

    public static class Airspace {
        private String defaultCityId = "mumbai";
        private int maxActiveCityCount = 1;
        private int requestCacheTtlSeconds = 120;
        public String getDefaultCityId() { return defaultCityId; }
        public void setDefaultCityId(String defaultCityId) { this.defaultCityId = defaultCityId; }
        public int getMaxActiveCityCount() { return maxActiveCityCount; }
        public void setMaxActiveCityCount(int maxActiveCityCount) { this.maxActiveCityCount = maxActiveCityCount; }
        public int getRequestCacheTtlSeconds() { return requestCacheTtlSeconds; }
        public void setRequestCacheTtlSeconds(int requestCacheTtlSeconds) { this.requestCacheTtlSeconds = requestCacheTtlSeconds; }
    }

    public static class Freshness {
        private int liveThresholdSeconds = 20;
        private int unavailableThresholdSeconds = 90;
        private int aircraftStaleThresholdSeconds = 90;
        public int getLiveThresholdSeconds() { return liveThresholdSeconds; }
        public void setLiveThresholdSeconds(int liveThresholdSeconds) { this.liveThresholdSeconds = liveThresholdSeconds; }
        public int getUnavailableThresholdSeconds() { return unavailableThresholdSeconds; }
        public void setUnavailableThresholdSeconds(int unavailableThresholdSeconds) { this.unavailableThresholdSeconds = unavailableThresholdSeconds; }
        public int getAircraftStaleThresholdSeconds() { return aircraftStaleThresholdSeconds; }
        public void setAircraftStaleThresholdSeconds(int aircraftStaleThresholdSeconds) { this.aircraftStaleThresholdSeconds = aircraftStaleThresholdSeconds; }
    }

    public static class Trail {
        private int retentionMinutes = 60;
        public int getRetentionMinutes() { return retentionMinutes; }
        public void setRetentionMinutes(int retentionMinutes) { this.retentionMinutes = retentionMinutes; }
    }

    public static class History {
        private int retentionDays = 7;
        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    }

    public static class Prediction {
        private int cacheTtlSeconds = 900;
        private int retentionDays = 7;
        public int getCacheTtlSeconds() { return cacheTtlSeconds; }
        public void setCacheTtlSeconds(int cacheTtlSeconds) { this.cacheTtlSeconds = cacheTtlSeconds; }
        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    }

    public static class Alert {
        private int retentionDays = 30;
        private int delayChangeThresholdMinutes = 15;
        private int dedupWindowSeconds = 300;
        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
        public int getDelayChangeThresholdMinutes() { return delayChangeThresholdMinutes; }
        public void setDelayChangeThresholdMinutes(int delayChangeThresholdMinutes) { this.delayChangeThresholdMinutes = delayChangeThresholdMinutes; }
        public int getDedupWindowSeconds() { return dedupWindowSeconds; }
        public void setDedupWindowSeconds(int dedupWindowSeconds) { this.dedupWindowSeconds = dedupWindowSeconds; }
    }

    public static class Conflict {
        private double highHorizontalM = 3000;
        private double highVerticalM = 150;
        private double mediumHorizontalM = 9000;
        private double mediumVerticalM = 300;
        private int predictionHorizonSeconds = 300;
        private double candidateRadiusKm = 50;
        public double getHighHorizontalM() { return highHorizontalM; }
        public void setHighHorizontalM(double highHorizontalM) { this.highHorizontalM = highHorizontalM; }
        public double getHighVerticalM() { return highVerticalM; }
        public void setHighVerticalM(double highVerticalM) { this.highVerticalM = highVerticalM; }
        public double getMediumHorizontalM() { return mediumHorizontalM; }
        public void setMediumHorizontalM(double mediumHorizontalM) { this.mediumHorizontalM = mediumHorizontalM; }
        public double getMediumVerticalM() { return mediumVerticalM; }
        public void setMediumVerticalM(double mediumVerticalM) { this.mediumVerticalM = mediumVerticalM; }
        public int getPredictionHorizonSeconds() { return predictionHorizonSeconds; }
        public void setPredictionHorizonSeconds(int predictionHorizonSeconds) { this.predictionHorizonSeconds = predictionHorizonSeconds; }
        public double getCandidateRadiusKm() { return candidateRadiusKm; }
        public void setCandidateRadiusKm(double candidateRadiusKm) { this.candidateRadiusKm = candidateRadiusKm; }
    }

    public static class Analytics {
        private String timezone = "Asia/Kolkata";
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
    }

    public static class Map {
        private double defaultCenterLat = 22.0;
        private double defaultCenterLon = 79.0;
        private int defaultZoom = 5;
        public double getDefaultCenterLat() { return defaultCenterLat; }
        public void setDefaultCenterLat(double defaultCenterLat) { this.defaultCenterLat = defaultCenterLat; }
        public double getDefaultCenterLon() { return defaultCenterLon; }
        public void setDefaultCenterLon(double defaultCenterLon) { this.defaultCenterLon = defaultCenterLon; }
        public int getDefaultZoom() { return defaultZoom; }
        public void setDefaultZoom(int defaultZoom) { this.defaultZoom = defaultZoom; }
    }

    public static class Weather {
        private int cacheTtlMinutes = 15;
        private String openMeteoBaseUrl = "https://api.open-meteo.com/v1/forecast";
        public int getCacheTtlMinutes() { return cacheTtlMinutes; }
        public void setCacheTtlMinutes(int cacheTtlMinutes) { this.cacheTtlMinutes = cacheTtlMinutes; }
        public String getOpenMeteoBaseUrl() { return openMeteoBaseUrl; }
        public void setOpenMeteoBaseUrl(String openMeteoBaseUrl) { this.openMeteoBaseUrl = openMeteoBaseUrl; }
    }
}
