package com.oceano.atlas.config;

import com.oceano.atlas.model.Animal;
import com.oceano.atlas.service.AnimalService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * CARGADOR DE DATOS INICIALES.
 *
 * CommandLineRunner: Spring ejecuta el método run() UNA vez, justo después de arrancar y de que Hibernate
 * haya creado la tabla "animales". Si la tabla está vacía, inserta 12 animales de ejemplo con su imagen.
 * Si ya hay datos (por ejemplo, la segunda vez que arrancas) no hace nada, así no se duplican.
 *
 * Cadena: DataSeeder -> AnimalService.guardar() -> AnimalRepository.save() -> INSERT en PostgreSQL.
 * Las imágenes (.svg) están en src/main/resources/static/img/animales/ y se piden como /img/animales/...
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final AnimalService animalService;

    public DataSeeder(AnimalService animalService) {
        this.animalService = animalService;
    }

    /** Inserta los animales de ejemplo solo si la base está vacía. */
    @Override
    public void run(String... args) {
        if (animalService.contar() > 0) {
            return;
        }

        crear("Ballena azul", "Balaenoptera musculus",
                "Es el animal más grande que ha existido en la Tierra. Su corazón late muy despacio y su canto puede "
                        + "viajar decenas de kilómetros bajo el agua. A pesar de su tamaño colosal, se alimenta de organismos diminutos.",
                "Todos los océanos, desde aguas polares hasta tropicales", "Krill (pequeños crustáceos)",
                "Hasta 30 m de largo y más de 100 toneladas", "ballena-azul");

        crear("Delfín nariz de botella", "Tursiops truncatus",
                "Mamífero marino muy inteligente y social que vive en grupos llamados manadas. Se comunica con silbidos, "
                        + "usa la ecolocalización para cazar y es famoso por sus saltos y su curiosidad hacia los barcos.",
                "Aguas costeras y oceánicas templadas y tropicales", "Peces y calamares",
                "De 2 a 4 m de largo", "delfin-nariz-de-botella");

        crear("Tiburón blanco", "Carcharodon carcharias",
                "Gran depredador en la cima de la cadena alimenticia. Detecta olores y campos eléctricos con gran precisión "
                        + "y renueva sus dientes durante toda la vida. Cumple un papel clave en el equilibrio de los ecosistemas marinos.",
                "Aguas costeras y de mar abierto, templadas", "Peces grandes, focas y lobos marinos",
                "De 4 a 6 m de largo", "tiburon-blanco");

        crear("Pulpo común", "Octopus vulgaris",
                "Molusco de ocho brazos con tres corazones y sangre azul. Es un excelente estratega: resuelve problemas, "
                        + "se esconde cambiando el color y la textura de su piel y se desliza por grietas muy estrechas.",
                "Fondos rocosos y arrecifes poco profundos", "Cangrejos, almejas y pequeños peces",
                "Hasta 1 m con los brazos extendidos", "pulpo-comun");

        crear("Medusa luna", "Aurelia aurita",
                "Medusa translúcida reconocible por los cuatro anillos que se ven a través de su campana. No tiene cerebro ni "
                        + "corazón: se desplaza pulsando su cuerpo suavemente y deja que las corrientes hagan el resto.",
                "Aguas costeras de casi todo el mundo", "Plancton y larvas pequeñas",
                "De 25 a 40 cm de diámetro", "medusa-luna");

        crear("Tortuga verde", "Chelonia mydas",
                "Viajera incansable que recorre miles de kilómetros entre sus zonas de alimentación y las playas donde nació, "
                        + "a las que regresa para anidar. De adulta se alimenta de algas y pastos marinos, y ayuda a mantenerlos sanos.",
                "Mares tropicales y subtropicales, incluidas las islas Galápagos", "Algas y pastos marinos (adultas)",
                "Hasta 1,5 m de largo", "tortuga-verde");

        crear("Caballito de mar", "Hippocampus",
                "Pez de aspecto singular que nada en posición vertical y se sujeta a algas y corales con su cola prensil. "
                        + "Es uno de los pocos animales en los que el macho incuba las crías en una bolsa especial.",
                "Praderas marinas, manglares y arrecifes poco profundos", "Pequeños crustáceos y plancton",
                "De 2 a 35 cm según la especie", "caballito-de-mar");

        crear("Estrella de mar", "Asteroidea",
                "Equinodermo sin cerebro ni sangre: usa agua de mar para mover cientos de diminutos pies tubulares. "
                        + "Puede regenerar brazos perdidos y, al comer, saca su estómago fuera del cuerpo para digerir a su presa.",
                "Todos los mares, desde la orilla hasta zonas profundas", "Moluscos, como mejillones y almejas",
                "De pocos centímetros a más de 50 cm según la especie", "estrella-de-mar");

        crear("Mantarraya gigante", "Mobula birostris",
                "Planea por el agua moviendo sus enormes aletas como si fueran alas. Tiene uno de los cerebros más "
                        + "grandes entre los peces y suele visitar \"estaciones de limpieza\", donde peces pequeños le quitan los parásitos.",
                "Aguas tropicales y subtropicales de mar abierto", "Zooplancton",
                "Hasta 7 m de envergadura", "mantarraya-gigante");

        crear("Pez payaso", "Amphiprion ocellaris",
                "Vive entre los tentáculos de las anémonas, cuyo veneno no le afecta gracias a una capa protectora de moco. "
                        + "Ambos se benefician: la anémona lo protege y el pez la limpia y la defiende de otros animales.",
                "Arrecifes de coral del Indo-Pacífico", "Omnívoro: algas, plancton y restos de comida",
                "De 8 a 11 cm", "pez-payaso");

        crear("Pez globo", "Tetraodontidae",
                "Cuando se siente amenazado traga agua y se infla hasta parecer una pelota, y en muchas especies eso deja "
                        + "al descubierto sus espinas. Varias especies contienen una toxina muy potente, por lo que no deben tocarse ni consumirse.",
                "Aguas tropicales y subtropicales, arrecifes y estuarios", "Invertebrados, algas y crustáceos",
                "De pocos centímetros a más de 60 cm según la especie", "pez-globo");

        crear("Cangrejo pata roja", "Grapsus grapsus",
                "Muy común en las rocas de la costa del Pacífico oriental, incluidas las islas Galápagos. Es ágil y "
                        + "difícil de atrapar, y recorre las rocas mojadas por las olas en busca de algas para alimentarse.",
                "Costas rocosas del Pacífico oriental", "Algas y pequeños restos orgánicos",
                "Hasta unos 8 cm de caparazón", "cangrejo-pata-roja");
    }

    /**
     * Método auxiliar: arma un objeto Animal y lo guarda a través del servicio.
     * @param archivoImagen nombre del .svg (sin extensión) dentro de static/img/animales/
     */
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
        animalService.guardar(animal);
    }
}
