package com.empresafac.backend_factu.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PagoController.class);

    private final PagoService pagoService;

    // ── Pago manual ───────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN') or hasRole('CAJERO')")
    @PostMapping("/pedido/{pedidoId}")
    public PagoResponse registrar(@PathVariable Long empresaId,
            @PathVariable Long pedidoId,
            @RequestBody RegistrarPagoRequest req) {
        pagoService.registrarPago(empresaId, pedidoId, Pago.Metodo.valueOf(req.getMetodo()),
                req.getMonto(), req.getCodigoCupon());
        List<Pago> pagos = pagoService.listarPorPedido(empresaId, pedidoId);
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
            log.error("Error MercadoPago API ({}): {}", e.getStatusCode(), e.getApiResponse().getContent());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error MP (" + e.getStatusCode() + "): " + e.getApiResponse().getContent());

        } catch (MPException e) {
            log.error("Error del SDK de MercadoPago", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error MP SDK: " + e.getMessage());

        } catch (Exception e) {
            log.error("Error interno al crear preferencia de MercadoPago", e);
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
            log.info("Webhook recibido: empresaId={} type={} paymentId={}", empresaId, type, paymentId);
            if ("payment".equals(type) && paymentId != null) {
                pagoService.procesarWebhook(empresaId, paymentId);
            }
        } catch (Exception e) {
            log.error("Error procesando webhook de MercadoPago para empresaId={}", empresaId, e);
        }
        return ResponseEntity.ok().build();
    }

    // ── Listar ────────────────────────────────────────────────────

    @GetMapping("/pedido/{pedidoId}")
    public List<PagoResponse> listarPorPedido(@PathVariable Long empresaId, @PathVariable Long pedidoId) {
        return pagoService.listarPorPedido(empresaId, pedidoId).stream()
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
