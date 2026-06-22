package com.fitproject.gestion.repository;

import com.fitproject.gestion.model.Evidence;
import com.fitproject.gestion.model.EvidenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad {@link Evidence}.
 *
 * <p>Los métodos que pueden retornar conjuntos grandes de registros disponen
 * de una variante paginada ({@link Pageable}) para optimizar el consumo de
 * memoria RAM y CPU del servidor (Green Computing).</p>
 */
@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, String> {

    /** Busca evidencias de un paso específico (uso interno, conjuntos acotados). */
    List<Evidence> findByStep_StepId(String stepId);

    /**
     * Busca evidencias por estado con paginación para limitar la carga en memoria.
     *
     * @param status   estado a filtrar ({@code PENDING}, {@code APPROVED}, {@code REJECTED})
     * @param pageable configuración de página y tamaño
     * @return página de evidencias con el estado indicado
     */
    Page<Evidence> findByStatus(EvidenceStatus status, Pageable pageable);

    /** @deprecated Usar {@link #findByStatus(EvidenceStatus, Pageable)} para evitar carga total. */
    @Deprecated
    List<Evidence> findByStatus(EvidenceStatus status);

    /**
     * Busca evidencias asignadas a un trabajador con paginación.
     *
     * @param assignedWorkerId identificador UUID del trabajador
     * @param pageable         configuración de página y tamaño
     * @return página de evidencias del trabajador
     */
    Page<Evidence> findByAssignedWorkerId(String assignedWorkerId, Pageable pageable);

    /** @deprecated Usar {@link #findByAssignedWorkerId(String, Pageable)} para evitar carga total. */
    @Deprecated
    List<Evidence> findByAssignedWorkerId(String assignedWorkerId);
}
