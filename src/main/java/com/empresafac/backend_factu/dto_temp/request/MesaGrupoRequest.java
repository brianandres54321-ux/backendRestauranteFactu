package com.empresafac.backend_factu.dto_temp.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MesaGrupoRequest {

    @NotNull
    private List<Long> mesasIds;
}
