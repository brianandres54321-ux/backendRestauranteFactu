package com.empresafac.backend_factu.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.dto_temp.response.MercadoPagoPreferenciaResponse;
import com.empresafac.backend_factu.entities.Mesa;
import com.empresafac.backend_factu.entities.Pago;
import com.empresafac.backend_factu.entities.Pedido;
import com.empresafac.backend_factu.repositories.MesaRepository;
import com.empresafac.backend_factu.repositories.PagoRepository;
import com.empresafac.backend_factu.repositories.PedidoRepository;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final PlanValidadorService planValidador;
    private final MesaGrupoService mesaGrupoService;
    private final CuponService cuponService;

    // URL pública de ngrok — actualiza este valor cada vez que reinicies ngrok
    // Ejemplo: https://abc123.ngrok-free.app
    @Value("${app.ngrok-url:http://localhost:8080}")
    private String ngrokUrl;

    // ─────────────────────────────────────────────────────────────
    // PAGO MANUAL
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void registrarPago(Long empresaId, Long pedidoId, Pago.Metodo metodo,
            BigDecimal monto, String codigoCupon) {

        Pedido pedido = pedidoRepository.findByIdAndEmpresaId(pedidoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() != Pedido.Estado.ABIERTO) {
            throw new RuntimeException("Pedido no está abierto");
        }

        // Aplicar descuento si viene cupón
        BigDecimal montoFinal = monto;
        if (codigoCupon != null && !codigoCupon.isBlank()) {
            BigDecimal descuento = cuponService.aplicar(
                    pedido.getEmpresa().getId(), codigoCupon, pedido.getTotal());
            montoFinal = monto.subtract(descuento).max(BigDecimal.ZERO);
        }

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodo(metodo);
        pago.setMonto(montoFinal);
        pagoRepository.save(pago);

        BigDecimal totalPagado = pagoRepository.findAllByPedidoId(pedidoId)
                .stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPagado.compareTo(pedido.getTotal()) >= 0) {
            cerrarPedido(pedido);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // MERCADO PAGO — Crear preferencia
    // ─────────────────────────────────────────────────────────────

    public MercadoPagoPreferenciaResponse crearPreferencia(Long empresaId, Long pedidoId, String urlRetorno)
            throws MPException, MPApiException {

        // ✅ Validar que el plan incluye MercadoPago
        planValidador.validarMercadoPago(empresaId);

        Pedido pedido = pedidoRepository.findByIdAndEmpresaId(pedidoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() != Pedido.Estado.ABIERTO) {
            throw new RuntimeException("El pedido ya fue pagado o cancelado");
        }

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title("Pedido #" + pedidoId)
                .quantity(1)
                .unitPrice(pedido.getTotal())
                .currencyId("COP")
                .build();

        // notificationUrl usa ngrok para que MP pueda llamar a localhost
        String webhookUrl = ngrokUrl + "/empresas/" + empresaId + "/pagos/mercadopago/webhook";
        log.debug("Webhook URL generada: {}", webhookUrl);

        PreferenceRequest request = PreferenceRequest.builder()
                .items(List.of(item))
                .externalReference(pedidoId.toString())
                .notificationUrl(webhookUrl)
                .build();

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(request);

        return new MercadoPagoPreferenciaResponse(
                preference.getId(),
                preference.getSandboxInitPoint(),
                preference.getInitPoint());
    }

    // ─────────────────────────────────────────────────────────────
    // MERCADO PAGO — Webhook
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void procesarWebhook(Long empresaId, String paymentId) throws MPException, MPApiException {

        log.info("Webhook MercadoPago recibido — empresaId={} paymentId={}", empresaId, paymentId);

        PaymentClient client = new PaymentClient();
        Payment payment = client.get(Long.parseLong(paymentId));

        log.debug("Estado del pago {}: {}", paymentId, payment.getStatus());

        if (!"approved".equals(payment.getStatus())) {
            log.info("Pago {} no aprobado, ignorando.", paymentId);
            return;
        }

        Long pedidoId = Long.parseLong(payment.getExternalReference());
        BigDecimal monto = payment.getTransactionAmount();

        // registrarPago valida que el pedido pertenezca a empresaId (findByIdAndEmpresaId),
        // así que un webhook manipulado no puede acreditar pagos a pedidos de otra empresa.
        registrarPago(empresaId, pedidoId, Pago.Metodo.MERCADOPAGO, monto, null);

        // Guardar referencia de MercadoPago en el pago
        List<Pago> pagos = pagoRepository.findAllByPedidoId(pedidoId);
        if (!pagos.isEmpty()) {
            Pago ultimo = pagos.get(pagos.size() - 1);
            ultimo.setReferencia(paymentId);
            pagoRepository.save(ultimo);
        }

        log.info("Pedido #{} cerrado y mesa liberada correctamente.", pedidoId);
    }

    // ─────────────────────────────────────────────────────────────
    // LISTAR
    // ─────────────────────────────────────────────────────────────

    public List<Pago> listarPorPedido(Long empresaId, Long pedidoId) {
        pedidoRepository.findByIdAndEmpresaId(pedidoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        return pagoRepository.findAllByPedidoId(pedidoId);
    }

    public List<Pago> listarPorEmpresa(Long empresaId) {
        return pagoRepository.findAllByPedidoMesaEmpresaId(empresaId);
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER PRIVADO
    // ─────────────────────────────────────────────────────────────

    private void cerrarPedido(Pedido pedido) {
        pedido.setEstado(Pedido.Estado.PAGADO);
        pedido.setFechaCierre(LocalDateTime.now());

        if (pedido.getGrupo() != null) {
            mesaGrupoService.liberarMesasDeGrupo(pedido.getGrupo());
        } else if (pedido.getMesa() != null) {
            pedido.getMesa().setEstado(Mesa.Estado.LIBRE);
            mesaRepository.save(pedido.getMesa());
        }

        pedidoRepository.save(pedido);
        log.info("Pedido #{} PAGADO — mesa liberada.", pedido.getId());
    }
}