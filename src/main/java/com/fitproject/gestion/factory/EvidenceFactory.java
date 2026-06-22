package com.fitproject.gestion.factory;

import com.fitproject.gestion.dto.EvidenceDTO;
import com.fitproject.gestion.model.ConstructionStep;
import com.fitproject.gestion.model.Evidence;
import com.fitproject.gestion.model.Project;

/**
 * Abstract Factory Method creator for {@link Evidence} entities.
 *
 * <p>Each concrete subclass encapsulates the construction logic for a specific
 * evidence submission scenario, removing that responsibility from the service layer.
 * The service selects the appropriate factory at runtime based on the request context.</p>
 *
 * @see DirectSubmissionEvidenceFactory
 * @see WorkerTaskEvidenceFactory
 */
public abstract class EvidenceFactory {

    /**
     * Factory method — implemented by each concrete subclass to produce
     * a fully initialized, unsaved {@link Evidence} entity.
     *
     * @param dto     incoming evidence request data
     * @param project the parent {@link Project} entity
     * @param step    the {@link ConstructionStep} this evidence belongs to
     * @return a new {@link Evidence} instance ready to be persisted
     */
    public abstract Evidence createEvidence(EvidenceDTO dto, Project project, ConstructionStep step);
}
