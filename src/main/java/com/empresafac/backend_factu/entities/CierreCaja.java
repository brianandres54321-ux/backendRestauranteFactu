package com.empresafac.backend_factu.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
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
@Table(name = "cierres_caja", uniqueConstraints = @UniqueConstraint(columnNames = { "empresa_id", "fecha" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CierreCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    // Quién ejecutó el cierre
    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // Fecha del día que se está cerrando (sin hora — un cierre por día)
    @Column(nullable = false)
    private LocalDate fecha;

    // Momento en que se hizo el corte
    @Column(name = "cerrado_en", nullable = false)
    private LocalDateTime cerradoEn = LocalDateTime.now();

    // Totales del día
    @Column(precision = 14, scale = 2)
    private BigDecimal totalVentas = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalEfectivo = BigDecimal.ZERO;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalMercadoPago = BigDecimal.ZERO;

    @Column(name = "cantidad_pedidos")
    private int cantidadPedidos = 0;

    // Notas libres del cajero/admin
    @Column(length = 500)
    private String notas;
}