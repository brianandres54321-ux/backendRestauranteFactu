package com.empresafac.backend_factu.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.extraerClaims(token);
                String username = claims.getSubject();
                String rol = claims.get("rol", String.class);
                Long empresaId = claims.get("empresaId", Long.class);

                // CORRECCIÓN: Evitar ROLE_ROLE_ADMIN
                String authority = rol.startsWith("ROLE_") ? rol : "ROLE_" + rol;

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                );

                // Guardamos el empresaId en los detalles para usarlo en los Services
                auth.setDetails(empresaId);

                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // Si el token falla, limpiamos el contexto para asegurar el 403 real
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
