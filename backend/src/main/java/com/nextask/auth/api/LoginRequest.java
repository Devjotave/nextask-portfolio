package com.nextask.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 254, message = "email must have at most 254 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(max = 128, message = "password must have at most 128 characters")
        String password) {
}
