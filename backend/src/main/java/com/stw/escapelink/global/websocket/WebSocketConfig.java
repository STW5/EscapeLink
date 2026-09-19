package com.stw.escapelink.global.websocket;

import com.stw.escapelink.global.config.CorsProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final HandshakeInterceptor teamSessionHandshakeInterceptor;
    private final TeamChannelInterceptor teamChannelInterceptor;
    private final CorsProperties corsProperties;

    public WebSocketConfig(TeamSessionHandshakeInterceptor teamSessionHandshakeInterceptor,
                            TeamChannelInterceptor teamChannelInterceptor,
                            CorsProperties corsProperties) {
        this.teamSessionHandshakeInterceptor = teamSessionHandshakeInterceptor;
        this.teamChannelInterceptor = teamChannelInterceptor;
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(teamSessionHandshakeInterceptor)
                .setAllowedOriginPatterns(corsProperties.getAllowedOrigins().toArray(new String[0]))
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(teamChannelInterceptor);
    }
}
