package com.empresafac.backend_factu.dto_temp.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MesaRequest {

    @NotBlank
    private String nombre;
    @NotNull
    private Long seccionId;
}
