package com.empresafac.backend_factu.dto_temp.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor // Genera constructor sin argumentos
@AllArgsConstructor
public class MesaResponse {

    private Long id;
    private String nombre;
    private String estado;
    private Boolean activa;
    private Long seccionId;
    private String seccionNombre; 
    private Long grupoId;

}
