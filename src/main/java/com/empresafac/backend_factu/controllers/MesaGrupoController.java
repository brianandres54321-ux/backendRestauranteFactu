package com.empresafac.backend_factu.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping
    public List<MesaGrupoResponse> listar(@PathVariable Long empresaId) {
        return mesaGrupoService.listar(empresaId).stream()
                .map(g -> new MesaGrupoResponse(g.getId(), g.getEstado().name(), g.getCreadoEn()))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public MesaGrupoResponse obtener(@PathVariable Long empresaId, @PathVariable Long id) {
        MesaGrupo g = mesaGrupoService.obtener(empresaId, id);
        return new MesaGrupoResponse(g.getId(), g.getEstado().name(), g.getCreadoEn());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/estado")
    public MesaGrupoResponse cambiarEstado(@PathVariable Long empresaId, @PathVariable Long id,
            @RequestBody String estado) {
        MesaGrupo g = mesaGrupoService.actualizarEstado(empresaId, id, MesaGrupo.Estado.valueOf(estado));
        return new MesaGrupoResponse(g.getId(), g.getEstado().name(), g.getCreadoEn());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long empresaId, @PathVariable Long id) {
        mesaGrupoService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
