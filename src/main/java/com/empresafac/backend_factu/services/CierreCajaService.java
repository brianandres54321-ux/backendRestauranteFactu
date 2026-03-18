package com.empresafac.backend_factu.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.empresafac.backend_factu.dto_temp.response.CierreCajaResponse;
import com.empresafac.backend_factu.dto_temp.response.CierreCajaResponse.FilaPagoDTO;
import com.empresafac.backend_factu.entities.CierreCaja;
import com.empresafac.backend_factu.entities.Pago;
import com.empresafac.backend_factu.entities.Usuario;
import com.empresafac.backend_factu.repositories.CierreCajaRepository;
import com.empresafac.backend_factu.repositories.EmpresaRepository;
import com.empresafac.backend_factu.repositories.PagoRepository;
import com.empresafac.backend_factu.repositories.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CierreCajaService {

    private final CierreCajaRepository cierreRepository;
    private final PagoRepository pagoRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;

    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ─────────────────────────────────────────────────────────────
    // PREVIEW — calcula el resumen sin guardar nada
    // GET /empresas/{id}/cierres/preview?fecha=2026-03-18
    // ─────────────────────────────────────────────────────────────

    public CierreCajaResponse preview(Long empresaId, LocalDate fecha) {

        boolean yaExiste = cierreRepository.existsByEmpresaIdAndFecha(empresaId, fecha);

        // Si ya existe, devuelve el cierre guardado directamente
        if (yaExiste) {
            CierreCaja existente = cierreRepository
                    .findByEmpresaIdAndFecha(empresaId, fecha).orElseThrow();
            CierreCajaResponse res = toResponse(existente);
            res.setYaExiste(true);
            return res;
        }

        List<Pago> pagos = getPagosDeDia(empresaId, fecha);
        return buildPreview(empresaId, fecha, pagos);
    }

    // ─────────────────────────────────────────────────────────────
    // EJECUTAR CIERRE — guarda el corte en la BD
    // POST /empresas/{id}/cierres
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public CierreCajaResponse ejecutarCierre(Long empresaId, Long usuarioId,
            LocalDate fecha, String notas) {

        if (cierreRepository.existsByEmpresaIdAndFecha(empresaId, fecha)) {
            throw new RuntimeException(
                    "Ya existe un cierre de caja para el " + fecha.format(FMT_FECHA));
        }

        List<Pago> pagos = getPagosDeDia(empresaId, fecha);

        BigDecimal totalVentas = sumar(pagos);
        BigDecimal totalEfectivo = sumarMetodo(pagos, Pago.Metodo.EFECTIVO);
        BigDecimal totalMercadoPago = sumarMetodo(pagos, Pago.Metodo.MERCADOPAGO);
        long cantidadPedidos = pagos.stream()
                .map(p -> p.getPedido().getId())
                .distinct().count();

        var empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        CierreCaja cierre = new CierreCaja();
        cierre.setEmpresa(empresa);
        cierre.setUsuario(usuario);
        cierre.setFecha(fecha);
        cierre.setCerradoEn(LocalDateTime.now());
        cierre.setTotalVentas(totalVentas);
        cierre.setTotalEfectivo(totalEfectivo);
        cierre.setTotalMercadoPago(totalMercadoPago);
        cierre.setCantidadPedidos((int) cantidadPedidos);
        cierre.setNotas(notas);

        cierreRepository.save(cierre);

        CierreCajaResponse res = toResponse(cierre);
        res.setYaExiste(false);
        res.setPagos(toFilas(pagos));
        return res;
    }

    // ─────────────────────────────────────────────────────────────
    // HISTORIAL — todos los cierres de la empresa
    // GET /empresas/{id}/cierres
    // ─────────────────────────────────────────────────────────────

    public List<CierreCajaResponse> historial(Long empresaId) {
        return cierreRepository
                .findAllByEmpresaIdOrderByFechaDesc(empresaId)
                .stream()
                .map(c -> {
                    CierreCajaResponse r = toResponse(c);
                    r.setYaExiste(true);
                    return r;
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // DETALLE de un cierre — incluye la lista de pagos del día
    // GET /empresas/{id}/cierres/{cierreId}
    // ─────────────────────────────────────────────────────────────

    public CierreCajaResponse detalle(Long empresaId, Long cierreId) {
        CierreCaja cierre = cierreRepository.findById(cierreId)
                .orElseThrow(() -> new RuntimeException("Cierre no encontrado"));

        if (!cierre.getEmpresa().getId().equals(empresaId)) {
            throw new RuntimeException("Cierre no pertenece a esta empresa");
        }

        List<Pago> pagos = getPagosDeDia(empresaId, cierre.getFecha());
        CierreCajaResponse res = toResponse(cierre);
        res.setYaExiste(true);
        res.setPagos(toFilas(pagos));
        return res;
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────

    private List<Pago> getPagosDeDia(Long empresaId, LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        return pagoRepository.findAllByPedidoEmpresaIdAndFechaBetween(empresaId, inicio, fin);
    }

    private BigDecimal sumar(List<Pago> pagos) {
        return pagos.stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarMetodo(List<Pago> pagos, Pago.Metodo metodo) {
        return pagos.stream()
                .filter(p -> p.getMetodo() == metodo)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CierreCajaResponse buildPreview(Long empresaId, LocalDate fecha, List<Pago> pagos) {
        BigDecimal totalVentas = sumar(pagos);
        BigDecimal totalEfectivo = sumarMetodo(pagos, Pago.Metodo.EFECTIVO);
        BigDecimal totalMercadoPago = sumarMetodo(pagos, Pago.Metodo.MERCADOPAGO);
        long cantidadPedidos = pagos.stream()
                .map(p -> p.getPedido().getId())
                .distinct().count();
        BigDecimal ticket = cantidadPedidos == 0 ? BigDecimal.ZERO
                : totalVentas.divide(BigDecimal.valueOf(cantidadPedidos), 2, RoundingMode.HALF_UP);

        CierreCajaResponse res = new CierreCajaResponse();
        res.setId(null);
        res.setFecha(fecha.format(FMT_FECHA));
        res.setCerradoEn(null);
        res.setUsuarioNombre("");
        res.setTotalVentas(totalVentas);
        res.setTotalEfectivo(totalEfectivo);
        res.setTotalMercadoPago(totalMercadoPago);
        res.setCantidadPedidos((int) cantidadPedidos);
        res.setTicketPromedio(ticket);
        res.setNotas("");
        res.setYaExiste(false);
        res.setPagos(toFilas(pagos));
        return res;
    }

    private CierreCajaResponse toResponse(CierreCaja c) {
        long cant = c.getCantidadPedidos();
        BigDecimal ticket = cant == 0 ? BigDecimal.ZERO
                : c.getTotalVentas().divide(BigDecimal.valueOf(cant), 2, RoundingMode.HALF_UP);

        CierreCajaResponse res = new CierreCajaResponse();
        res.setId(c.getId());
        res.setFecha(c.getFecha().format(FMT_FECHA));
        res.setCerradoEn(c.getCerradoEn() != null ? c.getCerradoEn().toString() : null);
        res.setUsuarioNombre(c.getUsuario().getNombre());
        res.setTotalVentas(c.getTotalVentas());
        res.setTotalEfectivo(c.getTotalEfectivo());
        res.setTotalMercadoPago(c.getTotalMercadoPago());
        res.setCantidadPedidos(c.getCantidadPedidos());
        res.setTicketPromedio(ticket);
        res.setNotas(c.getNotas());
        return res;
    }

    private List<FilaPagoDTO> toFilas(List<Pago> pagos) {
        return pagos.stream().map(p -> {
            String mesa = p.getPedido().getMesa() != null
                    ? p.getPedido().getMesa().getNombre()
                    : "Sin mesa";
            return new FilaPagoDTO(
                    p.getPedido().getId(),
                    mesa,
                    p.getMetodo().name(),
                    p.getMonto(),
                    p.getFecha().format(FMT_HORA));
        }).collect(Collectors.toList());
    }
}