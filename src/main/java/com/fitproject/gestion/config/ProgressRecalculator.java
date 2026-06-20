package com.fitproject.gestion.config;

import com.fitproject.gestion.model.ConstructionStep;
import com.fitproject.gestion.model.Evidence;
import com.fitproject.gestion.model.EvidenceStatus;
import com.fitproject.gestion.model.Project;
import com.fitproject.gestion.repository.EvidenceRepository;
import com.fitproject.gestion.repository.ProjectRepository;
import com.fitproject.gestion.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProgressRecalculator {

    private final StepRepository stepRepository;
    private final ProjectRepository projectRepository;
    private final EvidenceRepository evidenceRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recalculateAllOnStartup() {
        log.info("Recalculating step progress from current evidence data...");

        List<ConstructionStep> steps = stepRepository.findAll();
        for (ConstructionStep step : steps) {
            List<Evidence> evidences = evidenceRepository.findByStep_StepId(step.getStepId());
            long total    = evidences.size();
            long approved = evidences.stream()
                    .filter(e -> e.getStatus() == EvidenceStatus.APPROVED)
                    .count();
            int progress = total > 0 ? (int) Math.round(approved * 100.0 / total) : 0;
            step.setProgressValue(progress);
            step.setStepStatus(progress >= 100);
            stepRepository.save(step);
        }

        // Reload projects from DB so they see the updated step values
        List<Project> projects = projectRepository.findAll();
        for (Project project : projects) {
            project.recalculateProgress();
            projectRepository.save(project);
        }

        log.info("Done. {} steps recalculated.", steps.size());
    }
}
