package com.empresafac.backend_factu.services;

import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.entities.Empresa;
import com.empresafac.backend_factu.repositories.EmpresaRepository;
import com.empresafac.backend_factu.repositories.MesaRepository;
import com.empresafac.backend_factu.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Centraliza las reglas de límites por plan.
 *
 * BASICO → 5 mesas, 1 usuario, sin MercadoPago
 * PRO → 20 mesas, 5 usuarios, con MercadoPago
 * PREMIUM → ilimitado, ilimitado, todo incluido
 */
@Service
@RequiredArgsConstructor
public class PlanValidadorService {

    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmpresaRepository empresaRepository;

    // ── Límites por plan ──────────────────────────────────────────

    public int getLimiteMesas(String plan) {
        return switch (plan != null ? plan.toUpperCase() : "BASICO") {
            case "PRO" -> 20;
            case "PREMIUM" -> Integer.MAX_VALUE;
            default -> 5; // BASICO
        };
    }

    public int getLimiteUsuarios(String plan) {
        return switch (plan != null ? plan.toUpperCase() : "BASICO") {
            case "PRO" -> 5;
            case "PREMIUM" -> Integer.MAX_VALUE;
            default -> 1; // BASICO
        };
    }

    public boolean permiteMercadoPago(String plan) {
        if (plan == null)
            return false;
        return plan.equalsIgnoreCase("PRO") || plan.equalsIgnoreCase("PREMIUM");
    }

    // ── Validaciones ─────────────────────────────────────────────

    /**
     * Lanza excepción si la empresa ya alcanzó el límite de mesas del plan.
     */
    public void validarLimiteMesas(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        int actual = mesaRepository.findAllByEmpresaIdAndActivaTrue(empresaId).size();
        int limite = getLimiteMesas(empresa.getPlan());

        if (actual >= limite) {
            throw new RuntimeException(
                    "Tu plan " + empresa.getPlan() + " permite hasta " + limite +
                            " mesa(s). Tienes " + actual + ". Actualiza tu plan para agregar más.");
        }
    }

    /**
     * Lanza excepción si la empresa ya alcanzó el límite de usuarios del plan.
     */
    public void validarLimiteUsuarios(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        int actual = usuarioRepository.findAllByEmpresaId(empresaId).size();
        int limite = getLimiteUsuarios(empresa.getPlan());

        if (actual >= limite) {
            throw new RuntimeException(
                    "Tu plan " + empresa.getPlan() + " permite hasta " + limite +
                            " usuario(s). Tienes " + actual + ". Actualiza tu plan para agregar más.");
        }
    }

    /**
     * Lanza excepción si el plan no incluye MercadoPago.
     */
    public void validarMercadoPago(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        if (!permiteMercadoPago(empresa.getPlan())) {
            throw new RuntimeException(
                    "Tu plan " + empresa.getPlan() + " no incluye pagos con MercadoPago. " +
                            "Actualiza al plan PRO o PREMIUM para habilitarlo.");
        }
    }

    public void validarReportes(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        String plan = empresa.getPlan();
        boolean permitido = "PRO".equalsIgnoreCase(plan) || "PREMIUM".equalsIgnoreCase(plan);
        if (!permitido) {
            throw new RuntimeException(
                    "Tu plan " + plan + " no incluye reportes. " +
                            "Actualiza al plan PRO o PREMIUM para exportar.");
        }
    }
}