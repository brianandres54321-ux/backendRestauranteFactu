package com.empresafac.backend_factu.dto_temp;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    // @NotNull
    // private Long empresaId;
    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
