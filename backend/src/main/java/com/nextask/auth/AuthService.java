package com.nextask.auth;

import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.nextask.auth.api.AuthResponse;
import com.nextask.auth.api.LoginRequest;
import com.nextask.entity.User;
import com.nextask.user.UserRepository;
import com.nextask.user.UserService;
import com.nextask.user.api.CreateUserRequest;
import com.nextask.user.api.UserResponse;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenService tokenService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            UserService userService,
            JwtTokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.userService = userService;
        this.tokenService = tokenService;
    }

    public AuthResponse register(CreateUserRequest request) {
        return tokenService.issue(userService.create(request));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().strip().toLowerCase(Locale.ROOT);
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        return tokenService.issue(toResponse(user));
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
