package com.empresafac.backend_factu.dto_temp.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body para POST /empresas/{id}/cierres
 * fecha: "2026-03-18" (ISO LocalDate — si viene null se usa hoy)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CierreCajaRequest {
    private String fecha; // opcional — null = hoy
    private String notas; // notas libres del cajero
}