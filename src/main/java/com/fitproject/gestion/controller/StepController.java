package com.fitproject.gestion.controller;

import com.fitproject.gestion.dto.CreateStepRequest;
import com.fitproject.gestion.dto.StepDTO;
import com.fitproject.gestion.service.StepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para la gestión de pasos de construcción de un proyecto.
 *
 * <p>Un {@link com.fitproject.gestion.model.ConstructionStep} representa una fase
 * del proceso de fabricación (ej: Estructura, Instalaciones, Acabados). Su progreso
 * se calcula a partir de las evidencias aprobadas: cada aprobación suma un porcentaje
 * proporcional hasta alcanzar el 100% y marcar el paso como completado.</p>
 *
 * <p>Base URL: {@code /api/v1/steps}</p>
 *
 * @see StepService
 */
@RestController
@RequestMapping("/api/v1/steps")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class StepController {

    private final StepService stepService;

    /**
     * Obtiene todos los pasos de construcción asociados a un proyecto.
     *
     * @param projectId identificador UUID del proyecto
     * @return lista de pasos del proyecto ordenados por fecha de creación
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<StepDTO>> getByProject(@PathVariable String projectId) {
        return ResponseEntity.ok(stepService.getByProject(projectId));
    }

    /**
     * Crea un nuevo paso de construcción personalizado para un proyecto.
     *
     * <p>Complementa los 4 pasos generados automáticamente al crear el proyecto.
     * El paso inicia con {@code progressValue = 0} y {@code stepStatus = false}.</p>
     *
     * @param req datos del paso a crear (projectId y nombre del paso)
     * @return paso de construcción persistido con status 201 Created
     */
    @PostMapping
    public ResponseEntity<StepDTO> create(@RequestBody CreateStepRequest req) {
        log.info("POST /api/v1/steps projectId={} name={}", req.getProjectId(), req.getStepName());
        return ResponseEntity.status(HttpStatus.CREATED).body(stepService.create(req));
    }

    /**
     * Renombra un paso de construcción existente.
     *
     * @param stepId identificador UUID del paso a renombrar
     * @param body   mapa con clave {@code stepName} y el nuevo nombre
     * @return paso actualizado con el nuevo nombre
     */
    @PostMapping("/{stepId}/name")
    public ResponseEntity<StepDTO> rename(
            @PathVariable String stepId,
            @RequestBody Map<String, String> body) {
        String newName = body.get("stepName");
        log.info("POST /api/v1/steps/{}/name -> {}", stepId, newName);
        return ResponseEntity.ok(stepService.rename(stepId, newName));
    }
}
