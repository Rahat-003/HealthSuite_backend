package com.healthsuite.auth.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserProfileResponse user
) {
    public static AuthResponse of(String accessToken, String refreshToken,
                                   long expiresInSeconds, UserProfileResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
