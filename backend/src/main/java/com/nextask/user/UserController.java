package com.nextask.user;

import static com.nextask.openapi.OpenApiConfiguration.BEARER_AUTH;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nextask.auth.AuthenticatedUser;
import com.nextask.user.api.UpdateUserRequest;
import com.nextask.user.api.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Authenticated user profile")
@SecurityRequirement(name = BEARER_AUTH)
public class UserController {

    private final UserService userService;
    private final AuthenticatedUser authenticatedUser;

    public UserController(UserService userService, AuthenticatedUser authenticatedUser) {
        this.userService = userService;
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user profile")
    UserResponse findMe() {
        return userService.findById(authenticatedUser.id());
    }

    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user profile")
    UserResponse updateMe(@Valid @RequestBody UpdateUserRequest request) {
        return userService.update(authenticatedUser.id(), request);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete the authenticated user account")
    @ApiResponse(responseCode = "204", description = "Account deleted")
    ResponseEntity<Void> deleteMe() {
        userService.delete(authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }
}
