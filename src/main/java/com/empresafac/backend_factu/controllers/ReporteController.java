package com.empresafac.backend_factu.controllers;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.empresafac.backend_factu.dto_temp.response.ReporteVentasResponse;
import com.empresafac.backend_factu.services.ReporteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/empresas/{empresaId}/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    /**
     * GET /empresas/{id}/reportes/ventas?desde=2025-01-01&hasta=2025-01-31
     * Devuelve el reporte como JSON (para renderizar en Angular).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ventas")
    public ResponseEntity<ReporteVentasResponse> reporteJson(
            @PathVariable Long empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        return ResponseEntity.ok(reporteService.generarReporte(empresaId, desde, hasta));
    }

    /**
     * GET /empresas/{id}/reportes/ventas/pdf?desde=...&hasta=...
     * Descarga el reporte como PDF.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ventas/pdf")
    public ResponseEntity<byte[]> reportePdf(
            @PathVariable Long empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) throws Exception {

        byte[] pdf = reporteService.generarPdf(empresaId, desde, hasta);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte-ventas-" + desde + "-" + hasta + ".pdf\"")
                .body(pdf);
    }

    /**
     * GET /empresas/{id}/reportes/ventas/excel?desde=...&hasta=...
     * Descarga el reporte como Excel (.xlsx).
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ventas/excel")
    public ResponseEntity<byte[]> reporteExcel(
            @PathVariable Long empresaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) throws Exception {

        byte[] excel = reporteService.generarExcel(empresaId, desde, hasta);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte-ventas-" + desde + "-" + hasta + ".xlsx\"")
                .body(excel);
    }
}