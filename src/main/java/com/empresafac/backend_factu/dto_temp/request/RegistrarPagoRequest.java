package com.empresafac.backend_factu.dto_temp.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrarPagoRequest {

    private String metodo; // EFECTIVO, TARJETA...
    private BigDecimal monto;
    private String codigoCupon;
}
