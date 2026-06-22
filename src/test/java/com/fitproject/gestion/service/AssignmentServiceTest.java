package com.fitproject.gestion.service;

import com.fitproject.gestion.dto.TaskAssignmentDTO;
import com.fitproject.gestion.model.TaskAssignment;
import com.fitproject.gestion.repository.AssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link AssignmentService}.
 */
@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock private AssignmentRepository assignmentRepository;

    @InjectMocks
    private AssignmentService assignmentService;

    private TaskAssignment assignment;

    @BeforeEach
    void setUp() {
        assignment = TaskAssignment.builder()
                .assignmentId("asgn-1")
                .workerId("worker-1").workerName("Pedro Obrero")
                .stepId("step-1").stepName("Estructura")
                .projectId("proj-1").status("PENDING")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create: persiste la asignación con estado PENDING")
    void create_success_persistsWithPendingStatus() {
        Map<String, String> body = Map.of(
                "workerId", "worker-1", "workerName", "Pedro Obrero",
                "stepId", "step-1", "stepName", "Estructura",
                "projectId", "proj-1"
        );
        when(assignmentRepository.save(any(TaskAssignment.class))).thenReturn(assignment);

        TaskAssignmentDTO result = assignmentService.create(body);

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getWorkerId()).isEqualTo("worker-1");
        verify(assignmentRepository).save(any(TaskAssignment.class));
    }

    // ─── getByWorker ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByWorker: retorna asignaciones del trabajador")
    void getByWorker_returnsAssignments() {
        when(assignmentRepository.findByWorkerId("worker-1")).thenReturn(List.of(assignment));

        List<TaskAssignmentDTO> result = assignmentService.getByWorker("worker-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWorkerName()).isEqualTo("Pedro Obrero");
    }

    // ─── getByStep ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByStep: retorna asignaciones del paso")
    void getByStep_returnsAssignments() {
        when(assignmentRepository.findByStepId("step-1")).thenReturn(List.of(assignment));

        List<TaskAssignmentDTO> result = assignmentService.getByStep("step-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStepId()).isEqualTo("step-1");
    }

    // ─── updateStatus ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus: cambia el estado de la asignación a IN_PROGRESS")
    void updateStatus_success_changesStatus() {
        when(assignmentRepository.findById("asgn-1")).thenReturn(Optional.of(assignment));
        when(assignmentRepository.save(assignment)).thenReturn(assignment);

        TaskAssignmentDTO result = assignmentService.updateStatus("asgn-1", "in_progress");

        assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("updateStatus: lanza excepción si la asignación no existe")
    void updateStatus_notFound_throwsException() {
        when(assignmentRepository.findById("xxx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assignmentService.updateStatus("xxx", "DONE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Asignación no encontrada");
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete: elimina la asignación existente")
    void delete_success_deletesAssignment() {
        when(assignmentRepository.existsById("asgn-1")).thenReturn(true);

        assignmentService.delete("asgn-1");

        verify(assignmentRepository).deleteById("asgn-1");
    }

    @Test
    @DisplayName("delete: lanza excepción si la asignación no existe")
    void delete_notFound_throwsException() {
        when(assignmentRepository.existsById("xxx")).thenReturn(false);

        assertThatThrownBy(() -> assignmentService.delete("xxx"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Asignación no encontrada");
    }
}
