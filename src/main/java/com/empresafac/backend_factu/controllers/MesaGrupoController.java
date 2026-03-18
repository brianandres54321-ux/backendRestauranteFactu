package com.empresafac.backend_factu.controllers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
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
import com.empresafac.backend_factu.dto_temp.response.MesaGrupoResponse;
import com.empresafac.backend_factu.entities.MesaGrupo;
import com.empresafac.backend_factu.services.MesaGrupoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/mesas/grupos")
@RequiredArgsConstructor
public class MesaGrupoController {

    private final MesaGrupoService mesaGrupoService;
    private final EmpresaContext empresaContext;

    private void validarAccesoEmpresa(Long empresaIdUrl) {
        Long empresaIdToken = empresaContext.getEmpresaIdActual();
        if (!empresaIdToken.equals(empresaIdUrl)) {
            throw new RuntimeException("Acceso denegado: ID de empresa no válido.");
        }
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @GetMapping
    public List<MesaGrupoResponse> listar(@PathVariable Long empresaId) {
        validarAccesoEmpresa(empresaId);
        return mesaGrupoService.listar(empresaId).stream()
                .map(g -> new MesaGrupoResponse(g.getId(), g.getEstado().name(), g.getCreadoEn()))
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @GetMapping("/{id}")
    public MesaGrupoResponse obtener(@PathVariable Long empresaId, @PathVariable Long id) {
        validarAccesoEmpresa(empresaId);
        MesaGrupo g = mesaGrupoService.obtener(empresaId, id);
        return new MesaGrupoResponse(g.getId(), g.getEstado().name(), g.getCreadoEn());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/estado")
    public MesaGrupoResponse cambiarEstado(@PathVariable Long empresaId, @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        validarAccesoEmpresa(empresaId);
        String nuevoEstado = request.get("estado").replace("\"", "").trim();
        MesaGrupo g = mesaGrupoService.actualizarEstado(empresaId, id, MesaGrupo.Estado.valueOf(nuevoEstado));
        return new MesaGrupoResponse(g.getId(), g.getEstado().name(), g.getCreadoEn());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'MESERO')")
    @PostMapping("/unir")
    public ResponseEntity<Void> unirMesas(@PathVariable Long empresaId,
            @RequestBody Map<String, List<Long>> request) {
        validarAccesoEmpresa(empresaId);
        List<Long> mesasIds = request.get("mesasIds");
        mesaGrupoService.unirMesas(empresaId, mesasIds);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'MESERO')")
    @DeleteMapping("/desunir/{mesaId}")
    public ResponseEntity<Void> desunirMesa(@PathVariable Long empresaId,
            @PathVariable Long mesaId) {
        validarAccesoEmpresa(empresaId);
        mesaGrupoService.desunirMesa(empresaId, mesaId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long empresaId, @PathVariable Long id) {
        validarAccesoEmpresa(empresaId);
        mesaGrupoService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
