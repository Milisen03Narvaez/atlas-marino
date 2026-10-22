package com.oceano.atlas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PUNTO DE ENTRADA de toda la aplicación.
 *
 * @SpringBootApplication activa tres cosas a la vez:
 *   1. @Configuration        -> esta clase puede declarar beans.
 *   2. @EnableAutoConfiguration -> Spring Boot configura solo Tomcat, Thymeleaf, JPA, etc.
 *      según las librerías del pom.xml y los valores de application.properties.
 *   3. @ComponentScan        -> Spring escanea ESTE paquete (com.oceano.atlas) y todos los subpaquetes
 *      buscando clases con @Controller, @RestController, @Service, @Repository, @Component y @Configuration,
 *      crea UNA instancia de cada una y las conecta entre sí (inyección de dependencias por constructor).
 *
 * Por eso todos los paquetes (model, repository, service, controller, config) cuelgan de com.oceano.atlas:
 * si estuvieran fuera, Spring no los encontraría.
 */
@SpringBootApplication
public class AtlasMarinoApplication {

    /**
     * main: método que ejecuta IntelliJ al pulsar "Run".
     * SpringApplication.run arranca el contexto de Spring, conecta con PostgreSQL y levanta el servidor web
     * en http://localhost:8080
     */
    public static void main(String[] args) {
        SpringApplication.run(AtlasMarinoApplication.class, args);
    }
}
