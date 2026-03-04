package com.empresafac.backend_factu.dto_temp.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SeccionResponse {

    private Long id;
    private String nombre;
    private Boolean activa;
}
