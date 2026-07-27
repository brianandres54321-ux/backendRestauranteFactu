package com.empresafac.backend_factu.Security;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Aplica una única regla a TODAS las rutas /empresas/{empresaId}/**:
 * el {empresaId} de la URL debe coincidir con el empresaId del JWT.
 *
 * Antes esta validación se repetía (o se olvidaba) controller por controller,
 * dejando la mayoría de los endpoints expuestos a que un usuario autenticado
 * de una empresa leyera/modificara datos de otra empresa con solo cambiar el
 * ID en la URL.
 */
@Component
public class TenantAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Object attr = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attr instanceof Map<?, ?> pathVariables)) {
            return true;
        }

        Object empresaIdRaw = pathVariables.get("empresaId");
        if (empresaIdRaw == null) {
            return true;
        }

        Long empresaIdUrl;
        try {
            empresaIdUrl = Long.valueOf(empresaIdRaw.toString());
        } catch (NumberFormatException e) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getDetails() instanceof Long empresaIdToken)) {
            // Sin empresaId en el token: ruta pública (p.ej. webhook de MercadoPago).
            // Spring Security ya decide si esta ruta puede pasar sin autenticación.
            return true;
        }

        if (!empresaIdToken.equals(empresaIdUrl)) {
            throw new TenantAccessDeniedException("Acceso denegado: la empresa del token no coincide con la empresa solicitada.");
        }

        return true;
    }
}
