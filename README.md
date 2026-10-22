# 🌊 Atlas Marino - Arquitectura MVC & REST

Aplicación web full-stack construida con **Java 17** y **Spring Boot 3.4**, que muestra un catálogo de animales marinos guardado en **PostgreSQL**. El proyecto combina **MVC (Model-View-Controller)** para el renderizado inicial del lado del servidor (SSR) con Thymeleaf, y una **API REST interna** que consume JavaScript Vanilla para buscar y abrir fichas sin recargar la página. Incluye un **panel de administración** protegido por login para crear, editar y eliminar animales.

A diferencia de un proyecto que consume una API externa, aquí **los datos son propios**: viven en la tabla `animales` de PostgreSQL y se administran desde `/admin`.

---

## 🚀 Funcionalidades de la Interfaz y Características Técnicas

*   **Renderizado Híbrido:** La primera carga de la página (`/`) la pinta Thymeleaf en el servidor con los primeros 9 animales. Después, `buscador.js` toma el control y actualiza las tarjetas desde la API interna.
*   **Buscador inmediato (sin debounce):** Se consulta al servidor con **cada letra** escrita (evento `input`). No hay retraso ni botón de buscar. Si escribes rápido, `AbortController` cancela la petición anterior para que nunca se pinten resultados viejos.
*   **Búsqueda por nombre o nombre científico:** Coincidencia parcial (`contiene`) sin distinguir mayúsculas de minúsculas. **Sí distingue tildes** (buscar `delfin` no encuentra `Delfín`).
*   **Rejilla 3 × 3:** 3 columnas y máximo 9 tarjetas por página (3 filas). El tamaño sale de la constante `AnimalService.TAMANO_PAGINA = 9`.
*   **Paginación Asíncrona:** Botones `← Anterior` / `Siguiente →` con el texto `Página X de Y`. Solo aparecen si hay más de 9 resultados.
*   **Diseño Responsivo:** 3 columnas en escritorio y **1 columna en pantallas de 720 px o menos**.
*   **Modal de Detalles (Popup):** Al seleccionar una tarjeta se abre una ficha con imagen, nombre, nombre científico, descripción, hábitat, alimentación y tamaño. Tiene botón `← Regresar` (también se cierra con la tecla `Esc` o con clic en el fondo oscuro). Las tarjetas se pueden abrir con teclado (`Enter` o `Espacio`).
*   **Panel de Administración:** Login con sesión HTTP, tabla de animales, formulario de crear/editar con validación, eliminación con confirmación y subida de imágenes.
*   **Datos iniciales automáticos:** La primera vez que arranca con la tabla vacía, `DataSeeder` inserta 12 animales con su ilustración.

---

## 🛠️ Requisitos

*   **Java 17** o superior (JDK).
*   **IntelliJ IDEA** (ya trae Maven integrado) o **Maven 3.8+** si ejecutas por terminal.
*   **PostgreSQL** (probado con la versión 17) y **pgAdmin 4** para ver los datos.
*   **Internet solo la primera vez**, para que Maven descargue las librerías. La aplicación en sí **no necesita internet** para funcionar.

---

## ⚙️ Instalación y Ejecución

1.  **Clonar el repositorio:**
    ```bash
    git clone https://github.com/TU_USUARIO/atlas-marino.git
    cd atlas-marino
    git switch dev
    ```

2.  **Crear la base de datos** (solo se crea la base vacía; las tablas las crea la app):
    ```sql
    CREATE DATABASE oceano_db WITH ENCODING 'UTF8' TEMPLATE template0;
    ```
    También está en `sql/crear_base_datos.sql`, o puedes crearla desde pgAdmin (*Databases → Create → Database → `oceano_db`*).

3.  **Configurar la conexión** en `src/main/resources/application.properties`:
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/oceano_db
    spring.datasource.username=postgres
    spring.datasource.password=TU_CONTRASEÑA
    ```

4.  **Ejecutar la aplicación:**
    *   **IntelliJ:** abre `AtlasMarinoApplication.java` y pulsa el ▶ junto a `main`.
    *   **Terminal:**
        ```bash
        mvn spring-boot:run
        ```
    Espera el mensaje `Started AtlasMarinoApplication in ... seconds`.

5.  **Acceder en el navegador:**
    *   Sitio: [http://localhost:8080](http://localhost:8080)
    *   Admin: [http://localhost:8080/admin](http://localhost:8080/admin) (usuario `admin`, clave `admin123`)

> ⚠️ No abras `index.html` con doble clic: necesita que la aplicación esté corriendo para recibir los datos.

---

## 🏗️ Flujo General de Peticiones

El sistema actúa como intermediario entre el navegador y PostgreSQL. Ningún controlador habla directo con la base: siempre pasan por el servicio, y solo el repositorio ejecuta consultas.

**1. Carga Inicial (MVC):**
`Navegador (GET /)` ➔ `HomeController.inicio()` ➔ `AnimalService.buscar("", 0)` ➔ `AnimalRepository.buscar()` ➔ `PostgreSQL` ➔ `Model` ➔ `templates/index.html (Thymeleaf)` ➔ HTML con 9 tarjetas

**2. Búsqueda y paginación (REST):**
`Navegador (JS: GET /api/animales?q=ball&pagina=0)` ➔ `AnimalApiController.buscar()` ➔ `AnimalService.buscar()` ➔ `AnimalRepository.buscar()` ➔ `PostgreSQL` ➔ `RespuestaBusqueda` (JSON) ➔ `buscador.js` repinta la rejilla

**3. Popup de detalle (REST):**
`Navegador (JS: GET /api/animales/5)` ➔ `AnimalApiController.detalle()` ➔ `AnimalService.obtenerPorId()` ➔ `AnimalRepository.findById()` ➔ JSON del animal ➔ `buscador.js` llena y abre el modal

**4. Administración (MVC protegido):**
`Navegador (/admin/**)` ➔ `AdminInterceptor` (¿hay sesión?) ➔ `AdminController` ➔ `AnimalService` / `AlmacenamientoService` ➔ `PostgreSQL` / carpeta `uploads/` ➔ `templates/admin/*.html`

---

## 📂 Estructura del Código y Conexiones

### Backend (Java)
Ubicado en `src/main/java/com/oceano/atlas/`:

*   **`AtlasMarinoApplication.java`**: Clase principal con el `main()` que arranca Spring Boot. `@SpringBootApplication` activa el escaneo de componentes en todos los subpaquetes.
*   **`config/`**:
    *   `WebConfig.java`: Registra `AdminInterceptor` sobre `/admin/**` (excluyendo `/admin/login`) y publica la carpeta física `uploads/` como la URL `/uploads/**`.
    *   `AdminInterceptor.java`: "Guardia" que se ejecuta antes del controlador. Si la sesión no tiene el atributo `adminAutenticado`, redirige a `/admin/login`.
    *   `DataSeeder.java`: `CommandLineRunner` que inserta los 12 animales de ejemplo solo si la tabla está vacía.
*   **`model/` (MODELO)**:
    *   `Animal.java`: Entidad JPA. Una instancia = una fila de la tabla `animales`. Incluye las validaciones (`@NotBlank`, `@Size`).
*   **`dto/`**:
    *   `RespuestaBusqueda.java`: `record` con el "sobre" JSON del buscador: `animales`, `paginaActual`, `totalPaginas`, `totalElementos`.
*   **`repository/`**:
    *   `AnimalRepository.java`: Interfaz que extiende `JpaRepository<Animal, Long>`. Spring Data genera la implementación. Incluye la consulta JPQL `buscar(texto, pageable)`.
*   **`service/`**:
    *   `AnimalService.java`: Reglas de negocio: limpia el texto de búsqueda, fuerza página ≥ 0, ordena por nombre, fija 9 por página y asigna imagen por defecto al guardar.
    *   `AlmacenamientoService.java`: Valida y guarda las imágenes subidas desde el admin en la carpeta `uploads/` con nombre aleatorio (UUID).
*   **`controller/`**:
    *   `HomeController.java`: CONTROLADOR MVC. `GET /` devuelve la vista `index`.
    *   `AnimalApiController.java`: CONTROLADOR REST (`@RestController`). Expone 2 endpoints JSON para el frontend.
    *   `AdminController.java`: CONTROLADOR MVC del panel: login, logout, listado, crear, editar, guardar y eliminar.

### Frontend (Recursos estáticos y Plantillas)
Ubicado en `src/main/resources/`:

*   **`application.properties`**: Puerto, conexión a PostgreSQL, Hibernate, subida de archivos y credenciales del admin.
*   **`templates/index.html`**: VISTA Thymeleaf principal: encabezado, barra de búsqueda, rejilla, paginador y modal.
*   **`templates/admin/login.html`**: Formulario de acceso.
*   **`templates/admin/panel.html`**: Tabla con miniatura, nombre, nombre científico y botones Editar / Eliminar.
*   **`templates/admin/formulario.html`**: Formulario único para crear y editar (con vista previa de imagen y mensajes de validación).
*   **`static/css/estilos.css`**: Estilos minimalistas con variables CSS, rejilla 3 columnas y media query a 1 columna en ≤720 px.
*   **`static/js/buscador.js`**: Toda la lógica del navegador (ver tabla de funciones más abajo).
*   **`static/img/animales/*.svg`**: 12 ilustraciones minimalistas, una por animal.
*   **`static/img/placeholder.svg`**: Imagen por defecto si un animal se guarda sin imagen.

### Funciones de `buscador.js`
| Función | Qué hace |
| :--- | :--- |
| `crearTarjeta(animal)` | Construye una tarjeta con `createElement` y `textContent` (evita inyección de HTML). |
| `pintarResultados(datos)` | Vacía la rejilla, dibuja las tarjetas, actualiza el texto de estado y el paginador. Muestra "No encontramos ningún animal…" si no hay resultados. |
| `actualizarPaginador()` | Muestra u oculta la navegación y desactiva Anterior/Siguiente en los extremos. |
| `cargarResultados()` | Hace `fetch` a `/api/animales?q=..&pagina=..` y cancela la petición anterior con `AbortController`. |
| `abrirDetalle(id)` | Hace `fetch` a `/api/animales/{id}`, llena el modal y lo abre. |
| `cerrarDetalle()` | Cierra el modal y devuelve el foco a la tarjeta que lo abrió. |

Los clics en las tarjetas usan **delegación de eventos** sobre la rejilla, así funcionan tanto para las que pinta Thymeleaf como para las que pinta JavaScript.

---

## 🗄️ Base de Datos (PostgreSQL)

*   **Base:** `oceano_db`
*   **Tabla:** `animales` (la crea Hibernate con `spring.jpa.hibernate.ddl-auto=update`)

| Columna | Tipo | Notas |
| :--- | :--- | :--- |
| `id` | `bigint` (autoincremental) | Clave primaria |
| `nombre` | `varchar(100)` | Obligatorio |
| `nombre_cientifico` | `varchar(150)` | Opcional |
| `descripcion` | `text` | Obligatoria |
| `habitat` | `varchar(200)` | Opcional |
| `dieta` | `varchar(200)` | Opcional |
| `tamano` | `varchar(100)` | Opcional |
| `imagen_url` | `varchar(500)` | Ruta o URL de la imagen (la base no guarda el archivo) |

Consulta para verificar los datos en pgAdmin:
```sql
SELECT id, nombre, nombre_cientifico FROM animales ORDER BY nombre;
```

**Animales precargados (12):** Ballena azul, Delfín nariz de botella, Tiburón blanco, Pulpo común, Medusa luna, Tortuga verde, Caballito de mar, Estrella de mar, Mantarraya gigante, Pez payaso, Pez globo y Cangrejo pata roja.

---

## 🔌 Los 2 Endpoints REST (Internos)

| # | Método y Ruta | Qué hace |
| :--- | :--- | :--- |
| 1 | `GET /api/animales?q=ball&pagina=0` | Lista paginada (máx. 9) filtrada por nombre o nombre científico. Sin `q` devuelve todos. Sin `pagina` usa la 0. |
| 2 | `GET /api/animales/{id}` | Detalle completo de un animal (descripción, hábitat, alimentación, tamaño, imagen). Devuelve `404` si el id no existe. |

Ejemplo de respuesta del endpoint 1:
```json
{
  "animales": [
    {
      "id": 1,
      "nombre": "Ballena azul",
      "nombreCientifico": "Balaenoptera musculus",
      "descripcion": "...",
      "habitat": "...",
      "dieta": "...",
      "tamano": "...",
      "imagenUrl": "/img/animales/ballena-azul.svg"
    }
  ],
  "paginaActual": 0,
  "totalPaginas": 1,
  "totalElementos": 1
}
```

---

## 🧭 Rutas de la Aplicación

| Ruta | Método | Controlador | Resultado |
| :--- | :--- | :--- | :--- |
| `/` | GET | `HomeController` | Página principal (`index.html`) |
| `/api/animales` | GET | `AnimalApiController` | JSON de búsqueda |
| `/api/animales/{id}` | GET | `AnimalApiController` | JSON de un animal |
| `/admin/login` | GET / POST | `AdminController` | Formulario de acceso / validación |
| `/admin/logout` | GET | `AdminController` | Cierra la sesión |
| `/admin` | GET | `AdminController` | Panel con la tabla |
| `/admin/nuevo` | GET | `AdminController` | Formulario vacío |
| `/admin/editar/{id}` | GET | `AdminController` | Formulario con datos |
| `/admin/guardar` | POST | `AdminController` | Crea o actualiza (con imagen opcional) |
| `/admin/eliminar/{id}` | POST | `AdminController` | Elimina un animal |
| `/uploads/**` | GET | `WebConfig` (recurso estático) | Imágenes subidas desde el admin |

---

## 🔐 Seguridad del Admin

*   Todas las rutas `/admin/**` pasan por `AdminInterceptor`; solo `/admin/login` es pública.
*   Credenciales en `application.properties` (`atlas.admin.usuario` y `atlas.admin.clave`). **Cámbialas** antes de compartir el proyecto.
*   La comparación de usuario y clave usa `MessageDigest.isEqual` (tiempo constante) y se renueva el id de sesión al iniciar sesión (`changeSessionId`).
*   La sesión dura 30 minutos (`server.servlet.session.timeout`).
*   Eliminar es `POST`, no `GET`, y pide confirmación en el navegador.
*   **Subida de imágenes:** solo `jpg`, `jpeg`, `png`, `webp` y `gif`, máximo 5 MB. Se excluye SVG a propósito porque un SVG subido puede contener scripts. El archivo se guarda con nombre UUID.
*   **Limitación conocida:** este login es sencillo y no incluye protección CSRF ni contraseñas cifradas. Para un sitio público real se recomienda migrar a Spring Security.

---

## 🚨 Manejo de Errores

Este proyecto **no usa** un `GlobalExceptionHandler`; los errores se manejan en cada punto:

| Situación | Comportamiento |
| :--- | :--- |
| Formulario del admin con campos inválidos | Se vuelve a mostrar el formulario con el mensaje junto a cada campo (`@Valid` + `BindingResult`). |
| Imagen con formato no permitido | Mensaje de error bajo el bloque de imagen; no se guarda el animal. |
| Editar un id que no existe | Redirige al panel con el mensaje "El animal no existe". |
| `GET /api/animales/{id}` con id inexistente | Responde `404` sin cuerpo; `buscador.js` muestra "No se pudo abrir la ficha del animal." |
| Falla la petición de búsqueda | `buscador.js` muestra "No se pudo cargar la información. Intenta de nuevo." |
| Login incorrecto | Se vuelve a mostrar el login con "Usuario o contraseña incorrectos". |
| Sesión no iniciada en `/admin/**` | Redirección a `/admin/login`. |

---

## 🧯 Solución de Problemas

| Error en la consola | Causa probable |
| :--- | :--- |
| `password authentication failed` | La contraseña de `application.properties` no coincide con la de PostgreSQL. |
| `database "oceano_db" does not exist` | Falta crear la base de datos. |
| `Connection refused` | El servicio de PostgreSQL no está corriendo o el puerto no es 5432. |
| `Port 8080 was already in use` | Hay otra aplicación en ese puerto; cámbialo con `server.port=8081`. |
| `relation "animales" does not exist` (en pgAdmin) | La app aún no arrancó correctamente, por eso Hibernate no creó la tabla. |
| Las tarjetas salen sin imagen | Revisa que existan los `.svg` en `static/img/animales/` y que `imagen_url` empiece con `/img/animales/`. |

---

## 🌿 Flujo de Ramas (Git)

`feature/*` ➔ `dev` ➔ `qa` ➔ `main`

| Rama | Propósito |
| :--- | :--- |
| `main` | Versiones estables y etiquetadas (`v1.0.0`, ...). |
| `qa` | Pruebas y validación antes de liberar. |
| `dev` | Integración del desarrollo diario. |
| `feature/nombre-corto` | Una rama por cada cambio, creada desde `dev`. |

Reglas:
*   Nunca se trabaja directo en `dev`, `qa` ni `main`: todo entra por Pull Request.
*   Feature ➔ `dev`: **Squash and merge**. Promociones `dev` ➔ `qa` y `qa` ➔ `main`: **Create a merge commit**.
*   Mensajes de commit: `feat:`, `fix:`, `docs:`, `style:`, `refactor:`, `chore:`.
*   Las versiones se marcan con `git tag -a v1.0.0 -m "mensaje"` sobre `main`.

---

## 🔭 Posibles Mejoras
*   Búsqueda que ignore tildes (`delfin` ➔ `Delfín`).
*   Spring Security con contraseñas cifradas y CSRF.
*   Variables de entorno para la contraseña de la base de datos.
*   `GlobalExceptionHandler` con respuestas JSON estandarizadas.
*   Pruebas automáticas (JUnit) para el servicio y los controladores.
*   Eliminar del disco la imagen subida cuando se borra o se reemplaza un animal.
