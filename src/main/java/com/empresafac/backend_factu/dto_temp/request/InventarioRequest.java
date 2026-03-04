package com.empresafac.backend_factu.dto_temp.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventarioRequest {

    @NotNull
    private Long productoId;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
}
