package com.empresafac.backend_factu.dto_temp.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Respuesta que se envía al frontend con los datos
 * de la preferencia creada en Mercado Pago.
 */
@Getter
@Setter
@AllArgsConstructor
public class MercadoPagoPreferenciaResponse {

    /** ID de la preferencia generada por MercadoPago */
    private String preferenceId;

    /**
     * URL de pago en el entorno SANDBOX (pruebas).
     * En producción usar initPoint en su lugar.
     */
    private String sandboxInitPoint;

    /**
     * URL de pago en producción.
     */
    private String initPoint;
}