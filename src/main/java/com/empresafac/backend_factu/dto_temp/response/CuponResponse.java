package com.empresafac.backend_factu.dto_temp.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Usado para el listado de gestión de cupones (admin). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CuponResponse {
    private Long id;
    private String codigo;
    private String descripcion;
    private String tipo;
    private BigDecimal valor;
    private Integer usosMaximos;
    private int usosActuales;
    private String fechaInicio;
    private String fechaFin;
    private BigDecimal montoMinimo;
    private boolean activo;
    private String creadoEn;
}