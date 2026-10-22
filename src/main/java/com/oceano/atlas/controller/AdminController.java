package com.oceano.atlas.controller;

import com.oceano.atlas.model.Animal;
import com.oceano.atlas.service.AlmacenamientoService;
import com.oceano.atlas.service.AnimalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * CONTROLADOR DEL ADMIN: login, panel con tabla, crear, editar y eliminar animales.
 *
 * Seguridad (muy sencilla, pensada para aprender):
 *   - Todas las rutas /admin/** pasan primero por AdminInterceptor (registrado en WebConfig).
 *     Si no existe en la sesión el atributo SESION_ADMIN, redirige a /admin/login.
 *   - Excepción: /admin/login, que está excluida del interceptor.
 *
 * Rutas y plantillas:
 *   GET  /admin/login          -> templates/admin/login.html
 *   POST /admin/login          -> valida usuario/clave y abre sesión
 *   GET  /admin/logout         -> cierra sesión
 *   GET  /admin                -> templates/admin/panel.html      (tabla de animales)
 *   GET  /admin/nuevo          -> templates/admin/formulario.html (vacío)
 *   GET  /admin/editar/{id}    -> templates/admin/formulario.html (con datos)
 *   POST /admin/guardar        -> crea o actualiza (con imagen opcional)
 *   POST /admin/eliminar/{id}  -> borra
 */
@Controller
@RequestMapping("/admin")   // prefijo de todas las rutas de esta clase
public class AdminController {

    /** Nombre del atributo de sesión que marca "ya inició sesión". También lo consulta AdminInterceptor. */
    public static final String SESION_ADMIN = "adminAutenticado";

    private final AnimalService animalService;
    private final AlmacenamientoService almacenamientoService;
    private final String usuarioAdmin;
    private final String claveAdmin;

    /**
     * Constructor: Spring inyecta los dos servicios y, con @Value, los textos de application.properties
     * (atlas.admin.usuario y atlas.admin.clave).
     */
    public AdminController(AnimalService animalService,
                           AlmacenamientoService almacenamientoService,
                           @Value("${atlas.admin.usuario}") String usuarioAdmin,
                           @Value("${atlas.admin.clave}") String claveAdmin) {
        this.animalService = animalService;
        this.almacenamientoService = almacenamientoService;
        this.usuarioAdmin = usuarioAdmin;
        this.claveAdmin = claveAdmin;
    }

    // ------------------------------------------------------------------ LOGIN

    /** Muestra el formulario de login. Si ya hay sesión, va directo al panel. */
    @GetMapping("/login")
    public String mostrarLogin(HttpSession sesion) {
        if (Boolean.TRUE.equals(sesion.getAttribute(SESION_ADMIN))) {
            return "redirect:/admin";
        }
        return "admin/login";   // -> templates/admin/login.html
    }

    /**
     * Procesa el formulario de login (campos "usuario" y "clave" de login.html).
     * Compara con MessageDigest.isEqual (comparación en tiempo constante) contra los valores de application.properties.
     * Si son correctos: renueva el id de sesión (evita fijación de sesión), marca SESION_ADMIN y redirige al panel.
     * Si no: vuelve a mostrar login.html con un mensaje de error.
     */
    @PostMapping("/login")
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
            request.changeSessionId();
            sesion.setAttribute(SESION_ADMIN, Boolean.TRUE);
            return "redirect:/admin";
        }
        model.addAttribute("error", "Usuario o contraseña incorrectos");
        return "admin/login";
    }

    /** Cierra la sesión y vuelve al login. */
    @GetMapping("/logout")
    public String cerrarSesion(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        return "redirect:/admin/login";
    }

    // ------------------------------------------------------------------ PANEL

    /** Panel principal: tabla con TODOS los animales. Los datos van a panel.html como "animales". */
    @GetMapping
    public String panel(Model model) {
        model.addAttribute("animales", animalService.listarTodos());
        return "admin/panel";
    }

    // ------------------------------------------------------------ CREAR / EDITAR

    /** Muestra el formulario vacío. "new Animal()" es el objeto que th:object de formulario.html va a rellenar. */
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("animal", new Animal());
        return "admin/formulario";
    }

    /**
     * Muestra el formulario con los datos de un animal existente.
     * Si el id no existe, vuelve al panel con un mensaje de error (RedirectAttributes sobrevive a la redirección).
     */
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes flash) {
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

    /**
     * Recibe el formulario (crear y editar usan este mismo método).
     *
     * - @Valid + BindingResult: Spring llena el objeto Animal con los campos del formulario y comprueba las
     *   anotaciones de Animal (@NotBlank...). Los errores quedan en "resultado" y formulario.html los muestra.
     * - archivoImagen: archivo opcional. Si viene, AlmacenamientoService lo guarda y su ruta reemplaza imagenUrl.
     *   Si no viene, se conserva lo que haya en el campo imagenUrl (ruta anterior o URL escrita a mano).
     * - Si todo está bien: AnimalService.guardar() -> repositorio -> PostgreSQL, y redirige al panel
     *   (patrón Post/Redirect/Get: evita duplicar el registro si se recarga la página).
     */
    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("animal") Animal animal,
                          BindingResult resultado,
                          @RequestParam(value = "archivoImagen", required = false) MultipartFile archivo,
                          Model model,
                          RedirectAttributes flash) {
        if (resultado.hasErrors()) {
            return "admin/formulario";   // vuelve a mostrar el formulario con los mensajes de error
        }

        try {
            if (archivo != null && !archivo.isEmpty()) {
                animal.setImagenUrl(almacenamientoService.guardar(archivo));
            }
        } catch (IllegalArgumentException | IOException e) {
            model.addAttribute("errorImagen", e.getMessage());
            return "admin/formulario";
        }

        animalService.guardar(animal);
        flash.addFlashAttribute("mensaje", "Animal guardado correctamente");
        return "redirect:/admin";
    }

    // ---------------------------------------------------------------- ELIMINAR

    /** Elimina un animal. Es POST (no GET) para que un enlace o un buscador no pueda borrar datos por accidente. */
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        animalService.eliminar(id);
        flash.addFlashAttribute("mensaje", "Animal eliminado");
        return "redirect:/admin";
    }
}
