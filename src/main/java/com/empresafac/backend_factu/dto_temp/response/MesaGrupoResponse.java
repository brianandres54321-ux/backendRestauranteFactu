package com.empresafac.backend_factu.dto_temp.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MesaGrupoResponse {

    private Long id;
    private String estado;
    private LocalDateTime creadoEn;
}
