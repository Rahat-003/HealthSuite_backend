package com.healthsuite.concierge.controller;

import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.common.response.ApiResponse;
import com.healthsuite.common.response.PagedResponse;
import com.healthsuite.concierge.dto.request.CancelTicketRequest;
import com.healthsuite.concierge.dto.request.ConfirmTicketRequest;
import com.healthsuite.concierge.dto.request.CreateTicketRequest;
import com.healthsuite.concierge.dto.response.ConciergeTicketResponse;
import com.healthsuite.concierge.service.ConciergeTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/concierge/tickets")
@RequiredArgsConstructor
@Tag(name = "Concierge Booking", description = "Premium-only appointment booking queue. State machine: PENDING → PROCESSING → CONFIRMED | CANCELLED")
public class ConciergeTicketController {

    private final ConciergeTicketService ticketService;

    @Operation(summary = "Submit a booking request",
            description = "Requires `is_premium = true` in the database. Supports proxy booking for verified family members.")
    @PostMapping
    public ResponseEntity<ApiResponse<ConciergeTicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Ticket submitted", ticketService.createTicket(request, principal.getId())));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PagedResponse<ConciergeTicketResponse>>> getMyTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getMyTickets(principal.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @Operation(summary = "Get pending tickets queue", description = "ROLE_SUPPORT only. Returns PENDING tickets sorted oldest-first for fair assignment.")
    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<PagedResponse<ConciergeTicketResponse>>> getPendingQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(ticketService.getPendingQueue(
                PageRequest.of(page, size, Sort.by("createdAt").ascending()))));
    }

    @Operation(summary = "Claim a ticket", description = "ROLE_SUPPORT only. Uses a pessimistic DB lock — first agent wins; simultaneous claims return 409.")
    @PutMapping("/{ticketId}/claim")
    public ResponseEntity<ApiResponse<ConciergeTicketResponse>> claimTicket(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket claimed",
                ticketService.claimTicket(ticketId, principal.getId())));
    }

    @Operation(summary = "Confirm a ticket", description = "ROLE_SUPPORT only. Provides serial number and ETA. Triggers FCM push notification to the patient's device.")
    @PutMapping("/{ticketId}/confirm")
    public ResponseEntity<ApiResponse<ConciergeTicketResponse>> confirmTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody ConfirmTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket confirmed",
                ticketService.confirmTicket(ticketId, request, principal.getId())));
    }

    @PutMapping("/{ticketId}/cancel")
    public ResponseEntity<ApiResponse<ConciergeTicketResponse>> cancelTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody CancelTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket cancelled",
                ticketService.cancelTicket(ticketId, request, principal.getId())));
    }
}
