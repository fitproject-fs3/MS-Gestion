package com.fitproject.gestion.repository;

import com.fitproject.gestion.model.TaskAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<TaskAssignment, String> {
    List<TaskAssignment> findByWorkerId(String workerId);
    List<TaskAssignment> findByStepId(String stepId);
}
