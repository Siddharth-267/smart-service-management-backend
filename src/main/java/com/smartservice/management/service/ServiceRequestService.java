
package com.smartservice.management.service;

import com.smartservice.management.model.ServiceRequest;
import com.smartservice.management.model.ServiceRequestCreateRequest;
import com.smartservice.management.model.RequestAssignment;
import com.smartservice.management.model.User;

import com.smartservice.management.repository.ServiceRequestRepository;
import com.smartservice.management.repository.UserRepository;
import com.smartservice.management.repository.RequestCategoryRepository;
import com.smartservice.management.repository.RequestAssignmentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final RequestCategoryRepository categoryRepository;
    private final RequestAssignmentRepository assignmentRepository;

    public ServiceRequestService(
            ServiceRequestRepository requestRepository,
            UserRepository userRepository,
            RequestCategoryRepository categoryRepository,
            RequestAssignmentRepository assignmentRepository) {

        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.assignmentRepository = assignmentRepository;
    }

    // GET ALL REQUESTS BASED ON AUTHENTICATED ROLE
    public List<ServiceRequest> getAllRequests(
            int authenticatedUserId,
            String authenticatedRole) {

        List<ServiceRequest> requests;

        if ("ADMIN".equals(authenticatedRole)) {

            requests = requestRepository.findAll();

        } else if ("USER".equals(authenticatedRole)) {

            requests = new ArrayList<>();

            for (ServiceRequest request : requestRepository.findAll()) {

                if (request.getUserId() == authenticatedUserId) {
                    requests.add(request);
                }
            }

        } else if ("STAFF".equals(authenticatedRole)) {

            requests = new ArrayList<>();

            List<RequestAssignment> assignments =
                    assignmentRepository.findByStaffId(authenticatedUserId);

            for (RequestAssignment assignment : assignments) {

                ServiceRequest request =
                        requestRepository.findById(assignment.getRequestId());

                if (request != null) {
                    requests.add(request);
                }
            }

        } else {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
        }

        for (ServiceRequest request : requests) {
            enrichRecurringIssueDetails(request);
        }

        return requests;
    }

    // GET REQUEST BY ID WITH ACCESS CONTROL
    public ServiceRequest getRequestById(
            int requestId,
            int authenticatedUserId,
            String authenticatedRole) {

        if (requestId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid request ID"
            );
        }

        ServiceRequest request = requestRepository.findById(requestId);

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Service request not found"
            );
        }

        if ("ADMIN".equals(authenticatedRole)) {

            // ADMIN can access all requests.

        } else if ("USER".equals(authenticatedRole)) {

            if (request.getUserId() != authenticatedUserId) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Users can only access their own requests"
                );
            }

        } else if ("STAFF".equals(authenticatedRole)) {

            boolean assigned =
                    assignmentRepository.existsByRequestIdAndStaffId(
                            requestId,
                            authenticatedUserId
                    );

            if (!assigned) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Staff can only access assigned requests"
                );
            }

        } else {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
        }

        enrichRecurringIssueDetails(request);

        return request;
    }

    // ENRICH RECURRING ISSUE INFORMATION
    public ServiceRequest enrichRecurringIssueDetails(ServiceRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Service request not found"
            );
        }

        int count = requestRepository.countRepeatedIssues(
                request.getCategoryId(),
                request.getLocation()
        );

        request.setRecurringIssueCount(count);
        request.setRecurringIssue(count >= 3);

        return request;
    }

    // CREATE SERVICE REQUEST
    public ServiceRequest createRequest(ServiceRequestCreateRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request body cannot be empty"
            );
        }

        if (request.getUserId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid user ID"
            );
        }

        if (request.getCategoryId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid category ID"
            );
        }

        // TITLE VALIDATION
        if (request.getTitle() == null ||
                request.getTitle().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Title cannot be empty"
            );
        }

        if (request.getTitle().trim().length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Title cannot exceed 150 characters"
            );
        }

        // DESCRIPTION VALIDATION
        if (request.getDescription() == null ||
                request.getDescription().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Description cannot be empty"
            );
        }

        if (request.getDescription().length() > 5000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Description cannot exceed 5000 characters"
            );
        }

        // LOCATION VALIDATION
        if (request.getLocation() == null ||
                request.getLocation().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Location cannot be empty"
            );
        }

        if (request.getLocation().trim().length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Location cannot exceed 150 characters"
            );
        }

        // VERIFY USER
        User user = userRepository.findById(request.getUserId());

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Inactive users cannot create service requests"
            );
        }

        // VERIFY CATEGORY
        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Request category not found"
            );
        }

        // CLEAN INPUT
        String title = request.getTitle().trim();
        String description = request.getDescription().trim();
        String location = request.getLocation().trim();

        if (title.isEmpty() ||
                description.isEmpty() ||
                location.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Title, description and location cannot be empty"
            );
        }

        // CALCULATE BASE PRIORITY
        String priority = calculatePriority(description);

        // CHECK REPEATED ISSUES BEFORE INSERTING
        int repeatedIssueCount =
                requestRepository.countRepeatedIssues(
                        request.getCategoryId(),
                        location
                );

        boolean recurringIssue = repeatedIssueCount >= 2;

        // UPGRADE PRIORITY FOR REPEATED ISSUES
        if (repeatedIssueCount >= 5) {

            priority = "HIGH";

        } else if (repeatedIssueCount >= 3 &&
                "LOW".equals(priority)) {

            priority = "MEDIUM";
        }

        // CREATE REQUEST OBJECT
        ServiceRequest newRequest = new ServiceRequest();

        newRequest.setUserId(request.getUserId());
        newRequest.setCategoryId(request.getCategoryId());
        newRequest.setTitle(title);
        newRequest.setDescription(description);
        newRequest.setLocation(location);
        newRequest.setPriority(priority);
        newRequest.setStatus("OPEN");

        newRequest.setRecurringIssue(recurringIssue);
        newRequest.setRecurringIssueCount(repeatedIssueCount + 1);

        // SAVE REQUEST
        int generatedId = requestRepository.createRequest(newRequest);

        newRequest.setRequestId(generatedId);

        return newRequest;
    }

    // RULE-BASED PRIORITY CALCULATION
    private String calculatePriority(String description) {

        String text = description.toLowerCase();

        // HIGH PRIORITY KEYWORDS
        String[] highPriorityKeywords = {
                "electric shock",
                "electrical shock",
                "sparking",
                "exposed wire",
                "exposed electrical wire",
                "electrical hazard",
                "short circuit",
                "gas leak",
                "smoke",
                "fire"
        };

        for (String keyword : highPriorityKeywords) {

            if (text.contains(keyword)) {
                return "HIGH";
            }
        }

        // MEDIUM PRIORITY KEYWORDS
        String[] mediumPriorityKeywords = {
                "water leakage",
                "leak",
                "internet",
                "wifi",
                "water supply",
                "drainage",
                "blocked pipe",
                "broken"
        };

        for (String keyword : mediumPriorityKeywords) {

            if (text.contains(keyword)) {
                return "MEDIUM";
            }
        }

        return "LOW";
    }
}
