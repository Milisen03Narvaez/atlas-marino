package com.oceano.atlas.config;

import com.oceano.atlas.controller.AdminController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * INTERCEPTOR = "guardia de la puerta". Se ejecuta ANTES de que Spring llame al controlador.
 *
 * WebConfig lo registra para /admin/**. Flujo:
 *   Navegador pide /admin/nuevo -> AdminInterceptor.preHandle()
 *       ├─ ¿hay sesión con adminAutenticado = true?  SÍ -> deja pasar a AdminController
 *       └─ NO -> redirige a /admin/login y el controlador ni se ejecuta.
 *
 * Así no hay que repetir la comprobación dentro de cada método del AdminController.
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    /**
     * @return true = continuar hacia el controlador; false = detener la petición (ya se envió la redirección).
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        HttpSession sesion = request.getSession(false);   // false = no crear sesión si no existe
        boolean autenticado = sesion != null
                && Boolean.TRUE.equals(sesion.getAttribute(AdminController.SESION_ADMIN));

        if (!autenticado) {
            response.sendRedirect(request.getContextPath() + "/admin/login");
            return false;
        }
        return true;
    }
}
