package com.empresafac.backend_factu.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findAllByEmpresaIdAndEstado(
            Long empresaId,
            Pedido.Estado estado
    );

    Optional<Pedido> findByMesaIdAndEstado(
            Long mesaId,
            Pedido.Estado estado
    ); 

    Optional<Pedido> findByIdAndEmpresaId(Long id, Long empresaId);
    List<Pedido> findAllByEmpresaId(Long empresaId);
}
