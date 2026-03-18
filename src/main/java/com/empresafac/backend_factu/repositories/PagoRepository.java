package com.empresafac.backend_factu.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.empresafac.backend_factu.entities.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findAllByPedidoId(Long pedidoId);

    // Listar pagos de todos los pedidos de una empresa
    List<Pago> findAllByPedidoMesaEmpresaId(Long empresaId);

    /**
     * Pagos de una empresa en un rango de fechas.
     * JOIN FETCH evita el problema N+1: carga pedido + mesa + items
     * en una sola query en lugar de una query por cada pago.
     * Usado por ReporteService y CierreCajaService.
     */
    @Query("""
                SELECT p FROM Pago p
                JOIN FETCH p.pedido ped
                LEFT JOIN FETCH ped.mesa
                LEFT JOIN FETCH ped.items items
                LEFT JOIN FETCH items.producto
                WHERE ped.empresa.id = :empresaId
                  AND p.fecha BETWEEN :desde AND :hasta
                ORDER BY p.fecha ASC
            """)
    List<Pago> findAllByPedidoEmpresaIdAndFechaBetween(
            @Param("empresaId") Long empresaId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}