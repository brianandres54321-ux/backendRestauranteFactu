package com.empresafac.backend_factu.dto_temp.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InventarioResponse {

    private Long productoId;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
}
