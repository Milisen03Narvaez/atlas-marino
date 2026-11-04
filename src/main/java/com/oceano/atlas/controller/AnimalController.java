package com.oceano.atlas.controller;

import com.oceano.atlas.model.Animal;
import com.oceano.atlas.service.AnimalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * CONTROLADOR ÚNICO. Reúne lo que antes hacían HomeController, AnimalApiController y AdminController:
 *
 *   PÁGINA   GET /                       -> templates/index.html
 *   API JSON GET /api/animales           -> buscador en vivo (lo llama buscador.js)
 *            GET /api/animales/{id}      -> datos del popup
 *   IMÁGENES GET /uploads/{nombre}       -> imágenes subidas por el admin
 *   ADMIN    /admin/**                   -> login, panel, crear, editar y eliminar (requiere sesión)
 *
 *   Navegador ──► AnimalController ──► AnimalService ──► AnimalRepository ──► PostgreSQL
 */
@Controller
public class AnimalController {

    /** Atributo de sesión que marca "ya inició sesión". */
    private static final String SESION_ADMIN = "adminAutenticado";
    private static final String IR_AL_LOGIN = "redirect:/admin/login";

    private final AnimalService animalService;
    private final String usuarioAdmin;
    private final String claveAdmin;

    /** Spring inyecta el servicio y las credenciales de application.properties (atlas.admin.*). */
    public AnimalController(AnimalService animalService,
                            @Value("${atlas.admin.usuario}") String usuarioAdmin,
                            @Value("${atlas.admin.clave}") String claveAdmin) {
        this.animalService = animalService;
        this.usuarioAdmin = usuarioAdmin;
        this.claveAdmin = claveAdmin;
    }

    // ================================================================== PÁGINA PRINCIPAL

    /** Primera pantalla: los primeros 9 animales. Los datos llegan a index.html por el Model. */
    @GetMapping("/")
    public String inicio(Model model) {
        Page<Animal> pagina = animalService.buscar("", 0);
        model.addAttribute("animales", pagina.getContent());
        model.addAttribute("totalPaginas", pagina.getTotalPages());      // lo lee buscador.js
        model.addAttribute("totalElementos", pagina.getTotalElements()); // texto "12 especies"
        return "index";
    }

    // ================================================================== API JSON (buscador.js)

    /** GET /api/animales?q=texto&pagina=0 -> hasta 9 animales + datos del paginador (JSON). */
    @GetMapping("/api/animales")
    @ResponseBody
    public Map<String, Object> buscar(@RequestParam(defaultValue = "") String q,
                                      @RequestParam(defaultValue = "0") int pagina) {
        Page<Animal> resultado = animalService.buscar(q, pagina);
        return Map.of(
                "animales", resultado.getContent(),
                "paginaActual", resultado.getNumber(),
                "totalPaginas", resultado.getTotalPages(),
                "totalElementos", resultado.getTotalElements());
    }

    /** GET /api/animales/{id} -> un animal completo para el popup. 404 si no existe. */
    @GetMapping("/api/animales/{id}")
    @ResponseBody
    public ResponseEntity<Animal> detalle(@PathVariable Long id) {
        return animalService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ================================================================== IMÁGENES SUBIDAS

    /** GET /uploads/xxx.jpg -> devuelve el archivo guardado por el admin (antes lo hacía WebConfig). */
    @GetMapping("/uploads/{nombre}")
    @ResponseBody
    public ResponseEntity<Resource> imagenSubida(@PathVariable String nombre) {
        return animalService.obtenerImagen(nombre)
                .map(recurso -> ResponseEntity.ok()
                        .contentType(MediaTypeFactory.getMediaType(recurso).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .body(recurso))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ================================================================== ADMIN: LOGIN

    /** Muestra el login. Si ya hay sesión, va directo al panel. */
    @GetMapping("/admin/login")
    public String mostrarLogin(HttpServletRequest request) {
        return esAdmin(request) ? "redirect:/admin" : "admin/login";
    }

    /** Valida usuario y clave (comparación en tiempo constante) y abre la sesión. */
    @PostMapping("/admin/login")
    public String procesarLogin(@RequestParam String usuario,
                                @RequestParam String clave,
                                HttpServletRequest request,
                                Model model) {
        boolean usuarioOk = MessageDigest.isEqual(
                usuario.getBytes(StandardCharsets.UTF_8), usuarioAdmin.getBytes(StandardCharsets.UTF_8));
        boolean claveOk = MessageDigest.isEqual(
                clave.getBytes(StandardCharsets.UTF_8), claveAdmin.getBytes(StandardCharsets.UTF_8));

        if (usuarioOk && claveOk) {
            HttpSession sesion = request.getSession(true);
            request.changeSessionId();                       // evita la fijación de sesión
            sesion.setAttribute(SESION_ADMIN, Boolean.TRUE);
            return "redirect:/admin";
        }
        model.addAttribute("error", "Usuario o contraseña incorrectos");
        return "admin/login";
    }

    /** Cierra la sesión y vuelve al login. */
    @GetMapping("/admin/logout")
    public String cerrarSesion(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        return IR_AL_LOGIN;
    }

    // ================================================================== ADMIN: PANEL, CREAR, EDITAR, ELIMINAR
    // Cada método empieza con esAdmin(): si no hay sesión, redirige al login (antes lo hacía AdminInterceptor).

    /** Panel con la tabla de TODOS los animales. */
    @GetMapping("/admin")
    public String panel(HttpServletRequest request, Model model) {
        if (!esAdmin(request)) return IR_AL_LOGIN;
        model.addAttribute("animales", animalService.listarTodos());
        return "admin/panel";
    }

    /** Formulario vacío para crear un animal. */
    @GetMapping("/admin/nuevo")
    public String nuevo(HttpServletRequest request, Model model) {
        if (!esAdmin(request)) return IR_AL_LOGIN;
        model.addAttribute("animal", new Animal());
        return "admin/formulario";
    }

    /** Formulario con los datos de un animal existente. Si el id no existe, vuelve al panel con un error. */
    @GetMapping("/admin/editar/{id}")
    public String editar(@PathVariable Long id, HttpServletRequest request,
                         Model model, RedirectAttributes flash) {
        if (!esAdmin(request)) return IR_AL_LOGIN;
        return animalService.obtenerPorId(id)
                .map(animal -> {
                    model.addAttribute("animal", animal);
                    return "admin/formulario";
                })
                .orElseGet(() -> {
                    flash.addFlashAttribute("error", "El animal no existe");
                    return "redirect:/admin";
                });
    }

    /** Crea o actualiza (mismo método). @Valid comprueba las reglas de Animal; la imagen es opcional. */
    @PostMapping("/admin/guardar")
    public String guardar(@Valid @ModelAttribute("animal") Animal animal,
                          BindingResult resultado,
                          @RequestParam(value = "archivoImagen", required = false) MultipartFile archivo,
                          HttpServletRequest request,
                          Model model,
                          RedirectAttributes flash) {
        if (!esAdmin(request)) return IR_AL_LOGIN;
        if (resultado.hasErrors()) {
            return "admin/formulario";   // vuelve a mostrar el formulario con los errores
        }

        try {
            if (archivo != null && !archivo.isEmpty()) {
                animal.setImagenUrl(animalService.guardarImagen(archivo));
            }
        } catch (IllegalArgumentException | IOException e) {
            model.addAttribute("errorImagen", e.getMessage());
            return "admin/formulario";
        }

        animalService.guardar(animal);
        flash.addFlashAttribute("mensaje", "Animal guardado correctamente");
        return "redirect:/admin";
    }

    /** Elimina un animal. Es POST (no GET) para que un enlace no pueda borrar datos por accidente. */
    @PostMapping("/admin/eliminar/{id}")
    public String eliminar(@PathVariable Long id, HttpServletRequest request, RedirectAttributes flash) {
        if (!esAdmin(request)) return IR_AL_LOGIN;
        animalService.eliminar(id);
        flash.addFlashAttribute("mensaje", "Animal eliminado");
        return "redirect:/admin";
    }

    // ================================================================== AUXILIAR

    /** true si la sesión actual ya inició sesión como admin. No crea sesión nueva si no existe. */
    private boolean esAdmin(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        return sesion != null && Boolean.TRUE.equals(sesion.getAttribute(SESION_ADMIN));
    }
}