package com.healthsuite.family.controller;

import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.common.response.ApiResponse;
import com.healthsuite.family.dto.request.CreateShareTokenRequest;
import com.healthsuite.family.dto.response.ShareTokenResponse;
import com.healthsuite.family.service.ShareTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/share-tokens")
@RequiredArgsConstructor
@Tag(name = "Share Tokens", description = "Create time-limited cryptographic tokens to share a single PHR record without granting full account access")
public class ShareTokenController {

    private final ShareTokenService shareTokenService;

    @Operation(summary = "Create a share token",
            description = "The raw token is returned **once** in the response and is never stored in plaintext. SHA-256 hash is stored in the database.")
    @PostMapping
    public ResponseEntity<ApiResponse<ShareTokenResponse>> createToken(
            @Valid @RequestBody CreateShareTokenRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Share token created — save the token, it will not be shown again",
                        shareTokenService.createToken(request, principal.getId())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShareTokenResponse>>> getMyActiveTokens(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(shareTokenService.getMyActiveTokens(principal.getId())));
    }

    @DeleteMapping("/{tokenId}")
    public ResponseEntity<ApiResponse<Void>> revokeToken(
            @PathVariable Long tokenId,
            @AuthenticationPrincipal UserPrincipal principal) {
        shareTokenService.revokeToken(tokenId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Share token revoked"));
    }
}
