package com.empresafac.backend_factu.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresafac.backend_factu.entities.Empresa;
import com.empresafac.backend_factu.repositories.EmpresaRepository;
import com.empresafac.backend_factu.repositories.MesaRepository;
import com.empresafac.backend_factu.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanValidadorService planValidador;

    public Empresa crear(Empresa empresa) {
        return empresaRepository.save(empresa);
    }

    public List<Empresa> listar() {
        return empresaRepository.findAllByActivaTrue();
    }

    public Optional<Empresa> buscarPorId(Long id) {
        return empresaRepository.findByIdAndActivaTrue(id);
    }

    @Transactional
    public Empresa actualizar(Long id, Empresa datos) {
        Empresa existente = empresaRepository.findByIdAndActivaTrue(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        // ✅ Si cambia el plan, validar que el nuevo plan soporta lo que ya tiene
        String planActual = existente.getPlan();
        String planNuevo = datos.getPlan() != null ? datos.getPlan().toUpperCase() : planActual;

        if (!planNuevo.equals(planActual)) {
            validarDowngrade(id, planActual, planNuevo);
        }

        existente.setNombre(datos.getNombre());
        existente.setNitRut(datos.getNitRut());
        existente.setPlan(planNuevo);
        return empresaRepository.save(existente);
    }

    /**
     * Valida que al bajar de plan no se excedan los nuevos límites.
     * Lanza excepción con mensaje descriptivo si hay conflicto.
     */
    private void validarDowngrade(Long empresaId, String planActual, String planNuevo) {
        // Solo importa si es un downgrade real
        if (esUpgrade(planActual, planNuevo))
            return;

        int mesasActuales = mesaRepository.findAllByEmpresaIdAndActivaTrue(empresaId).size();
        int usuariosActuales = usuarioRepository.findAllByEmpresaId(empresaId).size();

        int limiteMesasNuevo = planValidador.getLimiteMesas(planNuevo);
        int limiteUsuariosNuevo = planValidador.getLimiteUsuarios(planNuevo);

        StringBuilder errores = new StringBuilder();

        if (mesasActuales > limiteMesasNuevo) {
            errores.append("Tienes ").append(mesasActuales)
                    .append(" mesa(s) activas y el plan ").append(planNuevo)
                    .append(" permite máximo ").append(limiteMesasNuevo).append(". ");
        }

        if (usuariosActuales > limiteUsuariosNuevo) {
            errores.append("Tienes ").append(usuariosActuales)
                    .append(" usuario(s) y el plan ").append(planNuevo)
                    .append(" permite máximo ").append(limiteUsuariosNuevo).append(". ");
        }

        // Si el nuevo plan no permite MercadoPago, solo advertir (no bloquear)
        // porque los pagos anteriores ya están registrados

        if (errores.length() > 0) {
            throw new RuntimeException(
                    "No puedes bajar al plan " + planNuevo + ". " + errores +
                            "Elimina el excedente antes de cambiar de plan.");
        }
    }

    /**
     * Determina si el cambio es una subida de plan (upgrade).
     * Orden: BASICO < PRO < PREMIUM
     */
    private boolean esUpgrade(String planActual, String planNuevo) {
        int[] nivel = { nivelPlan(planActual), nivelPlan(planNuevo) };
        return nivel[1] > nivel[0];
    }

    private int nivelPlan(String plan) {
        return switch (plan != null ? plan.toUpperCase() : "BASICO") {
            case "PRO" -> 2;
            case "PREMIUM" -> 3;
            default -> 1; // BASICO
        };
    }

    @Transactional
    public void eliminar(Long id) {
        Empresa existente = empresaRepository.findByIdAndActivaTrue(id)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        existente.setActiva(false);
        empresaRepository.save(existente);
    }
}