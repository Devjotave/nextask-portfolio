package com.nextask.auth.api;

import com.nextask.user.api.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user) {
}
