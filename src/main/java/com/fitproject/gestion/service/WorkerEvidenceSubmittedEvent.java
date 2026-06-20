package com.fitproject.gestion.service;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class WorkerEvidenceSubmittedEvent extends ApplicationEvent {

    private final String evidenceId;
    private final String taskName;
    private final String workerName;
    private final String projectId;
    private final String stepId;

    public WorkerEvidenceSubmittedEvent(Object source,
                                        String evidenceId,
                                        String taskName,
                                        String workerName,
                                        String projectId,
                                        String stepId) {
        super(source);
        this.evidenceId = evidenceId;
        this.taskName   = taskName;
        this.workerName = workerName;
        this.projectId  = projectId;
        this.stepId     = stepId;
    }
}
