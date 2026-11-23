
package com.smartservice.management.controller;

import com.smartservice.management.model.ServiceRequest;
import com.smartservice.management.service.RequestEscalationService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/escalation")
public class RequestEscalationController {

    private final RequestEscalationService escalationService;

    public RequestEscalationController(
            RequestEscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @PostMapping("/run")
    public List<ServiceRequest> escalateOverdueRequests() {

        return escalationService.escalateOverdueRequests();
    }
}
