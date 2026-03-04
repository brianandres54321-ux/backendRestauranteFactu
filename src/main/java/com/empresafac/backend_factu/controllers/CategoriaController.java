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
import com.empresafac.backend_factu.dto_temp.request.CategoriaRequest;
import com.empresafac.backend_factu.dto_temp.response.CategoriaResponse;
import com.empresafac.backend_factu.entities.Categoria;
import com.empresafac.backend_factu.services.CategoriaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;
    private final EmpresaContext empresaContext;

    @GetMapping
    public List<CategoriaResponse> listar(@PathVariable Long empresaId) {
        return categoriaService.listar(empresaId).stream()
                .map(c -> new CategoriaResponse(c.getId(), c.getNombre(), c.getActiva()))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public CategoriaResponse obtener(@PathVariable Long empresaId, @PathVariable Long id) {
        Categoria c = categoriaService.obtener(empresaId, id);
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getActiva());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public CategoriaResponse crear(@PathVariable Long empresaId,
            @RequestBody CategoriaRequest req) {
        Categoria c = categoriaService.crear(empresaId, req.getNombre());
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getActiva());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public CategoriaResponse actualizar(@PathVariable Long empresaId,
            @PathVariable Long id,
            @RequestBody CategoriaRequest req) {
        Categoria c = categoriaService.actualizar(empresaId, id, req.getNombre());
        return new CategoriaResponse(c.getId(), c.getNombre(), c.getActiva());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long empresaId,
            @PathVariable Long id) {
        categoriaService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
