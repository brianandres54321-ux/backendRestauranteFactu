package com.empresafac.backend_factu.dto_temp.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Reporte consolidado de ventas por rango de fechas.
 * Devuelto por GET /empresas/{id}/reportes/ventas
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReporteVentasResponse {

    // ── Totales generales ─────────────────────────────────────────
    private BigDecimal totalVentas;
    private long      cantidadPedidos;
    private BigDecimal ticketPromedio;

    // ── Desglose por método de pago ───────────────────────────────
    private BigDecimal totalEfectivo;
    private BigDecimal totalMercadoPago;

    // ── Top 5 productos más vendidos ──────────────────────────────
    private List<ProductoVendidoDTO> topProductos;

    // ── Detalle de cada pago (para la tabla y el Excel) ───────────
    private List<FilaPagoDTO> pagos;

    // ── Rango consultado ──────────────────────────────────────────
    private String fechaDesde;
    private String fechaHasta;
    private String empresaNombre;

    // ─── Inner DTOs ───────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ProductoVendidoDTO {
        private String     nombre;
        private long       cantidadVendida;
        private BigDecimal ingresoGenerado;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class FilaPagoDTO {
        private Long          pagoId;
        private Long          pedidoId;
        private String        mesa;
        private String        metodo;
        private BigDecimal    monto;
        private String        fecha;   // ISO-8601 → el frontend lo formatea
    }
}