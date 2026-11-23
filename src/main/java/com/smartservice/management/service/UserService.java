
package com.smartservice.management.service;

import com.smartservice.management.model.User;
import com.smartservice.management.model.UserUpdateRequest;
import com.smartservice.management.repository.UserRepository;
import com.smartservice.management.exception.ResourceNotFoundException;
import com.smartservice.management.dto.LoginRequest;
import com.smartservice.management.dto.LoginResponse;
import com.smartservice.management.security.JwtService;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.jwtService = jwtService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(int userId) {

        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        User user = userRepository.findById(userId);

        if (user == null) {
            throw new ResourceNotFoundException(
                    "User not found with ID: " + userId
            );
        }

        return user;
    }

    public int registerUser(User user) {

        if (user.getName() == null || user.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        if (user.getPassword() == null || user.getPassword().length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters"
            );
        }

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("USER");
        }

        if (!List.of("USER", "STAFF", "ADMIN").contains(user.getRole())) {
            throw new IllegalArgumentException("Invalid user role");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        return userRepository.save(user);
    }

    // Authenticate user and generate JWT
    public LoginResponse loginUser(LoginRequest request) {

        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {

            throw new IllegalArgumentException(
                    "Email and password are required"
            );
        }

        User user = userRepository.findByEmail(request.getEmail());

        if (user == null ||
                !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your account is deactivated"
            );
        }

        String token = jwtService.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getRole()
        );

        return new LoginResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                "Login successful",
                token
        );
    }

    public void updateUser(int userId, UserUpdateRequest request) {

        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID");
        }

        User existingUser = userRepository.findById(userId);

        if (existingUser == null) {
            throw new ResourceNotFoundException(
                    "User not found with ID: " + userId
            );
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (!existingUser.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        existingUser.setName(request.getName());
        existingUser.setEmail(request.getEmail());

        userRepository.update(existingUser);
    }

    public void deactivateUser(int userId) {

        if (userId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid user ID"
            );
        }

        User user = userRepository.findById(userId);

        if (user == null) {
            throw new ResourceNotFoundException(
                    "User not found with ID: " + userId
            );
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "User is already deactivated"
            );
        }

        userRepository.deactivateUser(userId);
    }

}
