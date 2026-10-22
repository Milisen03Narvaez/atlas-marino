package com.oceano.atlas.controller;

import com.oceano.atlas.dto.RespuestaBusqueda;
import com.oceano.atlas.model.Animal;
import com.oceano.atlas.service.AnimalService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CONTROLADOR REST: responde con JSON (no con HTML). Es el "puente" entre la página y la base de datos
 * para las acciones que ocurren SIN recargar la página:
 *
 *   1. Buscador inmediato:  buscador.js hace fetch("/api/animales?q=ball&pagina=0")  -> método buscar()
 *   2. Popup de detalle:    buscador.js hace fetch("/api/animales/5")                -> método detalle()
 *
 * @RestController = @Controller + @ResponseBody: lo que devuelve cada método se convierte a JSON
 * automáticamente con Jackson (incluye getters de Animal).
 *
 * Cadena: buscador.js -> AnimalApiController -> AnimalService -> AnimalRepository -> PostgreSQL -> JSON -> buscador.js
 */
@RestController
@RequestMapping("/api/animales")   // prefijo común de las dos rutas de abajo
public class AnimalApiController {

    private final AnimalService animalService;

    /** Inyección del servicio por constructor. */
    public AnimalApiController(AnimalService animalService) {
        this.animalService = animalService;
    }

    /**
     * GET /api/animales?q=texto&pagina=0
     * Devuelve hasta 9 animales que coinciden con el texto + datos para el paginador.
     *
     * @param q      parámetro de la URL; si no viene, vale "" (todos los animales)
     * @param pagina parámetro de la URL; si no viene, vale 0 (primera página)
     */
    @GetMapping
    public RespuestaBusqueda buscar(@RequestParam(defaultValue = "") String q,
                                    @RequestParam(defaultValue = "0") int pagina) {
        Page<Animal> resultado = animalService.buscar(q, pagina);
        return new RespuestaBusqueda(
                resultado.getContent(),
                resultado.getNumber(),
                resultado.getTotalPages(),
                resultado.getTotalElements());
    }

    /**
     * GET /api/animales/{id}
     * Devuelve UN animal completo (descripción, hábitat, dieta...) para llenar el popup.
     * {id} es una variable de la ruta que Spring pasa al parámetro marcado con @PathVariable.
     *
     * @return 200 con el animal en JSON, o 404 si el id no existe
     */
    @GetMapping("/{id}")
    public ResponseEntity<Animal> detalle(@PathVariable Long id) {
        return animalService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
