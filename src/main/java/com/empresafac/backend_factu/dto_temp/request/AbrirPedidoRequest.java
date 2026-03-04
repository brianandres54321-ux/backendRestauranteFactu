package com.empresafac.backend_factu.dto_temp.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbrirPedidoRequest {

    @NotNull
    private Long mesaId;
    @NotNull
    private Long usuarioId;
}
