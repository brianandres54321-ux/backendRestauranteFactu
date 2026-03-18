package com.empresafac.backend_factu.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    // Listar productos
    @GetMapping
    public List<ProductoResponse> listar(@PathVariable Long empresaId) {
        return productoService.listar(empresaId);
    }

    // Obtener producto por ID
    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long empresaId, @PathVariable Long id) {
        return productoService.obtener(empresaId, id);
    }

    // Crear producto
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse crear(@PathVariable Long empresaId, @RequestBody ProductoRequest req) {

        // 1. Mapeo de datos
        Producto producto = new Producto();
        producto.setNombre(req.getNombre());
        producto.setDescripcion(req.getDescripcion());
        producto.setCodigoBarras(req.getCodigoBarras());
        producto.setImagenUrl(req.getImagenUrl());

        // 2. Llamada al servicio (Asegúrate de que NO haya un return antes de esta
        // línea)
        return productoService.crear(
                empresaId,
                producto,
                req.getCategoriaId(),
                req.getPrecioVenta(),
                req.getCosto(),
                req.getStockInicial());
    }

    // Actualizar producto
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse actualizar(
            @PathVariable Long empresaId,
            @PathVariable Long id,
            @RequestBody ProductoRequest req) {

        Producto producto = new Producto();
        producto.setNombre(req.getNombre());
        producto.setDescripcion(req.getDescripcion());
        producto.setCodigoBarras(req.getCodigoBarras());
        producto.setImagenUrl(req.getImagenUrl());

        return productoService.actualizar(
                empresaId,
                id,
                producto,
                req.getCategoriaId(),
                req.getPrecioVenta(),
                req.getStockInicial());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long empresaId, @PathVariable Long id) {
        productoService.eliminar(empresaId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activar(@PathVariable Long empresaId, @PathVariable Long id) {
        productoService.activar(empresaId, id);
        return ResponseEntity.noContent().build();
    }

    // Listar solo productos eliminados (Inactivos)
    @GetMapping("/inactivos")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProductoResponse> listarInactivos(@PathVariable Long empresaId) {
        return productoService.listarInactivos(empresaId);
    }

}