package com.aeroai.backend.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Section 51 - every external HTTP client gets a short, explicit timeout so
 * one slow dependency never hangs an entire request thread pool.
 * OpenSky/Open-Meteo: 5s connect / 10s read. FastAPI (internal): 3s connect / 8s read.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient openSkyRestClient() {
        return buildClient(5_000, 10_000);
    }

    @Bean
    public RestClient openMeteoRestClient() {
        return buildClient(5_000, 10_000);
    }

    @Bean
    public RestClient predictionRestClient(AppProperties appProperties) {
        return RestClient.builder()
                .baseUrl(appProperties.getMl().getServiceUrl())
                .requestFactory(requestFactory(3_000, 8_000))
                .build();
    }

    private RestClient buildClient(int connectTimeoutMs, int readTimeoutMs) {
        return RestClient.builder()
                .requestFactory(requestFactory(connectTimeoutMs, readTimeoutMs))
                .build();
    }

    private HttpComponentsClientHttpRequestFactory requestFactory(int connectTimeoutMs, int readTimeoutMs) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultConnectionConfig(connectionConfig);

        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
                .build();

        var httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
