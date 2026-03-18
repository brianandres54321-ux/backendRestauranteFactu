package com.empresafac.backend_factu.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresafac.backend_factu.Security.EmpresaContext;
import com.empresafac.backend_factu.dto_temp.request.EmpresaRequest;
import com.empresafac.backend_factu.dto_temp.response.EmpresaResponse;
import com.empresafac.backend_factu.entities.Empresa;
import com.empresafac.backend_factu.services.EmpresaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaService empresaService;
    private final EmpresaContext empresaContext;

    private EmpresaResponse toResponse(Empresa e) {
        return new EmpresaResponse(e.getId(), e.getNombre(), e.getNitRut(), e.getPlan(), e.getActiva());
    }

    // Público — para el login
    @GetMapping("/public")
    public List<EmpresaResponse> listar() {
        return empresaService.listar().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public EmpresaResponse obtener(@PathVariable Long id) {
        Empresa e = empresaService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        return toResponse(e);
    }

    // Obtener MI empresa (desde el token)
    @GetMapping("/mi-empresa")
    @PreAuthorize("hasRole('ADMIN')")
    public EmpresaResponse miEmpresa() {
        Long empresaId = empresaContext.getEmpresaIdActual();
        Empresa e = empresaService.buscarPorId(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        return toResponse(e);
    }

    @PostMapping
    public EmpresaResponse crear(@Valid @RequestBody EmpresaRequest req) {
        Empresa empresa = new Empresa();
        empresa.setNombre(req.getNombre());
        empresa.setNitRut(req.getNitRut());
        empresa.setPlan(req.getPlan() != null ? req.getPlan() : "BASICO");
        return toResponse(empresaService.crear(empresa));
    }

    // Actualizar MI empresa
    @PutMapping("/mi-empresa")
    @PreAuthorize("hasRole('ADMIN')")
    public EmpresaResponse actualizarMiEmpresa(@Valid @RequestBody EmpresaRequest req) {
        Long empresaId = empresaContext.getEmpresaIdActual();
        Empresa datos = new Empresa();
        datos.setNombre(req.getNombre());
        datos.setNitRut(req.getNitRut());
        datos.setPlan(req.getPlan());
        return toResponse(empresaService.actualizar(empresaId, datos));
    }

    // Cambiar plan — solo para superadmin (sin empresa en token, llamada directa)
    @PutMapping("/{id}/plan")
    public EmpresaResponse cambiarPlan(@PathVariable Long id, @RequestBody String plan) {
        String planLimpio = plan.replace("\"", "").trim().toUpperCase();
        Empresa e = empresaService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        e.setPlan(planLimpio);
        return toResponse(empresaService.crear(e));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        empresaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}