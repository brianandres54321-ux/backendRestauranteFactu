package com.empresafac.backend_factu.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.empresafac.backend_factu.dto_temp.request.InventarioRequest;
import com.empresafac.backend_factu.dto_temp.response.InventarioResponse;
import com.empresafac.backend_factu.entities.Inventario;
import com.empresafac.backend_factu.entities.Producto;
import com.empresafac.backend_factu.services.InventarioService;
import com.empresafac.backend_factu.services.ProductoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;
    private final ProductoService productoService;

    @GetMapping
    public List<InventarioResponse> listar(@PathVariable Long empresaId) {

        return inventarioService.listar(empresaId)
                .stream()
                .map(i -> new InventarioResponse(
                        i.getProducto().getId(),
                        i.getStockActual(),
                        i.getStockMinimo()))
                .collect(Collectors.toList());
    }

    @GetMapping("/producto/{productoId}")
    public InventarioResponse obtener(
            @PathVariable Long empresaId,
            @PathVariable Long productoId) {

        Inventario i = inventarioService.obtener(empresaId, productoId);

        return new InventarioResponse(
                i.getProducto().getId(),
                i.getStockActual(),
                i.getStockMinimo());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public InventarioResponse crear( @PathVariable Long empresaId, @RequestBody InventarioRequest req) {

        Producto producto = productoService.obtenerEntidad(
                empresaId,
                req.getProductoId()
        );

        Inventario i = inventarioService.crear(
                empresaId,
                producto,
                req.getStockActual(),
                req.getStockMinimo()
        );

        return new InventarioResponse(
                i.getProducto().getId(),
                i.getStockActual(),
                i.getStockMinimo());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/producto/{productoId}")
    public InventarioResponse actualizar(
            @PathVariable Long empresaId,
            @PathVariable Long productoId,
            @RequestBody InventarioRequest req) {

        Inventario i = inventarioService.actualizar(
                empresaId,
                productoId,
                req.getStockActual(),
                req.getStockMinimo()
        );

        return new InventarioResponse(
                i.getProducto().getId(),
                i.getStockActual(),
                i.getStockMinimo());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/producto/{productoId}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long empresaId,
            @PathVariable Long productoId) {

        inventarioService.eliminar(empresaId, productoId);

        return ResponseEntity.noContent().build();
    }
}