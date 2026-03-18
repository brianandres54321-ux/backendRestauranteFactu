package com.empresafac.backend_factu.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.dto_temp.response.ProductoResponse;
import com.empresafac.backend_factu.entities.Categoria;
import com.empresafac.backend_factu.entities.Empresa;
import com.empresafac.backend_factu.entities.Inventario;
import com.empresafac.backend_factu.entities.Precio;
import com.empresafac.backend_factu.entities.Producto;
import com.empresafac.backend_factu.repositories.CategoriaRepository;
import com.empresafac.backend_factu.repositories.EmpresaRepository;
import com.empresafac.backend_factu.repositories.InventarioRepository;
import com.empresafac.backend_factu.repositories.PrecioRepository;
import com.empresafac.backend_factu.repositories.ProductoRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoService {

        private final ProductoRepository productoRepository;
        private final InventarioRepository inventarioRepository;
        private final PrecioRepository precioRepository;
        private final EmpresaRepository empresaRepository;
        private final CategoriaRepository categoriaRepository;

        private ProductoResponse construirResponse(Producto p, Long empresaId) {

                // 1. Buscar Stock Actual
                BigDecimal stock = inventarioRepository
                                .findByProductoId(p.getId())
                                .map(Inventario::getStockActual)
                                .orElse(BigDecimal.ZERO);

                // 2. Buscar Precio de Venta Activo
                BigDecimal precioActual = precioRepository
                                .findFirstByProductoIdAndEmpresaIdAndActivoTrueOrderByFechaInicioDesc(p.getId(),
                                                empresaId)
                                .map(Precio::getPrecioVenta)
                                .orElse(BigDecimal.ZERO);

                // 3. Mapear al DTO ProductoResponse
                return new ProductoResponse(
                                p.getId(),
                                p.getNombre(),
                                p.getDescripcion(),
                                p.getCodigoBarras(),
                                p.getActivo(),
                                precioActual,
                                p.getCategoria() != null ? p.getCategoria().getId() : null,
                                p.getCategoria() != null ? p.getCategoria().getNombre() : null,
                                stock,
                                p.getImagenUrl());
        }

        public List<ProductoResponse> listar(Long empresaId) {
                // Usamos el método que solo trae Activos
                List<Producto> productos = productoRepository.findAllByEmpresaIdAndActivoTrue(empresaId);

                return productos.stream()
                                .map(p -> construirResponse(p, empresaId))
                                .toList();
        }

        public ProductoResponse obtener(Long empresaId, Long productoId) {
                Producto p = productoRepository
                                .findByIdAndEmpresaIdAndActivoTrue(productoId, empresaId) // Filtro activo
                                .orElseThrow(() -> new RuntimeException("Producto no encontrado o está inactivo"));

                return construirResponse(p, empresaId);
        }

        @Transactional
        public ProductoResponse crear(Long empresaId, Producto producto, Long categoriaId,
                        BigDecimal precioVenta, BigDecimal costo, BigDecimal stockInicial) {

                // 1. Validar Empresa
                Empresa empresa = empresaRepository.findById(empresaId)
                                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

                producto.setEmpresa(empresa);
                producto.setActivo(true);

                // 2. Validar Categoría
                if (categoriaId != null) {
                        Categoria categoria = categoriaRepository
                                        .findByIdAndEmpresaIdAndActivaTrue(categoriaId, empresaId)
                                        .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));
                        producto.setCategoria(categoria);
                }

                // 3. Guardar Producto base
                Producto saved = productoRepository.save(producto);

                // 4. Crear Inventario con Stock Inicial
                Inventario inventario = new Inventario();
                inventario.setProducto(saved); // Gracias a @MapsId, esto asigna el ID automáticamente
                inventario.setStockActual(stockInicial != null ? stockInicial : BigDecimal.ZERO);
                inventario.setStockMinimo(BigDecimal.ZERO);
                inventarioRepository.save(inventario);

                // 5. Crear Precio y Costo Inicial
                Precio precio = new Precio();
                precio.setProducto(saved);
                precio.setEmpresa(empresa);
                precio.setPrecioVenta(precioVenta != null ? precioVenta : BigDecimal.ZERO);
                precio.setCosto(costo != null ? costo : BigDecimal.ZERO);
                precio.setActivo(true);
                precio.setFechaInicio(LocalDateTime.now());
                precioRepository.save(precio);

                return construirResponse(saved, empresaId);
        }

        @Transactional
        public ProductoResponse actualizar(Long empresaId, Long id, Producto datosNuevos, Long categoriaId,
                        BigDecimal precioVenta, BigDecimal nuevoStock) { // <--- Agregamos nuevoStock

                Producto productoExistente = productoRepository.findByIdAndEmpresaId(id, empresaId)
                                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                // 1. Actualizar campos básicos del Producto
                productoExistente.setNombre(datosNuevos.getNombre());
                productoExistente.setDescripcion(datosNuevos.getDescripcion());
                productoExistente.setCodigoBarras(datosNuevos.getCodigoBarras());
                productoExistente.setImagenUrl(datosNuevos.getImagenUrl());

                // 2. Actualizar categoría
                if (categoriaId != null) {
                        Categoria cat = categoriaRepository.findById(categoriaId).orElse(null);
                        productoExistente.setCategoria(cat);
                }
                productoRepository.save(productoExistente);

                // 3. ACTUALIZAR PRECIO (Si el precio cambió, creamos uno nuevo activo)
                // 3. ACTUALIZAR PRECIO
                if (precioVenta != null) {
                        // Buscamos el precio anterior para obtener el costo actual
                        BigDecimal costoActual = precioRepository
                                        .findFirstByProductoIdAndEmpresaIdAndActivoTrueOrderByFechaInicioDesc(id,
                                                        empresaId)
                                        .map(Precio::getCosto)
                                        .orElse(BigDecimal.ZERO); // Si no hay anterior, ponemos 0

                        // Desactivamos el precio anterior
                        precioRepository.findFirstByProductoIdAndEmpresaIdAndActivoTrueOrderByFechaInicioDesc(id,
                                        empresaId)
                                        .ifPresent(p -> {
                                                p.setActivo(false);
                                                precioRepository.save(p);
                                        });

                        // Creamos el nuevo precio
                        Precio nuevoPrecioObj = new Precio();
                        nuevoPrecioObj.setProducto(productoExistente);
                        nuevoPrecioObj.setEmpresa(productoExistente.getEmpresa());
                        nuevoPrecioObj.setPrecioVenta(precioVenta);

                        // SOLUCIÓN AL ERROR: Asignamos un costo (no puede ser null)
                        nuevoPrecioObj.setCosto(costoActual);

                        nuevoPrecioObj.setActivo(true);
                        nuevoPrecioObj.setFechaInicio(LocalDateTime.now());
                        precioRepository.save(nuevoPrecioObj);
                }

                // 4. ACTUALIZAR STOCK
                if (nuevoStock != null) {
                        inventarioRepository.findByProductoId(id).ifPresent(inv -> {
                                inv.setStockActual(nuevoStock);
                                inventarioRepository.save(inv);
                        });
                }

                return construirResponse(productoExistente, empresaId);
        }

        @Transactional
        public void eliminar(Long empresaId, Long productoId) {
                Producto producto = productoRepository.findByIdAndEmpresaId(productoId, empresaId)
                                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                producto.setActivo(false);

                // El flush obliga a la base de datos a escribir el cambio YA
                productoRepository.saveAndFlush(producto);
        }

        public Producto obtenerEntidad(Long empresaId, Long productoId) {
                return productoRepository
                                .findByIdAndEmpresaId(productoId, empresaId)
                                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
        }

        // Para ver los productos en la "papelera"
        public List<ProductoResponse> listarInactivos(Long empresaId) {
                List<Producto> productos = productoRepository.findAllByEmpresaIdAndActivoFalse(empresaId);
                return productos.stream()
                                .map(p -> construirResponse(p, empresaId))
                                .toList();
        }

        // Para sacar un producto de la papelera
        @Transactional
        public void activar(Long empresaId, Long productoId) {
                // Buscamos el producto (sin importar si está activo o no)
                Producto producto = productoRepository.findByIdAndEmpresaId(productoId, empresaId)
                                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                producto.setActivo(true);
                productoRepository.save(producto);
        }
}
 