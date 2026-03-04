package com.empresafac.backend_factu.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.Mesa;

public interface MesaRepository extends JpaRepository<Mesa, Long> {

    List<Mesa> findAllByEmpresaIdAndActivaTrue(Long empresaId);

    List<Mesa> findAllBySeccionIdAndEmpresaId(Long seccionId, Long empresaId);

    Optional<Mesa> findByIdAndEmpresaIdAndActivaTrue(Long id, Long empresaId);

    // filtros por estado, útil para vistas específicas
    List<Mesa> findAllByEmpresaIdAndEstado(Long empresaId, Mesa.Estado estado);
}
