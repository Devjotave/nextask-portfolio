package com.nextask.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextask.auth.AuthenticatedUser;
import com.nextask.user.api.UpdateUserRequest;
import com.nextask.user.api.UserResponse;
import com.nextask.user.error.UserNotFoundException;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    private static final OffsetDateTime TIMESTAMP = OffsetDateTime.of(
            2026, 8, 19, 12, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticatedUser authenticatedUser;

    @BeforeEach
    void setUp() {
        when(authenticatedUser.id()).thenReturn(7L);
    }

    @Test
    void returnsAuthenticatedUserWithoutPasswordData() throws Exception {
        when(userService.findById(7L)).thenReturn(userResponse());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void updatesOnlyAuthenticatedUser() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("Ada", "ada@example.com");
        when(userService.update(org.mockito.ArgumentMatchers.eq(7L), any(UpdateUserRequest.class)))
                .thenReturn(userResponse());

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(userService).update(
                org.mockito.ArgumentMatchers.eq(7L),
                any(UpdateUserRequest.class));
    }

    @Test
    void deletesOnlyAuthenticatedUser() throws Exception {
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isNoContent());

        verify(userService).delete(7L);
    }

    @Test
    void returnsNotFoundWhenTokenUserNoLongerExists() throws Exception {
        when(userService.findById(7L)).thenThrow(new UserNotFoundException(7L));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:nextask:problem:user-not-found"));
    }

    private static UserResponse userResponse() {
        return new UserResponse(7L, "Ada", "ada@example.com", TIMESTAMP, TIMESTAMP);
    }
}
