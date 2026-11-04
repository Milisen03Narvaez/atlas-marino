# 🌊 Atlas Marino — Arquitectura MVC

Aplicación web construida con **Java 17** y **Spring Boot 3.4**, que muestra un catálogo de animales marinos guardado en **PostgreSQL**. Sigue una arquitectura **MVC (Modelo-Vista-Controlador)** sencilla: el servidor genera todo el HTML con **Thymeleaf** y los estilos usan **Tailwind CSS**. No hay JavaScript propio, ni archivos CSS propios, ni Node.js.

Incluye un panel de administración protegido por login para crear, editar y eliminar animales.

> A diferencia de un proyecto que consume una API externa, aquí los datos son propios: viven en la tabla `animales` de PostgreSQL y se administran desde `/admin`.

## 🧰 Tecnologías

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Java 17 |
| Framework | Spring Boot 3.4.1 (Web, Data JPA, Validation) |
| Vista | Thymeleaf (renderizado en el servidor) |
| Estilos | Tailwind CSS 3.4.17 (librería por CDN) |
| Base de datos | PostgreSQL |
| Construcción | Maven |

## 🚀 Funcionalidades

- **Renderizado en el servidor:** cada página llega ya armada desde Thymeleaf.
- **Buscador:** formulario `GET`; al dejar de escribir (400 ms) se envía solo y el servidor responde. Busca por nombre o nombre científico, sin distinguir mayúsculas, pero sí tildes (`delfin` no encuentra `Delfín`).
- **Rejilla 3 × 3:** máximo 9 tarjetas por página (`AnimalService.TAMANO_PAGINA = 9`).
- **Paginación:** enlaces normales `← Anterior` / `Siguiente →` con el texto `Página X de Y`, que conservan la búsqueda. Solo aparecen si hay más de 9 resultados.
- **Popup de detalle:** al abrir una tarjeta se ve imagen, nombre, nombre científico, descripción, hábitat, alimentación y tamaño. Funciona con CSS puro (`:target` mediante la variante `target:flex` de Tailwind). Se cierra con `← Regresar` o con clic en el fondo oscuro.
- **Diseño responsivo:** 3 columnas en escritorio y 1 en celular.
- **Panel de administración:** login con sesión HTTP, tabla de animales, formulario de crear/editar con validación, eliminación con confirmación y subida de imágenes.
- **Datos iniciales:** al arrancar con la tabla vacía se insertan 12 animales de ejemplo con su ilustración.

## 🛠️ Requisitos

- Java 17 o superior (JDK).
- IntelliJ IDEA (trae Maven integrado) o Maven 3.8+.
- PostgreSQL (probado con la versión 17) y pgAdmin 4 para ver los datos.
- **Conexión a internet:** Maven descarga las librerías la primera vez, y el navegador carga Tailwind desde su CDN cada vez que se abre una página.

## ⚙️ Instalación y ejecución

1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/Milisen03Narvaez/atlas-marino.git
   cd atlas-marino
   git switch Dev
   ```

2. **Crear la base de datos** (solo la base vacía; las tablas las crea la aplicación):
   ```sql
   CREATE DATABASE oceano_db WITH ENCODING 'UTF8' TEMPLATE template0;
   ```
   También está en `sql/crear_base_datos.sql`.

3. **Configurar la conexión** en `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/oceano_db
   spring.datasource.username=postgres
   spring.datasource.password=TU_CONTRASEÑA
   ```

4. **Ejecutar**
   - IntelliJ: abre `AtlasMarinoApplication.java` y pulsa ▶.
   - Terminal: `mvn spring-boot:run`

5. **Abrir en el navegador**
   - Sitio: http://localhost:8080
   - Admin: http://localhost:8080/admin (usuario `admin`, clave `admin123`)

> ⚠️ No abras `index.html` con doble clic: necesita la aplicación corriendo para recibir los datos.

## 🏗️ Arquitectura MVC

```
Navegador ──► AnimalController ──► AnimalService ──► AnimalRepository ──► PostgreSQL
                   │
                   └──► Model ──► plantilla Thymeleaf (HTML)
```

Ningún controlador habla directo con la base: siempre pasa por el servicio, y solo el repositorio ejecuta consultas.

| Flujo | Recorrido |
|-------|-----------|
| Página principal, búsqueda y paginación | `GET /?q=ball&pagina=1` → `AnimalController.inicio()` → `AnimalService.buscar()` → `AnimalRepository` → PostgreSQL → `Model` → `index.html` |
| Popup de detalle | Sin petición extra: cada animal de la página trae su ventana en el HTML y se muestra con CSS (`#animal-ID`) |
| Administración | `/admin/**` → `AnimalController` (cada método revisa `esAdmin()`) → `AnimalService` → PostgreSQL / carpeta `uploads/` → `templates/admin/*.html` |

## 📂 Estructura del proyecto

```
atlas-marino/
├── sql/crear_base_datos.sql
├── pom.xml
└── src/main/
    ├── java/com/oceano/atlas/
    │   ├── AtlasMarinoApplication.java
    │   ├── controller/AnimalController.java
    │   ├── model/Animal.java
    │   ├── repository/AnimalRepository.java
    │   └── service/AnimalService.java
    └── resources/
        ├── application.properties
        ├── static/img/            (ilustraciones .svg y placeholder)
        └── templates/
            ├── index.html
            └── admin/ (login.html, panel.html, formulario.html)
```

### Backend (Java)

- **`AtlasMarinoApplication`**: clase con el `main()` que arranca Spring Boot.
- **`controller/AnimalController`** (la **C**): único controlador. Contiene la página principal, la entrega de imágenes subidas y todo el admin (login, logout, panel, crear, editar, guardar, eliminar).
- **`model/Animal`** (la **M**): entidad JPA. Una instancia = una fila de `animales`. Incluye las validaciones (`@NotBlank`, `@Size`).
- **`repository/AnimalRepository`**: interfaz que extiende `JpaRepository<Animal, Long>`. Spring Data genera la implementación e incluye la consulta de búsqueda paginada.
- **`service/AnimalService`**: reglas de negocio. Limpia el texto de búsqueda, fuerza página ≥ 0, ordena por nombre, fija 9 por página, asigna imagen por defecto al guardar y valida y guarda las imágenes subidas con nombre aleatorio (UUID).

### Frontend (`src/main/resources/`)

- **`templates/index.html`** (la **V**): encabezado, buscador, rejilla, paginador y popups.
- **`templates/admin/`**: `login.html`, `panel.html` y `formulario.html`.
- **`static/img/`**: 12 ilustraciones `.svg` y la imagen por defecto.
- **Estilos:** clases de Tailwind escritas directamente en las plantillas. No existe ningún `.css` ni `.js` propio.

## 🗄️ Base de datos (PostgreSQL)

Base: `oceano_db` · Tabla: `animales` (la crea Hibernate con `spring.jpa.hibernate.ddl-auto=update`)

| Columna | Tipo | Notas |
|---------|------|-------|
| id | bigint (autoincremental) | Clave primaria |
| nombre | varchar(100) | Obligatorio |
| nombre_cientifico | varchar(150) | Opcional |
| descripcion | text | Obligatoria |
| habitat | varchar(200) | Opcional |
| dieta | varchar(200) | Opcional |
| tamano | varchar(100) | Opcional |
| imagen_url | varchar(500) | Ruta o URL de la imagen (la base no guarda el archivo) |

Para verificar los datos en pgAdmin:
```sql
SELECT id, nombre, nombre_cientifico FROM animales ORDER BY nombre;
```

## 🧭 Rutas de la aplicación

Todas están en `AnimalController`.

| Ruta | Método | Resultado |
|------|--------|-----------|
| `/` | GET | Página principal. Parámetros opcionales: `q` y `pagina` |
| `/uploads/{nombre}` | GET | Imagen subida desde el admin |
| `/admin/login` | GET / POST | Formulario de acceso / validación |
| `/admin/logout` | GET | Cierra la sesión |
| `/admin` | GET | Panel con la tabla |
| `/admin/nuevo` | GET | Formulario vacío |
| `/admin/editar/{id}` | GET | Formulario con datos |
| `/admin/guardar` | POST | Crea o actualiza (imagen opcional) |
| `/admin/eliminar/{id}` | POST | Elimina un animal |

## 🔐 Seguridad del admin

- Cada método de `/admin/**` empieza con `esAdmin()`; si no hay sesión, redirige a `/admin/login`.
- Las credenciales están en `application.properties` (`atlas.admin.usuario` y `atlas.admin.clave`). **Cámbialas antes de compartir el proyecto.**
- Usuario y clave se comparan con `MessageDigest.isEqual` (tiempo constante) y se renueva el id de sesión al entrar (`changeSessionId`).
- Eliminar es `POST`, no `GET`, y pide confirmación en el navegador.
- Subida de imágenes: solo jpg, jpeg, png, webp y gif, máximo 5 MB. Se excluye SVG a propósito porque puede contener scripts. El archivo se guarda con nombre UUID.
- **Limitación conocida:** el login es sencillo y no incluye protección CSRF ni contraseñas cifradas. Para un sitio público se recomienda migrar a Spring Security.

## 🚨 Manejo de errores

| Situación | Comportamiento |
|-----------|----------------|
| Formulario con campos inválidos | Se vuelve a mostrar con el mensaje junto a cada campo (`@Valid` + `BindingResult`) |
| Imagen con formato no permitido | Mensaje de error bajo el bloque de imagen; no se guarda el animal |
| Editar un id que no existe | Redirige al panel con "El animal no existe" |
| Imagen subida inexistente | Responde 404 |
| Login incorrecto | Se vuelve a mostrar el login con "Usuario o contraseña incorrectos" |
| Sesión no iniciada en `/admin/**` | Redirección a `/admin/login` |

## 🧯 Solución de problemas

| Error en la consola | Causa probable |
|---------------------|----------------|
| `password authentication failed` | La contraseña de `application.properties` no coincide con la de PostgreSQL |
| `database "oceano_db" does not exist` | Falta crear la base de datos |
| `Connection refused` | PostgreSQL no está corriendo o el puerto no es 5432 |
| `Port 8080 was already in use` | Otra aplicación usa el puerto; cambia `server.port=8081` |
| La página se ve sin estilos | No hay internet: Tailwind se carga desde su CDN |
| Las tarjetas salen sin imagen | Revisa que existan los `.svg` en `static/img/animales/` y que `imagen_url` empiece con `/img/animales/` |

## 🌿 Flujo de ramas (Git)

`feature/*` ➔ `Dev` ➔ `qa` ➔ `main`

| Rama | Propósito |
|------|-----------|
| `main` | Versiones estables y etiquetadas (`v1.0.0`, ...) |
| `qa` | Pruebas y validación antes de liberar |
| `Dev` | Integración del desarrollo diario |
| `feature/ATL-N-nombre` | Una rama por tarea de Jira, creada desde `Dev` |

Reglas:
- Nunca se trabaja directo en `Dev`, `qa` ni `main`: todo entra por Pull Request.
- Feature ➔ Dev: **Squash and merge**. Promociones Dev ➔ qa y qa ➔ main: **Create a merge commit**.
- Mensajes de commit: `feat:`, `fix:`, `docs:`, `style:`, `refactor:`, `chore:`, con el código de la tarea, por ejemplo `feat(ATL-2): ...`.
- Las versiones se marcan con `git tag -a v1.0.0 -m "mensaje"` sobre `main`.

## 🔭 Próximos pasos

- Actualizar el proyecto y su configuración a la versión indicada por el documento de Docker (ATL-3).
- Publicar detrás de AWS API Gateway, ocultando el backend en EC2 (ATL-4).
- Compilar Tailwind para producción en lugar de usar el CDN.
- Búsqueda que ignore tildes.
- Spring Security con contraseñas cifradas y CSRF.
- Variables de entorno para la contraseña de la base de datos.
- Pruebas automáticas (JUnit) para el servicio y el controlador.
- Eliminar del disco la imagen subida cuando se borra o reemplaza un animal.