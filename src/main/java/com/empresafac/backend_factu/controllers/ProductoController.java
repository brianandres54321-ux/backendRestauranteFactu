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
import com.empresafac.backend_factu.dto_temp.request.ProductoRequest;
import com.empresafac.backend_factu.dto_temp.response.ProductoResponse;
import com.empresafac.backend_factu.entities.Producto;
import com.empresafac.backend_factu.services.ProductoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;
    private final EmpresaContext empresaContext;

    @GetMapping
    public List<ProductoResponse> listar(@PathVariable Long empresaId) {
        return productoService.listar(empresaId).stream()
                .map(p -> new ProductoResponse(
                p.getId(), p.getNombre(), p.getDescripcion(), p.getCodigoBarras(),
                p.getActivo(), null))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long empresaId,
            @PathVariable Long id) {
        Producto p = productoService.obtener(empresaId, id);
        return new ProductoResponse(
                p.getId(), p.getNombre(), p.getDescripcion(), p.getCodigoBarras(),
                p.getActivo(), null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ProductoResponse crear(@PathVariable Long empresaId,
            @RequestBody ProductoRequest req) {
        Producto p = new Producto();
        // empresa se asigna automáticamente en servicio si es necesario
        p.setNombre(req.getNombre());
        p.setDescripcion(req.getDescripcion());
        p.setCodigoBarras(req.getCodigoBarras());
        Producto saved = productoService.guardar(p);
        return new ProductoResponse(
                saved.getId(), saved.getNombre(), saved.getDescripcion(), saved.getCodigoBarras(),
                saved.getActivo(), null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long empresaId,
            @PathVariable Long id,
            @RequestBody ProductoRequest req) {
        Producto p = productoService.obtener(empresaId, id);
        p.setNombre(req.getNombre());
        p.setDescripcion(req.getDescripcion());
        p.setCodigoBarras(req.getCodigoBarras());
        Producto saved = productoService.guardar(p);
        return new ProductoResponse(
                saved.getId(), saved.getNombre(), saved.getDescripcion(), saved.getCodigoBarras(),
                saved.getActivo(), null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long empresaId,
            @PathVariable Long id) {
        productoService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
