package com.empresafac.backend_factu.controllers;

import java.util.List;
import java.util.stream.Collectors;

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
import com.empresafac.backend_factu.dto_temp.request.MesaRequest;
import com.empresafac.backend_factu.dto_temp.response.MesaResponse;
import com.empresafac.backend_factu.entities.Mesa;
import com.empresafac.backend_factu.repositories.MesaGrupoDetalleRepository;
import com.empresafac.backend_factu.services.MesaGrupoService;
import com.empresafac.backend_factu.services.MesaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/mesas")
@RequiredArgsConstructor
public class MesaController {

    private final MesaService mesaService;
    private final MesaGrupoService mesaGrupoService;
    private final EmpresaContext empresaContext;
    private final MesaGrupoDetalleRepository detalleRepository;

    /**
     * Método unificado para construir MesaResponse con los 7 campos requeridos.
     */
    private MesaResponse mapearAMesaResponse(Mesa mesa) {
        // Buscamos si la mesa pertenece a un grupo activo
        Long grupoId = detalleRepository.findByMesaId(mesa.getId())
                .map(detalle -> detalle.getGrupo().getId())
                .orElse(null);

        return new MesaResponse(
                mesa.getId(),
                mesa.getNombre(),
                mesa.getEstado().name(),
                mesa.getActiva(),
                mesa.getSeccion() != null ? mesa.getSeccion().getId() : null,
                mesa.getSeccion() != null ? mesa.getSeccion().getNombre() : "Sin Sección",
                grupoId);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @PostMapping
    public MesaResponse crear(@PathVariable Long empresaId, @RequestBody MesaRequest req) {
        validarAcceso(empresaId);
        Mesa mesa = mesaService.crear(empresaId, req.getSeccionId(), req.getNombre());
        return mapearAMesaResponse(mesa);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'MESERO')")
    @GetMapping
    public List<MesaResponse> listar(@PathVariable Long empresaId) {
        validarAcceso(empresaId);
        return mesaService.listar(empresaId).stream()
                .map(this::mapearAMesaResponse)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @PutMapping("/{id}")
    public MesaResponse actualizar(@PathVariable Long empresaId, @PathVariable Long id, @RequestBody MesaRequest req) {
        validarAcceso(empresaId);
        Mesa mesa = mesaService.actualizar(empresaId, id, req.getNombre(), req.getSeccionId());
        return mapearAMesaResponse(mesa);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @PutMapping("/{id}/estado")
    public MesaResponse cambiarEstado(@PathVariable Long empresaId, @PathVariable Long id, @RequestBody String estado) {
        validarAcceso(empresaId);
        // Limpiamos el string por si viene con comillas del JSON
        String estadoLimpio = estado.replace("\"", "").trim();
        Mesa mesa = mesaService.cambiarEstado(empresaId, id, Mesa.Estado.valueOf(estadoLimpio));
        return mapearAMesaResponse(mesa);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long empresaId, @PathVariable Long id) {
        validarAcceso(empresaId);
        mesaService.eliminar(empresaId, id);
    }

    private void validarAcceso(Long empresaIdUrl) {
        Long empresaIdToken = empresaContext.getEmpresaIdActual();
        if (!empresaIdToken.equals(empresaIdUrl)) {
            throw new RuntimeException("Acceso denegado: Empresa no válida.");
        }
    }

}
