package com.empresafac.backend_factu.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.entities.Precio;
import com.empresafac.backend_factu.entities.Producto;
import com.empresafac.backend_factu.repositories.PrecioRepository;
import com.empresafac.backend_factu.repositories.ProductoRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrecioService {

    private final PrecioRepository precioRepository;
    private final ProductoRepository productoRepository;

    @Transactional
    public Precio asignarPrecio(Long empresaId,
            Long productoId,
            BigDecimal precioVenta,
            BigDecimal costo) {

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Precio precio = new Precio();
        precio.setEmpresa(producto.getEmpresa());
        precio.setProducto(producto);
        precio.setPrecioVenta(precioVenta);
        precio.setCosto(costo);
        precio.setActivo(true);
        precio.setFechaInicio(LocalDateTime.now());

        return precioRepository.save(precio);
    }

    /**
     * Obtiene el precio activo más reciente para un producto.
     */
    public Precio obtenerPrecioActual(Long empresaId, Long productoId) {
        return precioRepository
                .findFirstByProductoIdAndEmpresaIdAndActivoTrueOrderByFechaInicioDesc(productoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Precio no encontrado"));
    }

    /**
     * Lista históricos de precio para un producto.
     */
    public List<Precio> historicoPrecios(Long empresaId, Long productoId) {
        return precioRepository.findAllByProductoIdAndEmpresaIdOrderByFechaInicioDesc(productoId, empresaId);
    }

    /**
     * Lista precios para la empresa.
     */
    public List<Precio> listarPorEmpresa(Long empresaId) {
        return precioRepository.findAllByEmpresaId(empresaId);
    }

    /**
     * Desactiva un precio (soft delete) por id.
     */
    @Transactional
    public void desactivarPrecio(Long empresaId, Long precioId) {
        Precio p = precioRepository.findById(precioId)
                .orElseThrow(() -> new RuntimeException("Precio no encontrado"));
        if (!p.getEmpresa().getId().equals(empresaId)) {
            throw new RuntimeException("Precio no pertenece a la empresa");
        }
        p.setActivo(false);
        precioRepository.save(p);
    }

}
