package com.empresafac.backend_factu.dto_temp.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoRequest {

    private String nombre;
    private String descripcion;
    private String codigoBarras;
    private Long categoriaId;
    private String imagenUrl;
    private BigDecimal precioVenta;
    private BigDecimal costo;
    private BigDecimal stockInicial;

}