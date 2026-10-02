package com.healthsuite.marketplace.ws;

import com.healthsuite.marketplace.service.ConsultationRoomService.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebRTC signaling relay for consultation rooms.
 *
 * One room per consultation, exactly two seats (PATIENT, DOCTOR); a
 * reconnecting participant replaces their previous session. The handler
 * never inspects SDP/ICE payloads — it relays them to the other seat and
 * emits presence events ({"type":"room-state"|"peer-joined"|"peer-left"}).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConsultSignalingHandler extends TextWebSocketHandler {

    public static final String ATTR_CONSULTATION_ID = "consultationId";
    public static final String ATTR_ROLE = "role";
    public static final String ATTR_USER_ID = "userId";

    private final ApplicationEventPublisher eventPublisher;

    private final Map<Long, Map<Role, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    /** A participant connected to a room while the other seat was empty. */
    public record PeerJoinedEvent(Long consultationId, Role joinerRole, Long joinerUserId) {}

    /** Live presence check for the given seat (used by the room-activity poll). */
    public boolean isSeatOccupied(Long consultationId, Role role) {
        Map<Role, WebSocketSession> room = rooms.get(consultationId);
        if (room == null) return false;
        WebSocketSession session = room.get(role);
        return session != null && session.isOpen();
    }

    /** Cheap short-circuit: no occupied rooms means nothing to look up. */
    public boolean anyRoomOccupied() {
        return !rooms.isEmpty();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        Long consultationId = (Long) session.getAttributes().get(ATTR_CONSULTATION_ID);
        Role role = (Role) session.getAttributes().get(ATTR_ROLE);

        Map<Role, WebSocketSession> room = rooms.computeIfAbsent(consultationId, k -> new ConcurrentHashMap<>());
        WebSocketSession previous = room.put(role, session);
        if (previous != null && previous.isOpen()) {
            previous.close(CloseStatus.POLICY_VIOLATION.withReason("Replaced by a newer connection"));
        }

        WebSocketSession peer = room.get(other(role));
        boolean peerPresent = peer != null && peer.isOpen();
        send(session, "{\"type\":\"room-state\",\"peerPresent\":" + peerPresent + "}");
        if (peerPresent) {
            send(peer, "{\"type\":\"peer-joined\"}");
        } else if (previous == null) {
            // First connection into an empty room (not a reconnect): tell the absent peer
            Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
            eventPublisher.publishEvent(new PeerJoinedEvent(consultationId, role, userId));
        }
        log.info("WS joined: consultation={} role={} peerPresent={}", consultationId, role, peerPresent);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long consultationId = (Long) session.getAttributes().get(ATTR_CONSULTATION_ID);
        Role role = (Role) session.getAttributes().get(ATTR_ROLE);
        Map<Role, WebSocketSession> room = rooms.get(consultationId);
        if (room == null) return;
        WebSocketSession peer = room.get(other(role));
        if (peer != null && peer.isOpen()) {
            peer.sendMessage(message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws IOException {
        Long consultationId = (Long) session.getAttributes().get(ATTR_CONSULTATION_ID);
        Role role = (Role) session.getAttributes().get(ATTR_ROLE);
        Map<Role, WebSocketSession> room = rooms.get(consultationId);
        if (room == null) return;

        // Only vacate the seat if it is still held by this session (not a replacement)
        if (room.remove(role, session)) {
            WebSocketSession peer = room.get(other(role));
            if (peer != null && peer.isOpen()) {
                send(peer, "{\"type\":\"peer-left\"}");
            }
            if (room.isEmpty()) {
                rooms.remove(consultationId, room);
            }
        }
        log.info("WS left: consultation={} role={}", consultationId, role);
    }

    private Role other(Role role) {
        return role == Role.PATIENT ? Role.DOCTOR : Role.PATIENT;
    }

    private void send(WebSocketSession session, String json) throws IOException {
        if (session.isOpen()) session.sendMessage(new TextMessage(json));
    }
}
