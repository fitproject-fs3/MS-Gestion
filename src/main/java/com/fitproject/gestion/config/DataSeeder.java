package com.fitproject.gestion.config;

import com.fitproject.gestion.model.ConstructionStep;
import com.fitproject.gestion.model.Project;
import com.fitproject.gestion.repository.ProjectRepository;
import com.fitproject.gestion.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ProjectRepository projectRepository;
    private final StepRepository stepRepository;

    public static final List<String> DEFAULT_STEP_NAMES = List.of(
        "Preparación estructural del container",
        "Instalación eléctrica y sanitaria",
        "Construcción y habilitación interior",
        "Terminaciones externas y detalles finales"
    );

    @Override
    public void run(String... args) {
        if (projectRepository.count() == 0) {
            seedDemoData();
        } else {
            ensureCanonicalStepNames();
        }
    }

    private void seedDemoData() {
        log.info("Seeding demo data for MS-Gestion...");

        Project p1 = projectRepository.save(Project.builder()
                .modelName("FitBox Alpha")
                .description("Gimnasio en contenedor 20ft - modelo básico")
                .imageUrl("https://via.placeholder.com/400x300")
                .budget(45000.0)
                .overallProgress(0)
                .build());

        Project p2 = projectRepository.save(Project.builder()
                .modelName("FitBox Pro")
                .description("Gimnasio en contenedor 40ft - modelo profesional")
                .imageUrl("https://via.placeholder.com/400x300")
                .budget(85000.0)
                .overallProgress(0)
                .build());

        for (String name : DEFAULT_STEP_NAMES) {
            stepRepository.save(ConstructionStep.builder()
                    .project(p1).stepName(name).progressValue(0).stepStatus(false).build());
            stepRepository.save(ConstructionStep.builder()
                    .project(p2).stepName(name).progressValue(0).stepStatus(false).build());
        }

        log.info("Demo data created: 2 projects, 8 steps.");
    }

    private void ensureCanonicalStepNames() {
        List<Project> projects = projectRepository.findAll();
        int updated = 0;

        for (Project project : projects) {
            List<ConstructionStep> steps = stepRepository.findByProject_ProjectId(project.getProjectId());

            // Sort by creation time so position is deterministic
            steps.sort(Comparator.comparing(ConstructionStep::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())));

            // Rename first N steps to canonical names (only if different)
            for (int i = 0; i < Math.min(steps.size(), DEFAULT_STEP_NAMES.size()); i++) {
                ConstructionStep step = steps.get(i);
                String canonical = DEFAULT_STEP_NAMES.get(i);
                if (!canonical.equals(step.getStepName())) {
                    step.setStepName(canonical);
                    stepRepository.save(step);
                    updated++;
                }
            }

            // Create any missing canonical steps
            if (steps.size() < DEFAULT_STEP_NAMES.size()) {
                for (int i = steps.size(); i < DEFAULT_STEP_NAMES.size(); i++) {
                    stepRepository.save(ConstructionStep.builder()
                            .project(project)
                            .stepName(DEFAULT_STEP_NAMES.get(i))
                            .progressValue(0)
                            .stepStatus(false)
                            .build());
                    updated++;
                }
            }
        }

        if (updated > 0) {
            log.info("Updated {} steps to canonical names across {} projects.", updated, projects.size());
        }
    }
}
