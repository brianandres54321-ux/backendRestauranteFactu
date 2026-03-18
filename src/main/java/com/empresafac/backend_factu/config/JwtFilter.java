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

        // ✅ Dejar pasar OPTIONS sin validar token — son preflights de CORS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            System.err.println("TOKEN RECIBIDO: " + header.substring(7));
        } else {
            System.err.println("AUTORIZACIÓN AUSENTE O MAL FORMADA en: " + request.getRequestURI());
        }

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.extraerClaims(token);
                String username = claims.getSubject();
                String rol = claims.get("rol", String.class);

                if (username != null && rol != null) {
                    Long empresaId = ((Number) claims.get("empresaId")).longValue();

                    String rolUpper = rol.toUpperCase();
                    String authority = rolUpper.startsWith("ROLE_") ? rolUpper : "ROLE_" + rolUpper;

                    System.out.println("DEBUG SECURITY: Usuario=" + username + " | Authority=" + authority);

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(new SimpleGrantedAuthority(authority)));

                    auth.setDetails(empresaId);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    System.err.println("TOKEN INVÁLIDO: username=" + username + " | rol=" + rol);
                }
            } catch (Exception e) {
                System.err.println("ERROR AL PROCESAR TOKEN: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}