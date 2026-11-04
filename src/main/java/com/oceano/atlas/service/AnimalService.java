package com.oceano.atlas.service;

import com.oceano.atlas.model.Animal;
import com.oceano.atlas.repository.AnimalRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * SERVICIO ÚNICO (lógica de negocio). Reúne tres trabajos que antes estaban en clases separadas:
 *   1. Reglas de los animales (buscar, guardar, eliminar).
 *   2. Guardado de las imágenes que sube el admin (antes AlmacenamientoService).
 *   3. Carga de los 12 animales iniciales (antes DataSeeder).
 *
 *   AnimalController  ──►  AnimalService  ──►  AnimalRepository  ──►  PostgreSQL
 */
@Service
public class AnimalService implements CommandLineRunner {

    /** 3 columnas x 3 filas = 9 tarjetas por página. */
    public static final int TAMANO_PAGINA = 9;

    /** Imagen que se usa si el admin no sube ninguna (archivo en static/img). */
    public static final String IMAGEN_POR_DEFECTO = "/img/placeholder.svg";

    /** Extensiones permitidas al subir. SVG se excluye a propósito: un SVG puede contener scripts. */
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final AnimalRepository animalRepository;
    private final Path carpetaUploads;

    /**
     * Spring entrega el repositorio y la ruta atlas.uploads.dir (application.properties).
     * Aquí se crea la carpeta de imágenes si no existe.
     */
    public AnimalService(AnimalRepository animalRepository,
                         @Value("${atlas.uploads.dir}") String directorio) throws IOException {
        this.animalRepository = animalRepository;
        this.carpetaUploads = Paths.get(directorio).toAbsolutePath().normalize();
        Files.createDirectories(this.carpetaUploads);
    }

    // ------------------------------------------------------------------ ANIMALES

    /** Devuelve UNA página (máx. 9) de animales que coinciden con el texto. Lo usan la portada y el buscador. */
    @Transactional(readOnly = true)
    public Page<Animal> buscar(String texto, int pagina) {
        String limpio = (texto == null) ? "" : texto.trim();   // regla: ignorar espacios sobrantes
        int paginaSegura = Math.max(pagina, 0);                // regla: nunca página negativa
        Pageable pageable = PageRequest.of(paginaSegura, TAMANO_PAGINA, Sort.by("nombre").ascending());
        return animalRepository.buscar(limpio, pageable);
    }

    /** Busca un animal por id (Optional porque puede no existir). Lo usan el popup y el editor del admin. */
    @Transactional(readOnly = true)
    public Optional<Animal> obtenerPorId(Long id) {
        return animalRepository.findById(id);
    }

    /** Todos los animales ordenados por nombre. Lo usa la tabla del panel de administración. */
    @Transactional(readOnly = true)
    public List<Animal> listarTodos() {
        return animalRepository.findAll(Sort.by("nombre").ascending());
    }

    /** Crea o actualiza un animal. Regla: si no trae imagen, se le pone la imagen por defecto. */
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

    /** Elimina un animal por id. */
    @Transactional
    public void eliminar(Long id) {
        animalRepository.deleteById(id);
    }

    /** Cuántos animales hay. Se usa para saber si hay que cargar los datos iniciales. */
    @Transactional(readOnly = true)
    public long contar() {
        return animalRepository.count();
    }

    // ------------------------------------------------------------------ IMÁGENES SUBIDAS

    /**
     * Valida y guarda la imagen con nombre aleatorio (UUID). Devuelve la ruta pública "/uploads/xxx.jpg",
     * que se almacena en Animal.imagenUrl.
     *
     * @throws IllegalArgumentException si no es una imagen permitida (el controlador muestra el mensaje)
     * @throws IOException              si falla la escritura en disco
     */
    public String guardarImagen(MultipartFile archivo) throws IOException {
        String tipo = archivo.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen (JPG, PNG, WEBP o GIF).");
        }

        String original = StringUtils.cleanPath(Objects.requireNonNullElse(archivo.getOriginalFilename(), ""));
        int punto = original.lastIndexOf('.');
        String extension = (punto >= 0) ? original.substring(punto + 1).toLowerCase() : "";
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("Formato no permitido. Usa JPG, PNG, WEBP o GIF.");
        }

        String nombreFinal = UUID.randomUUID() + "." + extension;
        try (InputStream entrada = archivo.getInputStream()) {
            Files.copy(entrada, carpetaUploads.resolve(nombreFinal), StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/" + nombreFinal;
    }

    /**
     * Busca una imagen subida por su nombre. El controlador la sirve en GET /uploads/{nombre}.
     * Se rechaza cualquier ruta que salga de la carpeta de uploads (protección contra "../").
     */
    public Optional<Resource> obtenerImagen(String nombre) {
        Path archivo = carpetaUploads.resolve(nombre).normalize();
        if (!archivo.startsWith(carpetaUploads) || !Files.isRegularFile(archivo)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(archivo));
    }

    // ------------------------------------------------------------------ DATOS INICIALES

    /**
     * CommandLineRunner: Spring ejecuta run() UNA vez al arrancar, cuando Hibernate ya creó la tabla.
     * Si la tabla está vacía inserta 12 animales de ejemplo; si ya hay datos, no hace nada.
     */
    @Override
    public void run(String... args) {
        if (contar() > 0) {
            return;
        }

        // >>> PEGA AQUÍ las 12 llamadas crear(...) que copiaste de DataSeeder.java <<<
    }

    /** Arma un Animal y lo guarda. archivoImagen = nombre del .svg (sin extensión) en static/img/animales/. */
    private void crear(String nombre, String cientifico, String descripcion,
                       String habitat, String dieta, String tamano, String archivoImagen) {
        Animal animal = new Animal();
        animal.setNombre(nombre);
        animal.setNombreCientifico(cientifico);
        animal.setDescripcion(descripcion);
        animal.setHabitat(habitat);
        animal.setDieta(dieta);
        animal.setTamano(tamano);
        animal.setImagenUrl("/img/animales/" + archivoImagen + ".svg");
        guardar(animal);
    }
}