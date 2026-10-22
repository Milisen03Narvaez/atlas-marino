/*
 * buscador.js  —  JavaScript del navegador para la página principal (index.html).
 *
 * Hace TRES cosas:
 *   1. Búsqueda inmediata: cada vez que escribes una letra, pide los resultados al servidor y repinta
 *      las tarjetas sin recargar la página.
 *   2. Paginación: botones Anterior / Siguiente (la rejilla muestra máximo 9 = 3 columnas x 3 filas).
 *   3. Popup de detalle: al hacer clic en una tarjeta pide los datos completos y abre el modal,
 *      que se cierra con "Regresar", con Esc o haciendo clic en el fondo.
 *
 * CONEXIONES CON EL RESTO DEL PROYECTO:
 *   index.html  ──carga──►  buscador.js
 *   buscador.js ──fetch GET /api/animales?q=..&pagina=..──►  AnimalApiController.buscar()   ──► JSON RespuestaBusqueda
 *   buscador.js ──fetch GET /api/animales/{id}──────────────►  AnimalApiController.detalle() ──► JSON Animal
 *   Los datos viajan: PostgreSQL → AnimalRepository → AnimalService → AnimalApiController → JSON → este archivo.
 */
(function () {
    'use strict';

    /* ---------- 1. Referencias a elementos de index.html (se buscan por su id) ---------- */
    const campo = document.getElementById('campoBusqueda');
    const rejilla = document.getElementById('rejilla');
    const estado = document.getElementById('estadoResultados');
    const paginacion = document.getElementById('paginacion');
    const btnAnterior = document.getElementById('btnAnterior');
    const btnSiguiente = document.getElementById('btnSiguiente');
    const indicador = document.getElementById('indicadorPagina');

    const modal = document.getElementById('modal');
    const modalFondo = document.getElementById('modalFondo');
    const btnRegresar = document.getElementById('btnRegresar');

    /* ---------- 2. Estado de la pantalla ---------- */
    let textoActual = '';          // lo que hay escrito en el buscador
    let paginaActual = 0;          // página que se está viendo (empieza en 0, igual que en el servidor)
    let totalPaginas = Number(rejilla.dataset.totalPaginas) || 1; // viene de index.html (th:attr data-total-paginas)
    let controlador = null;        // AbortController: permite cancelar la petición anterior si escribes rápido
    let elementoQueAbrio = null;   // tarjeta que abrió el popup, para devolverle el foco al cerrar

    /* ---------- 3. Pintar tarjetas ---------- */

    /**
     * crearTarjeta: construye el HTML de UNA tarjeta con la misma estructura que th:each en index.html.
     * Usa createElement + textContent (en lugar de innerHTML) para que ningún texto de la base de datos
     * pueda inyectar código HTML o JavaScript.
     * @param {Object} animal objeto JSON con id, nombre, nombreCientifico, imagenUrl...
     */
    function crearTarjeta(animal) {
        const tarjeta = document.createElement('article');
        tarjeta.className = 'tarjeta';
        tarjeta.dataset.id = animal.id;        // se usará al hacer clic para pedir el detalle
        tarjeta.tabIndex = 0;                  // permite enfocarla con el teclado
        tarjeta.setAttribute('role', 'button');

        const imagen = document.createElement('img');
        imagen.src = animal.imagenUrl;
        imagen.alt = animal.nombre;
        imagen.loading = 'lazy';

        const texto = document.createElement('div');
        texto.className = 'tarjeta-texto';

        const titulo = document.createElement('h2');
        titulo.textContent = animal.nombre;

        const cientifico = document.createElement('p');
        cientifico.className = 'cientifico';
        cientifico.textContent = animal.nombreCientifico || '';

        texto.append(titulo, cientifico);
        tarjeta.append(imagen, texto);
        return tarjeta;
    }

    /**
     * pintarResultados: recibe el JSON de AnimalApiController.buscar() y actualiza toda la pantalla:
     * tarjetas, texto de estado y paginador.
     * @param {{animales:Array, paginaActual:number, totalPaginas:number, totalElementos:number}} datos
     */
    function pintarResultados(datos) {
        paginaActual = datos.paginaActual;
        totalPaginas = datos.totalPaginas;

        rejilla.replaceChildren();   // vacía la rejilla

        if (datos.animales.length === 0) {
            const vacio = document.createElement('p');
            vacio.className = 'sin-resultados';
            vacio.textContent = 'No encontramos ningún animal con ese nombre.';
            rejilla.append(vacio);
            estado.textContent = 'Sin resultados';
        } else {
            datos.animales.forEach(function (animal) {
                rejilla.append(crearTarjeta(animal));
            });
            estado.textContent = textoActual
                ? datos.totalElementos + (datos.totalElementos === 1 ? ' resultado' : ' resultados')
                : datos.totalElementos + ' especies';
        }
        actualizarPaginador();
    }

    /** actualizarPaginador: muestra u oculta los botones según haya más de una página. */
    function actualizarPaginador() {
        paginacion.hidden = totalPaginas <= 1;
        indicador.textContent = 'Página ' + (paginaActual + 1) + ' de ' + totalPaginas;
        btnAnterior.disabled = paginaActual <= 0;
        btnSiguiente.disabled = paginaActual >= totalPaginas - 1;
    }

    /* ---------- 4. Pedir datos al servidor ---------- */

    /**
     * cargarResultados: llama a  GET /api/animales?q=<texto>&pagina=<n>  y pinta la respuesta.
     * AbortController cancela la petición anterior: si escribes "bal" y enseguida "ball",
     * solo cuenta la última y nunca se pintan resultados viejos por error.
     */
    async function cargarResultados() {
        if (controlador) {
            controlador.abort();
        }
        controlador = new AbortController();

        const url = '/api/animales?q=' + encodeURIComponent(textoActual) + '&pagina=' + paginaActual;
        try {
            const respuesta = await fetch(url, { signal: controlador.signal });
            if (!respuesta.ok) {
                throw new Error('HTTP ' + respuesta.status);
            }
            pintarResultados(await respuesta.json());
        } catch (error) {
            if (error.name === 'AbortError') {
                return;   // petición cancelada a propósito, no es un fallo
            }
            estado.textContent = 'No se pudo cargar la información. Intenta de nuevo.';
        }
    }

    /* ---------- 5. Eventos del buscador y del paginador ---------- */

    // "input" se dispara con CADA letra escrita o borrada -> resultados inmediatos, sin pulsar Enter.
    campo.addEventListener('input', function () {
        textoActual = campo.value.trim();
        paginaActual = 0;            // cada búsqueda nueva empieza en la primera página
        cargarResultados();
    });

    btnAnterior.addEventListener('click', function () {
        if (paginaActual > 0) {
            paginaActual--;
            cargarResultados();
        }
    });

    btnSiguiente.addEventListener('click', function () {
        if (paginaActual < totalPaginas - 1) {
            paginaActual++;
            cargarResultados();
        }
    });

    /* ---------- 6. Popup de detalle ---------- */

    /**
     * abrirDetalle: pide GET /api/animales/{id}, llena el modal con los datos y lo muestra.
     * @param {string} id identificador del animal (viene de data-id de la tarjeta)
     */
    async function abrirDetalle(id) {
        try {
            const respuesta = await fetch('/api/animales/' + encodeURIComponent(id));
            if (!respuesta.ok) {
                throw new Error('HTTP ' + respuesta.status);
            }
            const animal = await respuesta.json();

            document.getElementById('modalImagen').src = animal.imagenUrl;
            document.getElementById('modalImagen').alt = animal.nombre;
            document.getElementById('modalTitulo').textContent = animal.nombre;
            document.getElementById('modalCientifico').textContent = animal.nombreCientifico || '';
            document.getElementById('modalDescripcion').textContent = animal.descripcion;
            document.getElementById('modalHabitat').textContent = animal.habitat || '—';
            document.getElementById('modalDieta').textContent = animal.dieta || '—';
            document.getElementById('modalTamano').textContent = animal.tamano || '—';

            modal.hidden = false;                          // muestra el popup
            document.body.classList.add('sin-scroll');     // evita que la página de fondo se desplace
            btnRegresar.focus();
        } catch (error) {
            estado.textContent = 'No se pudo abrir la ficha del animal.';
        }
    }

    /** cerrarDetalle: oculta el popup y devuelve el foco a la tarjeta que lo abrió. */
    function cerrarDetalle() {
        modal.hidden = true;
        document.body.classList.remove('sin-scroll');
        if (elementoQueAbrio) {
            elementoQueAbrio.focus();
        }
    }

    /**
     * DELEGACIÓN DE EVENTOS: un solo "click" en la rejilla sirve para todas las tarjetas,
     * incluidas las que se crean después con crearTarjeta() (las que pinta Thymeleaf y las que pinta JS).
     */
    rejilla.addEventListener('click', function (evento) {
        const tarjeta = evento.target.closest('.tarjeta');
        if (tarjeta) {
            elementoQueAbrio = tarjeta;
            abrirDetalle(tarjeta.dataset.id);
        }
    });

    // Accesibilidad: Enter o Espacio sobre una tarjeta enfocada = clic.
    rejilla.addEventListener('keydown', function (evento) {
        if (evento.key === 'Enter' || evento.key === ' ') {
            const tarjeta = evento.target.closest('.tarjeta');
            if (tarjeta) {
                evento.preventDefault();
                elementoQueAbrio = tarjeta;
                abrirDetalle(tarjeta.dataset.id);
            }
        }
    });

    // Tres formas de cerrar el popup: botón Regresar, clic en el fondo oscuro, tecla Esc.
    btnRegresar.addEventListener('click', cerrarDetalle);
    modalFondo.addEventListener('click', cerrarDetalle);
    document.addEventListener('keydown', function (evento) {
        if (evento.key === 'Escape' && !modal.hidden) {
            cerrarDetalle();
        }
    });

    /* ---------- 7. Estado inicial ---------- */
    actualizarPaginador();   // muestra el paginador si la carga inicial tiene más de una página
})();
