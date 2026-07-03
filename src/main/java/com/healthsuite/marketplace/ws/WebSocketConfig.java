package com.healthsuite.marketplace.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ConsultSignalingHandler consultSignalingHandler;
    private final ConsultHandshakeInterceptor consultHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(consultSignalingHandler, "/ws/consult/*")
            .addInterceptors(consultHandshakeInterceptor)
            .setAllowedOriginPatterns("*"); // JWT check in the interceptor is the gate
    }
}
