package com.empresafac.backend_factu.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.empresafac.backend_factu.dto_temp.response.ReporteVentasResponse;
import com.empresafac.backend_factu.dto_temp.response.ReporteVentasResponse.FilaPagoDTO;
import com.empresafac.backend_factu.dto_temp.response.ReporteVentasResponse.ProductoVendidoDTO;
import com.empresafac.backend_factu.entities.Pago;
import com.empresafac.backend_factu.entities.PedidoItem;
import com.empresafac.backend_factu.repositories.EmpresaRepository;
import com.empresafac.backend_factu.repositories.PagoRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;         // iText Cell — única Cell importada
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final PagoRepository    pagoRepository;
    private final EmpresaRepository empresaRepository;
    private final PlanValidadorService planValidador;

    private static final DateTimeFormatter FMT_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATE    = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─────────────────────────────────────────────────────────────
    // REPORTE JSON
    // ─────────────────────────────────────────────────────────────

    public ReporteVentasResponse generarReporte(Long empresaId, LocalDate desde, LocalDate hasta) {

        planValidador.validarReportes(empresaId);

        String empresaNombre = empresaRepository.findById(empresaId)
                .map(e -> e.getNombre())
                .orElse("Empresa");

        LocalDateTime desdeTs = desde.atStartOfDay();
        LocalDateTime hastaTs = hasta.atTime(LocalTime.MAX);

        List<Pago> pagos = pagoRepository
                .findAllByPedidoEmpresaIdAndFechaBetween(empresaId, desdeTs, hastaTs);

        // ── Totales ──────────────────────────────────────────────
        BigDecimal totalVentas      = pagos.stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        long       cantidadPedidos  = pagos.stream().map(p -> p.getPedido().getId()).distinct().count();
        BigDecimal ticketPromedio   = cantidadPedidos == 0 ? BigDecimal.ZERO
                : totalVentas.divide(BigDecimal.valueOf(cantidadPedidos), 2, RoundingMode.HALF_UP);

        BigDecimal totalEfectivo    = pagos.stream()
                .filter(p -> p.getMetodo() == Pago.Metodo.EFECTIVO)
                .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalMercadoPago = pagos.stream()
                .filter(p -> p.getMetodo() == Pago.Metodo.MERCADOPAGO)
                .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);

        // ── Top productos ────────────────────────────────────────
        // getCantidad() y getSubtotal() son BigDecimal en PedidoItem
        Map<String, BigDecimal[]> mapaProductos = new LinkedHashMap<>();
        for (Pago pago : pagos) {
            for (PedidoItem item : pago.getPedido().getItems()) {
                String nombre = item.getProducto().getNombre();
                mapaProductos.computeIfAbsent(nombre, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                BigDecimal[] acum = mapaProductos.get(nombre);
                BigDecimal subtotalItem = item.getCantidad().multiply(item.getPrecioUnitario());
                acum[0] = acum[0].add(item.getCantidad());
                acum[1] = acum[1].add(subtotalItem);
            }
        }

        List<ProductoVendidoDTO> topProductos = mapaProductos.entrySet().stream()
                .sorted((a, b) -> b.getValue()[0].compareTo(a.getValue()[0]))
                .limit(5)
                .map(e -> new ProductoVendidoDTO(
                        e.getKey(),
                        e.getValue()[0].longValue(),
                        e.getValue()[1]))
                .collect(Collectors.toList());

        // ── Filas detalle ────────────────────────────────────────
        List<FilaPagoDTO> filas = new ArrayList<>();
        for (Pago p : pagos) {
            String mesa = p.getPedido().getMesa() != null
                    ? p.getPedido().getMesa().getNombre() : "Sin mesa";
            filas.add(new FilaPagoDTO(
                    p.getId(),
                    p.getPedido().getId(),
                    mesa,
                    p.getMetodo().name(),
                    p.getMonto(),
                    p.getFecha().toString()));
        }

        return new ReporteVentasResponse(
                totalVentas, cantidadPedidos, ticketPromedio,
                totalEfectivo, totalMercadoPago,
                topProductos, filas,
                desde.format(FMT_DATE), hasta.format(FMT_DATE), empresaNombre);
    }

    // ─────────────────────────────────────────────────────────────
    // PDF con iText 7
    // ─────────────────────────────────────────────────────────────

    public byte[] generarPdf(Long empresaId, LocalDate desde, LocalDate hasta) throws IOException {

        ReporteVentasResponse data = generarReporte(empresaId, desde, hasta);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter   writer   = new PdfWriter(baos);
        PdfDocument pdfDoc   = new PdfDocument(writer);
        Document    document = new Document(pdfDoc);

        DeviceRgb azul  = new DeviceRgb(37, 99, 235);
        DeviceRgb gris  = new DeviceRgb(248, 250, 252);
        DeviceRgb texto = new DeviceRgb(30, 41, 59);

        // ── Encabezado ───────────────────────────────────────────
        document.add(new Paragraph(data.getEmpresaNombre())
                .setFontSize(20).setBold().setFontColor(azul).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("Reporte de Ventas")
                .setFontSize(14).setFontColor(texto).setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("Período: " + data.getFechaDesde() + " → " + data.getFechaHasta())
                .setFontSize(10).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(16));

        // ── Resumen ──────────────────────────────────────────────
        Table resumen = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1})).useAllAvailableWidth();
        addResumenCell(resumen, "Total ventas",    "$" + fmt(data.getTotalVentas()),    azul);
        addResumenCell(resumen, "Pedidos",         String.valueOf(data.getCantidadPedidos()), azul);
        addResumenCell(resumen, "Ticket promedio", "$" + fmt(data.getTicketPromedio()), azul);
        document.add(resumen.setMarginBottom(12));

        Table metodos = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
        addResumenCell(metodos, "Efectivo",     "$" + fmt(data.getTotalEfectivo()),     new DeviceRgb(22, 163, 74));
        addResumenCell(metodos, "Mercado Pago", "$" + fmt(data.getTotalMercadoPago()),  new DeviceRgb(0, 158, 227));
        document.add(metodos.setMarginBottom(16));

        // ── Top productos ────────────────────────────────────────
        document.add(new Paragraph("Productos más vendidos")
                .setFontSize(12).setBold().setFontColor(texto).setMarginBottom(4));
        Table tProd = new Table(UnitValue.createPercentArray(new float[]{3, 1, 2})).useAllAvailableWidth();
        addHeaderCell(tProd, "Producto",   azul);
        addHeaderCell(tProd, "Cantidad",   azul);
        addHeaderCell(tProd, "Ingresos",   azul);
        for (ProductoVendidoDTO p : data.getTopProductos()) {
            tProd.addCell(bodyCell(p.getNombre()));
            tProd.addCell(bodyCell(String.valueOf(p.getCantidadVendida())).setTextAlignment(TextAlignment.CENTER));
            tProd.addCell(bodyCell("$" + fmt(p.getIngresoGenerado())).setTextAlignment(TextAlignment.RIGHT));
        }
        document.add(tProd.setMarginBottom(16));

        // ── Detalle de pagos ─────────────────────────────────────
        document.add(new Paragraph("Detalle de pagos")
                .setFontSize(12).setBold().setFontColor(texto).setMarginBottom(4));
        Table tPagos = new Table(UnitValue.createPercentArray(new float[]{1, 1, 2, 2, 2})).useAllAvailableWidth();
        addHeaderCell(tPagos, "Pago #",  azul);
        addHeaderCell(tPagos, "Pedido",  azul);
        addHeaderCell(tPagos, "Mesa",    azul);
        addHeaderCell(tPagos, "Método",  azul);
        addHeaderCell(tPagos, "Monto",   azul);

        boolean par = false;
        for (FilaPagoDTO f : data.getPagos()) {
            DeviceRgb bg = par ? gris : new DeviceRgb(255, 255, 255);
            tPagos.addCell(bodyCell(String.valueOf(f.getPagoId())).setBackgroundColor(bg));
            tPagos.addCell(bodyCell(String.valueOf(f.getPedidoId())).setBackgroundColor(bg));
            tPagos.addCell(bodyCell(f.getMesa()).setBackgroundColor(bg));
            tPagos.addCell(bodyCell(f.getMetodo()).setBackgroundColor(bg));
            tPagos.addCell(bodyCell("$" + fmt(f.getMonto())).setTextAlignment(TextAlignment.RIGHT).setBackgroundColor(bg));
            par = !par;
        }
        document.add(tPagos);

        // ── Pie ──────────────────────────────────────────────────
        document.add(new Paragraph("Generado el " + LocalDateTime.now().format(FMT_DISPLAY))
                .setFontSize(8).setFontColor(ColorConstants.GRAY).setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(12));

        document.close();
        return baos.toByteArray();
    }

    // ─────────────────────────────────────────────────────────────
    // EXCEL con Apache POI
    // ─────────────────────────────────────────────────────────────

    public byte[] generarExcel(Long empresaId, LocalDate desde, LocalDate hasta) throws IOException {

        ReporteVentasResponse data = generarReporte(empresaId, desde, hasta);

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // ── Estilos ──────────────────────────────────────────
            XSSFCellStyle headerStyle = crearEstiloHeader(wb);
            XSSFCellStyle montoStyle  = crearEstiloMonto(wb);
            XSSFCellStyle titleStyle  = crearEstiloTitle(wb);
            XSSFCellStyle subStyle    = crearEstiloSub(wb);

            // ── Hoja 1: Resumen ──────────────────────────────────
            XSSFSheet hResumen = wb.createSheet("Resumen");
            hResumen.setColumnWidth(0, 8000);
            hResumen.setColumnWidth(1, 6000);

            int r = 0;
            Row titulo = hResumen.createRow(r++);
            org.apache.poi.ss.usermodel.Cell cTitulo = titulo.createCell(0);
            cTitulo.setCellValue(data.getEmpresaNombre() + " — Reporte de Ventas");
            cTitulo.setCellStyle(titleStyle);
            hResumen.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

            Row periodo = hResumen.createRow(r++);
            periodo.createCell(0).setCellValue("Período: " + data.getFechaDesde() + " → " + data.getFechaHasta());
            r++;

            String[][] resumenData = {
                {"Total ventas",    "$" + fmt(data.getTotalVentas())},
                {"Total pedidos",   String.valueOf(data.getCantidadPedidos())},
                {"Ticket promedio", "$" + fmt(data.getTicketPromedio())},
                {"Efectivo",        "$" + fmt(data.getTotalEfectivo())},
                {"Mercado Pago",    "$" + fmt(data.getTotalMercadoPago())},
            };
            for (String[] fila : resumenData) {
                Row row = hResumen.createRow(r++);
                row.createCell(0).setCellValue(fila[0]);
                org.apache.poi.ss.usermodel.Cell c = row.createCell(1);
                c.setCellValue(fila[1]);
            }

            // ── Hoja 2: Productos top ────────────────────────────
            XSSFSheet hProd = wb.createSheet("Top Productos");
            hProd.setColumnWidth(0, 10000);
            hProd.setColumnWidth(1, 5000);
            hProd.setColumnWidth(2, 6000);

            Row hProdRow = hProd.createRow(0);
            for (int i = 0; i < 3; i++) hProdRow.createCell(i).setCellStyle(headerStyle);
            hProdRow.getCell(0).setCellValue("Producto");
            hProdRow.getCell(1).setCellValue("Cantidad vendida");
            hProdRow.getCell(2).setCellValue("Ingresos COP");

            int rp = 1;
            for (ProductoVendidoDTO p : data.getTopProductos()) {
                Row row = hProd.createRow(rp++);
                row.createCell(0).setCellValue(p.getNombre());
                row.createCell(1).setCellValue(p.getCantidadVendida());
                org.apache.poi.ss.usermodel.Cell cMonto = row.createCell(2);
                cMonto.setCellValue(p.getIngresoGenerado().doubleValue());
                cMonto.setCellStyle(montoStyle);
            }

            // ── Hoja 3: Detalle pagos ────────────────────────────
            XSSFSheet hPagos = wb.createSheet("Detalle Pagos");
            int[] anchos = {3000, 3000, 6000, 5000, 6000};
            for (int i = 0; i < anchos.length; i++) hPagos.setColumnWidth(i, anchos[i]);

            Row hPagosRow = hPagos.createRow(0);
            String[] encabezados = {"Pago #", "Pedido #", "Mesa", "Método", "Monto COP"};
            for (int i = 0; i < encabezados.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = hPagosRow.createCell(i);
                c.setCellValue(encabezados[i]);
                c.setCellStyle(headerStyle);
            }

            int rd = 1;
            for (FilaPagoDTO f : data.getPagos()) {
                Row row = hPagos.createRow(rd++);
                row.createCell(0).setCellValue(f.getPagoId());
                row.createCell(1).setCellValue(f.getPedidoId());
                row.createCell(2).setCellValue(f.getMesa());
                row.createCell(3).setCellValue(f.getMetodo());
                org.apache.poi.ss.usermodel.Cell cM = row.createCell(4);
                cM.setCellValue(f.getMonto().doubleValue());
                cM.setCellStyle(montoStyle);
            }

            wb.write(baos);
            return baos.toByteArray();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private String fmt(BigDecimal v) {
        if (v == null) return "0";
        return String.format("%,.0f", v);
    }

    private void addResumenCell(Table t, String label, String valor, DeviceRgb color) {
        Cell cell = new Cell()
                .add(new Paragraph(label).setFontSize(9).setFontColor(ColorConstants.GRAY))
                .add(new Paragraph(valor).setFontSize(14).setBold().setFontColor(color))
                .setPadding(10).setBorder(null)
                .setBackgroundColor(new DeviceRgb(248, 250, 252));
        t.addCell(cell);
    }

    private void addHeaderCell(Table t, String texto, DeviceRgb bg) {
        t.addHeaderCell(new Cell()
                .add(new Paragraph(texto).setFontSize(10).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(bg).setPadding(6).setBorder(null));
    }

    private Cell bodyCell(String texto) {
        return new Cell().add(new Paragraph(texto).setFontSize(9)).setPadding(5).setBorder(null);
    }

    private XSSFCellStyle crearEstiloHeader(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(new byte[]{(byte)37, (byte)99, (byte)235}, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(new byte[]{(byte)255, (byte)255, (byte)255}, null));
        f.setBold(true);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        return s;
    }

    private XSSFCellStyle crearEstiloMonto(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        s.setDataFormat(df.getFormat("#,##0"));
        s.setAlignment(HorizontalAlignment.RIGHT);
        return s;
    }

    private XSSFCellStyle crearEstiloTitle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setFontHeightInPoints((short)14);
        f.setBold(true);
        s.setFont(f);
        return s;
    }

    private XSSFCellStyle crearEstiloSub(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(new byte[]{(byte)100, (byte)116, (byte)139}, null));
        s.setFont(f);
        return s;
    }
}