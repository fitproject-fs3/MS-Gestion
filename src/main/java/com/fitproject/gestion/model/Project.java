package com.fitproject.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Project {

    @Id
    @Column(name = "project_id", nullable = false, unique = true)
    private String projectId;

    @Column(name = "container_id")
    private String containerId;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "budget")
    private Double budget;

    @Column(name = "supervisor_id")
    private String supervisorId;

    @Column(name = "supervisor_name")
    private String supervisorName;

    @Column(name = "overall_progress", nullable = false)
    @Builder.Default
    private Integer overallProgress = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private List<ConstructionStep> constructionSteps = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private List<Evidence> evidences = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (projectId == null) projectId = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void recalculateProgress() {
        if (constructionSteps == null || constructionSteps.isEmpty()) {
            overallProgress = 0;
            return;
        }
        double avg = constructionSteps.stream()
                .mapToInt(ConstructionStep::getProgressValue)
                .average()
                .orElse(0);
        overallProgress = (int) Math.round(avg);
    }
}
