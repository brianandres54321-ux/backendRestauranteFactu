package com.empresafac.backend_factu.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.dto_temp.response.PedidoItemResponse;
import com.empresafac.backend_factu.dto_temp.response.PedidoResponse;
import com.empresafac.backend_factu.entities.Mesa;
import com.empresafac.backend_factu.entities.MesaGrupo;
import com.empresafac.backend_factu.entities.MesaGrupoDetalle;
import com.empresafac.backend_factu.entities.Pago;
import com.empresafac.backend_factu.entities.Pedido;
import com.empresafac.backend_factu.entities.PedidoItem;
import com.empresafac.backend_factu.entities.Precio;
import com.empresafac.backend_factu.entities.Producto;
import com.empresafac.backend_factu.entities.Usuario;
import com.empresafac.backend_factu.repositories.MesaGrupoDetalleRepository;
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
    private final MesaGrupoDetalleRepository detalleRepository;
    private final MesaGrupoService mesaGrupoService;

    // --- 1. ABRIR PEDIDO ---
    @Transactional
    public PedidoResponse abrirPedido(Long empresaId, Long mesaId, Usuario usuario) {
        Mesa mesa = mesaRepository.findById(mesaId)
                .orElseThrow(() -> new RuntimeException("Mesa no encontrada"));

        Optional<MesaGrupo> grupoOpt = detalleRepository.findByMesaId(mesaId)
                .map(MesaGrupoDetalle::getGrupo);

        if (grupoOpt.isPresent()) {
            return pedidoRepository.findByGrupoIdAndEstado(grupoOpt.get().getId(), Pedido.Estado.ABIERTO)
                    .map(this::construirResponse)
                    .orElseGet(() -> crearPedidoGrupal(mesa, grupoOpt.get(), usuario));
        }

        Optional<Pedido> pedidoExistente = pedidoRepository.findByMesaIdAndEstado(mesaId, Pedido.Estado.ABIERTO);
        if (pedidoExistente.isPresent()) {
            return construirResponse(pedidoExistente.get());
        }

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

    private PedidoResponse crearPedidoGrupal(Mesa mesa, MesaGrupo grupo, Usuario usuario) {
        Pedido pedido = new Pedido();
        pedido.setEmpresa(mesa.getEmpresa());
        pedido.setMesa(mesa);
        pedido.setGrupo(grupo);
        pedido.setUsuario(usuario);
        pedido.setEstado(Pedido.Estado.ABIERTO);
        pedido.setTotal(BigDecimal.ZERO);
        pedido.setFechaApertura(LocalDateTime.now());
        pedidoRepository.save(pedido);
        return construirResponse(pedido);
    }

    // --- 2. AGREGAR PRODUCTO ---
    @Transactional
    public PedidoResponse agregarProducto(Long empresaId, Long pedidoId, Long productoId, BigDecimal cantidad) {
        Pedido pedido = obtener(empresaId, pedidoId);

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Precio precio = precioRepository
                .findFirstByProductoIdAndEmpresaIdAndActivoTrueOrderByFechaInicioDesc(productoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Precio no encontrado"));

        inventarioService.descontarStock(empresaId, producto, cantidad);

        PedidoItem item = new PedidoItem();
        item.setPedido(pedido);
        item.setProducto(producto);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(precio.getPrecioVenta());

        pedidoItemRepository.save(item);
        pedido.getItems().add(item);
        pedido.setTotal(pedido.getTotal().add(precio.getPrecioVenta().multiply(cantidad)));

        if (pedido.getMesa() != null && pedido.getMesa().getEstado() == Mesa.Estado.LIBRE) {
            pedido.getMesa().setEstado(Mesa.Estado.OCUPADA);
            mesaRepository.save(pedido.getMesa());
        }

        pedidoRepository.save(pedido);
        return construirResponse(pedido);
    }

    // --- 3. ELIMINAR ITEM ---
    @Transactional
    public PedidoResponse eliminarItem(Long empresaId, Long itemId) {
        PedidoItem item = pedidoItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Ítem de pedido no encontrado"));

        Pedido pedido = item.getPedido();

        if (!pedido.getEmpresa().getId().equals(empresaId)) {
            throw new RuntimeException("No tiene permisos sobre este pedido");
        }

        inventarioService.aumentarStock(empresaId, item.getProducto(), item.getCantidad());

        BigDecimal subtotalItem = item.getPrecioUnitario().multiply(item.getCantidad());
        pedido.setTotal(pedido.getTotal().subtract(subtotalItem));
        pedido.getItems().remove(item);
        pedidoItemRepository.delete(item);

        if (pedido.getItems().isEmpty()) {
            if (pedido.getGrupo() != null) {
                mesaGrupoService.liberarMesasDeGrupo(pedido.getGrupo());
                pedido.setGrupo(null);
            } else if (pedido.getMesa() != null) {
                Mesa mesa = pedido.getMesa();
                mesa.setEstado(Mesa.Estado.LIBRE);
                mesaRepository.save(mesa);
                pedido.setMesa(null);
            }
            pedido.setEstado(Pedido.Estado.CANCELADO);
            pedido.setFechaCierre(LocalDateTime.now());
            pedido.setTotal(BigDecimal.ZERO);
        }

        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        return construirResponse(pedidoGuardado);
    }

    // --- 4. REGISTRAR PAGO ---
    @Transactional
    public PedidoResponse registrarPago(Long empresaId, Long pedidoId, Pago.Metodo metodo, BigDecimal monto) {
        Pedido pedido = obtener(empresaId, pedidoId);

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMetodo(metodo);
        pago.setMonto(monto);
        pagoRepository.save(pago);

        BigDecimal pagado = pagoRepository.findAllByPedidoId(pedidoId).stream()
                .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (pagado.compareTo(pedido.getTotal()) >= 0) {
            pedido.setEstado(Pedido.Estado.PAGADO);
            pedido.setFechaCierre(LocalDateTime.now()); // ✅ Guardar fecha de cierre
            if (pedido.getMesa() != null) {
                pedido.getMesa().setEstado(Mesa.Estado.LIBRE);
            }
        }

        pedidoRepository.save(pedido);
        return construirResponse(pedido);
    }

    // --- 5. CANCELAR PEDIDO ---
    @Transactional
    public PedidoResponse cancelarPedido(Long empresaId, Long pedidoId) {
        Pedido pedido = obtener(empresaId, pedidoId);
        pedido.setEstado(Pedido.Estado.CANCELADO);
        pedido.setFechaCierre(LocalDateTime.now());
        if (pedido.getMesa() != null) {
            pedido.getMesa().setEstado(Mesa.Estado.LIBRE);
        }
        pedidoRepository.save(pedido);
        return construirResponse(pedido);
    }

    // --- 6. RESTAURAR PEDIDO CANCELADO ---
    @Transactional
    public PedidoResponse restaurarPedido(Long empresaId, Long pedidoId) {
        Pedido pedido = obtener(empresaId, pedidoId);

        if (pedido.getEstado() != Pedido.Estado.CANCELADO) {
            throw new RuntimeException("Solo se pueden restaurar pedidos cancelados");
        }

        if (pedido.getItems().isEmpty()) {
            throw new RuntimeException("No se puede restaurar un pedido sin productos");
        }

        pedido.setEstado(Pedido.Estado.ABIERTO);
        pedido.setFechaCierre(null);

        // Volver a ocupar la mesa si todavía existe
        if (pedido.getMesa() != null) {
            pedido.getMesa().setEstado(Mesa.Estado.OCUPADA);
            mesaRepository.save(pedido.getMesa());
        }

        pedidoRepository.save(pedido);
        return construirResponse(pedido);
    }

    // --- 7. CERRAR PEDIDO ---
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

    public Pedido obtener(Long empresaId, Long pedidoId) {
        return pedidoRepository.findByIdAndEmpresaId(pedidoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
    }

    public List<Pedido> listarPorEmpresa(Long empresaId, Pedido.Estado estado) {
        return pedidoRepository.findAllByEmpresaIdAndEstado(empresaId, estado);
    }

    public PedidoResponse construirResponse(Pedido p) {
        PedidoResponse res = new PedidoResponse();
        res.setId(p.getId());
        res.setEmpresaId(p.getEmpresa().getId());
        res.setMesaId(p.getMesa() != null ? p.getMesa().getId() : null);
        res.setMesa(p.getMesa() != null ? p.getMesa().getNombre() : "Grupal");
        res.setGrupoId(p.getGrupo() != null ? p.getGrupo().getId() : null);
        res.setUsuarioId(p.getUsuario().getId());
        res.setTotal(p.getTotal());
        res.setEstado(p.getEstado().name());
        res.setFechaApertura(p.getFechaApertura());
        res.setFechaCierre(p.getFechaCierre());
        res.setEstadoCocina(
                p.getEstadoCocina() != null ? p.getEstadoCocina().name() : "PENDIENTE");

        // ✅ Último método de pago registrado
        List<Pago> pagos = pagoRepository.findAllByPedidoId(p.getId());
        BigDecimal pagado = pagos.stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        res.setTotalPagado(pagado);

        if (!pagos.isEmpty()) {
            res.setMetodoPago(pagos.get(pagos.size() - 1).getMetodo().name());
        }

        res.setItems(p.getItems().stream().map(item -> {
            PedidoItemResponse d = new PedidoItemResponse();
            d.setId(item.getId());
            d.setProductoId(item.getProducto().getId());
            d.setProductoNombre(item.getProducto().getNombre());
            d.setImagen(item.getProducto().getImagenUrl());
            d.setCantidad(item.getCantidad());
            d.setPrecioUnitario(item.getPrecioUnitario());
            d.setSubtotal(item.getPrecioUnitario().multiply(item.getCantidad()));
            d.setNotas(item.getNotas());
            return d;
        }).collect(Collectors.toList()));

        return res;
    }

    @Transactional
    public PedidoResponse actualizarEstadoCocina(Long empresaId, Long pedidoId, String estadoStr) {
        Pedido pedido = obtener(empresaId, pedidoId);
        try {
            Pedido.EstadoCocina nuevoEstado = Pedido.EstadoCocina.valueOf(estadoStr.toUpperCase());
            pedido.setEstadoCocina(nuevoEstado);
            pedidoRepository.save(pedido);
            return construirResponse(pedido);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Estado de cocina inválido: " + estadoStr
                    + ". Valores válidos: PENDIENTE, EN_PREPARACION, LISTO");
        }
    }

}