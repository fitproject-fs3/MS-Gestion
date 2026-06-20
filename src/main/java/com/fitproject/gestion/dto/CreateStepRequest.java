package com.fitproject.gestion.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateStepRequest {
    private String projectId;
    private String stepName;
}
