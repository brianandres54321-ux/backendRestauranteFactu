package com.empresafac.backend_factu.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresafac.backend_factu.Security.EmpresaContext;
import com.empresafac.backend_factu.dto_temp.request.PrecioRequest;
import com.empresafac.backend_factu.dto_temp.response.PrecioResponse;
import com.empresafac.backend_factu.entities.Precio;
import com.empresafac.backend_factu.services.PrecioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/precios")
@RequiredArgsConstructor 
public class PrecioController {

    private final PrecioService precioService;
    private final EmpresaContext empresaContext;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public PrecioResponse asignar(@PathVariable Long empresaId,
            @RequestBody PrecioRequest req) {
        Precio p = precioService.asignarPrecio(empresaId, req.getProductoId(), req.getPrecioVenta(), req.getCosto());
        return new PrecioResponse(p.getId(), p.getProducto().getId(), p.getPrecioVenta(), p.getCosto(), p.getActivo(), p.getFechaInicio());
    }

    @GetMapping("/producto/{productoId}")
    public PrecioResponse obtenerActual(@PathVariable Long empresaId, @PathVariable Long productoId) {
        Precio p = precioService.obtenerPrecioActual(empresaId, productoId);
        return new PrecioResponse(p.getId(), p.getProducto().getId(), p.getPrecioVenta(), p.getCosto(), p.getActivo(), p.getFechaInicio());
    }

    @GetMapping("/producto/{productoId}/historico")
    public List<PrecioResponse> historico(@PathVariable Long empresaId, @PathVariable Long productoId) {
        return precioService.historicoPrecios(empresaId, productoId).stream()
                .map(p -> new PrecioResponse(p.getId(), p.getProducto().getId(), p.getPrecioVenta(), p.getCosto(), p.getActivo(), p.getFechaInicio()))
                .collect(Collectors.toList());
    }

    @GetMapping
    public List<PrecioResponse> listarEmpresa(@PathVariable Long empresaId) {
        return precioService.listarPorEmpresa(empresaId).stream()
                .map(p -> new PrecioResponse(p.getId(), p.getProducto().getId(), p.getPrecioVenta(), p.getCosto(), p.getActivo(), p.getFechaInicio()))
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long empresaId, @PathVariable Long id) {
        precioService.desactivarPrecio(empresaId, id);
        return ResponseEntity.noContent().build();
    }
}
