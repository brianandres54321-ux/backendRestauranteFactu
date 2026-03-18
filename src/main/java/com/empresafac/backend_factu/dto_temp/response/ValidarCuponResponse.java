package com.empresafac.backend_factu.dto_temp.response; // mismo paquete

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidarCuponResponse {
    private boolean valido;
    private String mensaje; // descripción del error si no es válido
    private String codigo;
    private String tipo; // "PORCENTAJE" | "MONTO_FIJO"
    private BigDecimal valor; // % o monto según tipo
    private BigDecimal descuento; // monto calculado a descontar
    private BigDecimal totalConDescuento;
}
