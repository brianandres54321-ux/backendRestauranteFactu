package com.empresafac.backend_factu.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresafac.backend_factu.dto_temp.request.CuponRequest;
import com.empresafac.backend_factu.dto_temp.response.CuponResponse;
import com.empresafac.backend_factu.dto_temp.response.ValidarCuponResponse;
import com.empresafac.backend_factu.services.CuponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/cupones")
@RequiredArgsConstructor
public class CuponController {

    private final CuponService cuponService;

    /** GET /empresas/{id}/cupones — listado para gestión (solo admin) */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<CuponResponse> listar(@PathVariable Long empresaId) {
        return cuponService.listar(empresaId);
    }

    /** POST /empresas/{id}/cupones — crear cupón */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CuponResponse> crear(
            @PathVariable Long empresaId,
            @RequestBody CuponRequest req) {
        return ResponseEntity.ok(cuponService.crear(empresaId, req));
    }

    /** PUT /empresas/{id}/cupones/{cuponId} — editar cupón */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{cuponId}")
    public ResponseEntity<CuponResponse> actualizar(
            @PathVariable Long empresaId,
            @PathVariable Long cuponId,
            @RequestBody CuponRequest req) {
        return ResponseEntity.ok(cuponService.actualizar(empresaId, cuponId, req));
    }

    /** DELETE /empresas/{id}/cupones/{cuponId} — eliminar cupón */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{cuponId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long empresaId,
            @PathVariable Long cuponId) {
        cuponService.eliminar(empresaId, cuponId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /empresas/{id}/cupones/validar?codigo=XXX&pedidoId=YYY
     * Valida un cupón y calcula el descuento sin aplicarlo todavía.
     * Accesible por Admin y Cajero (lo usan en el checkout).
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @GetMapping("/validar")
    public ResponseEntity<ValidarCuponResponse> validar(
            @PathVariable Long empresaId,
            @RequestParam String codigo,
            @RequestParam Long pedidoId) {
        return ResponseEntity.ok(cuponService.validar(empresaId, codigo, pedidoId));
    }
}