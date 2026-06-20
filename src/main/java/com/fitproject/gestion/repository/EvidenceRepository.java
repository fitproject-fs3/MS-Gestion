package com.fitproject.gestion.repository;

import com.fitproject.gestion.model.Evidence;
import com.fitproject.gestion.model.EvidenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, String> {
    List<Evidence> findByStep_StepId(String stepId);
    List<Evidence> findByStatus(EvidenceStatus status);
    List<Evidence> findByAssignedWorkerId(String assignedWorkerId);
}
