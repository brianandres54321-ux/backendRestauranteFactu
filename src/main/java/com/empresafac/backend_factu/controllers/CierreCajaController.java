package com.empresafac.backend_factu.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresafac.backend_factu.dto_temp.request.CierreCajaRequest;
import com.empresafac.backend_factu.dto_temp.response.CierreCajaResponse;
import com.empresafac.backend_factu.repositories.UsuarioRepository;
import com.empresafac.backend_factu.services.CierreCajaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/cierres")
@RequiredArgsConstructor
public class CierreCajaController {

    private final CierreCajaService cierreService;
    private final UsuarioRepository usuarioRepository;

    /**
     * GET /empresas/{id}/cierres/preview?fecha=2026-03-18
     * Devuelve el resumen del día sin guardar nada.
     * Si ya existe un cierre para esa fecha, lo devuelve con yaExiste=true.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @GetMapping("/preview")
    public ResponseEntity<CierreCajaResponse> preview(
            @PathVariable Long empresaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        return ResponseEntity.ok(cierreService.preview(empresaId, dia));
    }

    /**
     * POST /empresas/{id}/cierres
     * Ejecuta y guarda el cierre del día.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @PostMapping
    public ResponseEntity<CierreCajaResponse> ejecutar(
            @PathVariable Long empresaId,
            @RequestBody CierreCajaRequest req) {

        // El JwtFilter registra el username como String en el principal
        String username = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Long usuarioId = usuarioRepository
                .findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"))
                .getId();

        LocalDate fecha = (req.getFecha() != null && !req.getFecha().isBlank())
                ? LocalDate.parse(req.getFecha())
                : LocalDate.now();

        return ResponseEntity.ok(
                cierreService.ejecutarCierre(empresaId, usuarioId, fecha, req.getNotas()));
    }

    /**
     * GET /empresas/{id}/cierres
     * Historial de todos los cierres (sin detalle de pagos).
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @GetMapping
    public ResponseEntity<List<CierreCajaResponse>> historial(
            @PathVariable Long empresaId) {
        return ResponseEntity.ok(cierreService.historial(empresaId));
    }

    /**
     * GET /empresas/{id}/cierres/{cierreId}
     * Detalle de un cierre específico (incluye lista de pagos del día).
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @GetMapping("/{cierreId}")
    public ResponseEntity<CierreCajaResponse> detalle(
            @PathVariable Long empresaId,
            @PathVariable Long cierreId) {
        return ResponseEntity.ok(cierreService.detalle(empresaId, cierreId));
    }
}