package com.empresafac.backend_factu.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresafac.backend_factu.config.AuthService;
import com.empresafac.backend_factu.dto_temp.LoginRequest;
import com.empresafac.backend_factu.dto_temp.request.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String token = authService.registrar(request);
        return ResponseEntity.ok(Map.of("token", token));
    }

    /**
     * Emite un nuevo token JWT con el plan actualizado del usuario autenticado.
     * Llamar después de cambiar el plan de la empresa.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(Authentication authentication) {
        String token = authService.refreshToken(authentication.getName());
        return ResponseEntity.ok(Map.of("token", token));
    }

}
