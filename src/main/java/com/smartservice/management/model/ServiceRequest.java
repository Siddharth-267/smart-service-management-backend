
package com.smartservice.management.model;

import java.time.LocalDateTime;

public class ServiceRequest {

    private int requestId;
    private int userId;
    private int categoryId;
    private String title;
    private String description;
    private String priority;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String location;
    private int recurringIssueCount;
    public ServiceRequest() {
    }

    // Getters and setters

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    private boolean recurringIssue;

    public boolean isRecurringIssue() {
        return recurringIssue;
    }

    public void setRecurringIssue(boolean recurringIssue) {
        this.recurringIssue = recurringIssue;
    }
    public int getRecurringIssueCount() {
        return recurringIssueCount;
    }

    public void setRecurringIssueCount(int recurringIssueCount) {
        this.recurringIssueCount = recurringIssueCount;
    }
}
