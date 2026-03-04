package com.empresafac.backend_factu.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.entities.Mesa;
import com.empresafac.backend_factu.entities.MesaGrupo;
import com.empresafac.backend_factu.entities.MesaGrupoDetalle;
import com.empresafac.backend_factu.entities.Pedido;
import com.empresafac.backend_factu.entities.PedidoItem;
import com.empresafac.backend_factu.repositories.MesaGrupoDetalleRepository;
import com.empresafac.backend_factu.repositories.MesaGrupoRepository;
import com.empresafac.backend_factu.repositories.MesaRepository;
import com.empresafac.backend_factu.repositories.PedidoItemRepository;
import com.empresafac.backend_factu.repositories.PedidoRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MesaGrupoService {

    private final MesaRepository mesaRepository;
    private final MesaGrupoRepository mesaGrupoRepository;
    private final MesaGrupoDetalleRepository detalleRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoItemRepository pedidoItemRepository;

    /*
     * Une varias mesas en un grupo. La lógica ya existente permanece intacta.
     */
    @Transactional
    public void unirMesas(Long empresaId, List<Long> mesasIds) {

        if (mesasIds.size() < 2) {
            throw new RuntimeException("Se necesitan al menos 2 mesas");
        }

        List<Mesa> mesas = mesaRepository.findAllById(mesasIds);

        if (mesas.size() != mesasIds.size()) {
            throw new RuntimeException("Alguna mesa no existe");
        }

        // Validar empresa y estado
        for (Mesa mesa : mesas) {

            if (!mesa.getEmpresa().getId().equals(empresaId)) {
                throw new RuntimeException("Mesa no pertenece a la empresa");
            }

            if (mesa.getEstado() == Mesa.Estado.BLOQUEADA) {
                throw new RuntimeException("Mesa bloqueada");
            }
        }

        // Crear grupo
        MesaGrupo grupo = new MesaGrupo();
        grupo.setEmpresa(mesas.get(0).getEmpresa());
        grupo.setEstado(MesaGrupo.Estado.ACTIVO);
        mesaGrupoRepository.save(grupo);

        // Asociar mesas 
        for (Mesa mesa : mesas) {

            MesaGrupoDetalle detalle = new MesaGrupoDetalle();
            detalle.setGrupo(grupo);
            detalle.setMesa(mesa);
            detalleRepository.save(detalle);

            mesa.setEstado(Mesa.Estado.OCUPADA);
        }

        fusionarPedidosSiExisten(empresaId, mesas, grupo);
    }

    /* CRUD helpers for groups */
    /**
     * Lista todos los grupos de la empresa. Por defecto sólo ACTIVOS; se pueden
     * añadir parámetros si se necesita otro filtro.
     */
    public List<MesaGrupo> listar(Long empresaId) {
        return mesaGrupoRepository.findAllByEmpresaIdAndEstado(empresaId, MesaGrupo.Estado.ACTIVO);
    }

    /**
     * Recupera un grupo por id y empresa.
     */
    public MesaGrupo obtener(Long empresaId, Long grupoId) {
        return mesaGrupoRepository
                .findByIdAndEmpresaId(grupoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
    }

    /**
     * Actualiza el estado de un grupo (p.ej. cerrar).
     */
    @Transactional
    public MesaGrupo actualizarEstado(Long empresaId, Long grupoId, MesaGrupo.Estado nuevoEstado) {
        MesaGrupo grupo = obtener(empresaId, grupoId);
        grupo.setEstado(nuevoEstado);
        return mesaGrupoRepository.save(grupo);
    }

    /**
     * Cierra un grupo (cambia a CERRADO).
     */
    @Transactional
    public MesaGrupo cerrar(Long empresaId, Long grupoId) {
        return actualizarEstado(empresaId, grupoId, MesaGrupo.Estado.CERRADO);
    }

    /**
     * Elimina físicamente un grupo y sus detalles asociados.
     */
    @Transactional
    public void eliminar(Long empresaId, Long grupoId) {
        MesaGrupo grupo = obtener(empresaId, grupoId);
        // borrar detalles primero
        detalleRepository.deleteAllByGrupoId(grupo.getId());
        mesaGrupoRepository.delete(grupo);
    }

    private void fusionarPedidosSiExisten(Long empresaId,
            List<Mesa> mesas,
            MesaGrupo grupo) {

        List<Pedido> pedidosAbiertos = mesas.stream()
                .map(mesa -> pedidoRepository
                .findByMesaIdAndEstado(mesa.getId(), Pedido.Estado.ABIERTO))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (pedidosAbiertos.isEmpty()) {
            return;
        }

        Pedido principal = pedidosAbiertos.get(0);

        for (int i = 1; i < pedidosAbiertos.size(); i++) {

            Pedido secundario = pedidosAbiertos.get(i);

            // mover items
            List<PedidoItem> items
                    = pedidoItemRepository.findByPedidoId(secundario.getId());

            for (PedidoItem item : items) {
                item.setPedido(principal);
                pedidoItemRepository.save(item);
            }

            // sumar totales
            principal.setTotal(
                    principal.getTotal().add(secundario.getTotal())
            );

            secundario.setEstado(Pedido.Estado.CANCELADO);
        }

        principal.setGrupo(grupo);
    }
}
