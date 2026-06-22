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

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de lógica de negocio para la gestión de proyectos de fabricación.
 *
 * <p>Orquesta las operaciones sobre {@link Project} y su creación automática
 * de pasos de construcción. El progreso global de un proyecto es el promedio
 * del {@code progressValue} de todos sus pasos.</p>
 *
 * @see com.fitproject.gestion.controller.ProjectController
 * @see com.fitproject.gestion.repository.ProjectRepository
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final StepRepository stepRepository;

    /**
     * Retorna proyectos del sistema con paginación para evitar cargar toda la
     * tabla en memoria cuando el volumen de proyectos escala (Green Computing).
     *
     * @param page número de página, base cero
     * @param size cantidad máxima de proyectos por página
     * @return lista paginada de proyectos con pasos y evidencias anidadas
     */
    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects(int page, int size) {
        return projectRepository.findAll(PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca un proyecto por su identificador único.
     *
     * @param projectId identificador UUID del proyecto
     * @return DTO del proyecto con pasos y evidencias anidadas
     * @throws IllegalArgumentException si no existe un proyecto con el ID dado
     */
    @Transactional(readOnly = true)
    public ProjectDTO getProjectById(String projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + projectId));
        return toDTO(project);
    }

    /**
     * Crea un nuevo proyecto y auto-genera los 4 pasos de construcción estándar.
     *
     * <p>Los nombres de los pasos se obtienen de
     * {@link DataSeeder#DEFAULT_STEP_NAMES}. Cada paso inicia con
     * {@code progressValue = 0} y {@code stepStatus = false}.</p>
     *
     * @param req datos del proyecto (modelName, description, budget, supervisorId, supervisorName)
     * @return proyecto creado incluyendo los pasos auto-generados
     */
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

    /**
     * Actualiza los campos editables de un proyecto existente.
     *
     * <p>Aplica semántica de patch: solo actualiza los campos no nulos del request.</p>
     *
     * @param projectId identificador UUID del proyecto a actualizar
     * @param req       campos a modificar (descripción, imageUrl, budget, supervisorId, supervisorName)
     * @return proyecto actualizado como DTO
     * @throws IllegalArgumentException si no existe el proyecto
     */
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

    /**
     * Recalcula y persiste el progreso global de un proyecto.
     *
     * <p>El progreso es el promedio de los {@code progressValue} de todos sus pasos.</p>
     *
     * @param project proyecto cuyo progreso debe recalcularse
     */
    public void refreshProgress(Project project) {
        project.recalculateProgress();
        projectRepository.save(project);
    }

    /**
     * Convierte la entidad {@link Project} a su representación DTO con pasos
     * y evidencias anidadas.
     *
     * @param project entidad JPA del proyecto
     * @return DTO completo listo para serializar hacia el cliente
     */
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
