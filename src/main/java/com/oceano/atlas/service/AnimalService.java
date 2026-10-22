package com.oceano.atlas.service;

import com.oceano.atlas.model.Animal;
import com.oceano.atlas.repository.AnimalRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * SERVICIO = capa de LÓGICA DE NEGOCIO. Es el intermediario entre los controladores y el repositorio.
 *
 *   Controller  ──►  AnimalService  ──►  AnimalRepository  ──►  PostgreSQL
 *
 * ¿Por qué existe, si el repositorio ya hace el trabajo? Porque aquí van las REGLAS:
 * limpiar el texto de búsqueda, fijar 9 resultados por página, poner una imagen por defecto, etc.
 * Así los controladores quedan delgados y las reglas están en un solo lugar.
 *
 * Quién lo usa:
 *   - HomeController       (página principal)
 *   - AnimalApiController  (buscador en vivo y popup)
 *   - AdminController      (panel de administración)
 *   - DataSeeder           (datos iniciales)
 */
@Service
public class AnimalService {

    /** 3 columnas x 3 filas = 9 tarjetas por página. */
    public static final int TAMANO_PAGINA = 9;

    /** Imagen que se usa si el admin no sube ninguna (archivo en static/img). */
    public static final String IMAGEN_POR_DEFECTO = "/img/placeholder.svg";

    private final AnimalRepository animalRepository;

    /**
     * Constructor: Spring detecta que este servicio necesita un AnimalRepository y se lo entrega solo
     * (INYECCIÓN DE DEPENDENCIAS por constructor; no hace falta @Autowired).
     */
    public AnimalService(AnimalRepository animalRepository) {
        this.animalRepository = animalRepository;
    }

    /**
     * Busca animales y devuelve UNA página (máximo 9).
     * Llamado desde HomeController (carga inicial) y desde AnimalApiController (cada vez que se teclea).
     *
     * @param texto  texto del buscador; si es null o vacío devuelve todos
     * @param pagina número de página empezando en 0
     */
    @Transactional(readOnly = true)
    public Page<Animal> buscar(String texto, int pagina) {
        String limpio = (texto == null) ? "" : texto.trim();   // regla: ignorar espacios sobrantes
        int paginaSegura = Math.max(pagina, 0);                // regla: nunca página negativa
        Pageable pageable = PageRequest.of(paginaSegura, TAMANO_PAGINA, Sort.by("nombre").ascending());
        return animalRepository.buscar(limpio, pageable);
    }

    /**
     * Busca un animal por su id. Devuelve Optional porque puede no existir.
     * Lo usa AnimalApiController (popup) y AdminController (editar).
     */
    @Transactional(readOnly = true)
    public Optional<Animal> obtenerPorId(Long id) {
        return animalRepository.findById(id);
    }

    /**
     * Lista TODOS los animales ordenados por nombre. Lo usa la tabla del panel de administración.
     */
    @Transactional(readOnly = true)
    public List<Animal> listarTodos() {
        return animalRepository.findAll(Sort.by("nombre").ascending());
    }

    /**
     * Crea o actualiza un animal (save decide: si el id es null hace INSERT; si trae id, UPDATE).
     * Regla de negocio: si no hay imagen, se asigna la imagen por defecto.
     * Lo llaman AdminController.guardar() y DataSeeder.
     */
    @Transactional
    public Animal guardar(Animal animal) {
        String imagen = animal.getImagenUrl();
        if (imagen == null || imagen.isBlank()) {
            animal.setImagenUrl(IMAGEN_POR_DEFECTO);
        } else {
            animal.setImagenUrl(imagen.trim());
        }
        return animalRepository.save(animal);
    }

    /** Elimina un animal por id. Lo llama AdminController.eliminar(). */
    @Transactional
    public void eliminar(Long id) {
        animalRepository.deleteById(id);
    }

    /** Cuántos animales hay en total. DataSeeder lo usa para saber si la tabla está vacía. */
    @Transactional(readOnly = true)
    public long contar() {
        return animalRepository.count();
    }
}
