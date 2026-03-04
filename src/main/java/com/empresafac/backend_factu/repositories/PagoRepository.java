package com.empresafac.backend_factu.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.empresafac.backend_factu.entities.Pago;


public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findAllByPedidoId(Long pedidoId);
    
    // listar pagos de todos los pedidos de una empresa (a través del pedido)
    List<Pago> findAllByPedidoMesaEmpresaId(Long empresaId);
}
