package com.fitproject.gestion.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class EvidenceDTO {
    private String evidenceId;
    private String projectId;
    private String stepId;
    private String evidenceUrl;
    private String description;
    private String name;
    private String submittedBy;
    private String supervisorId;
    private String assignedWorkerId;
    private String assignedWorkerName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<InsumoUsadoDTO> insumosUsados;
}
