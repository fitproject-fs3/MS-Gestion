package com.fitproject.gestion.controller;

import com.fitproject.gestion.dto.TaskAssignmentDTO;
import com.fitproject.gestion.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<TaskAssignmentDTO> create(@RequestBody Map<String, String> body) {
        log.info("POST /api/v1/assignments workerId={}", body.get("workerId"));
        return ResponseEntity.ok(assignmentService.create(body));
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<TaskAssignmentDTO>> getByWorker(@PathVariable String workerId) {
        return ResponseEntity.ok(assignmentService.getByWorker(workerId));
    }

    @GetMapping("/step/{stepId}")
    public ResponseEntity<List<TaskAssignmentDTO>> getByStep(@PathVariable String stepId) {
        return ResponseEntity.ok(assignmentService.getByStep(stepId));
    }

    @PatchMapping("/{assignmentId}/status")
    public ResponseEntity<TaskAssignmentDTO> updateStatus(
            @PathVariable String assignmentId,
            @RequestParam String status) {
        return ResponseEntity.ok(assignmentService.updateStatus(assignmentId, status));
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> delete(@PathVariable String assignmentId) {
        assignmentService.delete(assignmentId);
        return ResponseEntity.noContent().build();
    }
}
