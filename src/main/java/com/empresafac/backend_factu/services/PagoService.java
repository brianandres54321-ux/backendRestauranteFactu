package com.empresafac.backend_factu.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.entities.Mesa;
import com.empresafac.backend_factu.entities.Pago;
import com.empresafac.backend_factu.entities.Pedido;
import com.empresafac.backend_factu.repositories.PagoRepository;
import com.empresafac.backend_factu.repositories.PedidoRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;

    @Transactional
    public void registrarPago(Long pedidoId,
            Pago.Metodo metodo,
            BigDecimal monto) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (pedido.getEstado() != Pedido.Estado.ABIERTO) {
            throw new RuntimeException("Pedido no está abierto");
        }

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodo(metodo);
        pago.setMonto(monto);

        pagoRepository.save(pago);

        BigDecimal totalPagado = pagoRepository.findAllByPedidoId(pedidoId)
                .stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPagado.compareTo(pedido.getTotal()) >= 0) {
            pedido.setEstado(Pedido.Estado.PAGADO);
            pedido.setFechaCierre(LocalDateTime.now());

            if (pedido.getMesa() != null) {
                pedido.getMesa().setEstado(Mesa.Estado.LIBRE);
            } 
        }
    }

    /**
     * Lista todos los pagos asociados a un pedido.
     */
    public List<Pago> listarPorPedido(Long pedidoId) {
        return pagoRepository.findAllByPedidoId(pedidoId);
    }

    /**
     * Lista los pagos correspondientes a los pedidos de una empresa.
     */
    public List<Pago> listarPorEmpresa(Long empresaId) {
        return pagoRepository.findAllByPedidoMesaEmpresaId(empresaId);
    }
}
