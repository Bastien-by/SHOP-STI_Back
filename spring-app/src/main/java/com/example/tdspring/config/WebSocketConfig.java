package com.example.tdspring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Canal WebSocket (STOMP) utilisé pour pousser en temps réel les scans lus
 * sur l'automate aux clients Angular, à la place du polling HTTP individuel
 * que faisait chaque page (login, scan, check, dépôt).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket natif (pas de fallback SockJS) : le parc de postes cible
        // uniquement des navigateurs modernes sur le réseau interne.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
