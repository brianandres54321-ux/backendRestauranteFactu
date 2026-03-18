package com.empresafac.backend_factu.dto_temp.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Datos que llegan desde el frontend para crear
 * una preferencia de pago en Mercado Pago.
 */
@Getter
@Setter
public class MercadoPagoPreferenciaRequest {

    /** ID del pedido que se va a pagar */
    private Long pedidoId;

    /**
     * URL a la que MercadoPago redirige al usuario después
     * del pago (éxito, fallo, pendiente).
     * Ejemplo: "http://localhost:4200/pagos/resultado"
     */
    private String urlRetorno;
}