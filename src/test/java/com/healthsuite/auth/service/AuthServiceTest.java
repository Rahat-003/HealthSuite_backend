package com.healthsuite.auth.service;

import com.healthsuite.auth.dto.request.RegisterRequest;
import com.healthsuite.auth.dto.response.AuthResponse;
import com.healthsuite.auth.entity.RefreshToken;
import com.healthsuite.auth.entity.Role;
import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.enums.RoleName;
import com.healthsuite.auth.repository.RoleRepository;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.auth.security.JwtService;
import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.common.exception.ConflictException;
import com.healthsuite.marketplace.repository.DoctorProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private OAuth2UserService oAuth2UserService;
    @Mock private DoctorProfileRepository doctorProfileRepository;

    @InjectMocks private AuthService authService;

    private final RegisterRequest request =
            new RegisterRequest("rahim@example.com", "secret123", "01712345678", "Rahim Uddin");

    @Test
    void registerHashesPasswordAssignsUserRoleAndIssuesTokens() {
        Role userRole = Role.builder().id(1L).name(RoleName.ROLE_USER).build();
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("secret123")).thenReturn("{bcrypt}hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("access-jwt");
        when(refreshTokenService.createToken(any(User.class)))
                .thenReturn(RefreshToken.builder().token("refresh-uuid").build());

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPassword()).isEqualTo("{bcrypt}hash").isNotEqualTo("secret123");
        assertThat(saved.getValue().getRoles()).containsExactly(userRole);

        assertThat(response.accessToken()).isEqualTo("access-jwt");
        assertThat(response.refreshToken()).isEqualTo("refresh-uuid");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().id()).isEqualTo(42L);
        assertThat(response.user().roles()).containsExactly("ROLE_USER");
    }

    @Test
    void duplicateEmailIsRejectedBeforeAnythingIsSaved() {
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email");
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).createToken(any());
    }

    @Test
    void duplicatePhoneIsRejected() {
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Phone");
        verify(userRepository, never()).save(any());
    }

    @Test
    void refreshRotatesTokenAndIssuesNewAccessToken() {
        User user = User.builder().id(7L).email("x@y.com").fullName("X").phoneNumber("01712345678").build();
        when(refreshTokenService.rotate("old-refresh"))
                .thenReturn(RefreshToken.builder().token("new-refresh").user(user).build());
        when(jwtService.generateAccessToken(any(UserPrincipal.class))).thenReturn("new-access");

        AuthResponse response = authService.refresh("old-refresh");

        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.user().id()).isEqualTo(7L);
    }
}
