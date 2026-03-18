package com.empresafac.backend_factu.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresafac.backend_factu.config.AuthService;
import com.empresafac.backend_factu.dto_temp.LoginRequest;

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

    /**
     * Emite un nuevo token JWT con el plan actualizado del usuario autenticado.
     * Llamar después de cambiar el plan de la empresa.
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@AuthenticationPrincipal UserDetails userDetails) {
        String token = authService.refreshToken(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("token", token));
    }
}