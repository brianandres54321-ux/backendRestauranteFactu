package com.empresafac.backend_factu.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresafac.backend_factu.dto_temp.request.AbrirPedidoRequest;
import com.empresafac.backend_factu.dto_temp.request.AgregarProductoRequest;
import com.empresafac.backend_factu.dto_temp.request.RegistrarPagoRequest;
import com.empresafac.backend_factu.dto_temp.response.PedidoResponse;
import com.empresafac.backend_factu.entities.Pago;
import com.empresafac.backend_factu.entities.Pedido;
import com.empresafac.backend_factu.entities.Usuario;
import com.empresafac.backend_factu.services.PedidoService;
import com.empresafac.backend_factu.services.UsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'MESERO')")
    @PostMapping("/abrir")
    public PedidoResponse abrir(@PathVariable Long empresaId,
            @RequestBody AbrirPedidoRequest req) {
        Usuario usuario = usuarioService.obtenerPorId(empresaId, req.getUsuarioId());
        return pedidoService.abrirPedido(empresaId, req.getMesaId(), usuario);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'MESERO')")
    @PostMapping("/{pedidoId}/productos")
    public PedidoResponse agregarProducto(@PathVariable Long empresaId,
            @PathVariable Long pedidoId,
            @RequestBody AgregarProductoRequest req) {
        return pedidoService.agregarProducto(empresaId, pedidoId, req.getProductoId(), req.getCantidad());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'MESERO')")
    @DeleteMapping("/items/{itemId}")
    public PedidoResponse eliminarItem(
            @PathVariable("empresaId") Long empresaId,
            @PathVariable("itemId") Long itemId) {
        return pedidoService.eliminarItem(empresaId, itemId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'MESERO')")
    @PostMapping("/{pedidoId}/pago")
    public PedidoResponse registrarPago(@PathVariable Long empresaId,
            @PathVariable Long pedidoId,
            @RequestBody RegistrarPagoRequest req) {
        return pedidoService.registrarPago(empresaId, pedidoId, Pago.Metodo.valueOf(req.getMetodo()), req.getMonto());
    }

    @GetMapping
    public List<PedidoResponse> listar(@PathVariable Long empresaId,
            @RequestParam(required = false) Pedido.Estado estado) {
        if (estado == null) {
            estado = Pedido.Estado.ABIERTO;
        }
        return pedidoService.listarPorEmpresa(empresaId, estado).stream()
                .map(pedidoService::construirResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{pedidoId}")
    public PedidoResponse obtener(@PathVariable Long empresaId,
            @PathVariable Long pedidoId) {
        Pedido p = pedidoService.obtener(empresaId, pedidoId);
        return pedidoService.construirResponse(p);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @PutMapping("/{pedidoId}/cerrar")
    public PedidoResponse cerrar(@PathVariable Long empresaId,
            @PathVariable Long pedidoId) {
        return pedidoService.cerrarPedido(empresaId, pedidoId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO')")
    @PutMapping("/{pedidoId}/cancelar")
    public PedidoResponse cancelar(@PathVariable Long empresaId,
            @PathVariable Long pedidoId) {
        return pedidoService.cancelarPedido(empresaId, pedidoId);
    }

    // ✅ NUEVO — Restaurar pedido cancelado a ABIERTO
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{pedidoId}/restaurar")
    public PedidoResponse restaurar(@PathVariable Long empresaId,
            @PathVariable Long pedidoId) {
        return pedidoService.restaurarPedido(empresaId, pedidoId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CAJERO', 'MESERO')")
    @PatchMapping("/{pedidoId}/cocina")
    public PedidoResponse actualizarEstadoCocina(
            @PathVariable Long empresaId,
            @PathVariable Long pedidoId,
            @RequestParam String estado) {
        return pedidoService.actualizarEstadoCocina(empresaId, pedidoId, estado);
    }
}