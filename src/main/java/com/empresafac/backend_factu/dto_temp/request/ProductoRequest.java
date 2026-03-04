package com.empresafac.backend_factu.dto_temp.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoRequest {

    @NotBlank
    private String nombre;
    private String descripcion;
    private String codigoBarras;
    private Long categoriaId;
}
