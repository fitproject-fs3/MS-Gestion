package com.fitproject.gestion.service;

import com.fitproject.gestion.dto.TaskAssignmentDTO;
import com.fitproject.gestion.model.TaskAssignment;
import com.fitproject.gestion.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de asignaciones de tareas de construcción a trabajadores.
 *
 * <p>Una {@link TaskAssignment} representa el vínculo entre un trabajador y un paso
 * de construcción. Sirve como punto de control del supervisor para delegar tareas
 * que los trabajadores deben completar y evidenciar fotográficamente. Su ciclo de
 * vida es: {@code PENDING → IN_PROGRESS → DONE}.</p>
 *
 * @see com.fitproject.gestion.controller.AssignmentController
 * @see com.fitproject.gestion.repository.AssignmentRepository
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;

    /**
     * Crea una nueva asignación de tarea para un trabajador en un paso de construcción.
     *
     * <p>La asignación inicia con estado {@code PENDING}.</p>
     *
     * @param body mapa con claves: {@code workerId}, {@code workerName},
     *             {@code stepId}, {@code stepName}, {@code projectId}
     * @return asignación creada como DTO
     */
    @Transactional
    public TaskAssignmentDTO create(Map<String, String> body) {
        TaskAssignment assignment = TaskAssignment.builder()
                .workerId(body.get("workerId"))
                .workerName(body.get("workerName"))
                .stepId(body.get("stepId"))
                .stepName(body.get("stepName"))
                .projectId(body.get("projectId"))
                .status("PENDING")
                .build();
        return toDTO(assignmentRepository.save(assignment));
    }

    /**
     * Obtiene todas las asignaciones de tareas de un trabajador específico.
     *
     * @param workerId identificador UUID del trabajador
     * @return lista de asignaciones del trabajador
     */
    @Transactional(readOnly = true)
    public List<TaskAssignmentDTO> getByWorker(String workerId) {
        return assignmentRepository.findByWorkerId(workerId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Obtiene todas las asignaciones de tareas vinculadas a un paso de construcción.
     *
     * @param stepId identificador UUID del paso de construcción
     * @return lista de asignaciones del paso
     */
    @Transactional(readOnly = true)
    public List<TaskAssignmentDTO> getByStep(String stepId) {
        return assignmentRepository.findByStepId(stepId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Actualiza el estado de una asignación de tarea.
     *
     * <p>El valor del estado se convierte a mayúsculas antes de persistirse.</p>
     *
     * @param assignmentId identificador UUID de la asignación
     * @param status       nuevo estado: {@code PENDING}, {@code IN_PROGRESS} o {@code DONE}
     * @return asignación con el estado actualizado
     * @throws IllegalArgumentException si la asignación no existe
     */
    @Transactional
    public TaskAssignmentDTO updateStatus(String assignmentId, String status) {
        TaskAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada: " + assignmentId));
        assignment.setStatus(status.toUpperCase());
        return toDTO(assignmentRepository.save(assignment));
    }

    /**
     * Elimina una asignación de tarea por su identificador.
     *
     * @param assignmentId identificador UUID de la asignación a eliminar
     * @throws IllegalArgumentException si la asignación no existe
     */
    @Transactional
    public void delete(String assignmentId) {
        if (!assignmentRepository.existsById(assignmentId))
            throw new IllegalArgumentException("Asignación no encontrada: " + assignmentId);
        assignmentRepository.deleteById(assignmentId);
    }

    /**
     * Convierte la entidad {@link TaskAssignment} a su representación DTO.
     *
     * @param a entidad JPA de la asignación de tarea
     * @return DTO listo para serializar hacia el cliente
     */
    private TaskAssignmentDTO toDTO(TaskAssignment a) {
        return TaskAssignmentDTO.builder()
                .assignmentId(a.getAssignmentId())
                .workerId(a.getWorkerId())
                .workerName(a.getWorkerName())
                .stepId(a.getStepId())
                .stepName(a.getStepName())
                .projectId(a.getProjectId())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
