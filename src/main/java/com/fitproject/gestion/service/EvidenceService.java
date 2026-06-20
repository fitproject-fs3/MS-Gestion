package com.fitproject.gestion.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitproject.gestion.client.InventarioClient;
import com.fitproject.gestion.dto.EvidenceDTO;
import com.fitproject.gestion.dto.InsumoUsadoDTO;
import com.fitproject.gestion.model.*;
import com.fitproject.gestion.repository.*;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final StepRepository stepRepository;
    private final ProjectRepository projectRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final InventarioClient inventarioClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public List<EvidenceDTO> getByStep(String stepId) {
        return evidenceRepository.findByStep_StepId(stepId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvidenceDTO> getPending() {
        return evidenceRepository.findByStatus(EvidenceStatus.PENDING).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvidenceDTO> getByWorker(String workerId) {
        return evidenceRepository.findByAssignedWorkerId(workerId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public EvidenceDTO submit(EvidenceDTO req) {
        ConstructionStep step = stepRepository.findById(req.getStepId())
                .orElseThrow(() -> new IllegalArgumentException("Paso no encontrado: " + req.getStepId()));
        Project project = projectRepository.findById(req.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado: " + req.getProjectId()));

        boolean isTaskAssignment = req.getAssignedWorkerId() != null && !req.getAssignedWorkerId().isBlank();
        EvidenceStatus initialStatus = EvidenceStatus.PENDING; // DB constraint only allows PENDING/APPROVED/REJECTED

        Evidence evidence = Evidence.builder()
                .project(project)
                .step(step)
                .evidenceUrl(req.getEvidenceUrl() != null ? req.getEvidenceUrl() : "")
                .description(req.getDescription() != null ? req.getDescription() : "")
                .name(req.getName())
                .submittedBy(req.getSubmittedBy())
                .assignedWorkerId(isTaskAssignment ? req.getAssignedWorkerId() : null)
                .assignedWorkerName(isTaskAssignment ? req.getAssignedWorkerName() : null)
                .status(initialStatus)
                .build();

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
        return toDTO(evidence);
    }

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
