package com.nextask.user;

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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthenticatedUser authenticatedUser;

    public UserController(UserService userService, AuthenticatedUser authenticatedUser) {
        this.userService = userService;
        this.authenticatedUser = authenticatedUser;
    }

    @GetMapping("/me")
    UserResponse findMe() {
        return userService.findById(authenticatedUser.id());
    }

    @PutMapping("/me")
    UserResponse updateMe(@Valid @RequestBody UpdateUserRequest request) {
        return userService.update(authenticatedUser.id(), request);
    }

    @DeleteMapping("/me")
    ResponseEntity<Void> deleteMe() {
        userService.delete(authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }
}
