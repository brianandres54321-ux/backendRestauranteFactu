package com.empresafac.backend_factu.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mesa_grupo_detalle",
        uniqueConstraints = @UniqueConstraint(columnNames = {"grupo_id", "mesa_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MesaGrupoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "grupo_id")
    private MesaGrupo grupo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;
}
