package com.empresafac.backend_factu.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.Inventario;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    Optional<Inventario> findByProductoId(Long productoId);

    // company-scoped queries
    Optional<Inventario> findByProductoIdAndProductoEmpresaId(Long productoId, Long empresaId);

    List<Inventario> findAllByProductoEmpresaId(Long empresaId);
}
