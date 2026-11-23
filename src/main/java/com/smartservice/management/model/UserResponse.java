
package com.smartservice.management.model;

import java.time.LocalDateTime;

public class UserResponse {

    private final int userId;
    private final String name;
    private final String email;
    private final String role;
    private final LocalDateTime createdAt;
    private final boolean active;

    public UserResponse(int userId, String name, String email,
                        String role, LocalDateTime createdAt,
                        boolean active) {

        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
        this.active = active;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }
}
