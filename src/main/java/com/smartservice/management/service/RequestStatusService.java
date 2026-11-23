

package com.smartservice.management.service;

import org.springframework.transaction.annotation.Transactional;

import com.smartservice.management.model.ServiceRequest;
import com.smartservice.management.model.User;
import com.smartservice.management.model.RequestStatusHistory;

import com.smartservice.management.repository.ServiceRequestRepository;
import com.smartservice.management.repository.RequestStatusHistoryRepository;
import com.smartservice.management.repository.UserRepository;
import com.smartservice.management.repository.RequestAssignmentRepository;

import com.smartservice.management.exception.ResourceNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RequestStatusService {

    private final ServiceRequestRepository requestRepository;
    private final RequestStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final RequestAssignmentRepository assignmentRepository;
    private final ServiceRequestService serviceRequestService;

    public RequestStatusService(
            ServiceRequestRepository requestRepository,
            RequestStatusHistoryRepository historyRepository,
            UserRepository userRepository,
            RequestAssignmentRepository assignmentRepository,
            ServiceRequestService serviceRequestService) {

        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.serviceRequestService = serviceRequestService;
    }

    // UPDATE REQUEST STATUS
    @Transactional
    public ServiceRequest updateStatus(
            int requestId,
            int changedBy,
            String newStatus,
            String remarks) {

        if (requestId <= 0 || changedBy <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request ID and user ID must be positive"
            );
        }

        if (newStatus == null || newStatus.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New status is required"
            );
        }

        ServiceRequest request = requestRepository.findById(requestId);

        if (request == null) {
            throw new ResourceNotFoundException(
                    "Service request not found with ID: " + requestId
            );
        }

        User user = userRepository.findById(changedBy);

        if (user == null) {
            throw new ResourceNotFoundException(
                    "User not found with ID: " + changedBy
            );
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Inactive users cannot update request status"
            );
        }

        String oldStatus = request.getStatus();
        String targetStatus = newStatus.trim().toUpperCase();

        // STAFF STATUS PERMISSIONS
        if ("STAFF".equalsIgnoreCase(user.getRole())) {

            boolean assigned =
                    assignmentRepository.existsByRequestIdAndStaffId(
                            requestId,
                            changedBy
                    );

            if (!assigned) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Staff can only update requests assigned to them"
                );
            }

            if ("OPEN".equals(oldStatus)
                    && "IN_PROGRESS".equals(targetStatus)) {

                // Allowed

            } else if ("IN_PROGRESS".equals(oldStatus)
                    && "RESOLVED".equals(targetStatus)) {

                // Allowed

            } else {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Staff can only move OPEN to IN_PROGRESS "
                                + "or IN_PROGRESS to RESOLVED"
                );
            }

            // USER STATUS PERMISSIONS
        } else if ("USER".equalsIgnoreCase(user.getRole())) {

            if (request.getUserId() != changedBy) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Only the request owner can verify or reopen it"
                );
            }

            if (!"RESOLVED".equals(oldStatus)
                    || !("CLOSED".equals(targetStatus)
                    || "OPEN".equals(targetStatus))) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Users can only close or reopen resolved requests"
                );
            }

        } else {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This role cannot update request status"
            );
        }

        // UPDATE DATABASE STATUS
        requestRepository.updateStatus(requestId, targetStatus);

        // SAVE STATUS HISTORY
        historyRepository.save(
                requestId,
                oldStatus,
                targetStatus,
                changedBy,
                remarks
        );

        // RETURN UPDATED AND ENRICHED REQUEST
        ServiceRequest updatedRequest =
                requestRepository.findById(requestId);

        return serviceRequestService.enrichRecurringIssueDetails(
                updatedRequest
        );
    }

    // GET REQUEST STATUS HISTORY WITH ACCESS CONTROL
    public List<RequestStatusHistory> getStatusHistory(
            int requestId,
            int authenticatedUserId,
            String authenticatedRole) {

        if (requestId <= 0 || authenticatedUserId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request ID and user ID must be positive"
            );
        }

        ServiceRequest request = requestRepository.findById(requestId);

        if (request == null) {
            throw new ResourceNotFoundException(
                    "Service request not found with ID: " + requestId
            );
        }

        // ADMIN CAN VIEW ALL REQUEST HISTORIES
        if ("ADMIN".equalsIgnoreCase(authenticatedRole)) {

            return historyRepository.findByRequestId(requestId);

            // USER CAN VIEW OWN REQUEST HISTORY
        } else if ("USER".equalsIgnoreCase(authenticatedRole)) {

            if (request.getUserId() != authenticatedUserId) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Users can only view history of their own requests"
                );
            }

            // STAFF CAN VIEW HISTORY OF ASSIGNED REQUESTS
        } else if ("STAFF".equalsIgnoreCase(authenticatedRole)) {

            boolean assigned =
                    assignmentRepository.existsByRequestIdAndStaffId(
                            requestId,
                            authenticatedUserId
                    );

            if (!assigned) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Staff can only view history of assigned requests"
                );
            }

        } else {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This role cannot view request history"
            );
        }

        return historyRepository.findByRequestId(requestId);
    }
}
