package com.healthsuite.concierge.service;

import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.ConflictException;
import com.healthsuite.concierge.entity.ConciergeTicket;
import com.healthsuite.concierge.enums.TicketStatus;
import com.healthsuite.concierge.repository.ConciergeTicketRepository;
import com.healthsuite.support.IntegrationTest;
import com.healthsuite.support.TestData;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two support agents click "claim" on the same ticket at the same moment.
 * {@code findPendingForClaim} takes a PESSIMISTIC_WRITE row lock (SELECT … FOR UPDATE),
 * so PostgreSQL serialises them: the second transaction waits, re-checks
 * {@code status = 'PENDING'} after the first commits, finds nothing and gets a 409.
 */
@IntegrationTest
class ConciergeClaimConcurrencyIT {

    @Autowired private ConciergeTicketService ticketService;
    @Autowired private ConciergeTicketRepository ticketRepository;
    @Autowired private UserRepository userRepository;

    private User newUser(String prefix) {
        return userRepository.save(User.builder()
                .email(TestData.email(prefix))
                .phoneNumber(TestData.phone())
                .fullName(prefix)
                .password("{noop}x")
                .build());
    }

    @RepeatedTest(5)
    void exactlyOneAgentWinsAConcurrentClaim() throws Exception {
        User patient = newUser("patient");
        User agentA = newUser("agent-a");
        User agentB = newUser("agent-b");
        ConciergeTicket ticket = ticketRepository.save(ConciergeTicket.builder()
                .owner(patient).createdByUser(patient)
                .doctorName("Dr. Karim").chamberAddress("Dhanmondi")
                .status(TicketStatus.PENDING)
                .build());

        CountDownLatch startGun = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Long>> claims = List.of(agentA.getId(), agentB.getId()).stream()
                    .<Callable<Long>>map(agentId -> () -> {
                        startGun.await();
                        ticketService.claimTicket(ticket.getId(), agentId);
                        return agentId;
                    })
                    .toList();
            List<Future<Long>> futures = claims.stream().map(pool::submit).toList();
            startGun.countDown();

            int winners = 0;
            int conflicts = 0;
            for (Future<Long> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                    winners++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(ConflictException.class);
                    conflicts++;
                }
            }

            assertThat(winners).isEqualTo(1);
            assertThat(conflicts).isEqualTo(1);

            ConciergeTicket reloaded = ticketRepository.findById(ticket.getId()).orElseThrow();
            assertThat(reloaded.getStatus()).isEqualTo(TicketStatus.PROCESSING);
            assertThat(reloaded.getVersion()).isEqualTo(1L);
        } finally {
            pool.shutdownNow();
        }
    }
}
