package com.nextask.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextask.user.api.CreateUserRequest;
import com.nextask.user.api.UserResponse;
import com.nextask.user.error.EmailAlreadyInUseException;
import com.nextask.user.error.UserNotFoundException;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void createsUserWithoutExposingPasswordData() throws Exception {
        OffsetDateTime timestamp = OffsetDateTime.of(
                2026, 8, 18, 12, 0, 0, 0, ZoneOffset.UTC);
        UserResponse response = new UserResponse(
                7L,
                "Ada Lovelace",
                "ada@example.com",
                timestamp,
                timestamp);
        when(userService.create(any(CreateUserRequest.class))).thenReturn(response);

        CreateUserRequest request = new CreateUserRequest(
                "Ada Lovelace",
                "ada@example.com",
                "a-secure-password");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/7"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void returnsProblemDetailForInvalidRequest() throws Exception {
        CreateUserRequest request = new CreateUserRequest(" ", "invalid", "short");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:nextask:problem:validation-failed"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("name is required"))
                .andExpect(jsonPath("$.errors.email").value("email must be valid"))
                .andExpect(jsonPath("$.errors.password")
                        .value("password must have between 8 and 128 characters"));
    }

    @Test
    void returnsConflictForDuplicateEmail() throws Exception {
        when(userService.create(any(CreateUserRequest.class)))
                .thenThrow(new EmailAlreadyInUseException());

        CreateUserRequest request = new CreateUserRequest(
                "Ada Lovelace",
                "ada@example.com",
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
    void returnsNotFoundForUnknownUser() throws Exception {
        when(userService.findById(404L)).thenThrow(new UserNotFoundException(404L));

        mockMvc.perform(get("/api/users/404"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:nextask:problem:user-not-found"))
                .andExpect(jsonPath("$.detail").value("User 404 was not found"));
    }
}
