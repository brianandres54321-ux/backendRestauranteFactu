package com.empresafac.backend_factu.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.empresafac.backend_factu.entities.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameAndEmpresaId(String username, Long empresaId);

    List<Usuario> findAllByEmpresaId(Long empresaId);

    boolean existsByUsernameAndEmpresaId(String username, Long empresaId);
    Optional<Usuario> findByEmpresaIdAndUsername(Long empresaId, String username);

    Optional<Usuario> findByUsername(String username);
}
