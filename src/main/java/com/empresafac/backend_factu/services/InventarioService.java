package com.empresafac.backend_factu.services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.entities.Inventario;
import com.empresafac.backend_factu.entities.MovimientoInventario;
import com.empresafac.backend_factu.entities.Producto;
import com.empresafac.backend_factu.repositories.InventarioRepository;
import com.empresafac.backend_factu.repositories.MovimientoInventarioRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    /**
     * Descuenta stock asociado a una venta. Ya existía.
     */
    @Transactional
    public void descontarStock(Long empresaId, Producto producto, BigDecimal cantidad) {

        Inventario inventario = inventarioRepository
                .findByProductoIdAndProductoEmpresaId(producto.getId(), empresaId)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado"));

        if (inventario.getStockActual().compareTo(cantidad) < 0) {
            throw new RuntimeException("Stock insuficiente");
        }

        inventario.setStockActual(
                inventario.getStockActual().subtract(cantidad)
        );

        inventarioRepository.save(inventario);

        MovimientoInventario mov = new MovimientoInventario();
        mov.setEmpresa(producto.getEmpresa());
        mov.setProducto(producto);
        mov.setTipo(MovimientoInventario.Tipo.SALIDA);
        mov.setCantidad(cantidad);
        mov.setMotivo("Venta");

        movimientoRepository.save(mov);
    }

    /**
     * Inicializa o crea un inventario para un producto.
     */
    @Transactional
    public Inventario crear(Long empresaId, Producto producto,
            BigDecimal stockInicial, BigDecimal stockMinimo) {
        if (!producto.getEmpresa().getId().equals(empresaId)) {
            throw new RuntimeException("Producto no pertenece a la empresa");
        }
        Inventario inv = new Inventario();
        inv.setProducto(producto);
        inv.setStockActual(stockInicial != null ? stockInicial : BigDecimal.ZERO);
        inv.setStockMinimo(stockMinimo != null ? stockMinimo : BigDecimal.ZERO);
        return inventarioRepository.save(inv);
    }

    /**
     * Lista todos los inventarios pertenecientes a una empresa.
     */
    public List<Inventario> listar(Long empresaId) {
        return inventarioRepository.findAllByProductoEmpresaId(empresaId);
    }

    /**
     * Obtiene el inventario de un producto.
     */
    public Inventario obtener(Long empresaId, Long productoId) {
        return inventarioRepository
                .findByProductoIdAndProductoEmpresaId(productoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado"));
    }

    /**
     * Actualiza los valores de stock en el inventario.
     */
    @Transactional
    public Inventario actualizar(Long empresaId, Long productoId,
            BigDecimal nuevoStockActual, BigDecimal nuevoStockMinimo) {
        Inventario inv = inventarioRepository
                .findByProductoIdAndProductoEmpresaId(productoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado"));
        if (nuevoStockActual != null) {
            inv.setStockActual(nuevoStockActual);
        }
        if (nuevoStockMinimo != null) {
            inv.setStockMinimo(nuevoStockMinimo);
        }
        return inventarioRepository.save(inv);
    }

    /**
     * Elimina el registro de inventario (borrado físico).
     */
    @Transactional
    public void eliminar(Long empresaId, Long productoId) {
        Inventario inv = inventarioRepository
                .findByProductoIdAndProductoEmpresaId(productoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado"));
        inventarioRepository.delete(inv);
    }

    /**
     * Aumenta el stock cuando se elimina un ítem de un pedido o se cancela.
     */
    @Transactional
    public void aumentarStock(Long empresaId, Producto producto, BigDecimal cantidad) {

        Inventario inventario = inventarioRepository
                .findByProductoIdAndProductoEmpresaId(producto.getId(), empresaId)
                .orElseThrow(() -> new RuntimeException("Inventario no encontrado para el producto: " + producto.getNombre()));

        // Sumar al stock actual
        inventario.setStockActual(
                inventario.getStockActual().add(cantidad)
        );

        inventarioRepository.save(inventario);

        // Registrar el movimiento de entrada
        MovimientoInventario mov = new MovimientoInventario();
        mov.setEmpresa(producto.getEmpresa());
        mov.setProducto(producto);
        mov.setTipo(MovimientoInventario.Tipo.ENTRADA); // Es una entrada porque el producto vuelve al estante
        mov.setCantidad(cantidad);
        mov.setMotivo("Anulación/Devolución de ítem en pedido");

        movimientoRepository.save(mov);
    }
}
