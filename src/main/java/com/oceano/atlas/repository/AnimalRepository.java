package com.oceano.atlas.repository;

import com.oceano.atlas.model.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * REPOSITORIO = capa de acceso a datos (habla con PostgreSQL).
 *
 * Es una INTERFAZ: no escribimos la implementación. Spring Data JPA la genera al arrancar.
 * Al extender JpaRepository<Animal, Long> heredamos gratis (sin escribir SQL):
 *   save(animal)      -> INSERT o UPDATE
 *   findById(id)      -> SELECT ... WHERE id = ?
 *   findAll(sort)     -> SELECT * ...
 *   deleteById(id)    -> DELETE ...
 *   count()           -> SELECT count(*)
 *
 * Quién la usa: SOLO AnimalService (y DataSeeder a través del servicio).
 * Los controladores nunca hablan directo con el repositorio: siempre pasan por el servicio.
 */
@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {

    /**
     * Búsqueda por texto en nombre común O nombre científico, sin distinguir mayúsculas.
     *
     * - JPQL (no SQL): trabaja con la clase Animal y sus atributos, no con la tabla.
     * - LIKE '%texto%' -> "contiene". Si el texto es vacío ('%%') devuelve TODOS los animales,
     *   por eso la página inicial usa este mismo método.
     * - Pageable: Spring añade automáticamente LIMIT/OFFSET (qué página y cuántos por página)
     *   y Page<Animal> trae además el total de resultados y de páginas (lo usa el paginador de la web).
     *
     * @param texto lo que escribió la persona en el buscador
     * @param pageable página solicitada + tamaño (9) + orden; lo construye AnimalService
     */
    @Query("""
           SELECT a FROM Animal a
           WHERE LOWER(a.nombre) LIKE LOWER(CONCAT('%', :texto, '%'))
              OR LOWER(a.nombreCientifico) LIKE LOWER(CONCAT('%', :texto, '%'))
           """)
    Page<Animal> buscar(@Param("texto") String texto, Pageable pageable);
}
