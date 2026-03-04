package com.empresafac.backend_factu.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.MovimientoInventario;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findAllByEmpresaIdAndProductoId(
            Long empresaId,
            Long productoId
    );
}
