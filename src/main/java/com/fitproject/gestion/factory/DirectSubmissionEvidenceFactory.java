package com.fitproject.gestion.factory;

import com.fitproject.gestion.dto.EvidenceDTO;
import com.fitproject.gestion.model.ConstructionStep;
import com.fitproject.gestion.model.Evidence;
import com.fitproject.gestion.model.EvidenceStatus;
import com.fitproject.gestion.model.Project;
import org.springframework.stereotype.Component;

/**
 * Concrete Factory for direct evidence submissions.
 *
 * <p>Handles the scenario where a supervisor or worker uploads an evidence photo
 * without a prior task assignment. No worker identity fields are set; the evidence
 * starts in {@link EvidenceStatus#PENDING} awaiting supervisor approval.</p>
 *
 * @see EvidenceFactory
 * @see WorkerTaskEvidenceFactory
 */
@Component
public class DirectSubmissionEvidenceFactory extends EvidenceFactory {

    /**
     * Creates an {@link Evidence} entity for a direct (non-assigned) submission.
     *
     * @param dto     the incoming evidence request data
     * @param project the parent project
     * @param step    the construction step this evidence documents
     * @return a new {@link Evidence} with status {@link EvidenceStatus#PENDING} and no worker assignment
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
                .status(EvidenceStatus.PENDING)
                .build();
    }
}
