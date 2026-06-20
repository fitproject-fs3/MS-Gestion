package com.fitproject.gestion.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "ms-inventario", url = "${services.ms-inventario.url:http://localhost:8094/api/v1}")
public interface InventarioClient {

    @PostMapping("/insumos/{id}/consumir")
    Map<String, Object> consumir(@PathVariable("id") String insumoId,
                                 @RequestBody Map<String, Object> body);
}
