package com.oceano.atlas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * MODELO (la "M" de MVC).
 *
 * Una instancia de esta clase = UNA FILA de la tabla "animales" en PostgreSQL.
 * Hibernate (JPA) hace la traducción objeto <-> fila gracias a las anotaciones.
 *
 * Quién la usa:
 *   - AnimalRepository  la guarda y la lee de la base de datos.
 *   - AnimalService     la recibe/devuelve en cada operación.
 *   - Los controladores la pasan a la vista (Thymeleaf) o la convierten a JSON (API del buscador).
 *   - formulario.html   la rellena con th:object / th:field cuando el admin crea o edita un animal.
 *
 * Las anotaciones de validación (@NotBlank, @Size) se comprueban en AdminController.guardar()
 * gracias a @Valid, ANTES de tocar la base de datos.
 */
@Entity                       // Le dice a JPA: "esta clase representa una tabla".
@Table(name = "animales")     // Nombre real de la tabla en PostgreSQL.
public class Animal {

    /** Clave primaria. IDENTITY = PostgreSQL la autoincrementa (columna serial). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre común, p. ej. "Ballena azul". Es lo que se busca y se muestra en la tarjeta. */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;

    /** Nombre científico, p. ej. "Balaenoptera musculus". También es buscable. */
    @Size(max = 150, message = "Máximo 150 caracteres")
    @Column(name = "nombre_cientifico", length = 150)
    private String nombreCientifico;

    /** Descripción larga que aparece en el popup. TEXT = sin límite práctico en PostgreSQL. */
    @NotBlank(message = "La descripción es obligatoria")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Size(max = 200, message = "Máximo 200 caracteres")
    @Column(length = 200)
    private String habitat;

    @Size(max = 200, message = "Máximo 200 caracteres")
    @Column(length = 200)
    private String dieta;

    @Size(max = 100, message = "Máximo 100 caracteres")
    @Column(length = 100)
    private String tamano;

    /**
     * Ruta o URL de la imagen. La base de datos NO guarda el archivo, solo su ubicación:
     *   "/img/animales/xxx.svg"  -> imagen incluida en el proyecto (carpeta static)
     *   "/uploads/uuid.jpg"      -> imagen subida desde el admin (carpeta uploads, ver WebConfig)
     *   "https://..."            -> imagen externa
     * El navegador la usa directamente en <img src="...">.
     */
    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    /** JPA exige un constructor vacío. */
    public Animal() {
    }

    // ----- Getters y setters -----
    // Thymeleaf y Jackson (JSON) leen los datos con los getters; el formulario del admin escribe con los setters.

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getNombreCientifico() { return nombreCientifico; }
    public void setNombreCientifico(String nombreCientifico) { this.nombreCientifico = nombreCientifico; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getHabitat() { return habitat; }
    public void setHabitat(String habitat) { this.habitat = habitat; }

    public String getDieta() { return dieta; }
    public void setDieta(String dieta) { this.dieta = dieta; }

    public String getTamano() { return tamano; }
    public void setTamano(String tamano) { this.tamano = tamano; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
}
