package com.empresafac.backend_factu.config;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.entities.Usuario;
import com.empresafac.backend_factu.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public String login(String email, String password) {

        Usuario usuario = usuarioRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!usuario.getActivo()) {
            throw new RuntimeException("Usuario inactivo");
        }

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        return jwtService.generarToken(usuario);
    }

    /**
     * Genera un nuevo token con el plan actualizado del usuario.
     * Se usa cuando la empresa cambia de plan, para refrescar los claims del JWT.
     */
    public String refreshToken(String username) {
        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return jwtService.generarToken(usuario);
    }
}