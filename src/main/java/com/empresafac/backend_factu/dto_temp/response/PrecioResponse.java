package com.empresafac.backend_factu.dto_temp.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PrecioResponse {

    private Long id;
    private Long productoId;
    private BigDecimal precioVenta;
    private BigDecimal costo;
    private Boolean activo;
    private LocalDateTime fechaInicio;

}
