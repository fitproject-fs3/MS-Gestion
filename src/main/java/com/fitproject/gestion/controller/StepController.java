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

@RestController
@RequestMapping("/api/v1/steps")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class StepController {

    private final StepService stepService;

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<StepDTO>> getByProject(@PathVariable String projectId) {
        return ResponseEntity.ok(stepService.getByProject(projectId));
    }

    @PostMapping
    public ResponseEntity<StepDTO> create(@RequestBody CreateStepRequest req) {
        log.info("POST /api/v1/steps projectId={} name={}", req.getProjectId(), req.getStepName());
        return ResponseEntity.status(HttpStatus.CREATED).body(stepService.create(req));
    }

    @PostMapping("/{stepId}/name")
    public ResponseEntity<StepDTO> rename(
            @PathVariable String stepId,
            @RequestBody Map<String, String> body) {
        String newName = body.get("stepName");
        log.info("POST /api/v1/steps/{}/name -> {}", stepId, newName);
        return ResponseEntity.ok(stepService.rename(stepId, newName));
    }
}
