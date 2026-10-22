package com.oceano.atlas.config;

import com.oceano.atlas.service.AlmacenamientoService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CONFIGURACIÓN DE SPRING MVC. Hace dos conexiones importantes:
 *
 *  1. Registra AdminInterceptor para proteger todo /admin/** (menos el login).
 *  2. Publica la carpeta física "uploads" como la URL /uploads/**, para que el navegador pueda mostrar
 *     las imágenes que sube el admin (Spring solo sirve por defecto la carpeta resources/static).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;
    private final AlmacenamientoService almacenamientoService;

    /** Spring inyecta el interceptor y el servicio de archivos (necesitamos saber dónde está la carpeta). */
    public WebConfig(AdminInterceptor adminInterceptor, AlmacenamientoService almacenamientoService) {
        this.adminInterceptor = adminInterceptor;
        this.almacenamientoService = almacenamientoService;
    }

    /** Conexión 1: el guardia protege /admin/** excepto /admin/login. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/login");
    }

    /** Conexión 2: URL /uploads/xxx.jpg  ->  archivo físico  <carpeta uploads>/xxx.jpg */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(almacenamientoService.getCarpeta().toUri().toString());
    }
}
