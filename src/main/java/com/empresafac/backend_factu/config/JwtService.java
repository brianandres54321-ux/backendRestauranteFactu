package com.empresafac.backend_factu.config;

import java.security.Key;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.entities.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final String SECRET
            = "MI_CLAVE_SUPER_SECRETA_DE_32_BYTES_MINIMO_123456";

    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String generarToken(Usuario usuario) {

        return Jwts.builder()
                .setSubject(usuario.getUsername())
                .claim("empresaId", usuario.getEmpresa().getId())
                .claim("rol", usuario.getRol().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 8))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extraerClaims(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
