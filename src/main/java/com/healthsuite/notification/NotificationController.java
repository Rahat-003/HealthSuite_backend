package com.healthsuite.notification;

import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification feed")
public class NotificationController {

    private final NotificationService notificationService;

    public record NotificationItem(Long id, String type, String title, String body,
                                   String link, boolean read, LocalDateTime createdAt) {}

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> latest(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "15") int size) {
        List<NotificationItem> items = notificationService.latest(principal.getId(), Math.min(size, 50))
            .map(n -> new NotificationItem(n.getId(), n.getType(), n.getTitle(), n.getBody(),
                n.getLink(), n.isRead(), n.getCreatedAt()))
            .getContent();
        long unread = notificationService.unreadCount(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("items", items, "unread", unread)));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked read.", null));
    }
}
