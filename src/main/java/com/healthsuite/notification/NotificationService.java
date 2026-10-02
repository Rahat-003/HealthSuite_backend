package com.healthsuite.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository repository;

    /**
     * Fire-and-forget: a failed notification must never roll back the
     * business transaction that triggered it.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(Long userId, String type, String title, String body, String link) {
        try {
            repository.save(Notification.builder()
                .userId(userId).type(type).title(title).body(body).link(link)
                .build());
        } catch (Exception e) {
            log.warn("Failed to create notification for user {}: {}", userId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<Notification> latest(Long userId, int size) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, size));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllRead(Long userId) {
        repository.markAllRead(userId);
    }
}
