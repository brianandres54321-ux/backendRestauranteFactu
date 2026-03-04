package com.empresafac.backend_factu.dto_temp.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgregarProductoRequest {

    private Long productoId;
    private BigDecimal cantidad;
}
