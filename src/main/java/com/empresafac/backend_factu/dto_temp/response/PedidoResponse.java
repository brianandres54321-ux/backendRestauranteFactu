package com.empresafac.backend_factu.dto_temp.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PedidoResponse {

    private Long id;
    private String estado;
    private String mesa;
    private BigDecimal total;
    private BigDecimal totalPagado;
}
