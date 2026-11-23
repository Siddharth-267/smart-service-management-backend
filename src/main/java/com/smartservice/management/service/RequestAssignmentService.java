
package com.smartservice.management.service;

import com.smartservice.management.model.RequestAssignment;
import com.smartservice.management.model.ServiceRequest;
import com.smartservice.management.model.User;
import com.smartservice.management.repository.RequestAssignmentRepository;
import com.smartservice.management.repository.ServiceRequestRepository;
import com.smartservice.management.repository.UserRepository;
import com.smartservice.management.exception.ResourceNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RequestAssignmentService {

    private final RequestAssignmentRepository assignmentRepository;
    private final ServiceRequestRepository requestRepository;
    private final UserRepository userRepository;

    public RequestAssignmentService(
            RequestAssignmentRepository assignmentRepository,
            ServiceRequestRepository requestRepository,
            UserRepository userRepository) {

        this.assignmentRepository = assignmentRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    public RequestAssignment assignRequest(int requestId, int staffId) {

        if (requestId <= 0 || staffId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request ID and Staff ID must be positive"
            );
        }

        ServiceRequest request = requestRepository.findById(requestId);

        if (request == null) {
            throw new ResourceNotFoundException(
                    "Service request not found with ID: " + requestId
            );
        }

        if (!"OPEN".equalsIgnoreCase(request.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only OPEN requests can be assigned to staff"
            );
        }

        User staff = userRepository.findById(staffId);

        if (staff == null) {
            throw new ResourceNotFoundException(
                    "Staff user not found with ID: " + staffId
            );
        }

        if (!"STAFF".equalsIgnoreCase(staff.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Selected user does not have STAFF role"
            );
        }

        if (!staff.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Staff account is inactive"
            );
        }

        if (assignmentRepository.existsByRequestIdAndStaffId(
                requestId, staffId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This staff member is already assigned to this request"
            );
        }

        assignmentRepository.assignRequest(requestId, staffId);

        List<RequestAssignment> assignments =
                assignmentRepository.findByRequestId(requestId);

        return assignments.get(0);
    }

    public List<RequestAssignment> getAssignmentsByRequestId(
            int requestId,
            int authenticatedUserId,
            String authenticatedRole) {

        if (requestId <= 0 || authenticatedUserId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Request ID and User ID must be positive"
            );
        }

        ServiceRequest request = requestRepository.findById(requestId);

        if (request == null) {
            throw new ResourceNotFoundException(
                    "Service request not found with ID: " + requestId
            );
        }

        boolean isAdmin = "ADMIN".equalsIgnoreCase(authenticatedRole);
        boolean isStaff = "STAFF".equalsIgnoreCase(authenticatedRole);

        if (isAdmin) {
            return assignmentRepository.findByRequestId(requestId);
        }

        if (isStaff) {
            if (!assignmentRepository.existsByRequestIdAndStaffId(
                    requestId, authenticatedUserId)) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Staff members can only view requests assigned to them"
                );
            }

            return assignmentRepository.findByRequestId(requestId);
        }

        if ("USER".equalsIgnoreCase(authenticatedRole)) {
            if (request.getUserId() != authenticatedUserId) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Users can only view assignments for their own requests"
                );
            }

            return assignmentRepository.findByRequestId(requestId);
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not authorized to view these assignments"
        );
    }

    public List<RequestAssignment> getAssignmentsByStaffId(
            int staffId,
            int authenticatedUserId,
            String authenticatedRole) {

        if (staffId <= 0 || authenticatedUserId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Staff ID and User ID must be positive"
            );
        }

        boolean isAdmin = "ADMIN".equalsIgnoreCase(authenticatedRole);
        boolean isStaff = "STAFF".equalsIgnoreCase(authenticatedRole);

        if (!isAdmin && !isStaff) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only STAFF or ADMIN can view staff assignments"
            );
        }

        if (!isAdmin && staffId != authenticatedUserId) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Staff members can only view their own assignments"
            );
        }

        return assignmentRepository.findByStaffId(staffId);
    }
}
