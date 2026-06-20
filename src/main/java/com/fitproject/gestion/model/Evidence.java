package com.fitproject.gestion.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "evidences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Evidence {

    @Id
    @Column(name = "evidence_id", nullable = false, unique = true)
    private String evidenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private ConstructionStep step;

    @Column(name = "evidence_url")
    private String evidenceUrl;  // null/empty for ASSIGNED tasks; populated when worker submits

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "submitted_by", nullable = false)
    private String submittedBy;

    @Column(name = "supervisor_id")
    private String supervisorId;

    @Column(name = "assigned_worker_id")
    private String assignedWorkerId;

    @Column(name = "assigned_worker_name")
    private String assignedWorkerName;

    // JSON array of consumed insumos: [{"insumoId":"...","nombre":"...","cantidad":N,"unidadMedida":"..."}]
    @Column(name = "insumos_usados", columnDefinition = "TEXT")
    private String insumosUsados;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private EvidenceStatus status = EvidenceStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (evidenceId == null) evidenceId = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
