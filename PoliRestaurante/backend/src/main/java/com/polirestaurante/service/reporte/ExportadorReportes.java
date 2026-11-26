package com.polirestaurante.service.reporte;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.polirestaurante.dto.FilaVentaDia;
import com.polirestaurante.dto.ItemVendido;
import com.polirestaurante.dto.ReporteVentasDTO;
import com.polirestaurante.dto.SerieValor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** CU-ADM-01 flujo alternativo 2: exportar el reporte de ventas a PDF o Excel. */
@Component
public class ExportadorReportes {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale CO = Locale.forLanguageTag("es-CO");

    public byte[] ventasPdf(ReporteVentasDTO r) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();
            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);

            doc.add(new Paragraph("PoliRestaurante - Reporte de ventas", titulo));
            doc.add(new Paragraph("Periodo: " + r.desde().format(FECHA) + " a " + r.hasta().format(FECHA)
                    + (r.categoria() != null ? "   Categoría: " + r.categoria() : ""), normal));
            doc.add(new Paragraph(" "));

            PdfPTable resumen = new PdfPTable(2);
            resumen.setWidthPercentage(60);
            resumen.setHorizontalAlignment(Element.ALIGN_LEFT);
            fila(resumen, "Monto total", moneda(r.montoTotal()), negrita, normal);
            fila(resumen, "Número de pedidos", String.valueOf(r.numeroPedidos()), negrita, normal);
            fila(resumen, "Ticket promedio", moneda(r.ticketPromedio()), negrita, normal);
            fila(resumen, "Descuentos otorgados", moneda(r.descuentos()), negrita, normal);
            fila(resumen, "Producto más vendido",
                    r.productoMasVendido() + " (" + r.unidadesProductoMasVendido() + " und)", negrita, normal);
            doc.add(resumen);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Detalle por día", negrita));
            PdfPTable dias = tabla(new String[]{"Fecha", "Pedidos", "Monto"}, negrita);
            for (FilaVentaDia d : r.ventasPorDia()) {
                dias.addCell(new Phrase(d.fecha(), normal));
                dias.addCell(new Phrase(String.valueOf(d.pedidos()), normal));
                dias.addCell(new Phrase(moneda(d.monto()), normal));
            }
            doc.add(dias);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Productos más vendidos", negrita));
            PdfPTable top = tabla(new String[]{"Producto", "Unidades", "Monto"}, negrita);
            for (ItemVendido i : r.topProductos()) {
                top.addCell(new Phrase(i.producto(), normal));
                top.addCell(new Phrase(String.valueOf(i.cantidad()), normal));
                top.addCell(new Phrase(moneda(i.monto()), normal));
            }
            doc.add(top);

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Ventas por categoría y por medio de pago", negrita));
            PdfPTable otras = tabla(new String[]{"Concepto", "Cantidad", "Monto"}, negrita);
            for (SerieValor s : r.ventasPorCategoria()) {
                otras.addCell(new Phrase("Categoría: " + s.etiqueta(), normal));
                otras.addCell(new Phrase(String.valueOf(s.cantidad()), normal));
                otras.addCell(new Phrase(moneda(s.valor()), normal));
            }
            for (SerieValor s : r.ventasPorMedioPago()) {
                otras.addCell(new Phrase("Medio: " + s.etiqueta(), normal));
                otras.addCell(new Phrase(String.valueOf(s.cantidad()), normal));
                otras.addCell(new Phrase(moneda(s.valor()), normal));
            }
            doc.add(otras);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible generar el PDF", e);
        }
    }

    public byte[] ventasExcel(ReporteVentasDTO r) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle negrita = wb.createCellStyle();
            org.apache.poi.ss.usermodel.Font f = wb.createFont();
            f.setBold(true);
            negrita.setFont(f);
            CellStyle dinero = wb.createCellStyle();
            dinero.setDataFormat(wb.createDataFormat().getFormat("$ #,##0.00"));

            Sheet hoja = wb.createSheet("Resumen");
            int n = 0;
            celdaTexto(hoja.createRow(n++), 0, "Reporte de ventas PoliRestaurante", negrita);
            Row p = hoja.createRow(n++);
            celdaTexto(p, 0, "Periodo", negrita);
            celdaTexto(p, 1, r.desde().format(FECHA) + " a " + r.hasta().format(FECHA), null);
            if (r.categoria() != null) {
                Row c = hoja.createRow(n++);
                celdaTexto(c, 0, "Categoría", negrita);
                celdaTexto(c, 1, r.categoria(), null);
            }
            n = filaNumero(hoja, n, "Monto total", r.montoTotal(), negrita, dinero);
            n = filaNumero(hoja, n, "Número de pedidos", r.numeroPedidos(), negrita, null);
            n = filaNumero(hoja, n, "Ticket promedio", r.ticketPromedio(), negrita, dinero);
            n = filaNumero(hoja, n, "Descuentos", r.descuentos(), negrita, dinero);
            Row mv = hoja.createRow(n);
            celdaTexto(mv, 0, "Producto más vendido", negrita);
            celdaTexto(mv, 1, r.productoMasVendido() + " (" + r.unidadesProductoMasVendido() + " und)", null);
            hoja.autoSizeColumn(0);
            hoja.autoSizeColumn(1);

            Sheet porDia = wb.createSheet("Por día");
            encabezado(porDia.createRow(0), negrita, "Fecha", "Pedidos", "Monto");
            int i = 1;
            for (FilaVentaDia d : r.ventasPorDia()) {
                Row row = porDia.createRow(i++);
                row.createCell(0).setCellValue(d.fecha());
                row.createCell(1).setCellValue(d.pedidos());
                Cell m = row.createCell(2);
                m.setCellValue(d.monto());
                m.setCellStyle(dinero);
            }
            for (int c = 0; c < 3; c++) porDia.autoSizeColumn(c);

            Sheet prods = wb.createSheet("Productos");
            encabezado(prods.createRow(0), negrita, "Producto", "Unidades", "Monto");
            i = 1;
            for (ItemVendido it : r.topProductos()) {
                Row row = prods.createRow(i++);
                row.createCell(0).setCellValue(it.producto());
                row.createCell(1).setCellValue(it.cantidad());
                Cell m = row.createCell(2);
                m.setCellValue(it.monto());
                m.setCellStyle(dinero);
            }
            for (int c = 0; c < 3; c++) prods.autoSizeColumn(c);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible generar el Excel", e);
        }
    }

    private String moneda(double v) {
        return NumberFormat.getCurrencyInstance(CO).format(v);
    }

    private void fila(PdfPTable t, String etiqueta, String valor, Font fe, Font fv) {
        t.addCell(new PdfPCell(new Phrase(etiqueta, fe)));
        t.addCell(new PdfPCell(new Phrase(valor, fv)));
    }

    private PdfPTable tabla(String[] columnas, Font f) {
        PdfPTable t = new PdfPTable(columnas.length);
        t.setWidthPercentage(100);
        for (String c : columnas) t.addCell(new PdfPCell(new Phrase(c, f)));
        return t;
    }

    private void celdaTexto(Row row, int col, String valor, CellStyle estilo) {
        Cell c = row.createCell(col);
        c.setCellValue(valor);
        if (estilo != null) c.setCellStyle(estilo);
    }

    private int filaNumero(Sheet hoja, int n, String etiqueta, double valor, CellStyle negrita, CellStyle formato) {
        Row row = hoja.createRow(n);
        celdaTexto(row, 0, etiqueta, negrita);
        Cell c = row.createCell(1);
        c.setCellValue(valor);
        if (formato != null) c.setCellStyle(formato);
        return n + 1;
    }

    private void encabezado(Row row, CellStyle estilo, String... titulos) {
        for (int i = 0; i < titulos.length; i++) celdaTexto(row, i, titulos[i], estilo);
    }
}
