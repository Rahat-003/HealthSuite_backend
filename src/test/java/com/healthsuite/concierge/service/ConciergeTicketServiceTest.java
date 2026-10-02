package com.healthsuite.concierge.service;

import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.ConflictException;
import com.healthsuite.common.exception.PremiumRequiredException;
import com.healthsuite.concierge.dto.request.CancelTicketRequest;
import com.healthsuite.concierge.dto.request.ConfirmTicketRequest;
import com.healthsuite.concierge.dto.request.CreateTicketRequest;
import com.healthsuite.concierge.dto.response.ConciergeTicketResponse;
import com.healthsuite.concierge.entity.ConciergeTicket;
import com.healthsuite.concierge.enums.TicketStatus;
import com.healthsuite.concierge.repository.ConciergeTicketRepository;
import com.healthsuite.family.service.FamilyMeshService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConciergeTicketServiceTest {

    @Mock private ConciergeTicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private FamilyMeshService familyMeshService;
    @Mock private FcmNotificationService fcmNotificationService;

    @InjectMocks private ConciergeTicketService service;

    private final User premium = User.builder().id(1L).fullName("Premium").isPremium(true).build();
    private final User free = User.builder().id(2L).fullName("Free").isPremium(false).build();
    private final User relative = User.builder().id(3L).fullName("Relative").build();
    private final User agent = User.builder().id(10L).fullName("Agent").build();
    private final User otherAgent = User.builder().id(11L).fullName("Other agent").build();

    private ConciergeTicket ticket(TicketStatus status, User assigned) {
        return ConciergeTicket.builder()
                .id(5L).owner(premium).createdByUser(premium).assignedSupport(assigned)
                .doctorName("Dr. Karim").chamberAddress("Dhanmondi").status(status).build();
    }

    private CreateTicketRequest createRequest(Long targetPatientId) {
        return new CreateTicketRequest(targetPatientId, "Dr. Karim", "Cardiology", "Dhanmondi", null);
    }

    @Test
    void nonPremiumUserCannotBook() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(free));

        assertThatThrownBy(() -> service.createTicket(createRequest(null), 2L))
                .isInstanceOf(PremiumRequiredException.class);
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void premiumUserCanBookForVerifiedFamilyMember() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(premium));
        when(familyMeshService.canAccess(1L, 3L)).thenReturn(true);
        when(userRepository.findById(3L)).thenReturn(Optional.of(relative));
        when(ticketRepository.save(any(ConciergeTicket.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createTicket(createRequest(3L), 1L);

        ArgumentCaptor<ConciergeTicket> saved = ArgumentCaptor.forClass(ConciergeTicket.class);
        verify(ticketRepository).save(saved.capture());
        assertThat(saved.getValue().getOwner()).isSameAs(relative);
        assertThat(saved.getValue().getCreatedByUser()).isSameAs(premium);
        assertThat(saved.getValue().getStatus()).isEqualTo(TicketStatus.PENDING);
    }

    @Test
    void cannotBookForSomeoneOutsideTheFamily() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(premium));
        when(familyMeshService.canAccess(1L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> service.createTicket(createRequest(99L), 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void claimFailsWhenTicketIsNoLongerPending() {
        // findPendingForClaim takes a row lock and filters on status = PENDING
        when(ticketRepository.findPendingForClaim(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.claimTicket(5L, 10L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void confirmSetsSerialAndNotifiesPatient() {
        ConciergeTicket processing = ticket(TicketStatus.PROCESSING, agent);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(processing));
        when(ticketRepository.save(processing)).thenReturn(processing);
        LocalDateTime eta = LocalDateTime.now().plusHours(2);

        ConciergeTicketResponse response =
                service.confirmTicket(5L, new ConfirmTicketRequest("A-17", eta), agent.getId());

        assertThat(processing.getStatus()).isEqualTo(TicketStatus.CONFIRMED);
        assertThat(processing.getSerialNumber()).isEqualTo("A-17");
        assertThat(response).isNotNull();
        verify(fcmNotificationService).sendTicketConfirmedNotification(processing);
    }

    @Test
    void notificationFailureDoesNotBreakConfirmation() {
        ConciergeTicket processing = ticket(TicketStatus.PROCESSING, agent);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(processing));
        when(ticketRepository.save(processing)).thenReturn(processing);
        doThrow(new RuntimeException("FCM down")).when(fcmNotificationService).sendTicketConfirmedNotification(any());

        service.confirmTicket(5L, new ConfirmTicketRequest("A-17", LocalDateTime.now().plusHours(1)), agent.getId());

        assertThat(processing.getStatus()).isEqualTo(TicketStatus.CONFIRMED);
    }

    @Test
    void onlyTheAssignedAgentCanConfirm() {
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket(TicketStatus.PROCESSING, agent)));

        assertThatThrownBy(() -> service.confirmTicket(5L,
                new ConfirmTicketRequest("A-17", LocalDateTime.now().plusHours(1)), otherAgent.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verify(fcmNotificationService, never()).sendTicketConfirmedNotification(any());
    }

    @Test
    void confirmedTicketCannotBeCancelled() {
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket(TicketStatus.CONFIRMED, agent)));

        assertThatThrownBy(() -> service.cancelTicket(5L,
                new CancelTicketRequest("Patient changed their mind"), agent.getId()))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void anyAgentCanCancelAPendingTicket() {
        ConciergeTicket pending = ticket(TicketStatus.PENDING, null);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(pending));
        when(ticketRepository.save(pending)).thenReturn(pending);

        service.cancelTicket(5L, new CancelTicketRequest("Doctor is on leave this week"), otherAgent.getId());

        assertThat(pending.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(pending.getCancellationNotes()).isEqualTo("Doctor is on leave this week");
    }
}
