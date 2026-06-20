package com.fitproject.gestion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for evidence events and dispatches notifications.
 * Uses AFTER_COMMIT so the notification only fires if the DB transaction succeeded.
 * Replace the log lines with RabbitMQ / HTTP call to MS-Notificaciones when ready.
 */
@Component
@Slf4j
public class EvidenceNotificationListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkerEvidenceSubmitted(WorkerEvidenceSubmittedEvent event) {
        log.info(
            "[NOTIFICATION] Worker '{}' submitted evidence for task '{}' (evidenceId={}, projectId={}, stepId={}). " +
            "Supervisor should be notified.",
            event.getWorkerName(),
            event.getTaskName(),
            event.getEvidenceId(),
            event.getProjectId(),
            event.getStepId()
        );

        // ── TODO: send to MS-Notificaciones via RabbitMQ ──────────────────────
        // NotificationPayload payload = NotificationPayload.builder()
        //     .type("WORKER_EVIDENCE_SUBMITTED")
        //     .title("Nueva evidencia para revisar")
        //     .body(event.getWorkerName() + " completó la tarea: " + event.getTaskName())
        //     .projectId(event.getProjectId())
        //     .evidenceId(event.getEvidenceId())
        //     .build();
        // rabbitTemplate.convertAndSend("fit.notifications", "evidence.submitted", payload);
        // ──────────────────────────────────────────────────────────────────────
    }
}
