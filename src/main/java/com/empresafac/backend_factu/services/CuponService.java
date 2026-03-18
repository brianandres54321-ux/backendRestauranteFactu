package com.empresafac.backend_factu.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresafac.backend_factu.dto_temp.request.CuponRequest;
import com.empresafac.backend_factu.dto_temp.response.CuponResponse;
import com.empresafac.backend_factu.dto_temp.response.ValidarCuponResponse;
import com.empresafac.backend_factu.entities.Cupon;
import com.empresafac.backend_factu.entities.Pedido;
import com.empresafac.backend_factu.repositories.CuponRepository;
import com.empresafac.backend_factu.repositories.EmpresaRepository;
import com.empresafac.backend_factu.repositories.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CuponService {

    private final CuponRepository cuponRepository;
    private final EmpresaRepository empresaRepository;
    private final PedidoRepository pedidoRepository;

    // ─────────────────────────────────────────────────────────────
    // GESTIÓN DE CUPONES (admin)
    // ─────────────────────────────────────────────────────────────

    public List<CuponResponse> listar(Long empresaId) {
        return cuponRepository.findAllByEmpresaIdOrderByCreadoEnDesc(empresaId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CuponResponse crear(Long empresaId, CuponRequest req) {
        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        // Código único por empresa (case-insensitive)
        if (cuponRepository.findByEmpresaIdAndCodigoIgnoreCase(empresaId,
                req.getCodigo().trim()).isPresent()) {
            throw new RuntimeException("Ya existe un cupón con el código '"
                    + req.getCodigo().toUpperCase() + "'");
        }

        Cupon cupon = new Cupon();
        cupon.setEmpresa(empresa);
        mapearRequest(cupon, req);
        return toResponse(cuponRepository.save(cupon));
    }

    @Transactional
    public CuponResponse actualizar(Long empresaId, Long cuponId, CuponRequest req) {
        Cupon cupon = getCuponDeEmpresa(empresaId, cuponId);

        // Si cambia el código, verificar que no exista otro con ese código
        if (!cupon.getCodigo().equalsIgnoreCase(req.getCodigo().trim())) {
            cuponRepository.findByEmpresaIdAndCodigoIgnoreCase(empresaId,
                    req.getCodigo().trim()).ifPresent(c -> {
                        throw new RuntimeException("Ya existe un cupón con el código '"
                                + req.getCodigo().toUpperCase() + "'");
                    });
        }
        mapearRequest(cupon, req);
        return toResponse(cuponRepository.save(cupon));
    }

    @Transactional
    public void eliminar(Long empresaId, Long cuponId) {
        Cupon cupon = getCuponDeEmpresa(empresaId, cuponId);
        cuponRepository.delete(cupon);
    }

    // ─────────────────────────────────────────────────────────────
    // VALIDAR CUPÓN EN CHECKOUT
    // GET /empresas/{id}/cupones/validar?codigo=XXX&pedidoId=YYY
    // ─────────────────────────────────────────────────────────────

    public ValidarCuponResponse validar(Long empresaId, String codigo, Long pedidoId) {

        // 1. Buscar el cupón
        var opt = cuponRepository.findByEmpresaIdAndCodigoIgnoreCase(empresaId, codigo.trim());
        if (opt.isEmpty()) {
            return invalido("Cupón no encontrado");
        }
        Cupon cupon = opt.get();

        // 2. Verificar activo
        if (!cupon.isActivo()) {
            return invalido("Este cupón está inactivo");
        }

        // 3. Verificar vigencia
        LocalDate hoy = LocalDate.now();
        if (cupon.getFechaInicio() != null && hoy.isBefore(cupon.getFechaInicio())) {
            return invalido("El cupón aún no está vigente");
        }
        if (cupon.getFechaFin() != null && hoy.isAfter(cupon.getFechaFin())) {
            return invalido("El cupón está vencido");
        }

        // 4. Verificar usos disponibles
        if (cupon.getUsosMaximos() != null
                && cupon.getUsosActuales() >= cupon.getUsosMaximos()) {
            return invalido("El cupón ya alcanzó su límite de usos");
        }

        // 5. Verificar monto mínimo del pedido
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (cupon.getMontoMinimo() != null
                && pedido.getTotal().compareTo(cupon.getMontoMinimo()) < 0) {
            return invalido("El pedido debe ser mínimo de $"
                    + String.format("%,.0f", cupon.getMontoMinimo())
                    + " para aplicar este cupón");
        }

        // 6. Calcular descuento
        BigDecimal descuento = calcularDescuento(cupon, pedido.getTotal());
        BigDecimal totalConDescuento = pedido.getTotal().subtract(descuento)
                .max(BigDecimal.ZERO);

        ValidarCuponResponse res = new ValidarCuponResponse();
        res.setValido(true);
        res.setMensaje("Cupón aplicado correctamente");
        res.setCodigo(cupon.getCodigo().toUpperCase());
        res.setTipo(cupon.getTipo().name());
        res.setValor(cupon.getValor());
        res.setDescuento(descuento);
        res.setTotalConDescuento(totalConDescuento);
        return res;
    }

    // ─────────────────────────────────────────────────────────────
    // APLICAR CUPÓN — lo llama PagoService al registrar el pago
    // Incrementa el contador de usos
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public BigDecimal aplicar(Long empresaId, String codigo, BigDecimal totalPedido) {
        Cupon cupon = cuponRepository
                .findByEmpresaIdAndCodigoIgnoreCase(empresaId, codigo.trim())
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));

        BigDecimal descuento = calcularDescuento(cupon, totalPedido);
        cupon.setUsosActuales(cupon.getUsosActuales() + 1);
        cuponRepository.save(cupon);
        return descuento;
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────

    private BigDecimal calcularDescuento(Cupon cupon, BigDecimal total) {
        if (cupon.getTipo() == Cupon.Tipo.PORCENTAJE) {
            return total.multiply(cupon.getValor())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            // MONTO_FIJO — no puede superar el total
            return cupon.getValor().min(total);
        }
    }

    private ValidarCuponResponse invalido(String mensaje) {
        ValidarCuponResponse r = new ValidarCuponResponse();
        r.setValido(false);
        r.setMensaje(mensaje);
        return r;
    }

    private Cupon getCuponDeEmpresa(Long empresaId, Long cuponId) {
        Cupon c = cuponRepository.findById(cuponId)
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado"));
        if (!c.getEmpresa().getId().equals(empresaId)) {
            throw new RuntimeException("Cupón no pertenece a esta empresa");
        }
        return c;
    }

    private void mapearRequest(Cupon cupon, CuponRequest req) {
        cupon.setCodigo(req.getCodigo().trim().toUpperCase());
        cupon.setDescripcion(req.getDescripcion());
        cupon.setTipo(Cupon.Tipo.valueOf(req.getTipo()));
        cupon.setValor(req.getValor());
        cupon.setUsosMaximos(req.getUsosMaximos());
        cupon.setFechaInicio(req.getFechaInicio() != null && !req.getFechaInicio().isBlank()
                ? LocalDate.parse(req.getFechaInicio())
                : null);
        cupon.setFechaFin(req.getFechaFin() != null && !req.getFechaFin().isBlank()
                ? LocalDate.parse(req.getFechaFin())
                : null);
        cupon.setMontoMinimo(req.getMontoMinimo());
        cupon.setActivo(req.isActivo());
    }

    private CuponResponse toResponse(Cupon c) {
        return new CuponResponse(
                c.getId(),
                c.getCodigo(),
                c.getDescripcion(),
                c.getTipo().name(),
                c.getValor(),
                c.getUsosMaximos(),
                c.getUsosActuales(),
                c.getFechaInicio() != null ? c.getFechaInicio().toString() : null,
                c.getFechaFin() != null ? c.getFechaFin().toString() : null,
                c.getMontoMinimo(),
                c.isActivo(),
                c.getCreadoEn().toString());
    }
}