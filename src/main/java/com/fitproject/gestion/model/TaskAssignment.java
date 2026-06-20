package com.fitproject.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaskAssignment {

    @Id
    @Column(name = "assignment_id", nullable = false, unique = true)
    private String assignmentId;

    @Column(name = "worker_id", nullable = false)
    private String workerId;

    @Column(name = "worker_name")
    private String workerName;

    @Column(name = "step_id", nullable = false)
    private String stepId;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (assignmentId == null) assignmentId = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
