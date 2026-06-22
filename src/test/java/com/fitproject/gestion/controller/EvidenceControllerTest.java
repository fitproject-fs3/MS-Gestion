package com.fitproject.gestion.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitproject.gestion.dto.EvidenceDTO;
import com.fitproject.gestion.service.EvidenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias de la capa HTTP para {@link EvidenceController}.
 * Usa {@code MockMvc} en modo standalone (sin Spring context) para máxima velocidad.
 */
@ExtendWith(MockitoExtension.class)
class EvidenceControllerTest {

    @Mock private EvidenceService evidenceService;

    @InjectMocks
    private EvidenceController evidenceController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private EvidenceDTO sampleDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(evidenceController).build();
        objectMapper = new ObjectMapper();
        sampleDTO = EvidenceDTO.builder()
                .evidenceId("ev-1")
                .projectId("proj-1").stepId("step-1")
                .name("Foto cimientos").submittedBy("supervisor1")
                .status("PENDING")
                .build();
    }

    @Test
    @DisplayName("GET /pending retorna 200 con lista de evidencias")
    void getPending_returns200WithList() throws Exception {
        when(evidenceService.getPending(0, 20)).thenReturn(List.of(sampleDTO));

        mockMvc.perform(get("/api/v1/evidences/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evidenceId").value("ev-1"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /worker/{workerId} retorna 200 con evidencias del trabajador")
    void getByWorker_returns200WithList() throws Exception {
        when(evidenceService.getByWorker("worker-1", 0, 20)).thenReturn(List.of(sampleDTO));

        mockMvc.perform(get("/api/v1/evidences/worker/worker-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evidenceId").value("ev-1"));
    }

    @Test
    @DisplayName("POST /submit retorna 200 con la evidencia creada")
    void submit_returns200WithCreatedEvidence() throws Exception {
        EvidenceDTO req = EvidenceDTO.builder()
                .projectId("proj-1").stepId("step-1")
                .name("Nueva foto").submittedBy("sup1").build();
        when(evidenceService.submit(any(EvidenceDTO.class))).thenReturn(sampleDTO);

        mockMvc.perform(post("/api/v1/evidences/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evidenceId").value("ev-1"));
    }

    @Test
    @DisplayName("POST /{evidenceId}/approve retorna 200 con evidencia APPROVED")
    void approve_returns200WithApprovedEvidence() throws Exception {
        EvidenceDTO approved = EvidenceDTO.builder().evidenceId("ev-1").status("APPROVED").build();
        when(evidenceService.approve("ev-1", "sup1")).thenReturn(approved);

        mockMvc.perform(post("/api/v1/evidences/ev-1/approve")
                        .param("supervisorId", "sup1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("DELETE /{evidenceId} retorna 204 No Content")
    void delete_returns204() throws Exception {
        doNothing().when(evidenceService).delete("ev-1");

        mockMvc.perform(delete("/api/v1/evidences/ev-1"))
                .andExpect(status().isNoContent());

        verify(evidenceService).delete("ev-1");
    }

    @Test
    @DisplayName("POST /{evidenceId}/reject retorna 200 con evidencia REJECTED")
    void reject_returns200WithRejectedEvidence() throws Exception {
        EvidenceDTO rejected = EvidenceDTO.builder().evidenceId("ev-1").status("REJECTED").build();
        when(evidenceService.reject("ev-1", "sup1")).thenReturn(rejected);

        mockMvc.perform(post("/api/v1/evidences/ev-1/reject")
                        .param("supervisorId", "sup1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
