package com.empresafac.backend_factu.repositories;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.Precio;

public interface PrecioRepository extends JpaRepository<Precio, Long> {

    Optional<Precio> findFirstByProductoIdAndEmpresaIdAndActivoTrueOrderByFechaInicioDesc(
            Long productoId,
            Long empresaId
    );

    List<Precio> findAllByProductoIdAndEmpresaIdOrderByFechaInicioDesc(Long productoId, Long empresaId);
    List<Precio> findAllByEmpresaId(Long empresaId);
}
