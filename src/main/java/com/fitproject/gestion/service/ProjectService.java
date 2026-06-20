package com.fitproject.gestion.service;

import com.fitproject.gestion.config.DataSeeder;
import com.fitproject.gestion.dto.*;
import com.fitproject.gestion.model.ConstructionStep;
import com.fitproject.gestion.model.Project;
import com.fitproject.gestion.repository.ProjectRepository;
import com.fitproject.gestion.repository.StepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final StepRepository stepRepository;

    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + projectId));
        return toDTO(project);
    }

    @Transactional
    public ProjectDTO createProject(CreateProjectRequest req) {
        Project project = projectRepository.save(Project.builder()
                .modelName(req.getModelName())
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .budget(req.getBudget())
                .supervisorId(req.getSupervisorId())
                .supervisorName(req.getSupervisorName())
                .overallProgress(0)
                .build());

        // Auto-create the 4 default construction steps
        for (String stepName : DataSeeder.DEFAULT_STEP_NAMES) {
            stepRepository.save(ConstructionStep.builder()
                    .project(project)
                    .stepName(stepName)
                    .progressValue(0)
                    .stepStatus(false)
                    .build());
        }

        return toDTO(projectRepository.findById(project.getProjectId()).orElse(project));
    }

    @Transactional
    public ProjectDTO updateProject(String projectId, UpdateProjectRequest req) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + projectId));
        if (req.getDescription() != null) project.setDescription(req.getDescription());
        if (req.getImageUrl() != null) project.setImageUrl(req.getImageUrl());
        if (req.getBudget() != null) project.setBudget(req.getBudget());
        if (req.getSupervisorId() != null) project.setSupervisorId(req.getSupervisorId());
        if (req.getSupervisorName() != null) project.setSupervisorName(req.getSupervisorName());
        return toDTO(projectRepository.save(project));
    }

    public void refreshProgress(Project project) {
        project.recalculateProgress();
        projectRepository.save(project);
    }

    public ProjectDTO toDTO(Project project) {
        List<EvidenceDTO> evidences = project.getEvidences() == null ? List.of() :
                project.getEvidences().stream().map(e -> EvidenceDTO.builder()
                        .evidenceId(e.getEvidenceId())
                        .projectId(project.getProjectId())
                        .stepId(e.getStep() != null ? e.getStep().getStepId() : null)
                        .evidenceUrl(e.getEvidenceUrl())
                        .description(e.getDescription())
                        .name(e.getName())
                        .submittedBy(e.getSubmittedBy())
                        .supervisorId(e.getSupervisorId())
                        .status(e.getStatus().name())
                        .createdAt(e.getCreatedAt())
                        .updatedAt(e.getUpdatedAt())
                        .build()).collect(Collectors.toList());

        List<StepDTO> steps = project.getConstructionSteps() == null ? List.of() :
                project.getConstructionSteps().stream().map(s -> {
                    List<EvidenceDTO> stepEvidences = s.getEvidences() == null ? List.of() :
                            s.getEvidences().stream().map(e -> EvidenceDTO.builder()
                                    .evidenceId(e.getEvidenceId())
                                    .projectId(project.getProjectId())
                                    .stepId(s.getStepId())
                                    .evidenceUrl(e.getEvidenceUrl())
                                    .description(e.getDescription())
                                    .name(e.getName())
                                    .submittedBy(e.getSubmittedBy())
                                    .supervisorId(e.getSupervisorId())
                                    .status(e.getStatus().name())
                                    .createdAt(e.getCreatedAt())
                                    .updatedAt(e.getUpdatedAt())
                                    .build()).collect(Collectors.toList());
                    return StepDTO.builder()
                            .stepId(s.getStepId())
                            .projectId(project.getProjectId())
                            .stepName(s.getStepName())
                            .stepStatus(s.getStepStatus())
                            .progressValue(s.getProgressValue())
                            .createdAt(s.getCreatedAt())
                            .updatedAt(s.getUpdatedAt())
                            .evidences(stepEvidences)
                            .build();
                }).collect(Collectors.toList());

        return ProjectDTO.builder()
                .projectId(project.getProjectId())
                .containerId(project.getContainerId())
                .modelName(project.getModelName())
                .description(project.getDescription())
                .imageUrl(project.getImageUrl())
                .budget(project.getBudget())
                .supervisorId(project.getSupervisorId())
                .supervisorName(project.getSupervisorName())
                .overallProgress(project.getOverallProgress())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .constructionSteps(steps)
                .evidences(evidences)
                .build();
    }
}
