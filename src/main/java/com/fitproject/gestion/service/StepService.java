package com.fitproject.gestion.service;

import com.fitproject.gestion.dto.CreateStepRequest;
import com.fitproject.gestion.dto.StepDTO;
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
public class StepService {

    private final StepRepository stepRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<StepDTO> getByProject(String projectId) {
        return stepRepository.findByProject_ProjectId(projectId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public StepDTO create(CreateStepRequest req) {
        Project project = projectRepository.findById(req.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + req.getProjectId()));
        ConstructionStep step = ConstructionStep.builder()
                .project(project)
                .stepName(req.getStepName())
                .progressValue(0)
                .stepStatus(false)
                .build();
        return toDTO(stepRepository.save(step));
    }

    @Transactional
    public StepDTO rename(String stepId, String newName) {
        ConstructionStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Paso no encontrado: " + stepId));
        step.setStepName(newName);
        return toDTO(stepRepository.save(step));
    }

    private StepDTO toDTO(ConstructionStep s) {
        return com.fitproject.gestion.dto.StepDTO.builder()
                .stepId(s.getStepId())
                .projectId(s.getProject() != null ? s.getProject().getProjectId() : null)
                .stepName(s.getStepName())
                .stepStatus(s.getStepStatus())
                .progressValue(s.getProgressValue())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
