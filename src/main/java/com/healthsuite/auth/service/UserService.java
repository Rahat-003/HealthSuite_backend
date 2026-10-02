package com.healthsuite.auth.service;

import com.healthsuite.auth.entity.Role;
import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.enums.AuthProvider;
import com.healthsuite.auth.enums.RoleName;
import com.healthsuite.auth.repository.RoleRepository;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.BadRequestException;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.marketplace.repository.DoctorProfileRepository;
import com.healthsuite.phr.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FileStorageService fileStorageService;
    private final DoctorProfileRepository doctorProfileRepository;

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

    // ── Admin: user management ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<User> listUsers() {
        return userRepository.findAllWithRoles();
    }

    @Transactional
    public User setUserEnabled(Long userId, boolean enabled) {
        User user = findById(userId);
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        if (isAdmin && !enabled) {
            throw new BadRequestException("Admin accounts cannot be disabled.");
        }
        user.setActive(enabled);
        return userRepository.save(user);
    }

    @Transactional
    public User updateProfilePhoto(Long userId, MultipartFile file) {
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new BadRequestException("Profile photo must be an image file.");
        }
        User user = findById(userId);
        String oldUrl = user.getProfilePhotoUrl();
        String url = fileStorageService.uploadFile(file, "user_photos");
        user.setProfilePhotoUrl(url);
        userRepository.save(user);

        // doctors: keep the marketplace profile photo in sync with the account photo
        doctorProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setProfilePhotoUrl(url);
            doctorProfileRepository.save(profile);
        });

        if (oldUrl != null && oldUrl.startsWith("/files/")) {
            fileStorageService.deleteFile(oldUrl);
        }
        return user;
    }
}
