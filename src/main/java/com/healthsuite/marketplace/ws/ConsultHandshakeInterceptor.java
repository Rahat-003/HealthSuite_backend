package com.healthsuite.marketplace.ws;

import com.healthsuite.auth.security.JwtService;
import com.healthsuite.marketplace.service.ConsultationRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Authenticates the signaling handshake: /ws/consult/{id}?token={accessToken}.
 * (Browsers cannot set Authorization headers on WebSocket connections.)
 * Rejects unless the JWT is valid and the user is the patient or the
 * accepting doctor of the consultation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsultHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final ConsultationRoomService consultationRoomService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String path = request.getURI().getPath();
            Long consultationId = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));

            List<String> tokens = UriComponentsBuilder.fromUri(request.getURI())
                .build().getQueryParams().get("token");
            String token = (tokens == null || tokens.isEmpty()) ? null : tokens.get(0);
            if (token == null || !jwtService.validateToken(token)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            Long userId = jwtService.extractUserId(token);
            var role = consultationRoomService.requireParticipant(consultationId, userId);

            attributes.put(ConsultSignalingHandler.ATTR_CONSULTATION_ID, consultationId);
            attributes.put(ConsultSignalingHandler.ATTR_USER_ID, userId);
            attributes.put(ConsultSignalingHandler.ATTR_ROLE, role);
            return true;
        } catch (Exception e) {
            log.warn("WS handshake rejected: {}", e.getMessage());
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
