package com.empresafac.backend_factu.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresafac.backend_factu.dto_temp.request.MercadoPagoPreferenciaRequest;
import com.empresafac.backend_factu.dto_temp.request.RegistrarPagoRequest;
import com.empresafac.backend_factu.dto_temp.response.MercadoPagoPreferenciaResponse;
import com.empresafac.backend_factu.dto_temp.response.PagoResponse;
import com.empresafac.backend_factu.entities.Pago;
import com.empresafac.backend_factu.services.PagoService;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    // ── Pago manual ───────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @PostMapping("/pedido/{pedidoId}")
    public PagoResponse registrar(@PathVariable Long empresaId,
            @PathVariable Long pedidoId,
            @RequestBody RegistrarPagoRequest req) {
        pagoService.registrarPago(pedidoId, Pago.Metodo.valueOf(req.getMetodo()),
                req.getMonto(), req.getCodigoCupon());
        List<Pago> pagos = pagoService.listarPorPedido(pedidoId);
        Pago p = pagos.get(pagos.size() - 1);
        return toResponse(p);
    }

    // ── Mercado Pago — crear preferencia ─────────────────────────

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @PostMapping("/mercadopago/preferencia")
    public ResponseEntity<?> crearPreferencia(
            @PathVariable Long empresaId,
            @RequestBody MercadoPagoPreferenciaRequest req) {
        try {
            MercadoPagoPreferenciaResponse response = pagoService.crearPreferencia(empresaId, req.getPedidoId(),
                    req.getUrlRetorno());
            return ResponseEntity.ok(response);

        } catch (MPApiException e) {
            System.err.println("=== ERROR MERCADOPAGO API ===");
            System.err.println("Status: " + e.getStatusCode());
            System.err.println("Respuesta: " + e.getApiResponse().getContent());
            System.err.println("=============================");
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error MP (" + e.getStatusCode() + "): " + e.getApiResponse().getContent());

        } catch (MPException e) {
            System.err.println("=== ERROR SDK MP: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error MP SDK: " + e.getMessage());

        } catch (Exception e) {
            System.err.println("=== ERROR INTERNO: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    // ── Mercado Pago — webhook (público, sin JWT) ─────────────────

    @PostMapping("/mercadopago/webhook")
    public ResponseEntity<Void> webhook(
            @PathVariable Long empresaId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "data.id", required = false) String paymentId) {
        try {
            System.out.println(">>> WEBHOOK: type=" + type + " | paymentId=" + paymentId);
            if ("payment".equals(type) && paymentId != null) {
                pagoService.procesarWebhook(paymentId);
            }
        } catch (Exception e) {
            System.err.println("Error en webhook: " + e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    // ── Listar ────────────────────────────────────────────────────

    @GetMapping("/pedido/{pedidoId}")
    public List<PagoResponse> listarPorPedido(@PathVariable Long pedidoId) {
        return pagoService.listarPorPedido(pedidoId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @GetMapping
    public List<PagoResponse> listarPorEmpresa(@PathVariable Long empresaId) {
        return pagoService.listarPorEmpresa(empresaId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    private PagoResponse toResponse(Pago p) {
        return new PagoResponse(p.getId(), p.getPedido().getId(),
                p.getMetodo().name(), p.getMonto(), p.getFecha());
    }
}