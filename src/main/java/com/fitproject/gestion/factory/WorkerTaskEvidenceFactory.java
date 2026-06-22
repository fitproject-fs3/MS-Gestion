package com.fitproject.gestion.factory;

import com.fitproject.gestion.dto.EvidenceDTO;
import com.fitproject.gestion.model.ConstructionStep;
import com.fitproject.gestion.model.Evidence;
import com.fitproject.gestion.model.EvidenceStatus;
import com.fitproject.gestion.model.Project;
import org.springframework.stereotype.Component;

/**
 * Concrete Factory for worker task-assignment evidences.
 *
 * <p>Handles the scenario where a supervisor assigns a construction task to a specific
 * worker. The resulting {@link Evidence} includes worker identity fields and starts
 * in {@link EvidenceStatus#PENDING}, waiting for the worker to upload the photo proof
 * via {@code workerSubmit()}.</p>
 *
 * @see EvidenceFactory
 * @see DirectSubmissionEvidenceFactory
 */
@Component
public class WorkerTaskEvidenceFactory extends EvidenceFactory {

    /**
     * Creates an {@link Evidence} entity representing a task assigned to a worker.
     *
     * @param dto     the incoming evidence request data, must contain {@code assignedWorkerId}
     *                and {@code assignedWorkerName}
     * @param project the parent project
     * @param step    the construction step this task belongs to
     * @return a new {@link Evidence} with worker identity fields and status {@link EvidenceStatus#PENDING}
     */
    @Override
    public Evidence createEvidence(EvidenceDTO dto, Project project, ConstructionStep step) {
        return Evidence.builder()
                .project(project)
                .step(step)
                .evidenceUrl(dto.getEvidenceUrl() != null ? dto.getEvidenceUrl() : "")
                .description(dto.getDescription() != null ? dto.getDescription() : "")
                .name(dto.getName())
                .submittedBy(dto.getSubmittedBy())
                .assignedWorkerId(dto.getAssignedWorkerId())
                .assignedWorkerName(dto.getAssignedWorkerName())
                .status(EvidenceStatus.PENDING)
                .build();
    }
}
