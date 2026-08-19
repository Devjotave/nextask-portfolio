package com.nextask.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.nextask.entity.User;
import com.nextask.user.api.CreateUserRequest;
import com.nextask.user.api.UserResponse;
import com.nextask.user.error.EmailAlreadyInUseException;
import com.nextask.user.error.UserHasWorkspaceMembershipsException;
import com.nextask.user.error.UserNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void createsUserWithNormalizedEmailAndEncodedPassword() {
        CreateUserRequest request = new CreateUserRequest(
                "Ada Lovelace",
                "ADA@Example.COM",
                "a-secure-password");

        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("a-secure-password")).thenReturn("{pbkdf2}encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            OffsetDateTime timestamp = OffsetDateTime.of(
                    2026, 8, 18, 12, 0, 0, 0, ZoneOffset.UTC);
            ReflectionTestUtils.setField(user, "id", 42L);
            ReflectionTestUtils.setField(user, "createdAt", timestamp);
            ReflectionTestUtils.setField(user, "updatedAt", timestamp);
            return user;
        });

        UserResponse response = userService.create(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());

        User storedUser = userCaptor.getValue();
        assertThat(storedUser.getEmail()).isEqualTo("ada@example.com");
        assertThat(storedUser.getPasswordHash()).isEqualTo("{pbkdf2}encoded-password");
        assertThat(storedUser.getPasswordHash()).doesNotContain("a-secure-password");
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.email()).isEqualTo("ada@example.com");
    }

    @Test
    void rejectsDuplicateEmailBeforeEncodingPassword() {
        CreateUserRequest request = new CreateUserRequest(
                "Ada Lovelace",
                "ada@example.com",
                "a-secure-password");
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(EmailAlreadyInUseException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void reportsMissingUser() {
        when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User 99 was not found");
    }

    @Test
    void rejectsDeletingUserThatStillBelongsToWorkspace() {
        User user = new User("Ada Lovelace", "ada@example.com", "encoded-password");
        when(userRepository.findById(42L)).thenReturn(java.util.Optional.of(user));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("foreign key"))
                .when(userRepository).flush();

        assertThatThrownBy(() -> userService.delete(42L))
                .isInstanceOf(UserHasWorkspaceMembershipsException.class)
                .hasMessage("User 42 still belongs to one or more workspaces");
    }
}
