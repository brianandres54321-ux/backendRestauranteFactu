package com.empresafac.backend_factu.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.MesaGrupo;

public interface MesaGrupoRepository extends JpaRepository<MesaGrupo, Long> {

    Optional<MesaGrupo> findByIdAndEmpresaId(Long id, Long empresaId);

    List<MesaGrupo> findAllByEmpresaId(Long empresaId);

    List<MesaGrupo> findAllByEmpresaIdAndEstado(Long empresaId, MesaGrupo.Estado estado);
}
