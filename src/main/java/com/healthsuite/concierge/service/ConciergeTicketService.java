package com.healthsuite.concierge.service;

import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.ConflictException;
import com.healthsuite.common.exception.PremiumRequiredException;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.common.response.PagedResponse;
import com.healthsuite.concierge.dto.request.CancelTicketRequest;
import com.healthsuite.concierge.dto.request.ConfirmTicketRequest;
import com.healthsuite.concierge.dto.request.CreateTicketRequest;
import com.healthsuite.concierge.dto.response.ConciergeTicketResponse;
import com.healthsuite.concierge.entity.ConciergeTicket;
import com.healthsuite.concierge.enums.TicketStatus;
import com.healthsuite.concierge.repository.ConciergeTicketRepository;
import com.healthsuite.family.service.FamilyMeshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConciergeTicketService {

    private final ConciergeTicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final FamilyMeshService familyMeshService;
    private final FcmNotificationService fcmNotificationService;

    /**
     * Creates a concierge ticket.
     * Premium guard: requester must have isPremium=true.
     * Proxy allowed: requester can book for a verified family member.
     */
    @Transactional
    public ConciergeTicketResponse createTicket(CreateTicketRequest request, Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", requesterId));

        if (!requester.isPremium()) {
            throw new PremiumRequiredException();
        }

        User patient;
        if (request.targetPatientId() != null && !request.targetPatientId().equals(requesterId)) {
            if (!familyMeshService.canAccess(requesterId, request.targetPatientId())) {
                throw new AccessDeniedException("You can only book on behalf of verified family members");
            }
            patient = userRepository.findById(request.targetPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.targetPatientId()));
        } else {
            patient = requester;
        }

        ConciergeTicket ticket = ConciergeTicket.builder()
                .owner(patient)
                .createdByUser(requester)
                .doctorName(request.doctorName())
                .doctorSpecialty(request.doctorSpecialty())
                .chamberAddress(request.chamberAddress())
                .description(request.description())
                .status(TicketStatus.PENDING)
                .build();

        return ConciergeTicketResponse.from(ticketRepository.save(ticket));
    }

    /**
     * Support agent claims a PENDING ticket.
     * Uses PESSIMISTIC_WRITE lock to guarantee at-most-one agent claims a ticket.
     */
    @Transactional
    public ConciergeTicketResponse claimTicket(Long ticketId, Long agentId) {
        ConciergeTicket ticket = ticketRepository.findPendingForClaim(ticketId)
                .orElseThrow(() -> new ConflictException(
                        "Ticket is not available for claim — it may already be claimed or does not exist"));

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", agentId));

        ticket.setStatus(TicketStatus.PROCESSING);
        ticket.setAssignedSupport(agent);

        return ConciergeTicketResponse.from(ticketRepository.save(ticket));
    }

    /**
     * Confirms a ticket: sets serial number + ETA, sends FCM push notification.
     * Only the assigned support agent can confirm.
     */
    @Transactional
    public ConciergeTicketResponse confirmTicket(Long ticketId, ConfirmTicketRequest request, Long agentId) {
        ConciergeTicket ticket = getTicketForAgent(ticketId, agentId, TicketStatus.PROCESSING);

        ticket.setStatus(TicketStatus.CONFIRMED);
        ticket.setSerialNumber(request.serialNumber());
        ticket.setEstimatedArrivalTime(request.estimatedArrivalTime());

        ticket = ticketRepository.save(ticket);

        // Send push notification asynchronously — failure must not roll back the transaction
        try {
            fcmNotificationService.sendTicketConfirmedNotification(ticket);
        } catch (Exception e) {
            log.error("Push notification failed for ticket {}: {}", ticketId, e.getMessage());
        }

        return ConciergeTicketResponse.from(ticket);
    }

    /**
     * Cancels a ticket. Notes are mandatory per business spec.
     */
    @Transactional
    public ConciergeTicketResponse cancelTicket(Long ticketId, CancelTicketRequest request, Long agentId) {
        ConciergeTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("ConciergeTicket", ticketId));

        // Only the assigned agent can cancel a PROCESSING ticket; any agent can cancel PENDING
        if (ticket.getStatus() == TicketStatus.PROCESSING
                && !ticket.getAssignedSupport().getId().equals(agentId)) {
            throw new AccessDeniedException("Only the assigned agent can cancel a ticket in PROCESSING state");
        }
        if (ticket.getStatus() == TicketStatus.CONFIRMED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ConflictException("Cannot cancel a ticket in " + ticket.getStatus() + " state");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancellationNotes(request.cancellationNotes());
        return ConciergeTicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ConciergeTicketResponse> getMyTickets(Long userId, Pageable pageable) {
        return PagedResponse.from(
                ticketRepository.findByOwnerIdOrderByCreatedAtDesc(userId, pageable)
                        .map(ConciergeTicketResponse::from));
    }

    @Transactional(readOnly = true)
    public PagedResponse<ConciergeTicketResponse> getPendingQueue(Pageable pageable) {
        return PagedResponse.from(
                ticketRepository.findByStatusOrderByCreatedAtAsc(TicketStatus.PENDING, pageable)
                        .map(ConciergeTicketResponse::from));
    }

    private ConciergeTicket getTicketForAgent(Long ticketId, Long agentId, TicketStatus requiredStatus) {
        ConciergeTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("ConciergeTicket", ticketId));

        if (ticket.getStatus() != requiredStatus) {
            throw new ConflictException("Ticket must be in " + requiredStatus + " state for this operation");
        }
        if (ticket.getAssignedSupport() == null || !ticket.getAssignedSupport().getId().equals(agentId)) {
            throw new AccessDeniedException("Only the assigned agent can perform this operation");
        }
        return ticket;
    }
}
