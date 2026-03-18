package com.empresafac.backend_factu.dto_temp.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PedidoResponse {

    private Long id;
    private Long empresaId;
    private Long mesaId;
    private String mesa;
    private Long grupoId;
    private Long usuarioId;
    private BigDecimal total;
    private BigDecimal totalPagado;
    private String estado;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private String metodoPago;
    private List<PedidoItemResponse> items;
    private String estadoCocina;

}