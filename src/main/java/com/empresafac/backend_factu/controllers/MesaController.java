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
import com.empresafac.backend_factu.dto_temp.request.MesaGrupoRequest;
import com.empresafac.backend_factu.dto_temp.request.MesaRequest;
import com.empresafac.backend_factu.dto_temp.response.MesaGrupoResponse;
import com.empresafac.backend_factu.dto_temp.response.MesaResponse;
import com.empresafac.backend_factu.entities.Mesa;
import com.empresafac.backend_factu.entities.MesaGrupo;
import com.empresafac.backend_factu.services.MesaGrupoService;
import com.empresafac.backend_factu.services.MesaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/mesas")
@RequiredArgsConstructor
public class MesaController {

    private final MesaService mesaService;
    private final MesaGrupoService mesaGrupoService;
    private final EmpresaContext empresaContext;

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @PostMapping
    public MesaResponse crear(@RequestBody MesaRequest req) {
        Long empresaId = empresaContext.getEmpresaIdActual();
        Mesa mesa = mesaService.crear(empresaId, req.getSeccionId(), req.getNombre());
        return new MesaResponse(mesa.getId(), mesa.getNombre(), mesa.getEstado().name(), mesa.getActiva());
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @GetMapping
    public List<MesaResponse> listar() {
        Long empresaId = empresaContext.getEmpresaIdActual();
        return mesaService.listar(empresaId).stream()
                .map(m -> new MesaResponse(m.getId(), m.getNombre(), m.getEstado().name(), m.getActiva()))
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/unir")
    public MesaGrupoResponse unirMesas(@RequestBody MesaGrupoRequest req) {
        Long empresaId = empresaContext.getEmpresaIdActual();
        mesaGrupoService.unirMesas(empresaId, req.getMesasIds());
        // We could return created group info but service returns void; let's return OK.
        return new MesaGrupoResponse(null, MesaGrupo.Estado.ACTIVO.name(), null);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @PutMapping("/{id}")
    public MesaResponse actualizar(@PathVariable Long id, @RequestBody MesaRequest req) {
        Long empresaId = empresaContext.getEmpresaIdActual();
        Mesa mesa = mesaService.actualizar(empresaId, id, req.getNombre(), req.getSeccionId());
        return new MesaResponse(mesa.getId(), mesa.getNombre(), mesa.getEstado().name(), mesa.getActiva());
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @PutMapping("/{id}/estado")
    public MesaResponse cambiarEstado(@PathVariable Long id, @RequestBody String estado) {
        Long empresaId = empresaContext.getEmpresaIdActual();
        Mesa mesa = mesaService.cambiarEstado(empresaId, id, Mesa.Estado.valueOf(estado));
        return new MesaResponse(mesa.getId(), mesa.getNombre(), mesa.getEstado().name(), mesa.getActiva());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        Long empresaId = empresaContext.getEmpresaIdActual();
        mesaService.eliminar(empresaId, id);
    }

}
