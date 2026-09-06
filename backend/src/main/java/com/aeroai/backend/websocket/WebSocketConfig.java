package com.aeroai.backend.websocket;

import com.aeroai.backend.config.AppProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Section 19 - STOMP over WebSocket with SockJS fallback. Clients subscribe
 * to `/topic/aircraft/{airspaceId}` for city-scoped live deltas (Section 18.4).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AppProperties appProperties;

    public WebSocketConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(appProperties.getCors().getAllowedOrigins().toArray(new String[0]))
                .withSockJS();
        // Raw WebSocket endpoint (no SockJS) for clients that support it directly.
        registry.addEndpoint("/ws")
                .setAllowedOrigins(appProperties.getCors().getAllowedOrigins().toArray(new String[0]));
    }
}
