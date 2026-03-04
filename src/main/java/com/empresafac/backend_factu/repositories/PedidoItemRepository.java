package com.empresafac.backend_factu.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.PedidoItem;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {

    List<PedidoItem> findByPedidoId(Long pedidoId);

}
