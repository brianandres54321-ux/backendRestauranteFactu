package com.empresafac.backend_factu.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findAllByEmpresaIdAndActivoTrue(Long empresaId);

    Optional<Producto> findByCodigoBarrasAndEmpresaId(String codigoBarras, Long empresaId);

    Optional<Producto> findByIdAndEmpresaIdAndActivoTrue(Long id, Long empresaId);

    Optional<Producto> findByIdAndEmpresaId(Long id, Long empresaId);

    List<Producto> findAllByEmpresaId(Long empresaId);

    List<Producto> findAllByEmpresaIdAndActivoFalse(Long empresaId);

}
