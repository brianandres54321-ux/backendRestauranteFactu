package com.empresafac.backend_factu.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
 
import com.empresafac.backend_factu.entities.Seccion;

public interface SeccionRepository extends JpaRepository<Seccion, Long> {

    List<Seccion> findAllByEmpresaIdAndActivaTrue(Long empresaId);

    Optional<Seccion> findByIdAndEmpresaIdAndActivaTrue(Long id, Long empresaId);

    Optional<Seccion> findByIdAndEmpresaId(Long id, Long empresaId);
}
