package com.empresafac.backend_factu.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByEmpresaIdAndActivaTrue(Long empresaId);

    Optional<Categoria> findByIdAndEmpresaIdAndActivaTrue(Long id, Long empresaId);

    Optional<Categoria> findByIdAndEmpresaId(Long id, Long empresaId);
}
