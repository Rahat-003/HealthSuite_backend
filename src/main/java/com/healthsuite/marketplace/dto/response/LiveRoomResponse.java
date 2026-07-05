package com.healthsuite.marketplace.dto.response;

/**
 * A consultation room where the other participant is connected right now.
 * {@code peerRole} is who is waiting in the room (DOCTOR or PATIENT).
 */
public record LiveRoomResponse(
    Long consultationId,
    String peerRole,
    String peerName,
    String peerPhotoUrl,
    String peerSubtitle,
    Long doctorProfileId
) {}
