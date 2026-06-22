package com.fitproject.gestion.service;

import com.fitproject.gestion.config.RabbitMQConfig;
import com.fitproject.gestion.dto.EvidenceDTO;
import com.fitproject.gestion.dto.InsumoUsadoDTO;
import com.fitproject.gestion.factory.DirectSubmissionEvidenceFactory;
import com.fitproject.gestion.factory.WorkerTaskEvidenceFactory;
import com.fitproject.gestion.model.*;
import com.fitproject.gestion.repository.EvidenceRepository;
import com.fitproject.gestion.repository.ProjectRepository;
import com.fitproject.gestion.repository.StepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link EvidenceService}.
 * Cubre escenarios de éxito y manejo de excepciones para garantizar ≥60% de cobertura JaCoCo.
 */
@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock private EvidenceRepository evidenceRepository;
    @Mock private StepRepository stepRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private com.fitproject.gestion.client.InventarioClient inventarioClient;
    @Mock private DirectSubmissionEvidenceFactory directSubmissionFactory;
    @Mock private WorkerTaskEvidenceFactory workerTaskFactory;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EvidenceService evidenceService;

    private Project project;
    private ConstructionStep step;
    private Evidence evidence;

    @BeforeEach
    void setUp() {
        project = Project.builder()
                .projectId("proj-1")
                .modelName("FitBox Pro")
                .constructionSteps(new ArrayList<>())
                .evidences(new ArrayList<>())
                .overallProgress(0)
                .build();

        step = ConstructionStep.builder()
                .stepId("step-1")
                .stepName("Estructura")
                .progressValue(0)
                .stepStatus(false)
                .project(project)
                .evidences(new ArrayList<>())
                .build();
        project.getConstructionSteps().add(step);

        evidence = Evidence.builder()
                .evidenceId("ev-1")
                .project(project)
                .step(step)
                .name("Foto cimientos")
                .submittedBy("supervisor1")
                .assignedWorkerId("worker-uuid")
                .assignedWorkerName("Juan Obrero")
                .status(EvidenceStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── getPending ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getPending: retorna lista paginada de evidencias PENDING")
    void getPending_returnsDTOList() {
        when(evidenceRepository.findByStatus(EvidenceStatus.PENDING, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(evidence)));

        List<EvidenceDTO> result = evidenceService.getPending(0, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEvidenceId()).isEqualTo("ev-1");
        assertThat(result.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("getPending: retorna lista vacía cuando no hay evidencias pendientes")
    void getPending_emptyPage_returnsEmptyList() {
        when(evidenceRepository.findByStatus(EvidenceStatus.PENDING, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of()));

        List<EvidenceDTO> result = evidenceService.getPending(0, 20);

        assertThat(result).isEmpty();
    }

    // ─── getByWorker ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByWorker: retorna evidencias asignadas al trabajador")
    void getByWorker_returnsDTOList() {
        when(evidenceRepository.findByAssignedWorkerId("worker-uuid", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(evidence)));

        List<EvidenceDTO> result = evidenceService.getByWorker("worker-uuid", 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssignedWorkerId()).isEqualTo("worker-uuid");
    }

    // ─── getByStep ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByStep: retorna evidencias del paso especificado")
    void getByStep_returnsDTOList() {
        when(evidenceRepository.findByStep_StepId("step-1"))
                .thenReturn(List.of(evidence));

        List<EvidenceDTO> result = evidenceService.getByStep("step-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStepId()).isEqualTo("step-1");
    }

    // ─── submit ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("submit: usa WorkerTaskFactory cuando hay assignedWorkerId")
    void submit_withWorker_usesWorkerTaskFactory() {
        EvidenceDTO req = EvidenceDTO.builder()
                .projectId("proj-1").stepId("step-1")
                .name("Tarea albañil").submittedBy("supervisor1")
                .assignedWorkerId("worker-uuid").assignedWorkerName("Juan Obrero")
                .build();

        when(stepRepository.findById("step-1")).thenReturn(Optional.of(step));
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(workerTaskFactory.createEvidence(req, project, step)).thenReturn(evidence);
        when(evidenceRepository.save(evidence)).thenReturn(evidence);
        when(evidenceRepository.findByStep_StepId("step-1")).thenReturn(List.of(evidence));
        when(stepRepository.save(step)).thenReturn(step);
        when(projectRepository.save(project)).thenReturn(project);

        EvidenceDTO result = evidenceService.submit(req);

        assertThat(result).isNotNull();
        verify(workerTaskFactory).createEvidence(req, project, step);
        verify(directSubmissionFactory, never()).createEvidence(any(), any(), any());
    }

    @Test
    @DisplayName("submit: usa DirectSubmissionFactory cuando no hay assignedWorkerId")
    void submit_withoutWorker_usesDirectSubmissionFactory() {
        EvidenceDTO req = EvidenceDTO.builder()
                .projectId("proj-1").stepId("step-1")
                .name("Foto directa").submittedBy("supervisor1")
                .build();

        Evidence directEvidence = Evidence.builder()
                .evidenceId("ev-2").project(project).step(step)
                .name("Foto directa").submittedBy("supervisor1")
                .status(EvidenceStatus.PENDING).build();

        when(stepRepository.findById("step-1")).thenReturn(Optional.of(step));
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(directSubmissionFactory.createEvidence(req, project, step)).thenReturn(directEvidence);
        when(evidenceRepository.save(directEvidence)).thenReturn(directEvidence);
        when(evidenceRepository.findByStep_StepId("step-1")).thenReturn(List.of(directEvidence));
        when(stepRepository.save(step)).thenReturn(step);
        when(projectRepository.save(project)).thenReturn(project);

        EvidenceDTO result = evidenceService.submit(req);

        assertThat(result).isNotNull();
        verify(directSubmissionFactory).createEvidence(req, project, step);
        verify(workerTaskFactory, never()).createEvidence(any(), any(), any());
    }

    @Test
    @DisplayName("submit: lanza excepción si el paso no existe")
    void submit_stepNotFound_throwsException() {
        EvidenceDTO req = EvidenceDTO.builder()
                .projectId("proj-1").stepId("step-xxx").name("Foto").submittedBy("s1").build();
        when(stepRepository.findById("step-xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evidenceService.submit(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Paso no encontrado");
    }

    @Test
    @DisplayName("submit: lanza excepción si el proyecto referenciado no existe")
    void submit_projectNotFound_throwsException() {
        EvidenceDTO req = EvidenceDTO.builder()
                .projectId("proj-xxx").stepId("step-1").name("Foto").submittedBy("s1").build();
        when(stepRepository.findById("step-1")).thenReturn(Optional.of(step));
        when(projectRepository.findById("proj-xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evidenceService.submit(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Proyecto no encontrado");
    }

    // ─── approve ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("approve: cambia estado a APPROVED, recalcula progreso y publica evento RabbitMQ")
    void approve_success_updatesStatusAndPublishesEvent() {
        when(evidenceRepository.findById("ev-1")).thenReturn(Optional.of(evidence));
        when(evidenceRepository.save(evidence)).thenReturn(evidence);
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(evidenceRepository.findByStep_StepId("step-1")).thenReturn(List.of(evidence));
        when(stepRepository.save(step)).thenReturn(step);
        when(projectRepository.save(project)).thenReturn(project);

        EvidenceDTO result = evidenceService.approve("ev-1", "supervisor1");

        assertThat(result.getStatus()).isEqualTo("APPROVED");
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.NOTIFICATIONS_EXCHANGE),
                eq(RabbitMQConfig.EVIDENCE_APPROVED_KEY),
                any(Object.class));
    }

    @Test
    @DisplayName("approve: lanza excepción si la evidencia no existe")
    void approve_notFound_throwsException() {
        when(evidenceRepository.findById("ev-xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evidenceService.approve("ev-xxx", "sup1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evidencia no encontrada");
    }

    // ─── reject ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("approve: el paso llega a 100% cuando todas las evidencias son APPROVED")
    void approve_allEvidencesApproved_markStepAsCompleted() {
        // 1 evidencia total → 1 aprobada → progreso = 100% → stepStatus = true
        evidence.setStatus(EvidenceStatus.APPROVED);
        when(evidenceRepository.findById("ev-1")).thenReturn(Optional.of(evidence));
        when(evidenceRepository.save(evidence)).thenReturn(evidence);
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(evidenceRepository.findByStep_StepId("step-1")).thenReturn(List.of(evidence));
        when(stepRepository.save(step)).thenReturn(step);
        when(projectRepository.save(project)).thenReturn(project);

        evidenceService.approve("ev-1", "sup1");

        assertThat(step.getProgressValue()).isEqualTo(100);
        assertThat(step.getStepStatus()).isTrue();
    }

    @Test
    @DisplayName("reject: cambia estado a REJECTED y recalcula progreso")
    void reject_success_updatesStatusToRejected() {
        when(evidenceRepository.findById("ev-1")).thenReturn(Optional.of(evidence));
        when(evidenceRepository.save(evidence)).thenReturn(evidence);
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(evidenceRepository.findByStep_StepId("step-1")).thenReturn(List.of(evidence));
        when(stepRepository.save(step)).thenReturn(step);
        when(projectRepository.save(project)).thenReturn(project);

        EvidenceDTO result = evidenceService.reject("ev-1", "supervisor1");

        assertThat(result.getStatus()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("reject: lanza excepción si la evidencia no existe")
    void reject_notFound_throwsException() {
        when(evidenceRepository.findById("ev-xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evidenceService.reject("ev-xxx", "sup1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evidencia no encontrada");
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: elimina la evidencia y recalcula el progreso del paso")
    void delete_success_deletesAndRecalculates() {
        when(evidenceRepository.findById("ev-1")).thenReturn(Optional.of(evidence));
        when(projectRepository.findById("proj-1")).thenReturn(Optional.of(project));
        when(evidenceRepository.findByStep_StepId("step-1")).thenReturn(List.of());
        when(stepRepository.save(step)).thenReturn(step);
        when(projectRepository.save(project)).thenReturn(project);

        evidenceService.delete("ev-1");

        verify(evidenceRepository).delete(evidence);
    }

    @Test
    @DisplayName("delete: lanza excepción si la evidencia no existe")
    void delete_notFound_throwsException() {
        when(evidenceRepository.findById("ev-xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> evidenceService.delete("ev-xxx"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── workerSubmit ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("workerSubmit: lanza excepción si la evidencia no tiene trabajador asignado")
    void workerSubmit_noWorkerAssigned_throwsException() {
        Evidence sinWorker = Evidence.builder()
                .evidenceId("ev-2").project(project).step(step)
                .name("Sin asignación").submittedBy("user1")
                .status(EvidenceStatus.PENDING).build();
        when(evidenceRepository.findById("ev-2")).thenReturn(Optional.of(sinWorker));

        assertThatThrownBy(() -> evidenceService.workerSubmit("ev-2", "url", "desc", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no tiene un trabajador asignado");
    }

    @Test
    @DisplayName("workerSubmit: lanza excepción si el trabajador ya subió la evidencia")
    void workerSubmit_alreadySubmitted_throwsException() {
        Evidence yaSubida = Evidence.builder()
                .evidenceId("ev-3").project(project).step(step)
                .name("Ya subida").submittedBy("user1")
                .assignedWorkerId("worker-uuid").assignedWorkerName("Juan")
                .evidenceUrl("https://s3.aws.com/existing.jpg")
                .status(EvidenceStatus.PENDING).build();
        when(evidenceRepository.findById("ev-3")).thenReturn(Optional.of(yaSubida));

        assertThatThrownBy(() -> evidenceService.workerSubmit("ev-3", "url", "desc", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya subió la evidencia");
    }

    @Test
    @DisplayName("workerSubmit: persiste URL y publica evento cuando no hay insumos")
    void workerSubmit_success_noInsumos() {
        when(evidenceRepository.findById("ev-1")).thenReturn(Optional.of(evidence));
        when(evidenceRepository.save(evidence)).thenReturn(evidence);

        EvidenceDTO result = evidenceService.workerSubmit("ev-1", "https://s3.img.jpg", "Desc ok", null);

        assertThat(result).isNotNull();
        verify(evidenceRepository).save(evidence);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("workerSubmit: descuenta inventario y publica evento cuando hay insumos")
    void workerSubmit_withInsumos_callsInventarioClientAndPublishesEvent() {
        InsumoUsadoDTO insumo = InsumoUsadoDTO.builder()
                .insumoId("insumo-1").nombre("Cemento").cantidad(5).build();

        when(evidenceRepository.findById("ev-1")).thenReturn(Optional.of(evidence));
        when(evidenceRepository.save(evidence)).thenReturn(evidence);
        // consumir() retorna Map, no es void — se usa thenReturn
        when(inventarioClient.consumir(anyString(), anyMap())).thenReturn(Map.of());

        EvidenceDTO result = evidenceService.workerSubmit(
                "ev-1", "https://s3.img.jpg", "Instalación completada", List.of(insumo));

        assertThat(result).isNotNull();
        verify(inventarioClient).consumir(eq("insumo-1"), anyMap());
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    @DisplayName("workerSubmit: lanza IllegalStateException cuando el inventario no tiene stock")
    void workerSubmit_insufficientStock_throwsIllegalStateException() {
        InsumoUsadoDTO insumo = InsumoUsadoDTO.builder()
                .insumoId("insumo-1").nombre("Acero").cantidad(100).build();

        // FeignException.Conflict no tiene constructor vacío → se crea con mock()
        feign.FeignException.Conflict conflictEx = mock(feign.FeignException.Conflict.class);

        when(evidenceRepository.findById("ev-1")).thenReturn(Optional.of(evidence));
        when(inventarioClient.consumir(anyString(), anyMap())).thenThrow(conflictEx);

        assertThatThrownBy(() -> evidenceService.workerSubmit(
                "ev-1", "https://s3.img.jpg", "desc", List.of(insumo)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stock insuficiente");
    }
}
