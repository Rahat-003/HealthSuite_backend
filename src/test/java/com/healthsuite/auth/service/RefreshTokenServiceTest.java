package com.healthsuite.auth.service;

import com.healthsuite.auth.entity.RefreshToken;
import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.repository.RefreshTokenRepository;
import com.healthsuite.common.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final long SEVEN_DAYS_MS = 7L * 24 * 60 * 60 * 1000;

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @InjectMocks private RefreshTokenService service;

    private final User user = User.builder().id(1L).email("a@b.com").build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshTokenExpirationMs", SEVEN_DAYS_MS);
    }

    private RefreshToken token(String raw, Instant expiresAt, boolean revoked) {
        return RefreshToken.builder().user(user).token(raw).expiresAt(expiresAt).isRevoked(revoked).build();
    }

    @Test
    void createTokenIsUniqueAndExpiresAfterConfiguredTtl() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken first = service.createToken(user);
        RefreshToken second = service.createToken(user);

        assertThat(first.getToken()).isNotEqualTo(second.getToken());
        assertThat(first.getExpiresAt())
                .isCloseTo(Instant.now().plusMillis(SEVEN_DAYS_MS), org.assertj.core.api.Assertions.within(5, ChronoUnit.SECONDS));
    }

    @Test
    void rotateRevokesOldTokenAndIssuesNewOne() {
        RefreshToken existing = token("old", Instant.now().plus(1, ChronoUnit.DAYS), false);
        when(refreshTokenRepository.findByToken("old")).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken rotated = service.rotate("old");

        assertThat(existing.isRevoked()).isTrue();
        assertThat(rotated.getToken()).isNotEqualTo("old");
        assertThat(rotated.getUser()).isSameAs(user);
        assertThat(rotated.isRevoked()).isFalse();
    }

    @Test
    void reusingARevokedTokenIsRejected() {
        when(refreshTokenRepository.findByToken("old"))
                .thenReturn(Optional.of(token("old", Instant.now().plus(1, ChronoUnit.DAYS), true)));

        assertThatThrownBy(() -> service.rotate("old"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("revoked");
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void expiredTokenIsRejected() {
        when(refreshTokenRepository.findByToken("old"))
                .thenReturn(Optional.of(token("old", Instant.now().minus(1, ChronoUnit.MINUTES), false)));

        assertThatThrownBy(() -> service.rotate("old"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void unknownTokenIsRejected() {
        when(refreshTokenRepository.findByToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("nope")).isInstanceOf(UnauthorizedException.class);
    }
}
