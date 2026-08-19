package com.nextask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    void createsAndReadsUserWithoutExposingOrPersistingRawPassword() throws Exception {
        String rawPassword = "a-secure-password";
        String email = "PHASE3-" + UUID.randomUUID() + "@Example.COM";
        CreateUserRequest request = new CreateUserRequest(
                "Phase 3 User",
                email,
                rawPassword);

        MvcResult creation = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.email").value(email.toLowerCase(Locale.ROOT)))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        JsonNode response = objectMapper.readTree(creation.getResponse().getContentAsByteArray());
        long userId = response.get("id").asLong();
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM dbo.users WHERE id = ?",
                String.class,
                userId);

        assertThat(storedHash).isNotEqualTo(rawPassword);
        assertThat(storedHash).startsWith("{pbkdf2@SpringSecurity_v5_8}");
        assertThat(passwordEncoder.matches(rawPassword, storedHash)).isTrue();

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Phase 3 User"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void returnsConflictWhenEmailAlreadyExists() throws Exception {
        String email = "phase3-duplicate-" + UUID.randomUUID() + "@example.com";
        userRepository.saveAndFlush(new User("Existing User", email, "existing-hash"));

        CreateUserRequest request = new CreateUserRequest(
                "Duplicate User",
                email.toUpperCase(Locale.ROOT),
                "a-secure-password");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:email-already-in-use"));
    }

    @Test
    void updatesListsAndDeletesUser() throws Exception {
        String originalEmail = "phase3-flow-" + UUID.randomUUID() + "@example.com";
        User user = userRepository.saveAndFlush(
                new User("Original Name", originalEmail, "existing-hash"));
        String updatedEmail = "updated-" + originalEmail;
        UpdateUserRequest update = new UpdateUserRequest("Updated Name", updatedEmail);

        mockMvc.perform(put("/api/users/{id}", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value(updatedEmail));

        mockMvc.perform(get("/api/users").param("page", "0").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + user.getId() + ")]").exists())
                .andExpect(jsonPath("$.size").value(100));

        mockMvc.perform(delete("/api/users/{id}", user.getId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get("/api/users/{id}", user.getId()))
                .andExpect(status().isNotFound());
    }
}
