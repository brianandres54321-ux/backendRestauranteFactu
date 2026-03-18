package com.empresafac.backend_factu.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.entities.Mesa;
import com.empresafac.backend_factu.entities.Pedido;
import com.empresafac.backend_factu.entities.Seccion;
import com.empresafac.backend_factu.repositories.MesaRepository;
import com.empresafac.backend_factu.repositories.PedidoItemRepository;
import com.empresafac.backend_factu.repositories.PedidoRepository;
import com.empresafac.backend_factu.repositories.SeccionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MesaService {

    private final MesaRepository mesaRepository;
    private final SeccionRepository seccionRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoItemRepository pedidoItemRepository;
    private final PlanValidadorService planValidador; // ✅ inyectado

    @Transactional
    public Mesa crear(Long empresaId, Long seccionId, String nombre) {

        // ✅ Validar límite del plan antes de crear
        planValidador.validarLimiteMesas(empresaId);

        Seccion seccion = seccionRepository.findById(seccionId)
                .orElseThrow(() -> new RuntimeException("Sección no encontrada"));
        if (!seccion.getEmpresa().getId().equals(empresaId)) {
            throw new RuntimeException("Sección no pertenece a la empresa");
        }

        Mesa mesa = new Mesa();
        mesa.setEmpresa(seccion.getEmpresa());
        mesa.setSeccion(seccion);
        mesa.setNombre(nombre);
        mesa.setEstado(Mesa.Estado.LIBRE);
        mesa.setActiva(true);

        return mesaRepository.save(mesa);
    }

    public List<Mesa> listar(Long empresaId) {
        return mesaRepository.findAllByEmpresaIdAndActivaTrue(empresaId);
    }

    public Mesa obtener(Long empresaId, Long mesaId) {
        return mesaRepository
                .findByIdAndEmpresaIdAndActivaTrue(mesaId, empresaId)
                .orElseThrow(() -> new RuntimeException("Mesa no encontrada"));
    }

    @Transactional
    public Mesa actualizar(Long empresaId, Long mesaId, String nombre, Long nuevaSeccionId) {
        Mesa mesa = obtener(empresaId, mesaId);
        if (nombre != null && !nombre.isBlank()) {
            mesa.setNombre(nombre);
        }
        if (nuevaSeccionId != null) {
            Seccion sec = seccionRepository.findById(nuevaSeccionId)
                    .orElseThrow(() -> new RuntimeException("Sección no encontrada"));
            if (!sec.getEmpresa().getId().equals(empresaId)) {
                throw new RuntimeException("Sección no pertenece a la empresa");
            }
            mesa.setSeccion(sec);
        }
        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa cambiarEstado(Long empresaId, Long mesaId, Mesa.Estado estado) {
        Mesa mesa = obtener(empresaId, mesaId);
        mesa.setEstado(estado);
        return mesaRepository.save(mesa);
    }

    @Transactional
    public void eliminar(Long empresaId, Long mesaId) {
        Mesa mesa = obtener(empresaId, mesaId);
        mesa.setActiva(false);
        mesaRepository.save(mesa);
    }

    private Pedido obtenerPedidoValidado(Long empresaId, Long mesaId) {
        Mesa mesa = mesaRepository.findByIdAndEmpresaIdAndActivaTrue(mesaId, empresaId)
                .orElseThrow(() -> new RuntimeException("La mesa no existe o no está activa"));

        Pedido pedido = pedidoRepository.findByMesaIdAndEstado(mesaId, Pedido.Estado.ABIERTO)
                .orElseThrow(() -> new RuntimeException("No hay un pedido abierto para la mesa: " + mesa.getNombre()));

        if (pedido.getItems() == null || pedido.getItems().isEmpty()) {
            throw new RuntimeException("El pedido no tiene productos registrados.");
        }

        return pedido;
    }
}