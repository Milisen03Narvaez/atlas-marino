package com.oceano.atlas.controller;

import com.oceano.atlas.model.Animal;
import com.oceano.atlas.service.AnimalService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * CONTROLADOR de la página principal (la "C" de MVC, devuelve una VISTA HTML).
 *
 * Cuándo actúa: cuando el navegador pide  GET http://localhost:8080/
 *
 * Cadena completa:
 *   Navegador -> HomeController.inicio() -> AnimalService.buscar() -> AnimalRepository -> PostgreSQL
 *   PostgreSQL -> Repository -> Service -> HomeController -> Model -> index.html (Thymeleaf) -> HTML al navegador
 *
 * @Controller (a diferencia de @RestController) significa: "el String que devuelvo es el NOMBRE de una plantilla".
 */
@Controller
public class HomeController {

    private final AnimalService animalService;

    /** Spring inyecta aquí el servicio (constructor). */
    public HomeController(AnimalService animalService) {
        this.animalService = animalService;
    }

    /**
     * Prepara los datos de la primera pantalla: los primeros 9 animales (3x3).
     *
     * @param model "mochila" de datos: todo lo que se agregue con addAttribute queda disponible
     *              en index.html mediante expresiones ${...}.
     * @return "index" -> Thymeleaf busca  src/main/resources/templates/index.html  y lo renderiza.
     */
    @GetMapping("/")
    public String inicio(Model model) {
        Page<Animal> pagina = animalService.buscar("", 0);

        model.addAttribute("animales", pagina.getContent());            // -> th:each="animal : ${animales}"
        model.addAttribute("totalPaginas", pagina.getTotalPages());     // -> lo lee buscador.js (data-total-paginas)
        model.addAttribute("totalElementos", pagina.getTotalElements()); // -> texto "12 especies"
        return "index";
    }
}
