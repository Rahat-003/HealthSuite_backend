package com.healthsuite.concierge.repository;

import com.healthsuite.concierge.entity.ConciergeTicket;
import com.healthsuite.concierge.enums.TicketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConciergeTicketRepository extends JpaRepository<ConciergeTicket, Long> {

    Page<ConciergeTicket> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);

    Page<ConciergeTicket> findByStatusOrderByCreatedAtAsc(TicketStatus status, Pageable pageable);

    Page<ConciergeTicket> findByAssignedSupportIdOrderByCreatedAtDesc(Long supportId, Pageable pageable);

    /**
     * Acquires a PESSIMISTIC_WRITE (SELECT FOR UPDATE) lock on the row.
     * Used by claim operation to prevent two agents claiming the same ticket.
     * Only returns the ticket if it is still PENDING — claim attempt on already-claimed ticket
     * returns empty Optional, which the service translates to a ConflictException.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM ConciergeTicket t WHERE t.id = :id AND t.status = 'PENDING'")
    Optional<ConciergeTicket> findPendingForClaim(@Param("id") Long id);
}
