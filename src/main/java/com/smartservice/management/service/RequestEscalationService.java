
package com.smartservice.management.service;

import com.smartservice.management.model.ServiceRequest;
import com.smartservice.management.repository.ServiceRequestRepository;
import com.smartservice.management.repository.RequestStatusHistoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RequestEscalationService {

    private final ServiceRequestRepository requestRepository;
    private final RequestStatusHistoryRepository historyRepository;

    public RequestEscalationService(
            ServiceRequestRepository requestRepository,
            RequestStatusHistoryRepository historyRepository) {

        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public List<ServiceRequest> escalateOverdueRequests() {

        List<ServiceRequest> overdueRequests =
                requestRepository.findOverdueRequests();

        List<ServiceRequest> escalatedRequests = new ArrayList<>();

        for (ServiceRequest request : overdueRequests) {

            String oldStatus = request.getStatus();

            int updatedRows = requestRepository.updateStatus(
                    request.getRequestId(),
                    "ESCALATED"
            );

            if (updatedRows != 1) {
                throw new IllegalStateException(
                        "Failed to escalate request ID: "
                                + request.getRequestId()
                );
            }

            historyRepository.save(
                    request.getRequestId(),
                    oldStatus,
                    "ESCALATED",
                    null,
                    "Automatically escalated because the request exceeded its priority-based time limit."
            );

            escalatedRequests.add(
                    requestRepository.findById(request.getRequestId())
            );
        }

        return escalatedRequests;
    }
}
