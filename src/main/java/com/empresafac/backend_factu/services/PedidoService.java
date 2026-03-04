package com.empresafac.backend_factu.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.dto_temp.response.PedidoResponse;
import com.empresafac.backend_factu.entities.Mesa;
import com.empresafac.backend_factu.entities.Pago;
import com.empresafac.backend_factu.entities.Pedido;
import com.empresafac.backend_factu.entities.PedidoItem;
import com.empresafac.backend_factu.entities.Precio;
import com.empresafac.backend_factu.entities.Producto;
import com.empresafac.backend_factu.entities.Usuario;
import com.empresafac.backend_factu.repositories.MesaRepository;
import com.empresafac.backend_factu.repositories.PagoRepository;
import com.empresafac.backend_factu.repositories.PedidoItemRepository;
import com.empresafac.backend_factu.repositories.PedidoRepository;
import com.empresafac.backend_factu.repositories.PrecioRepository;
import com.empresafac.backend_factu.repositories.ProductoRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final ProductoRepository productoRepository;
    private final PrecioRepository precioRepository;
    private final PedidoItemRepository pedidoItemRepository;
    private final InventarioService inventarioService;
    private final PagoRepository pagoRepository;

    // =============================
    // 1️⃣ ABRIR PEDIDO
    // ============================= 
    @Transactional
    public PedidoResponse abrirPedido(Long empresaId,
            Long mesaId,
            Usuario usuario) {

        Mesa mesa = mesaRepository.findById(mesaId)
                .orElseThrow(() -> new RuntimeException("Mesa no encontrada"));

        if (!mesa.getEmpresa().getId().equals(empresaId)) {
            throw new RuntimeException("Mesa no pertenece a la empresa");
        }

        if (mesa.getEstado() != Mesa.Estado.LIBRE) {
            throw new RuntimeException("Mesa no está disponible");
        }

        Optional<Pedido> existente
                = pedidoRepository.findByMesaIdAndEstado(
                        mesaId,
                        Pedido.Estado.ABIERTO
                );

        if (existente.isPresent()) {
            throw new RuntimeException("Ya existe pedido abierto");
        }

        mesa.setEstado(Mesa.Estado.OCUPADA);

        Pedido pedido = new Pedido();
        pedido.setEmpresa(mesa.getEmpresa());
        pedido.setMesa(mesa);
        pedido.setUsuario(usuario);
        pedido.setEstado(Pedido.Estado.ABIERTO);
        pedido.setTotal(BigDecimal.ZERO);
        pedido.setFechaApertura(LocalDateTime.now());

        pedidoRepository.save(pedido);

        return construirResponse(pedido);
    }

    // =============================
    // 2️⃣ AGREGAR PRODUCTO
    // =============================
    @Transactional
    public PedidoResponse agregarProducto(Long empresaId,
            Long pedidoId,
            Long productoId,
            BigDecimal cantidad) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (!pedido.getEmpresa().getId().equals(empresaId)) {
            throw new RuntimeException("Pedido no pertenece a la empresa");
        }

        if (pedido.getEstado() != Pedido.Estado.ABIERTO) {
            throw new RuntimeException("Pedido cerrado");
        }

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Precio precio = precioRepository
                .findFirstByProductoIdAndEmpresaIdAndActivoTrueOrderByFechaInicioDesc(
                        productoId,
                        empresaId
                )
                .orElseThrow(() -> new RuntimeException("Precio no encontrado"));

        // 🔥 Descontar stock dentro de la misma transacción
        inventarioService.descontarStock(empresaId, producto, cantidad);

        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(precio.getPrecioVenta());

        pedidoItemRepository.save(item);

        BigDecimal subtotal
                = precio.getPrecioVenta().multiply(cantidad);

        pedido.setTotal(
                pedido.getTotal().add(subtotal)
        );

        pedidoRepository.save(pedido);

        return construirResponse(pedido);
    }

    // =============================
    // 3️⃣ REGISTRAR PAGO
    // =============================
    @Transactional
    public PedidoResponse registrarPago(Long empresaId,
            Long pedidoId,
            Pago.Metodo metodo,
            BigDecimal monto) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (!pedido.getEmpresa().getId().equals(empresaId)) {
            throw new RuntimeException("Pedido no pertenece a la empresa");
        }

        if (pedido.getEstado() != Pedido.Estado.ABIERTO) {
            throw new RuntimeException("Pedido no está abierto");
        }

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodo(metodo);
        pago.setMonto(monto);
        pagoRepository.save(pago);

        BigDecimal totalPagado = pagoRepository
                .findAllByPedidoId(pedidoId)
                .stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 🔥 Cierre automático
        if (totalPagado.compareTo(pedido.getTotal()) >= 0) {

            pedido.setEstado(Pedido.Estado.PAGADO);
            pedido.setFechaCierre(LocalDateTime.now());

            if (pedido.getMesa() != null) {
                pedido.getMesa().setEstado(Mesa.Estado.LIBRE);
            }
        }

        return construirResponse(pedido);
    }

    // =============================
    // MÉTODO PRIVADO
    // =============================
    public PedidoResponse construirResponse(Pedido pedido) {

        BigDecimal totalPagado = pagoRepository
                .findAllByPedidoId(pedido.getId())
                .stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PedidoResponse(
                pedido.getId(),
                pedido.getEstado().name(),
                pedido.getMesa() != null ? pedido.getMesa().getNombre() : null,
                pedido.getTotal(),
                totalPagado
        );
    }

    /**
     * Lista pedidos abiertos (u otro estado) de la empresa.
     */
    public java.util.List<Pedido> listarPorEmpresa(Long empresaId, Pedido.Estado estado) {
        return pedidoRepository.findAllByEmpresaIdAndEstado(empresaId, estado);
    }

    /**
     * Obtiene un pedido validando empresa.
     */
    public Pedido obtener(Long empresaId, Long pedidoId) {
        return pedidoRepository
                .findByIdAndEmpresaId(pedidoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
    }

    /**
     * Cierra un pedido (marca PAGADO) y libera mesa si corresponde.
     */
    @Transactional
    public PedidoResponse cerrarPedido(Long empresaId, Long pedidoId) {
        Pedido pedido = obtener(empresaId, pedidoId);
        pedido.setEstado(Pedido.Estado.PAGADO);
        pedido.setFechaCierre(LocalDateTime.now());
        if (pedido.getMesa() != null) {
            pedido.getMesa().setEstado(Mesa.Estado.LIBRE);
        }
        pedidoRepository.save(pedido);
        return construirResponse(pedido);
    }

    /**
     * Cancela un pedido (marcar CANCELADO) y revertir acciones si es necesario.
     */
    @Transactional
    public PedidoResponse cancelarPedido(Long empresaId, Long pedidoId) {
        Pedido pedido = obtener(empresaId, pedidoId);
        pedido.setEstado(Pedido.Estado.CANCELADO);
        if (pedido.getMesa() != null) {
            pedido.getMesa().setEstado(Mesa.Estado.LIBRE);
        }
        pedidoRepository.save(pedido);
        return construirResponse(pedido);
    }
}
