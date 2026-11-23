


package com.smartservice.management.controller;

import com.smartservice.management.model.StatusUpdateRequest;
import com.smartservice.management.model.ServiceRequest;
import com.smartservice.management.model.RequestStatusHistory;
import com.smartservice.management.service.RequestStatusService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RequestStatusController {

    private final RequestStatusService statusService;

    public RequestStatusController(RequestStatusService statusService) {
        this.statusService = statusService;
    }

    @PatchMapping("/{requestId}/status")
    public ResponseEntity<ServiceRequest> updateStatus(
            @PathVariable int requestId,
            @RequestBody StatusUpdateRequest request,
            Authentication authentication) {

        int authenticatedUserId =
                Integer.parseInt(authentication.getName());

        ServiceRequest updatedRequest =
                statusService.updateStatus(
                        requestId,
                        authenticatedUserId,
                        request.getNewStatus(),
                        request.getRemarks()
                );

        return ResponseEntity.ok(updatedRequest);
    }

    @GetMapping("/{requestId}/history")
    public ResponseEntity<List<RequestStatusHistory>> getStatusHistory(
            @PathVariable int requestId,
            Authentication authentication) {

        int authenticatedUserId =
                Integer.parseInt(authentication.getName());

        String authenticatedRole =
                authentication.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority()
                        .replace("ROLE_", "");

        return ResponseEntity.ok(
                statusService.getStatusHistory(
                        requestId,
                        authenticatedUserId,
                        authenticatedRole
                )
        );
    }
}
