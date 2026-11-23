

package com.smartservice.management.controller;

import com.smartservice.management.model.RequestAssignment;
import com.smartservice.management.service.RequestAssignmentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
public class RequestAssignmentController {

    private final RequestAssignmentService assignmentService;

    public RequestAssignmentController(
            RequestAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    public ResponseEntity<RequestAssignment> assignRequest(
            @RequestBody Map<String, Integer> request,
            Authentication authentication) {

        Integer requestId = request.get("requestId");

        if (requestId == null) {
            return ResponseEntity.badRequest().build();
        }

        int authenticatedStaffId =
                Integer.parseInt(authentication.getName());

        RequestAssignment assignment =
                assignmentService.assignRequest(
                        requestId, authenticatedStaffId);

        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }

    @GetMapping("/request/{requestId}")
    public ResponseEntity<List<RequestAssignment>> getAssignmentsByRequest(
            @PathVariable int requestId,
            Authentication authentication) {

        int authenticatedUserId =
                Integer.parseInt(authentication.getName());

        String authenticatedRole = authentication.getAuthorities()
                .stream()
                .findFirst()
                .orElseThrow()
                .getAuthority()
                .replace("ROLE_", "");

        return ResponseEntity.ok(
                assignmentService.getAssignmentsByRequestId(
                        requestId, authenticatedUserId, authenticatedRole));
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<RequestAssignment>> getAssignmentsByStaff(
            @PathVariable int staffId,
            Authentication authentication) {

        int authenticatedUserId =
                Integer.parseInt(authentication.getName());

        String authenticatedRole = authentication.getAuthorities()
                .stream()
                .findFirst()
                .orElseThrow()
                .getAuthority()
                .replace("ROLE_", "");

        return ResponseEntity.ok(
                assignmentService.getAssignmentsByStaffId(
                        staffId, authenticatedUserId, authenticatedRole));
    }
}
