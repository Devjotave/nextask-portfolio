package com.nextask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextask.entity.User;
import com.nextask.user.UserRepository;
import com.nextask.user.api.CreateUserRequest;
import com.nextask.user.api.UpdateUserRequest;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registersAndReadsAuthenticatedUserWithoutExposingRawPassword() throws Exception {
        String rawPassword = "a-secure-password";
        String email = "PHASE6-" + UUID.randomUUID() + "@Example.COM";
        CreateUserRequest request = new CreateUserRequest("Phase 6 User", email, rawPassword);

        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andExpect(jsonPath("$.user.email").value(email.toLowerCase(Locale.ROOT)))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                registration.getResponse().getContentAsByteArray());
        long userId = response.path("user").path("id").asLong();
        String token = response.path("accessToken").asText();
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM dbo.users WHERE id = ?",
                String.class,
                userId);

        assertThat(storedHash).isNotEqualTo(rawPassword);
        assertThat(storedHash).startsWith("{pbkdf2@SpringSecurity_v5_8}");
        assertThat(passwordEncoder.matches(rawPassword, storedHash)).isTrue();

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Phase 6 User"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void logsInWithNormalizedEmailAndRejectsWrongPassword() throws Exception {
        String email = "phase6-login-" + UUID.randomUUID() + "@example.com";
        String password = "a-secure-password";
        register("Login User", email, password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new com.nextask.auth.api.LoginRequest(
                                        email.toUpperCase(Locale.ROOT),
                                        password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new com.nextask.auth.api.LoginRequest(
                                        email,
                                        "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:authentication-failed"));
    }

    @Test
    void returnsConflictWhenRegistrationEmailAlreadyExists() throws Exception {
        String email = "phase6-duplicate-" + UUID.randomUUID() + "@example.com";
        userRepository.saveAndFlush(new User("Existing User", email, "existing-hash"));

        CreateUserRequest request = new CreateUserRequest(
                "Duplicate User",
                email.toUpperCase(Locale.ROOT),
                "a-secure-password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:email-already-in-use"));
    }

    @Test
    void updatesAndDeletesOnlyAuthenticatedAccount() throws Exception {
        String email = "phase6-flow-" + UUID.randomUUID() + "@example.com";
        JsonNode registration = register("Original Name", email, "a-secure-password");
        String token = registration.path("accessToken").asText();
        String updatedEmail = "updated-" + email;
        UpdateUserRequest update = new UpdateUserRequest("Updated Name", updatedEmail);

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value(updatedEmail));

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void protectsPrivateEndpointWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:authentication-required"));
    }

    private JsonNode register(String name, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(
                                new CreateUserRequest(name, email, password))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
