package com.fitproject.gestion.controller;

import com.fitproject.gestion.dto.*;
import com.fitproject.gestion.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectDTO>> getAll() {
        log.info("GET /api/v1/projects");
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> getById(@PathVariable String projectId) {
        log.info("GET /api/v1/projects/{}", projectId);
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjectDTO> create(@RequestBody CreateProjectRequest req) {
        log.info("POST /api/v1/projects modelName={}", req.getModelName());
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(req));
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<ProjectDTO> update(
            @PathVariable String projectId,
            @RequestBody UpdateProjectRequest req) {
        log.info("PATCH /api/v1/projects/{}", projectId);
        return ResponseEntity.ok(projectService.updateProject(projectId, req));
    }
}
