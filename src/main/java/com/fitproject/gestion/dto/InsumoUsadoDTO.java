package com.fitproject.gestion.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class InsumoUsadoDTO {
    private String insumoId;
    private String nombre;
    private Integer cantidad;
    private String unidadMedida;
}
