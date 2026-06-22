package com.fitproject.gestion.controller;

import com.fitproject.gestion.dto.*;
import com.fitproject.gestion.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para la gestión de proyectos de fabricación de gimnasios en contenedor.
 *
 * <p>Un proyecto representa una unidad de fabricación desde su creación hasta la
 * entrega final, compuesto por pasos de construcción y evidencias fotográficas.
 * Al crear un proyecto se generan automáticamente los 4 pasos de construcción estándar.</p>
 *
 * <p>Base URL: {@code /api/v1/projects}</p>
 *
 * @see ProjectService
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

    /**
     * Obtiene proyectos con paginación para evitar cargar toda la tabla en memoria
     * cuando el número de proyectos activos escale (Green Computing).
     *
     * @param page número de página, base cero (default: 0)
     * @param size cantidad máxima de proyectos por página (default: 20)
     * @return lista paginada de proyectos con pasos y evidencias anidadas
     */
    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/v1/projects page={} size={}", page, size);
        return ResponseEntity.ok(projectService.getAllProjects(page, size));
    }

    /**
     * Obtiene un proyecto por su identificador único.
     *
     * @param projectId identificador UUID del proyecto
     * @return datos completos del proyecto incluyendo pasos y evidencias
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> getById(@PathVariable String projectId) {
        log.info("GET /api/v1/projects/{}", projectId);
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }

    /**
     * Crea un nuevo proyecto de fabricación.
     *
     * <p>Al persistir el proyecto se auto-crean los 4 pasos de construcción por defecto
     * definidos en {@link com.fitproject.gestion.config.DataSeeder#DEFAULT_STEP_NAMES}.</p>
     *
     * @param req datos del proyecto a crear (nombre del modelo, descripción, supervisor, etc.)
     * @return proyecto creado con status 201 Created
     */
    @PostMapping
    public ResponseEntity<ProjectDTO> create(@RequestBody CreateProjectRequest req) {
        log.info("POST /api/v1/projects modelName={}", req.getModelName());
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(req));
    }

    /**
     * Actualiza los campos editables de un proyecto existente.
     *
     * <p>Solo se actualizan los campos presentes en el request (patch semántico);
     * los campos {@code null} se ignoran.</p>
     *
     * @param projectId identificador UUID del proyecto a actualizar
     * @param req       campos a modificar (descripción, imagen, presupuesto, supervisor)
     * @return proyecto actualizado con los nuevos valores
     */
    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> update(
            @PathVariable String projectId,
            @RequestBody UpdateProjectRequest req) {
        log.info("PATCH /api/v1/projects/{}", projectId);
        return ResponseEntity.ok(projectService.updateProject(projectId, req));
    }
}
