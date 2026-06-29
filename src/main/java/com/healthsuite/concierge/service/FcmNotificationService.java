package com.healthsuite.concierge.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.healthsuite.concierge.entity.ConciergeTicket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FcmNotificationService {

    @Nullable
    private final FirebaseMessaging firebaseMessaging;

    @Autowired
    public FcmNotificationService(@Nullable FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    public void sendTicketConfirmedNotification(ConciergeTicket ticket) {
        String fcmToken = ticket.getOwner().getFcmToken();
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("No FCM token for user {} — skipping push notification for ticket {}",
                    ticket.getOwner().getId(), ticket.getId());
            return;
        }
        if (firebaseMessaging == null) {
            log.warn("Firebase not configured — skipping push notification for ticket {}", ticket.getId());
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle("Appointment Confirmed")
                            .setBody("Your appointment with Dr. " + ticket.getDoctorName()
                                    + " has been confirmed. Serial: " + ticket.getSerialNumber())
                            .build())
                    .putData("ticketId", String.valueOf(ticket.getId()))
                    .putData("serialNumber", ticket.getSerialNumber())
                    .putData("estimatedArrivalTime",
                            ticket.getEstimatedArrivalTime() != null
                                    ? ticket.getEstimatedArrivalTime().toString() : "")
                    .putData("doctorName", ticket.getDoctorName())
                    .putData("chamberAddress", ticket.getChamberAddress())
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("FCM notification sent for ticket {}: {}", ticket.getId(), response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM send failed for ticket {}: {}", ticket.getId(), e.getMessage());
        }
    }
}
