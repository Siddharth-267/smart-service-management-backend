package com.smartservice.management.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RequestEscalationScheduler {

    private final RequestEscalationService escalationService;

    public RequestEscalationScheduler(
            RequestEscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @Scheduled(fixedRate = 300000)
    public void runEscalationCheck() {
        escalationService.escalateOverdueRequests();
    }
}
