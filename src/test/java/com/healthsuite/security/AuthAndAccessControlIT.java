package com.healthsuite.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.enums.RoleName;
import com.healthsuite.auth.repository.RoleRepository;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.support.IntegrationTest;
import com.healthsuite.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end through the real security filter chain: JWT issuing, validation,
 * refresh-token rotation and role-based URL rules from SecurityConfig.
 */
@IntegrationTest
@AutoConfigureMockMvc
class AuthAndAccessControlIT {

    private static final String PASSWORD = "secret123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    private JsonNode register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD,
                                "phoneNumber", TestData.phone(),
                                "fullName", "Test User"))))
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private JsonNode login(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private static String bearer(JsonNode auth) {
        return "Bearer " + auth.path("accessToken").asText();
    }

    @Test
    void publicCatalogNeedsNoToken() throws Exception {
        mvc.perform(get("/api/marketplace/specialties")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithoutTokenReturns401NotForbidden() throws Exception {
        // 401 (not 403) lets the frontend's refresh interceptor kick in
        mvc.perform(get("/api/phr/visits")).andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        JsonNode auth = register(TestData.email("tamper"));
        String token = auth.path("accessToken").asText();
        String tampered = token.substring(0, token.length() - 4) + "abcd";

        mvc.perform(get("/api/phr/visits").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registeredUserCanUseTheirToken() throws Exception {
        JsonNode auth = register(TestData.email("patient"));

        assertThat(auth.path("user").path("roles").toString()).contains("ROLE_USER");
        mvc.perform(get("/api/phr/visits").header("Authorization", bearer(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        String email = TestData.email("badpass");
        register(email);

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        String email = TestData.email("dup");
        register(email);

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", email, "password", PASSWORD,
                                "phoneNumber", TestData.phone(), "fullName", "Dup"))))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "email", "not-an-email", "password", "short",
                                "phoneNumber", "12345", "fullName", "X"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists());
    }

    @Test
    void refreshTokenRotatesAndOldTokenCannotBeReused() throws Exception {
        JsonNode auth = register(TestData.email("rotate"));
        String firstRefresh = auth.path("refreshToken").asText();

        MvcResult rotated = mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", firstRefresh))))
                .andExpect(status().isOk())
                .andReturn();
        String secondRefresh = json.readTree(rotated.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();
        assertThat(secondRefresh).isNotBlank().isNotEqualTo(firstRefresh);

        // replaying the rotated-out token must fail
        mvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("refreshToken", firstRefresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void regularUserIsForbiddenFromAdminAndDoctorAreas() throws Exception {
        JsonNode auth = register(TestData.email("plain"));

        mvc.perform(get("/api/admin/users").header("Authorization", bearer(auth)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/doctor/consultations/queue").header("Authorization", bearer(auth)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRoleUnlocksAdminEndpointsAfterReLogin() throws Exception {
        String email = TestData.email("admin");
        register(email);
        User user = userRepository.findByEmail(email).orElseThrow();
        user.getRoles().add(roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow());
        userRepository.save(user);

        // roles live in the JWT, so a fresh login is needed to pick up the new role
        JsonNode adminAuth = login(email);
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(adminAuth)))
                .andExpect(status().isOk());
    }
}
