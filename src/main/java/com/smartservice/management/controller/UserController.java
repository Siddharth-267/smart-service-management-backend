
package com.smartservice.management.controller;
import com.smartservice.management.model.UserUpdateRequest;
import com.smartservice.management.model.UserRegistrationRequest;
import com.smartservice.management.model.User;
import com.smartservice.management.model.UserResponse;
import com.smartservice.management.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Get all users
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers()
                .stream()
                .map(user -> new UserResponse(
                        user.getUserId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole(),
                        user.getCreatedAt(),
                        user.isActive()
                ))
                .toList();

        return ResponseEntity.ok(users);
    }

    // Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable int id) {
        User user = userService.getUserById(id);

        UserResponse response = new UserResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.isActive()
        );

        return ResponseEntity.ok(response);
    }
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(
            @PathVariable int id,
            @RequestBody UserUpdateRequest request) {

        userService.updateUser(id, request);

        return ResponseEntity.ok("User details updated successfully!");
    }

    // Register a new user
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(
            @RequestBody UserRegistrationRequest request) {

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole("USER");

        userService.registerUser(user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("User registered successfully!");
    }
    // Deactivate user
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateUser(
            @PathVariable int id) {

        userService.deactivateUser(id);

        return ResponseEntity.ok(
                "User deactivated successfully!"
        );
    }
}
