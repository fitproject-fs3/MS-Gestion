package com.fitproject.gestion.controller;

import com.fitproject.gestion.dto.EvidenceDTO;
import com.fitproject.gestion.service.EvidenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evidences")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EvidenceController {

    private final EvidenceService evidenceService;

    @GetMapping("/step/{stepId}")
    public ResponseEntity<List<EvidenceDTO>> getByStep(@PathVariable String stepId) {
        log.info("GET /api/v1/evidences/step/{}", stepId);
        return ResponseEntity.ok(evidenceService.getByStep(stepId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<EvidenceDTO>> getPending() {
        log.info("GET /api/v1/evidences/pending");
        return ResponseEntity.ok(evidenceService.getPending());
    }

    @GetMapping("/worker/{workerId}")
    public ResponseEntity<List<EvidenceDTO>> getByWorker(@PathVariable String workerId) {
        log.info("GET /api/v1/evidences/worker/{}", workerId);
        return ResponseEntity.ok(evidenceService.getByWorker(workerId));
    }

    @PostMapping("/submit")
    public ResponseEntity<EvidenceDTO> submit(@RequestBody EvidenceDTO req) {
        log.info("POST /api/v1/evidences/submit stepId={}", req.getStepId());
        return ResponseEntity.ok(evidenceService.submit(req));
    }

    @PostMapping("/{evidenceId}/approve")
    public ResponseEntity<EvidenceDTO> approve(
            @PathVariable String evidenceId,
            @RequestParam(required = false, defaultValue = "") String supervisorId) {
        log.info("POST /api/v1/evidences/{}/approve", evidenceId);
        return ResponseEntity.ok(evidenceService.approve(evidenceId, supervisorId));
    }

    @PostMapping("/{evidenceId}/reject")
    public ResponseEntity<EvidenceDTO> reject(
            @PathVariable String evidenceId,
            @RequestParam(required = false, defaultValue = "") String supervisorId) {
        log.info("POST /api/v1/evidences/{}/reject", evidenceId);
        return ResponseEntity.ok(evidenceService.reject(evidenceId, supervisorId));
    }

    @DeleteMapping("/{evidenceId}")
    public ResponseEntity<Void> delete(@PathVariable String evidenceId) {
        log.info("DELETE /api/v1/evidences/{}", evidenceId);
        evidenceService.delete(evidenceId);
        return ResponseEntity.noContent().build();
    }

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
