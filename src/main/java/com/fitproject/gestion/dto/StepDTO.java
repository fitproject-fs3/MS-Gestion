package com.fitproject.gestion.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class StepDTO {
    private String stepId;
    private String projectId;
    private String stepName;
    private Boolean stepStatus;
    private Integer progressValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<EvidenceDTO> evidences;
}
