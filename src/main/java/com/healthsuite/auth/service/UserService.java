package com.healthsuite.auth.service;

import com.healthsuite.auth.entity.Role;
import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.enums.AuthProvider;
import com.healthsuite.auth.enums.RoleName;
import com.healthsuite.auth.repository.RoleRepository;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        User user = findById(userId);
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    @Transactional
    public User createSocialUser(String email, String fullName, String phoneNumber,
                                  AuthProvider provider, String providerUserId) {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not seeded in database"));

        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .phoneNumber(phoneNumber)
                .authProvider(provider)
                .roles(Set.of(userRole))
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public void updateProfile(Long userId, String fullName, String fcmToken) {
        User user = findById(userId);
        user.setFullName(fullName);
        if (fcmToken != null) {
            user.setFcmToken(fcmToken);
        }
        userRepository.save(user);
    }
}
