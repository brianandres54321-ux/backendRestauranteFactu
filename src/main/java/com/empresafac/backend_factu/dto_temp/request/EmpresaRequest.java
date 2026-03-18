package com.empresafac.backend_factu.dto_temp.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmpresaRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    private String nitRut;

    private String plan;

}
