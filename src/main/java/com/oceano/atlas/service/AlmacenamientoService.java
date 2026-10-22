package com.oceano.atlas.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * SERVICIO DE ARCHIVOS: guarda en disco las imágenes que el admin sube desde el formulario.
 *
 * Flujo completo de una imagen subida:
 *   formulario.html (input type="file")
 *     -> AdminController.guardar(MultipartFile)
 *     -> AlmacenamientoService.guardar(...)  (escribe el archivo en la carpeta "uploads" y devuelve "/uploads/xxx.jpg")
 *     -> esa ruta se guarda en Animal.imagenUrl (PostgreSQL)
 *     -> el navegador pide /uploads/xxx.jpg y WebConfig lo sirve desde la carpeta física.
 */
@Service
public class AlmacenamientoService {

    /** Extensiones permitidas. SVG se excluye a propósito: un SVG subido puede contener scripts. */
    private static final Set<String> EXTENSIONES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path carpeta;

    /**
     * Constructor: recibe la ruta desde application.properties (atlas.uploads.dir) mediante @Value
     * y crea la carpeta si todavía no existe.
     */
    public AlmacenamientoService(@Value("${atlas.uploads.dir}") String directorio) throws IOException {
        this.carpeta = Paths.get(directorio).toAbsolutePath().normalize();
        Files.createDirectories(this.carpeta);
    }

    /**
     * Valida y guarda la imagen con un nombre aleatorio (UUID) para evitar choques de nombres
     * y nombres maliciosos. Devuelve la ruta pública que se almacenará en la base de datos.
     *
     * @throws IllegalArgumentException si el archivo no es una imagen permitida (AdminController muestra el mensaje)
     * @throws IOException              si falla la escritura en disco
     */
    public String guardar(MultipartFile archivo) throws IOException {
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
            Files.copy(entrada, carpeta.resolve(nombreFinal), StandardCopyOption.REPLACE_EXISTING);
        }
        return "/uploads/" + nombreFinal;
    }

    /** Carpeta física donde se guardan las imágenes. La usa WebConfig para publicarla como /uploads/**. */
    public Path getCarpeta() {
        return carpeta;
    }
}
