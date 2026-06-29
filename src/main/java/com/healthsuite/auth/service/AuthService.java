package com.healthsuite.auth.service;

import com.healthsuite.auth.dto.request.LoginRequest;
import com.healthsuite.auth.dto.request.RegisterRequest;
import com.healthsuite.auth.dto.request.SocialAuthRequest;
import com.healthsuite.auth.dto.response.AuthResponse;
import com.healthsuite.auth.dto.response.UserProfileResponse;
import com.healthsuite.auth.entity.RefreshToken;
import com.healthsuite.auth.entity.Role;
import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.enums.RoleName;
import com.healthsuite.auth.repository.RoleRepository;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.auth.security.JwtService;
import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.common.exception.ConflictException;
import com.healthsuite.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final OAuth2UserService oAuth2UserService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email address is already registered");
        }
        if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ConflictException("Phone number is already registered");
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not seeded"));

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(request.phoneNumber())
                .fullName(request.fullName())
                .roles(Set.of(userRole))
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse socialAuth(SocialAuthRequest request) {
        User user = oAuth2UserService.authenticateOrRegister(request);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken newToken = refreshTokenService.rotate(rawRefreshToken);
        User user = newToken.getUser();
        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtService.generateAccessToken(principal);
        return AuthResponse.of(accessToken, newToken.getToken(),
                accessTokenExpirationMs / 1000, UserProfileResponse.from(user));
    }

    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(refreshTokenService::revokeAll);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserPrincipal principal = UserPrincipal.create(user);
        String accessToken = jwtService.generateAccessToken(principal);
        RefreshToken refreshToken = refreshTokenService.createToken(user);
        return AuthResponse.of(accessToken, refreshToken.getToken(),
                accessTokenExpirationMs / 1000, UserProfileResponse.from(user));
    }
}
