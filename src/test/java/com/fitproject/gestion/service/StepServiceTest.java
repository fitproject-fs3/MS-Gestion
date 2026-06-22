package com.fitproject.gestion.service;

import com.fitproject.gestion.dto.CreateStepRequest;
import com.fitproject.gestion.dto.StepDTO;
import com.fitproject.gestion.model.ConstructionStep;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link StepService}.
 */
@ExtendWith(MockitoExtension.class)
class StepServiceTest {

    @Mock private StepRepository stepRepository;
    @Mock private ProjectRepository projectRepository;

    @InjectMocks
    private StepService stepService;

    private Project project;
    private ConstructionStep step;

    @BeforeEach
    void setUp() {
        project = Project.builder()
                .projectId("proj-1").modelName("FitBox")
                .constructionSteps(new ArrayList<>()).evidences(new ArrayList<>())
                .overallProgress(0).build();

        step = ConstructionStep.builder()
                .stepId("step-1").stepName("Estructura")
                .progressValue(0).stepStatus(false)
                .project(project).evidences(new ArrayList<>())
                .build();
    }

    // ─── getByProject ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByProject: retorna los pasos del proyecto")
    void getByProject_returnsStepDTOs() {
        when(stepRepository.findByProject_ProjectId("proj-1")).thenReturn(List.of(step));

        List<StepDTO> result = stepService.getByProject("proj-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStepName()).isEqualTo("Estructura");
        assertThat(result.get(0).getProgressValue()).isZero();
    }

    @Test
    @DisplayName("getByProject: retorna lista vacía si el proyecto no tiene pasos")
    void getByProject_noSteps_returnsEmpty() {
        when(stepRepository.findByProject_ProjectId("proj-2")).thenReturn(List.of());

        List<StepDTO> result = stepService.getByProject("proj-2");

        assertThat(result).isEmpty();
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: crea un paso con progressValue=0 y stepStatus=false")
    void create_success_persistsStep() {
        CreateStepRequest req = new CreateStepRequest();
        req.setProjectId("proj-1"); req.setStepName("Instalaciones");

        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(stepRepository.save(any(ConstructionStep.class))).thenReturn(step);

        StepDTO result = stepService.create(req);

        assertThat(result).isNotNull();
        assertThat(result.getProgressValue()).isZero();
        verify(stepRepository).save(any(ConstructionStep.class));
    }

    @Test
    @DisplayName("create: lanza excepción si el proyecto referenciado no existe")
    void create_projectNotFound_throwsException() {
        CreateStepRequest req = new CreateStepRequest();
        req.setProjectId("xxx"); req.setStepName("Paso");
        when(projectRepository.findById("xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stepService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Proyecto no encontrado");
    }

    // ─── rename ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("rename: actualiza el nombre del paso correctamente")
    void rename_success_updatesName() {
        when(stepRepository.findById("step-1")).thenReturn(Optional.of(step));
        when(stepRepository.save(step)).thenReturn(step);

        StepDTO result = stepService.rename("step-1", "Acabados");

        assertThat(result.getStepName()).isEqualTo("Acabados");
    }

    @Test
    @DisplayName("rename: lanza excepción si el paso no existe")
    void rename_notFound_throwsException() {
        when(stepRepository.findById("xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stepService.rename("xxx", "Nuevo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Paso no encontrado");
    }
}
