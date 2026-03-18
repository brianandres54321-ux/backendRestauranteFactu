package com.empresafac.backend_factu.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.entities.Mesa;
import com.empresafac.backend_factu.entities.MesaGrupo;
import com.empresafac.backend_factu.entities.MesaGrupoDetalle;
import com.empresafac.backend_factu.entities.Pedido;
import com.empresafac.backend_factu.entities.PedidoItem;
import com.empresafac.backend_factu.repositories.EmpresaRepository;
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

    private final EmpresaRepository empresaRepository;
    private final MesaRepository mesaRepository;
    private final MesaGrupoRepository mesaGrupoRepository;
    private final MesaGrupoDetalleRepository detalleRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoItemRepository pedidoItemRepository;

    @Transactional
    public void unirMesas(Long empresaId, List<Long> mesasIds) {
        if (mesasIds == null || mesasIds.size() < 2) {
            throw new RuntimeException("Seleccione al menos 2 mesas.");
        }

        // 1. Buscar o crear grupo (Tu lógica actual, está perfecta)
        MesaGrupo grupo = mesasIds.stream()
                .map(detalleRepository::findByMesaId)
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getGrupo())
                .filter(g -> g.getEstado() == MesaGrupo.Estado.ACTIVO)
                .findFirst()
                .orElseGet(() -> {
                    MesaGrupo nuevoGrupo = new MesaGrupo();
                    nuevoGrupo.setEmpresa(empresaRepository.getReferenceById(empresaId));
                    nuevoGrupo.setEstado(MesaGrupo.Estado.ACTIVO);
                    return mesaGrupoRepository.save(nuevoGrupo);
                });

        // 2. Vincular mesas al grupo
        List<Mesa> objetosMesa = new ArrayList<>(); // La usaremos para la fusión
        for (Long mesaId : mesasIds) {
            Mesa mesa = mesaRepository.findById(mesaId)
                    .orElseThrow(() -> new RuntimeException("Mesa no encontrada"));
            objetosMesa.add(mesa);

            if (!detalleRepository.existsByMesaIdAndGrupoId(mesaId, grupo.getId())) {
                MesaGrupoDetalle detalle = new MesaGrupoDetalle();
                detalle.setGrupo(grupo);
                detalle.setMesa(mesa);
                detalleRepository.save(detalle);
            }
        }

        // 🔥 3. LA PIEZA QUE FALTA: Fusionar las cuentas
        // Esto hará que si la Mesa 1 tenía productos, ahora el Pedido sea del Grupo
        fusionarPedidosSiExisten(objetosMesa, grupo);

        // 4. Sincronizar estados (Para que se vean ROJAS si hay consumo)
        actualizarEstadosPostUnion(grupo);
    }

    private void actualizarEstadosPostUnion(MesaGrupo grupo) {
        Optional<Pedido> pedidoOpt = pedidoRepository.findByGrupoIdAndEstado(grupo.getId(), Pedido.Estado.ABIERTO);
        boolean debeEstarOcupada = pedidoOpt.isPresent();
        List<MesaGrupoDetalle> detalles = detalleRepository.findAllByGrupoId(grupo.getId());
        for (MesaGrupoDetalle d : detalles) {
            Mesa m = d.getMesa();
            m.setEstado(debeEstarOcupada ? Mesa.Estado.OCUPADA : Mesa.Estado.LIBRE);
            mesaRepository.save(m);
        }
    }

    private void fusionarPedidosSiExisten(List<Mesa> mesas, MesaGrupo grupo) {
        List<Pedido> pedidosAbiertos = mesas.stream()
                .map(m -> pedidoRepository.findByMesaIdAndEstado(m.getId(), Pedido.Estado.ABIERTO))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        if (pedidosAbiertos.isEmpty()) {
            return;
        }

        // El primer pedido encontrado se convierte en el "Dominante" del grupo
        Pedido principal = pedidosAbiertos.get(0);

        for (int i = 1; i < pedidosAbiertos.size(); i++) {
            Pedido secundario = pedidosAbiertos.get(i);

            // Mover items del secundario al principal
            for (PedidoItem item : secundario.getItems()) {
                item.setPedido(principal);
                pedidoItemRepository.save(item);
            }

            principal.setTotal(principal.getTotal().add(secundario.getTotal()));

            // Cancelar el pedido que fue absorbido
            secundario.setEstado(Pedido.Estado.CANCELADO);
            pedidoRepository.save(secundario);
        }

        // IMPORTANTE: Ahora el pedido principal ya no pertenece a una mesa individual, sino al grupo
        principal.setGrupo(grupo);
        principal.setMesa(null);
        pedidoRepository.save(principal);
    }

    @Transactional
    public void desunirMesa(Long empresaId, Long mesaId) {
        MesaGrupoDetalle detalle = detalleRepository.findByMesaId(mesaId)
                .orElseThrow(() -> new RuntimeException("La mesa no está vinculada a ningún grupo."));

        MesaGrupo grupo = detalle.getGrupo();
        Mesa mesaQueSale = detalle.getMesa();

        // Buscamos el pedido activo del grupo
        Optional<Pedido> pedidoGrupo = pedidoRepository.findByGrupoIdAndEstado(grupo.getId(), Pedido.Estado.ABIERTO);

        // 1. La mesa que el usuario seleccionó para desunir SIEMPRE queda LIBRE
        detalleRepository.delete(detalle);
        mesaQueSale.setEstado(Mesa.Estado.LIBRE);
        mesaRepository.save(mesaQueSale);

        List<MesaGrupoDetalle> restantes = detalleRepository.findAllByGrupoId(grupo.getId());

        // 2. Si al quitar la mesa, el grupo se deshace (queda 1 o 0 mesas)
        if (restantes.size() < 2) {
            if (restantes.size() == 1) {
                Mesa ultimaMesa = restantes.get(0).getMesa();

                if (pedidoGrupo.isPresent()) {
                    Pedido p = pedidoGrupo.get();

                    // 🔥 CAMBIO CLAVE: Verificar si el pedido tiene productos
                    boolean tieneConsumo = !p.getItems().isEmpty();

                    if (tieneConsumo) {
                        // Si tiene productos, transferimos el pedido a la mesa y queda OCUPADA
                        p.setGrupo(null);
                        p.setMesa(ultimaMesa);
                        pedidoRepository.save(p);
                        ultimaMesa.setEstado(Mesa.Estado.OCUPADA);
                    } else {
                        // Si NO tiene productos, cancelamos el pedido y la mesa queda LIBRE
                        p.setEstado(Pedido.Estado.CANCELADO);
                        p.setGrupo(null);
                        p.setMesa(null);
                        pedidoRepository.save(p);
                        ultimaMesa.setEstado(Mesa.Estado.LIBRE);
                    }
                } else {
                    // No había pedido, la mesa queda libre
                    ultimaMesa.setEstado(Mesa.Estado.LIBRE);
                }

                mesaRepository.save(ultimaMesa);
                detalleRepository.deleteAllByGrupoId(grupo.getId());

            } else {
                // Caso donde no quedan mesas (size == 0)
                pedidoGrupo.ifPresent(p -> {
                    p.setEstado(Pedido.Estado.CANCELADO);
                    p.setGrupo(null);
                    pedidoRepository.save(p);
                });
            }

            // 3. Cerramos el grupo definitivamente
            grupo.setEstado(MesaGrupo.Estado.CERRADO);
            mesaGrupoRepository.save(grupo);
        }
    }

    public List<MesaGrupo> listar(Long empresaId) {
        return mesaGrupoRepository.findAllByEmpresaIdAndEstado(empresaId, MesaGrupo.Estado.ACTIVO);
    }

    public MesaGrupo obtener(Long empresaId, Long grupoId) {
        return mesaGrupoRepository.findByIdAndEmpresaId(grupoId, empresaId)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
    }

    @Transactional
    public MesaGrupo actualizarEstado(Long empresaId, Long grupoId, MesaGrupo.Estado estado) {
        MesaGrupo g = obtener(empresaId, grupoId);
        g.setEstado(estado);
        return mesaGrupoRepository.save(g);
    }

    @Transactional
    public void eliminar(Long empresaId, Long grupoId) {
        MesaGrupo g = obtener(empresaId, grupoId);
        detalleRepository.deleteAllByGrupoId(g.getId());
        mesaGrupoRepository.delete(g);
    }

    // src/main/java/com/empresafac/backend_factu/services/MesaGrupoService.java
    @Transactional
    public void liberarMesasDeGrupo(MesaGrupo grupo) {
        // 1. Buscamos todos los detalles (las mesas) que pertenecen a este grupo
        List<MesaGrupoDetalle> detalles = detalleRepository.findAllByGrupoId(grupo.getId());

        for (MesaGrupoDetalle detalle : detalles) {
            Mesa mesa = detalle.getMesa();
            // 2. Ponemos cada mesa en estado LIBRE
            mesa.setEstado(Mesa.Estado.LIBRE);
            mesaRepository.save(mesa);
        }

        // 3. Opcional: Marcamos el grupo como CERRADO para que no aparezca en listas activas
        grupo.setEstado(MesaGrupo.Estado.CERRADO);
        mesaGrupoRepository.save(grupo);

        // 4. Limpiamos la tabla de detalles (rompemos la unión física de las mesas)
        detalleRepository.deleteAllByGrupoId(grupo.getId());

        System.out.println("Grupo " + grupo.getId() + " liberado y mesas puestas en LIBRE.");
    }
}
