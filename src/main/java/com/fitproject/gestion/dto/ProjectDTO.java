package com.fitproject.gestion.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectDTO {
    private String projectId;
    private String containerId;
    private String modelName;
    private String description;
    private String imageUrl;
    private Double budget;
    private String supervisorId;
    private String supervisorName;
    private Integer overallProgress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StepDTO> constructionSteps;
    private List<EvidenceDTO> evidences;
}
