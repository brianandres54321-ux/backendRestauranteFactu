package com.empresafac.backend_factu.dto_temp.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductoResponse {
 
    private Long id;
    private String nombre;
    private String descripcion;
    private String codigoBarras;
    private Boolean activo;
    private BigDecimal precioActual;
    private Long categoriaId;
    private String categoriaNombre; 
    private BigDecimal stock;
    private String imagen;
}
