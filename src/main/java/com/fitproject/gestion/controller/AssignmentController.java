package com.fitproject.gestion.controller;

import com.fitproject.gestion.dto.TaskAssignmentDTO;
import com.fitproject.gestion.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para la gestión de asignaciones de tareas de construcción.
 *
 * <p>Una {@link com.fitproject.gestion.model.TaskAssignment} vincula a un trabajador
 * con un paso de construcción específico. Es el mecanismo mediante el cual el supervisor
 * delega tareas que los trabajadores deben evidenciar con fotografías. Su ciclo de vida
 * es: {@code PENDING → IN_PROGRESS → DONE}.</p>
 *
 * <p>Base URL: {@code /api/v1/assignments}</p>
 *
 * @see AssignmentService
 */
@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AssignmentController {

    private final AssignmentService assignmentService;

    /**
     * Crea una nueva asignación de tarea a un trabajador.
     *
     * @param body mapa con claves: {@code workerId}, {@code workerName},
     *             {@code stepId}, {@code stepName}, {@code projectId}
     * @return asignación creada con estado inicial {@code PENDING}
     */
    @PostMapping
    public ResponseEntity<TaskAssignmentDTO> create(@RequestBody Map<String, String> body) {
        log.info("POST /api/v1/assignments workerId={}", body.get("workerId"));
        return ResponseEntity.ok(assignmentService.create(body));
    }

    /**
     * Obtiene todas las asignaciones de tareas de un trabajador específico.
     *
     * @param workerId identificador UUID del trabajador
     * @return lista de asignaciones del trabajador
     */
    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<TaskAssignmentDTO>> getByWorker(@PathVariable String workerId) {
        return ResponseEntity.ok(assignmentService.getByWorker(workerId));
    }

    /**
     * Obtiene todas las asignaciones de tareas vinculadas a un paso de construcción.
     *
     * @param stepId identificador UUID del paso de construcción
     * @return lista de asignaciones del paso
     */
    @GetMapping("/step/{stepId}")
    public ResponseEntity<List<TaskAssignmentDTO>> getByStep(@PathVariable String stepId) {
        return ResponseEntity.ok(assignmentService.getByStep(stepId));
    }

    /**
     * Actualiza el estado de una asignación de tarea.
     *
     * @param assignmentId identificador UUID de la asignación
     * @param status       nuevo estado en mayúsculas: {@code PENDING}, {@code IN_PROGRESS} o {@code DONE}
     * @return asignación con el estado actualizado
     */
    @PatchMapping("/{assignmentId}/status")
    public ResponseEntity<TaskAssignmentDTO> updateStatus(
            @PathVariable String assignmentId,
            @RequestParam String status) {
        return ResponseEntity.ok(assignmentService.updateStatus(assignmentId, status));
    }

    /**
     * Elimina una asignación de tarea por su identificador.
     *
     * @param assignmentId identificador UUID de la asignación a eliminar
     * @return 204 No Content si la eliminación fue exitosa
     */
    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> delete(@PathVariable String assignmentId) {
        assignmentService.delete(assignmentId);
        return ResponseEntity.noContent().build();
    }
}
