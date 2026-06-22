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

/**
 * Servicio de lógica de negocio para los pasos de construcción de proyectos.
 *
 * <p>Gestiona el ciclo de vida de {@link ConstructionStep}: creación, consulta
 * y renombrado. El progreso de un paso se recalcula automáticamente en
 * {@link EvidenceService} cada vez que se aprueba o rechaza una evidencia.</p>
 *
 * @see com.fitproject.gestion.controller.StepController
 * @see com.fitproject.gestion.repository.StepRepository
 */
@Service
@RequiredArgsConstructor
public class StepService {

    private final StepRepository stepRepository;
    private final ProjectRepository projectRepository;

    /**
     * Obtiene todos los pasos de construcción de un proyecto.
     *
     * @param projectId identificador UUID del proyecto padre
     * @return lista de pasos del proyecto como DTOs
     */
    @Transactional(readOnly = true)
    public List<StepDTO> getByProject(String projectId) {
        return stepRepository.findByProject_ProjectId(projectId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Crea un nuevo paso de construcción personalizado para un proyecto.
     *
     * <p>El paso inicia con {@code progressValue = 0} y {@code stepStatus = false}
     * (no completado).</p>
     *
     * @param req datos del paso: {@code projectId} y {@code stepName}
     * @return paso persistido como DTO
     * @throws IllegalArgumentException si el proyecto referenciado no existe
     */
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

    /**
     * Renombra un paso de construcción existente.
     *
     * @param stepId  identificador UUID del paso a renombrar
     * @param newName nuevo nombre para el paso
     * @return paso actualizado como DTO
     * @throws IllegalArgumentException si el paso no existe
     */
    @Transactional
    public StepDTO rename(String stepId, String newName) {
        ConstructionStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new IllegalArgumentException("Paso no encontrado: " + stepId));
        step.setStepName(newName);
        return toDTO(stepRepository.save(step));
    }

    /**
     * Convierte la entidad {@link ConstructionStep} a su representación DTO.
     *
     * @param s entidad JPA del paso de construcción
     * @return DTO listo para serializar
     */
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
