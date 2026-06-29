package com.healthsuite.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthsuite.common.response.ErrorResponse;
import com.healthsuite.family.entity.ShareToken;
import com.healthsuite.family.repository.ShareTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShareTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String SHARE_TOKEN_HEADER = "X-Share-Token";

    private final ShareTokenRepository shareTokenRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rawToken = request.getHeader(SHARE_TOKEN_HEADER);

        if (!StringUtils.hasText(rawToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        String tokenHash = sha256Hex(rawToken);
        Optional<ShareToken> shareToken = shareTokenRepository.findByTokenHashAndIsRevokedFalse(tokenHash);

        if (shareToken.isEmpty()) {
            writeUnauthorized(response, request.getRequestURI(), "Invalid share token");
            return;
        }

        ShareToken token = shareToken.get();
        if (Instant.now().isAfter(token.getExpiresAt())) {
            writeUnauthorized(response, request.getRequestURI(), "Share token has expired");
            return;
        }

        ShareTokenAuthentication auth = new ShareTokenAuthentication(
                token.getOwnerUserId(), token.getRecordId(), token.getRecordType());
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String path, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse error = ErrorResponse.of(path, 401, "Unauthorized", message);
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
