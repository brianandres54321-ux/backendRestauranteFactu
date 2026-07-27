package com.empresafac.backend_factu.dto_temp.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Body para PUT /empresas/{id}/cierres/base
 * fecha: "2026-03-18" (opcional — null = hoy)
 */
@Getter
@Setter
public class RegistrarBaseCajaRequest {

    private String fecha;

    @NotNull
    private BigDecimal baseInicial;
}
