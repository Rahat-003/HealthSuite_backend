package com.healthsuite.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.healthsuite.auth.dto.request.SocialAuthRequest;
import com.healthsuite.auth.entity.SocialAccount;
import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.enums.AuthProvider;
import com.healthsuite.auth.enums.RoleName;
import com.healthsuite.auth.repository.SocialAccountRepository;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.AccountMergeException;
import com.healthsuite.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService {

    @Value("${app.social.google.client-id:}")
    private String googleClientId;

    @Value("${app.social.facebook.app-id:}")
    private String facebookAppId;

    @Value("${app.social.facebook.app-secret:}")
    private String facebookAppSecret;

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final UserService userService;
    private final RestTemplate restTemplate;

    /**
     * Verifies the social ID token and returns the existing or newly created User.
     * Enforces the unified account rule: same email across different providers → AccountMergeException.
     */
    @Transactional
    public User authenticateOrRegister(SocialAuthRequest request) {
        SocialUserInfo info = verifyToken(request.provider(), request.idToken());

        // 1. Look up existing social account
        Optional<SocialAccount> existingSocial = socialAccountRepository
                .findByProviderAndProviderUserId(request.provider(), info.providerUserId());
        if (existingSocial.isPresent()) {
            return existingSocial.get().getUser();
        }

        // 2. Check for existing local or different-provider account with same email
        Optional<User> existingUser = userRepository.findByEmail(info.email());
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            throw new AccountMergeException(info.email(), user.getAuthProvider().name());
        }

        // 3. New user — register with ROLE_USER
        User newUser = userService.createSocialUser(
                info.email(), request.fullName(), request.phoneNumber(),
                request.provider(), info.providerUserId());

        // Save social account link
        socialAccountRepository.save(SocialAccount.builder()
                .user(newUser)
                .provider(request.provider())
                .providerUserId(info.providerUserId())
                .providerEmail(info.email())
                .build());

        return newUser;
    }

    private SocialUserInfo verifyToken(AuthProvider provider, String idToken) {
        return switch (provider) {
            case GOOGLE -> verifyGoogleToken(idToken);
            case FACEBOOK -> verifyFacebookToken(idToken);
            case LOCAL -> throw new UnauthorizedException("LOCAL is not a valid social provider");
        };
    }

    private SocialUserInfo verifyGoogleToken(String idTokenStr) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenStr);
            if (idToken == null) {
                throw new UnauthorizedException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            return new SocialUserInfo(payload.getSubject(), payload.getEmail());
        } catch (Exception e) {
            log.warn("Google token verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Google token verification failed");
        }
    }

    @SuppressWarnings("unchecked")
    private SocialUserInfo verifyFacebookToken(String accessToken) {
        try {
            String debugUrl = String.format(
                    "https://graph.facebook.com/debug_token?input_token=%s&access_token=%s|%s",
                    accessToken, facebookAppId, facebookAppSecret);

            Map<String, Object> debugResponse = restTemplate.getForObject(debugUrl, Map.class);
            Map<String, Object> data = (Map<String, Object>) debugResponse.get("data");

            if (data == null || !(Boolean) data.get("is_valid")) {
                throw new UnauthorizedException("Invalid Facebook access token");
            }

            String userId = (String) data.get("user_id");

            // Fetch user email
            String userUrl = String.format(
                    "https://graph.facebook.com/me?fields=id,email&access_token=%s", accessToken);
            Map<String, Object> userInfo = restTemplate.getForObject(userUrl, Map.class);
            String email = (String) userInfo.get("email");

            if (email == null) {
                throw new UnauthorizedException("Facebook account does not provide a verified email address");
            }

            return new SocialUserInfo(userId, email);
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Facebook token verification failed: {}", e.getMessage());
            throw new UnauthorizedException("Facebook token verification failed");
        }
    }

    private record SocialUserInfo(String providerUserId, String email) {}
}
