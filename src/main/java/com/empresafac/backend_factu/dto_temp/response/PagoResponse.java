package com.empresafac.backend_factu.dto_temp.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PagoResponse {

    private Long id;
    private Long pedidoId;
    private String metodo;
    private BigDecimal monto;
    private LocalDateTime fecha;
}
