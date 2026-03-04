package com.empresafac.backend_factu.dto_temp;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotNull
    private Long empresaId;

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
