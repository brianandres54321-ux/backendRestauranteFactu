package com.empresafac.backend_factu.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.entities.Producto;
import com.empresafac.backend_factu.repositories.ProductoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;

    public List<Producto> listarActivos(Long empresaId) {
        return productoRepository.findAllByEmpresaIdAndActivoTrue(empresaId);
    }

    public Producto obtenerPorCodigo(Long empresaId, String codigo) {
        return productoRepository
                .findByCodigoBarrasAndEmpresaId(codigo, empresaId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    /**
     * Lista todos los productos (incluso inactivos) de la empresa.
     */
    public List<Producto> listar(Long empresaId) {
        return productoRepository.findAllByEmpresaId(empresaId);
    }

    /**
     * Recupera un producto por id validando la empresa y que esté activo.
     */
    public Producto obtener(Long empresaId, Long productoId) {
        return productoRepository
                .findByIdAndEmpresaIdAndActivoTrue(productoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    /**
     * Crea o actualiza un producto simple (no gestiona imagenes ni codigos duplicados aquí).
     */
    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }

    /**
     * Marca un producto como inactivo (soft delete).
     */
    public void eliminar(Long empresaId, Long productoId) {
        Producto p = productoRepository
                .findByIdAndEmpresaId(productoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        p.setActivo(false);
        productoRepository.save(p);
    }
}
