package com.empresafac.backend_factu.dto_temp.request;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CuponRequest {
    private String codigo;
    private String descripcion;
    private String tipo; // "PORCENTAJE" | "MONTO_FIJO"
    private BigDecimal valor;
    private Integer usosMaximos; // null = ilimitado
    private String fechaInicio; // "yyyy-MM-dd" o null
    private String fechaFin; // "yyyy-MM-dd" o null
    private BigDecimal montoMinimo; // null = sin mínimo
    private boolean activo;
}