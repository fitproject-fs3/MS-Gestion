package com.fitproject.gestion.service;

import com.fitproject.gestion.dto.TaskAssignmentDTO;
import com.fitproject.gestion.model.TaskAssignment;
import com.fitproject.gestion.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;

    @Transactional
    public TaskAssignmentDTO create(Map<String, String> body) {
        TaskAssignment assignment = TaskAssignment.builder()
                .workerId(body.get("workerId"))
                .workerName(body.get("workerName"))
                .stepId(body.get("stepId"))
                .stepName(body.get("stepName"))
                .projectId(body.get("projectId"))
                .status("PENDING")
                .build();
        return toDTO(assignmentRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    public List<TaskAssignmentDTO> getByWorker(String workerId) {
        return assignmentRepository.findByWorkerId(workerId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskAssignmentDTO> getByStep(String stepId) {
        return assignmentRepository.findByStepId(stepId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public TaskAssignmentDTO updateStatus(String assignmentId, String status) {
        TaskAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada: " + assignmentId));
        assignment.setStatus(status.toUpperCase());
        return toDTO(assignmentRepository.save(assignment));
    }

    @Transactional
    public void delete(String assignmentId) {
        if (!assignmentRepository.existsById(assignmentId))
            throw new IllegalArgumentException("Asignación no encontrada: " + assignmentId);
        assignmentRepository.deleteById(assignmentId);
    }

    private TaskAssignmentDTO toDTO(TaskAssignment a) {
        return TaskAssignmentDTO.builder()
                .assignmentId(a.getAssignmentId())
                .workerId(a.getWorkerId())
                .workerName(a.getWorkerName())
                .stepId(a.getStepId())
                .stepName(a.getStepName())
                .projectId(a.getProjectId())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
