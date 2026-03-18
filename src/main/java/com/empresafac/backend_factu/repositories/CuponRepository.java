package com.empresafac.backend_factu.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.Cupon;

public interface CuponRepository extends JpaRepository<Cupon, Long> {

    // Buscar por código (case-insensitive) para aplicar en checkout
    Optional<Cupon> findByEmpresaIdAndCodigoIgnoreCase(Long empresaId, String codigo);

    // Listar todos los cupones de la empresa para gestión
    List<Cupon> findAllByEmpresaIdOrderByCreadoEnDesc(Long empresaId);
}