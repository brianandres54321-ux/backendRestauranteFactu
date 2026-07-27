package com.empresafac.backend_factu.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {

    public enum TipoNegocio {
        RESTAURANTE, TIENDA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "nit_rut", nullable = false, unique = true, length = 30)
    private String nitRut;

    @Column(length = 30)
    private String plan = "BASICO";

    // columnDefinition con DEFAULT — así el ALTER TABLE puede rellenar
    // las filas existentes sin violar el NOT NULL.
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_negocio", nullable = false, length = 20,
            columnDefinition = "varchar(20) default 'RESTAURANTE'")
    private TipoNegocio tipoNegocio = TipoNegocio.RESTAURANTE;

    private Boolean activa = true;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();
}
