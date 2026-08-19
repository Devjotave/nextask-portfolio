package com.nextask.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextask.auth.api.AuthResponse;
import com.nextask.auth.api.LoginRequest;
import com.nextask.user.api.CreateUserRequest;
import com.nextask.user.api.UserResponse;
import com.nextask.user.error.EmailAlreadyInUseException;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.of(
            2026, 8, 19, 12, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void registersUserAndReturnsBearerToken() throws Exception {
        when(authService.register(any(CreateUserRequest.class))).thenReturn(authResponse());

        CreateUserRequest request = new CreateUserRequest(
                "Ada",
                "ada@example.com",
                "a-secure-password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").value("signed.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("ada@example.com"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void validatesRegistrationRequest() throws Exception {
        CreateUserRequest request = new CreateUserRequest(" ", "invalid", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.name").value("name is required"))
                .andExpect(jsonPath("$.errors.email").value("email must be valid"));
    }

    @Test
    void rejectsDuplicateRegistrationEmail() throws Exception {
        when(authService.register(any(CreateUserRequest.class)))
                .thenThrow(new EmailAlreadyInUseException());
        CreateUserRequest request = new CreateUserRequest(
                "Ada",
                "ada@example.com",
                "a-secure-password");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:email-already-in-use"));
    }

    @Test
    void rejectsInvalidLoginWithoutLeakingDetails() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        LoginRequest request = new LoginRequest("ada@example.com", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type")
                        .value("urn:nextask:problem:authentication-failed"))
                .andExpect(jsonPath("$.detail")
                        .value("Email or password is incorrect"));
    }

    private static AuthResponse authResponse() {
        UserResponse user = new UserResponse(
                7L,
                "Ada",
                "ada@example.com",
                TIMESTAMP,
                TIMESTAMP);
        return new AuthResponse("signed.jwt.token", "Bearer", 3600L, user);
    }
}
