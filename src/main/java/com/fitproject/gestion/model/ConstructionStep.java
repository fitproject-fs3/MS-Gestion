package com.fitproject.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "construction_steps")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConstructionStep {

    @Id
    @Column(name = "step_id", nullable = false, unique = true)
    private String stepId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Column(name = "step_status", nullable = false)
    @Builder.Default
    private Boolean stepStatus = false;

    @Column(name = "progress_value", nullable = false)
    @Builder.Default
    private Integer progressValue = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "step", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private List<Evidence> evidences = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (stepId == null) stepId = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void applyApproval() {
        progressValue = Math.min(100, progressValue + 20);
        if (progressValue >= 100) {
            stepStatus = true;
        }
        updatedAt = LocalDateTime.now();
    }
}
