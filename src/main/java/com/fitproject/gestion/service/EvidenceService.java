package com.fitproject.gestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitproject.gestion.client.InventarioClient;
import com.fitproject.gestion.dto.EvidenceDTO;
import com.fitproject.gestion.dto.InsumoUsadoDTO;
import com.fitproject.gestion.config.RabbitMQConfig;
import com.fitproject.gestion.factory.DirectSubmissionEvidenceFactory;
import com.fitproject.gestion.factory.EvidenceFactory;
import com.fitproject.gestion.factory.WorkerTaskEvidenceFactory;
import com.fitproject.gestion.messaging.NotificationEvent;
import com.fitproject.gestion.model.*;
import com.fitproject.gestion.repository.*;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de negocio central para la gestión de evidencias fotográficas en MS-Gestion.
 *
 * <p>Implementa el ciclo de vida completo de una {@link Evidence}: creación mediante
 * el patrón Factory Method, subida por trabajador, aprobación/rechazo por supervisor
 * y eliminación. Cada mutación recalcula automáticamente el progreso del
 * {@link ConstructionStep} y del {@link Project} padre.</p>
 *
 * <p>Al aprobar una evidencia, publica el evento {@code evidence.approved} en el
 * exchange {@code fit.notifications} de RabbitMQ para que MS-Notificaciones
 * envíe el email de confirmación al trabajador.</p>
 *
 * @see com.fitproject.gestion.factory.EvidenceFactory
 * @see com.fitproject.gestion.controller.EvidenceController
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final StepRepository stepRepository;
    private final ProjectRepository projectRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InventarioClient inventarioClient;
    private final DirectSubmissionEvidenceFactory directSubmissionFactory;
    private final WorkerTaskEvidenceFactory workerTaskFactory;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Retorna todas las evidencias asociadas a un paso de construcción.
     *
     * @param stepId identificador UUID del paso de construcción
     * @return lista de evidencias del paso, vacía si no existen
     */
    @Transactional(readOnly = true)
    public List<EvidenceDTO> getByStep(String stepId) {
        return evidenceRepository.findByStep_StepId(stepId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Retorna evidencias en estado {@code PENDING} con paginación para evitar
     * cargar toda la tabla en memoria (Green Computing).
     *
     * @param page número de página, base cero
     * @param size cantidad máxima de registros por página
     * @return lista paginada de evidencias pendientes de aprobación
     */
    @Transactional(readOnly = true)
    public List<EvidenceDTO> getPending(int page, int size) {
        return evidenceRepository
                .findByStatus(EvidenceStatus.PENDING, PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retorna evidencias asignadas a un trabajador con paginación para optimizar
     * el consumo de RAM y CPU del servidor (Green Computing).
     *
     * @param workerId identificador UUID del trabajador
     * @param page     número de página, base cero
     * @param size     cantidad máxima de registros por página
     * @return lista paginada de evidencias del trabajador
     */
    @Transactional(readOnly = true)
    public List<EvidenceDTO> getByWorker(String workerId, int page, int size) {
        return evidenceRepository
                .findByAssignedWorkerId(workerId, PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Crea una nueva evidencia usando el patrón Factory Method para seleccionar
     * la fábrica apropiada en tiempo de ejecución.
     *
     * <p>Si {@code req.assignedWorkerId} no es nulo ni vacío, se usa
     * {@link com.fitproject.gestion.factory.WorkerTaskEvidenceFactory} (asignación de tarea);
     * de lo contrario se usa {@link com.fitproject.gestion.factory.DirectSubmissionEvidenceFactory}
     * (subida directa). El progreso del paso se recalcula tras la creación.</p>
     *
     * @param req DTO con los datos de la evidencia a crear
     * @return evidencia persistida como DTO
     * @throws IllegalArgumentException si el paso o el proyecto referenciado no existen
     */
    @Transactional
    public EvidenceDTO submit(EvidenceDTO req) {
        ConstructionStep step = stepRepository.findById(req.getStepId())
                .orElseThrow(() -> new IllegalArgumentException("Paso no encontrado: " + req.getStepId()));
        Project project = projectRepository.findById(req.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + req.getProjectId()));

        boolean isTaskAssignment = req.getAssignedWorkerId() != null && !req.getAssignedWorkerId().isBlank();
        EvidenceFactory factory = isTaskAssignment ? workerTaskFactory : directSubmissionFactory;
        Evidence evidence = factory.createEvidence(req, project, step);

        Evidence saved = evidenceRepository.save(evidence);
        recalculateStepProgress(step, project);
        return toDTO(saved);
    }

    /**
     * Called exclusively when the assigned WORKER uploads their evidence photo.
     * This is the ONLY method that publishes WorkerEvidenceSubmittedEvent.
     * submit() does NOT publish any event, so supervisor task assignments are silent.
     * The BFF enforces role=TRABAJADOR before this method is reached.
     */
    @Transactional
    public EvidenceDTO workerSubmit(String evidenceId, String evidenceUrl, String description,
                                    List<InsumoUsadoDTO> insumosUsados) {
        Evidence evidence = findById(evidenceId);
        if (evidence.getAssignedWorkerId() == null || evidence.getAssignedWorkerId().isBlank()) {
            throw new IllegalArgumentException("Esta evidencia no tiene un trabajador asignado");
        }
        if (evidence.getEvidenceUrl() != null && !evidence.getEvidenceUrl().isBlank()) {
            throw new IllegalArgumentException("El trabajador ya subió la evidencia para esta tarea");
        }

        // Deduct stock in MS-Inventario before saving. If any insumo has insufficient stock,
        // MS-Inventario returns 409 and Feign throws FeignException — we catch and re-throw
        // as IllegalStateException so GlobalExceptionHandler maps it to 409 to the frontend.
        if (insumosUsados != null && !insumosUsados.isEmpty()) {
            for (InsumoUsadoDTO insumo : insumosUsados) {
                try {
                    inventarioClient.consumir(insumo.getInsumoId(), Map.of(
                        "cantidad",    insumo.getCantidad(),
                        "referencia",  evidenceId,
                        "realizadoPor", evidence.getAssignedWorkerId()
                    ));
                } catch (FeignException.Conflict ex) {
                    // 409 from MS-Inventario → stock insuficiente
                    throw new IllegalStateException(
                        "Stock insuficiente para el insumo '" + insumo.getNombre() + "'. " +
                        "Verifica las cantidades e inténtalo de nuevo.");
                } catch (FeignException ex) {
                    throw new IllegalStateException(
                        "Error al descontar inventario para '" + insumo.getNombre() + "': " + ex.getMessage());
                }
            }
        }

        evidence.setEvidenceUrl(evidenceUrl != null ? evidenceUrl : "");
        if (description != null && !description.isBlank()) {
            evidence.setDescription(description);
        }
        if (insumosUsados != null && !insumosUsados.isEmpty()) {
            try {
                evidence.setInsumosUsados(objectMapper.writeValueAsString(insumosUsados));
            } catch (JsonProcessingException ignored) { /* non-fatal */ }
        }
        evidenceRepository.save(evidence);

        // Event is picked up by EvidenceNotificationListener with @TransactionalEventListener(AFTER_COMMIT)
        eventPublisher.publishEvent(new WorkerEvidenceSubmittedEvent(
                this,
                evidence.getEvidenceId(),
                evidence.getName(),
                evidence.getAssignedWorkerName(),
                evidence.getProject().getProjectId(),
                evidence.getStep().getStepId()
        ));

        return toDTO(evidence);
    }

    /**
     * Aprueba una evidencia, recalcula el progreso del paso y publica el evento
     * {@code evidence.approved} en RabbitMQ para notificar al trabajador por email.
     *
     * @param evidenceId   identificador UUID de la evidencia a aprobar
     * @param supervisorId identificador del supervisor que aprueba
     * @return evidencia actualizada con estado {@code APPROVED}
     * @throws IllegalArgumentException si la evidencia o su proyecto no existen
     */
    @Transactional
    public EvidenceDTO approve(String evidenceId, String supervisorId) {
        Evidence evidence = findById(evidenceId);
        evidence.setStatus(EvidenceStatus.APPROVED);
        evidence.setSupervisorId(supervisorId);
        evidenceRepository.save(evidence);
        ConstructionStep step = evidence.getStep();
        Project project = projectRepository.findById(step.getProject().getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        recalculateStepProgress(step, project);

        publishEvidenceApprovedEvent(evidence, project);

        return toDTO(evidence);
    }

    /**
     * Publica un evento {@code evidence.approved} en el Exchange de RabbitMQ.
     * MS-Notificaciones consume este evento y envía el email al trabajador.
     *
     * <p>El email se deriva del identificador {@code submittedBy} ya que Evidence
     * no almacena el email directamente. En producción se reemplazaría por una
     * consulta a MS-Users para obtener el email real del trabajador.</p>
     *
     * @param evidence evidencia recién aprobada
     * @param project  proyecto al que pertenece la evidencia
     */
    private void publishEvidenceApprovedEvent(Evidence evidence, Project project) {
        String recipientName = evidence.getAssignedWorkerName() != null
                ? evidence.getAssignedWorkerName()
                : evidence.getSubmittedBy();
        NotificationEvent event = new NotificationEvent(
                evidence.getSubmittedBy() + "@fitproject.com",
                recipientName,
                "Evidencia aprobada: " + evidence.getName(),
                "Tu evidencia '" + evidence.getName() + "' en el proyecto '"
                        + project.getModelName() + "' ha sido aprobada por el supervisor.",
                "EVIDENCE_APPROVED"
        );
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.NOTIFICATIONS_EXCHANGE,
                    RabbitMQConfig.EVIDENCE_APPROVED_KEY,
                    event
            );
            log.info("[RabbitMQ] Evento EVIDENCE_APPROVED publicado para evidencia {}", evidence.getEvidenceId());
        } catch (Exception ex) {
            log.error("[RabbitMQ] Error al publicar EVIDENCE_APPROVED para {}: {}", evidence.getEvidenceId(), ex.getMessage());
        }
    }

    /**
     * Rechaza una evidencia y recalcula el progreso del paso padre.
     *
     * @param evidenceId   identificador UUID de la evidencia a rechazar
     * @param supervisorId identificador del supervisor que rechaza
     * @return evidencia actualizada con estado {@code REJECTED}
     * @throws IllegalArgumentException si la evidencia o su proyecto no existen
     */
    @Transactional
    public EvidenceDTO reject(String evidenceId, String supervisorId) {
        Evidence evidence = findById(evidenceId);
        evidence.setStatus(EvidenceStatus.REJECTED);
        evidence.setSupervisorId(supervisorId);
        evidenceRepository.save(evidence);
        ConstructionStep step = evidence.getStep();
        Project project = projectRepository.findById(step.getProject().getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        recalculateStepProgress(step, project);
        return toDTO(evidence);
    }

    /**
     * Elimina una evidencia y recalcula el progreso del paso y proyecto asociados.
     *
     * @param evidenceId identificador UUID de la evidencia a eliminar
     * @throws IllegalArgumentException si la evidencia o su proyecto no existen
     */
    @Transactional
    public void delete(String evidenceId) {
        Evidence evidence = findById(evidenceId);
        ConstructionStep step = evidence.getStep();
        Project project = projectRepository.findById(step.getProject().getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        evidenceRepository.delete(evidence);
        recalculateStepProgress(step, project);
    }

    private void recalculateStepProgress(ConstructionStep step, Project project) {
        List<Evidence> all = evidenceRepository.findByStep_StepId(step.getStepId());
        long total    = all.size();
        long approved = all.stream().filter(e -> e.getStatus() == EvidenceStatus.APPROVED).count();
        int progress  = total > 0 ? (int) Math.round(approved * 100.0 / total) : 0;
        step.setProgressValue(progress);
        step.setStepStatus(progress >= 100);
        stepRepository.save(step);

        project.recalculateProgress();
        projectRepository.save(project);
    }

    private Evidence findById(String evidenceId) {
        return evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new IllegalArgumentException("Evidencia no encontrada: " + evidenceId));
    }

    /**
     * Convierte la entidad {@link Evidence} a su representación DTO para la API.
     * Deserializa el campo JSON {@code insumosUsados} si está presente.
     *
     * @param e entidad JPA de la evidencia
     * @return DTO con todos los campos mapeados, listo para serializar hacia el cliente
     */
    public EvidenceDTO toDTO(Evidence e) {
        List<InsumoUsadoDTO> insumosList = null;
        if (e.getInsumosUsados() != null && !e.getInsumosUsados().isBlank()) {
            try {
                insumosList = objectMapper.readValue(e.getInsumosUsados(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, InsumoUsadoDTO.class));
            } catch (JsonProcessingException ignored) { /* return null list on malformed JSON */ }
        }
        return EvidenceDTO.builder()
                .evidenceId(e.getEvidenceId())
                .projectId(e.getProject() != null ? e.getProject().getProjectId() : null)
                .stepId(e.getStep() != null ? e.getStep().getStepId() : null)
                .evidenceUrl(e.getEvidenceUrl())
                .description(e.getDescription())
                .name(e.getName())
                .submittedBy(e.getSubmittedBy())
                .supervisorId(e.getSupervisorId())
                .assignedWorkerId(e.getAssignedWorkerId())
                .assignedWorkerName(e.getAssignedWorkerName())
                .status(e.getStatus().name())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .insumosUsados(insumosList)
                .build();
    }
}
