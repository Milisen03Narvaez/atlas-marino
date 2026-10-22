package com.oceano.atlas.dto;

import com.oceano.atlas.model.Animal;

import java.util.List;

/**
 * DTO (Data Transfer Object): un "sobre" con los datos que viajan en la respuesta JSON del buscador.
 * Es un record de Java: Java genera solo el constructor y los getters, y Jackson lo convierte a JSON:
 *
 *   { "animales": [...], "paginaActual": 0, "totalPaginas": 2, "totalElementos": 12 }
 *
 * Lo crea AnimalApiController.buscar() y lo lee buscador.js (función pintarResultados).
 */
public record RespuestaBusqueda(
        List<Animal> animales,
        int paginaActual,
        int totalPaginas,
        long totalElementos) {
}
