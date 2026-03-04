package com.empresafac.backend_factu.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empresafac.backend_factu.entities.Empresa;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByNitRut(String nitRut);

    boolean existsByNitRut(String nitRut);

    // active-specific queries
    List<Empresa> findAllByActivaTrue();

    Optional<Empresa> findByIdAndActivaTrue(Long id);
}
