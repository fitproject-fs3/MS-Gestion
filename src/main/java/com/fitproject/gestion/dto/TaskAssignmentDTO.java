package com.fitproject.gestion.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskAssignmentDTO {
    private String assignmentId;
    private String workerId;
    private String workerName;
    private String stepId;
    private String stepName;
    private String projectId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
