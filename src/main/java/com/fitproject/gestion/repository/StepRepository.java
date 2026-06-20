package com.fitproject.gestion.repository;

import com.fitproject.gestion.model.ConstructionStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StepRepository extends JpaRepository<ConstructionStep, String> {
    List<ConstructionStep> findByProject_ProjectId(String projectId);
}
