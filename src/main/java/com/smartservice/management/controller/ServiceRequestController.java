
package com.smartservice.management.controller;

import com.smartservice.management.model.ServiceRequest;
import com.smartservice.management.model.ServiceRequestCreateRequest;
import com.smartservice.management.service.ServiceRequestService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class ServiceRequestController {

    private final ServiceRequestService requestService;

    public ServiceRequestController(
            ServiceRequestService requestService) {
        this.requestService = requestService;
    }

    // Extract authenticated user ID from JWT
    private int getAuthenticatedUserId(Authentication authentication) {
        return Integer.parseInt(authentication.getName());
    }

    // Extract role from authenticated authorities
    private String getAuthenticatedRole(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .findFirst()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "User role not found"
                ));
    }

    // Get requests based on authenticated user's role
    @GetMapping
    public ResponseEntity<List<ServiceRequest>> getAllRequests(
            Authentication authentication) {

        int authenticatedUserId =
                getAuthenticatedUserId(authentication);

        String authenticatedRole =
                getAuthenticatedRole(authentication);

        List<ServiceRequest> requests =
                requestService.getAllRequests(
                        authenticatedUserId,
                        authenticatedRole
                );

        return ResponseEntity.ok(requests);
    }

    // Get an individual request with access verification
    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequest> getRequestById(
            @PathVariable("id") int id,
            Authentication authentication) {

        int authenticatedUserId =
                getAuthenticatedUserId(authentication);

        String authenticatedRole =
                getAuthenticatedRole(authentication);

        ServiceRequest request =
                requestService.getRequestById(
                        id,
                        authenticatedUserId,
                        authenticatedRole
                );

        return ResponseEntity.ok(request);
    }

    // Create a new service request
    @PostMapping
    public ResponseEntity<ServiceRequest> createRequest(
            @RequestBody ServiceRequestCreateRequest request,
            Authentication authentication) {

        int authenticatedUserId =
                getAuthenticatedUserId(authentication);

        // Always use the user ID from the JWT.
        request.setUserId(authenticatedUserId);

        ServiceRequest createdRequest =
                requestService.createRequest(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdRequest);
    }
}
