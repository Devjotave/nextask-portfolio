package com.nextask.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nextask.auth.api.AuthResponse;
import com.nextask.auth.api.LoginRequest;
import com.nextask.user.api.CreateUserRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Account registration and JWT authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a user and issue an access token")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    AuthResponse register(@Valid @RequestBody CreateUserRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Incorrect email or password")
    })
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
