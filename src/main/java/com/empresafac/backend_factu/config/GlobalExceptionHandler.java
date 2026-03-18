package com.empresafac.backend_factu.config;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Convierte RuntimeException en respuestas JSON con el mensaje correcto
 * en vez de devolver 403 o 500 genérico.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        String mensaje = ex.getMessage();

        // Errores de plan → 402 Payment Required
        if (mensaje != null && (mensaje.contains("plan") ||
                mensaje.contains("Plan") ||
                mensaje.contains("MercadoPago") ||
                mensaje.contains("Actualiza"))) {
            return ResponseEntity
                    .status(HttpStatus.PAYMENT_REQUIRED) // 402
                    .body(Map.of("message", mensaje));
        }

        // Errores de negocio genéricos → 400
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(Map.of("message", mensaje != null ? mensaje : "Error inesperado"));
    }
}