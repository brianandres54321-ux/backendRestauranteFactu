package com.empresafac.backend_factu.dto_temp.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta unificada usada tanto para el preview (GET) como para el cierre ya
 * guardado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CierreCajaResponse {

    private Long id; // null si es preview (aún no guardado)
    private String fecha; // "2026-03-18"
    private String cerradoEn; // ISO timestamp — null si es preview
    private String usuarioNombre;

    private BigDecimal totalVentas;
    private BigDecimal totalEfectivo;
    private BigDecimal totalMercadoPago;
    private int cantidadPedidos;
    private BigDecimal ticketPromedio;

    private String notas;
    private boolean yaExiste; // true si ese día ya tiene cierre registrado

    // Detalle de pagos del día (para mostrar en el resumen antes de confirmar)
    private List<FilaPagoDTO> pagos;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilaPagoDTO {
        private Long pedidoId;
        private String mesa;
        private String metodo;
        private BigDecimal monto;
        private String hora; // solo la hora "HH:mm"
    }
}