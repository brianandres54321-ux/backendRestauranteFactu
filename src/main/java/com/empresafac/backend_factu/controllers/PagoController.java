package com.empresafac.backend_factu.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.empresafac.backend_factu.Security.EmpresaContext;
import com.empresafac.backend_factu.dto_temp.request.RegistrarPagoRequest;
import com.empresafac.backend_factu.dto_temp.response.PagoResponse;
import com.empresafac.backend_factu.entities.Pago;
import com.empresafac.backend_factu.services.PagoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;
    private final EmpresaContext empresaContext;

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @PostMapping("/pedido/{pedidoId}")
    public PagoResponse registrar(@PathVariable Long empresaId,
            @PathVariable Long pedidoId,
            @RequestBody RegistrarPagoRequest req) {
        pagoService.registrarPago(pedidoId, Pago.Metodo.valueOf(req.getMetodo()), req.getMonto());
        // return last payment for simplicity
        Pago p = pagoService.listarPorPedido(pedidoId).get(pagoService.listarPorPedido(pedidoId).size() - 1);
        return new PagoResponse(p.getId(), p.getPedido().getId(), p.getMetodo().name(), p.getMonto(), p.getFecha());
    }

    @GetMapping("/pedido/{pedidoId}")
    public List<PagoResponse> listarPorPedido(@PathVariable Long pedidoId) {
        return pagoService.listarPorPedido(pedidoId).stream()
                .map(p -> new PagoResponse(p.getId(), p.getPedido().getId(), p.getMetodo().name(), p.getMonto(), p.getFecha()))
                .collect(Collectors.toList());
    }

    @GetMapping
    public List<PagoResponse> listarPorEmpresa(@PathVariable Long empresaId) {
        return pagoService.listarPorEmpresa(empresaId).stream()
                .map(p -> new PagoResponse(p.getId(), p.getPedido().getId(), p.getMetodo().name(), p.getMonto(), p.getFecha()))
                .collect(Collectors.toList());
    }
}
