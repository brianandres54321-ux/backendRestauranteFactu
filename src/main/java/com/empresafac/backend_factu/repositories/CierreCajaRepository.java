package com.empresafac.backend_factu.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.CierreCaja;

public interface CierreCajaRepository extends JpaRepository<CierreCaja, Long> {

    // Verificar si ya existe cierre para ese día
    boolean existsByEmpresaIdAndFecha(Long empresaId, LocalDate fecha);

    // Buscar cierre de un día específico
    Optional<CierreCaja> findByEmpresaIdAndFecha(Long empresaId, LocalDate fecha);

    // Historial completo ordenado más reciente primero
    List<CierreCaja> findAllByEmpresaIdOrderByFechaDesc(Long empresaId);
}