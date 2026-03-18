package com.empresafac.backend_factu.dto_temp.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EmpresaResponse {
    private Long id;
    private String nombre;
    private String nitRut;
    private String plan;
    private Boolean activa;
}