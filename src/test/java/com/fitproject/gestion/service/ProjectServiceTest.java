package com.fitproject.gestion.service;

import com.fitproject.gestion.dto.CreateProjectRequest;
import com.fitproject.gestion.dto.ProjectDTO;
import com.fitproject.gestion.dto.UpdateProjectRequest;
import com.fitproject.gestion.model.Project;
import com.fitproject.gestion.repository.ProjectRepository;
import com.fitproject.gestion.repository.StepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ProjectService}.
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private StepRepository stepRepository;

    @InjectMocks
    private ProjectService projectService;

    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.builder()
                .projectId("proj-1")
                .modelName("FitBox Basic")
                .description("Módulo básico")
                .budget(30000.0)
                .supervisorId("sup-uuid")
                .supervisorName("María Supervisora")
                .overallProgress(0)
                .constructionSteps(new ArrayList<>())
                .evidences(new ArrayList<>())
                .build();
    }

    // ─── getAllProjects ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllProjects: retorna lista paginada de proyectos")
    void getAllProjects_returnsDTOList() {
        when(projectRepository.findAll(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(project)));

        List<ProjectDTO> result = projectService.getAllProjects(0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProjectId()).isEqualTo("proj-1");
        assertThat(result.get(0).getModelName()).isEqualTo("FitBox Basic");
    }

    @Test
    @DisplayName("getAllProjects: retorna lista vacía cuando no hay proyectos")
    void getAllProjects_empty_returnsEmptyList() {
        when(projectRepository.findAll(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of()));

        List<ProjectDTO> result = projectService.getAllProjects(0, 20);

        assertThat(result).isEmpty();
    }

    // ─── getProjectById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getProjectById: retorna DTO del proyecto existente")
    void getProjectById_found_returnsDTO() {
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));

        ProjectDTO result = projectService.getProjectById("proj-1");

        assertThat(result.getProjectId()).isEqualTo("proj-1");
        assertThat(result.getSupervisorName()).isEqualTo("María Supervisora");
    }

    @Test
    @DisplayName("getProjectById: lanza excepción cuando el proyecto no existe")
    void getProjectById_notFound_throwsException() {
        when(projectRepository.findById("xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getProjectById("xxx"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Proyecto no encontrado");
    }

    // ─── createProject ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createProject: persiste el proyecto y auto-crea los 4 pasos de construcción")
    void createProject_success_createsProjectAndDefaultSteps() {
        CreateProjectRequest req = new CreateProjectRequest();
        req.setModelName("FitBox Pro"); req.setDescription("Módulo premium");
        req.setBudget(60000.0); req.setSupervisorId("sup-1"); req.setSupervisorName("Ana");

        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(stepRepository.save(any())).thenReturn(null);
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));

        ProjectDTO result = projectService.createProject(req);

        assertThat(result).isNotNull();
        // Se crean 4 pasos por defecto (DataSeeder.DEFAULT_STEP_NAMES)
        verify(stepRepository, times(4)).save(any());
    }

    // ─── updateProject ────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProject: actualiza solo los campos no nulos (patch semántico)")
    void updateProject_partialUpdate_onlyChangesProvidedFields() {
        UpdateProjectRequest req = new UpdateProjectRequest();
        req.setDescription("Nueva descripción");

        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        ProjectDTO result = projectService.updateProject("proj-1", req);

        assertThat(result.getDescription()).isEqualTo("Nueva descripción");
        // El presupuesto no debe cambiar
        assertThat(result.getBudget()).isEqualTo(30000.0);
    }

    @Test
    @DisplayName("updateProject: lanza excepción cuando el proyecto no existe")
    void updateProject_notFound_throwsException() {
        when(projectRepository.findById("xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.updateProject("xxx", new UpdateProjectRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Proyecto no encontrado");
    }
}
