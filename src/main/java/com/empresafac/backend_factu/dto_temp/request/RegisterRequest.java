package com.empresafac.backend_factu.dto_temp.request;

import com.empresafac.backend_factu.entities.Empresa;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String empresaNombre;

    @NotNull
    private Empresa.TipoNegocio tipoNegocio;

    @NotBlank
    private String nitRut;

    @NotBlank
    private String adminNombre;

    @NotBlank
    private String username;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
        message = "La contraseña debe tener mínimo 8 caracteres, mayúsculas, minúsculas, números y un símbolo especial"
    )
    private String password;
}
