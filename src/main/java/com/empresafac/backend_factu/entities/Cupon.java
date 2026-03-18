package com.empresafac.backend_factu.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(
    name = "cupones",
    uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "codigo"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Cupon {

    public enum Tipo {
        PORCENTAJE,  // ej: 10% de descuento
        MONTO_FIJO   // ej: $5.000 de descuento
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    // Código que escribe el cajero: "VERANO20", "BIENVENIDO"
    @Column(nullable = false, length = 50)
    private String codigo;

    @Column(length = 100)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    // Para PORCENTAJE: valor entre 1 y 100. Para MONTO_FIJO: valor en pesos.
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    // Límite de usos (null = ilimitado)
    @Column(name = "usos_maximos")
    private Integer usosMaximos;

    // Cuántas veces ya fue usado
    @Column(name = "usos_actuales", nullable = false)
    private int usosActuales = 0;

    // Vigencia (null = sin vencimiento)
    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    // Monto mínimo del pedido para aplicar el cupón (null = sin mínimo)
    @Column(name = "monto_minimo", precision = 14, scale = 2)
    private BigDecimal montoMinimo;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn = LocalDateTime.now();
}