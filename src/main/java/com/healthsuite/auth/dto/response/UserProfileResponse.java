package com.healthsuite.auth.dto.response;

import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.enums.AuthProvider;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record UserProfileResponse(
        Long id,
        String email,
        String phoneNumber,
        String fullName,
        boolean isPremium,
        AuthProvider authProvider,
        Set<String> roles,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getFullName(),
                user.isPremium(),
                user.getAuthProvider(),
                user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()),
                user.getCreatedAt()
        );
    }
}
