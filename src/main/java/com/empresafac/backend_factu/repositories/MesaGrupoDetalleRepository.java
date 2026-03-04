package com.empresafac.backend_factu.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.MesaGrupoDetalle;

public interface MesaGrupoDetalleRepository extends JpaRepository<MesaGrupoDetalle, Long> {

    List<MesaGrupoDetalle> findAllByGrupoId(Long grupoId);

    void deleteAllByGrupoId(Long grupoId);
}
