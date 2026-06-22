package com.fitproject.gestion.controller;

import com.fitproject.gestion.dto.EvidenceDTO;
import com.fitproject.gestion.service.EvidenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para la gestión de evidencias fotográficas de proyectos de construcción.
 *
 * <p>Expone operaciones CRUD sobre {@link com.fitproject.gestion.model.Evidence},
 * cubriendo el flujo completo: creación, aprobación/rechazo por supervisor y
 * subida de foto por trabajador. Toda mutación de evidencia recalcula automáticamente
 * el progreso del paso y del proyecto padre.</p>
 *
 * <p>Base URL: {@code /api/v1/evidences}</p>
 *
 * @see EvidenceService
 */
@RestController
@RequestMapping("/api/v1/evidences")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EvidenceController {

    private final EvidenceService evidenceService;

    /**
     * Obtiene todas las evidencias asociadas a un paso de construcción específico.
     *
     * @param stepId identificador UUID del paso de construcción
     * @return lista de evidencias del paso, vacía si no existen
     */
    @GetMapping("/step/{stepId}")
    public ResponseEntity<List<EvidenceDTO>> getByStep(@PathVariable String stepId) {
        log.info("GET /api/v1/evidences/step/{}", stepId);
        return ResponseEntity.ok(evidenceService.getByStep(stepId));
    }

    /**
     * Lista evidencias en estado {@code PENDING} pendientes de aprobación con paginación.
     *
     * <p>La paginación evita cargar todo el historial de evidencias en memoria,
     * reduciendo el consumo de RAM y CPU del servidor (Green Computing).</p>
     *
     * @param page número de página, base cero (default: 0)
     * @param size cantidad máxima de registros por página (default: 20)
     * @return lista paginada de evidencias pendientes de revisión
     */
    @GetMapping("/pending")
    public ResponseEntity<List<EvidenceDTO>> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/v1/evidences/pending page={} size={}", page, size);
        return ResponseEntity.ok(evidenceService.getPending(page, size));
    }

    /**
     * Obtiene las evidencias asignadas a un trabajador con paginación.
     *
     * <p>La paginación evita cargar todo el historial del trabajador en memoria
     * cuando acumula muchas tareas (Green Computing).</p>
     *
     * @param workerId identificador UUID del trabajador
     * @param page     número de página, base cero (default: 0)
     * @param size     cantidad máxima de registros por página (default: 20)
     * @return lista paginada de evidencias asignadas al trabajador
     */
    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<EvidenceDTO>> getByWorker(
            @PathVariable String workerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/v1/evidences/worker/{} page={} size={}", workerId, page, size);
        return ResponseEntity.ok(evidenceService.getByWorker(workerId, page, size));
    }

    /**
     * Crea una nueva evidencia o asignación de tarea de construcción.
     *
     * <p>Si el DTO incluye {@code assignedWorkerId}, se delega a
     * {@link com.fitproject.gestion.factory.WorkerTaskEvidenceFactory};
     * de lo contrario se usa {@link com.fitproject.gestion.factory.DirectSubmissionEvidenceFactory}.</p>
     *
     * @param req datos de la evidencia a crear
     * @return evidencia persistida con su ID generado
     */
    @PostMapping("/submit")
    public ResponseEntity<EvidenceDTO> submit(@RequestBody EvidenceDTO req) {
        log.info("POST /api/v1/evidences/submit stepId={}", req.getStepId());
        return ResponseEntity.ok(evidenceService.submit(req));
    }

    /**
     * Aprueba una evidencia, suma progreso al paso y publica evento {@code evidence.approved}
     * en RabbitMQ para que MS-Notificaciones envíe el email al trabajador.
     *
     * @param evidenceId  identificador UUID de la evidencia a aprobar
     * @param supervisorId identificador del supervisor que aprueba (opcional)
     * @return evidencia actualizada con estado {@code APPROVED}
     */
    @PostMapping("/{evidenceId}/approve")
    public ResponseEntity<EvidenceDTO> approve(
            @PathVariable String evidenceId,
            @RequestParam(required = false, defaultValue = "") String supervisorId) {
        log.info("POST /api/v1/evidences/{}/approve", evidenceId);
        return ResponseEntity.ok(evidenceService.approve(evidenceId, supervisorId));
    }

    /**
     * Rechaza una evidencia, actualizando su estado a {@code REJECTED} y
     * recalculando el progreso del paso padre.
     *
     * @param evidenceId   identificador UUID de la evidencia a rechazar
     * @param supervisorId identificador del supervisor que rechaza (opcional)
     * @return evidencia actualizada con estado {@code REJECTED}
     */
    @PostMapping("/{evidenceId}/reject")
    public ResponseEntity<EvidenceDTO> reject(
            @PathVariable String evidenceId,
            @RequestParam(required = false, defaultValue = "") String supervisorId) {
        log.info("POST /api/v1/evidences/{}/reject", evidenceId);
        return ResponseEntity.ok(evidenceService.reject(evidenceId, supervisorId));
    }

    /**
     * Elimina una evidencia y recalcula el progreso del paso y proyecto asociados.
     *
     * @param evidenceId identificador UUID de la evidencia a eliminar
     * @return 204 No Content si la eliminación fue exitosa
     */
    @DeleteMapping("/{evidenceId}")
    public ResponseEntity<Void> delete(@PathVariable String evidenceId) {
        log.info("DELETE /api/v1/evidences/{}", evidenceId);
        evidenceService.delete(evidenceId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Permite al trabajador asignado subir su foto de evidencia y los insumos consumidos.
     *
     * <p>Solo puede ser llamado una vez por evidencia. El BFF valida que el
     * rol sea {@code TRABAJADOR} antes de enrutar a este endpoint.</p>
     *
     * @param evidenceId identificador UUID de la evidencia (tarea asignada)
     * @param req        DTO con la URL de la foto, descripción e insumos usados
     * @return evidencia actualizada con la URL de la foto y el inventario descontado
     */
    @PostMapping("/{evidenceId}/worker-submit")
    public ResponseEntity<EvidenceDTO> workerSubmit(
            @PathVariable String evidenceId,
            @RequestBody EvidenceDTO req) {
        log.info("POST /api/v1/evidences/{}/worker-submit insumosUsados={}", evidenceId,
                req.getInsumosUsados() != null ? req.getInsumosUsados().size() : 0);
        return ResponseEntity.ok(evidenceService.workerSubmit(
                evidenceId, req.getEvidenceUrl(), req.getDescription(), req.getInsumosUsados()));
    }
}
