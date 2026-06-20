package com.fitproject.gestion.model;

public enum EvidenceStatus {
    PENDING,   // Waiting for supervisor approval (includes tasks awaiting worker evidence)
    APPROVED,
    REJECTED
}
