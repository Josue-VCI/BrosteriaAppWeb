package com.upc.brosteria.Servicios;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.upc.brosteria.Entidades.PedidoEntidad;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfServicio {

    public byte[] generarReporteVentasPdf(List<PedidoEntidad> pedidos, Double totalVentas) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph titulo = new Paragraph("CRM LA BROSTERÍA - REPORTES DE VENTAS", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            document.add(titulo);

            // Resumen Ejecutivo
            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Total de Pedidos Procesados: " + pedidos.size(), fontSub));
            document.add(new Paragraph("Venta Bruta Total: S/. " + String.format("%.2f", totalVentas), fontSub));
            document.add(new Paragraph("Generado el: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), fontSub));
            document.add(new Paragraph("\n"));

            // Tabla de Ventas
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.5f, 1.5f, 1.5f, 1.5f, 1.5f});

            // Cabeceras de Tabla
            String[] headers = {"ID", "Cliente", "Fecha", "Tipo", "Pago", "Total"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                table.addCell(cell);
            }

            // Filas
            for (PedidoEntidad ped : pedidos) {
                table.addCell(ped.getId().toString());
                table.addCell(ped.getCustomerName());
                table.addCell(ped.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                table.addCell(ped.getType());
                table.addCell(ped.getPaymentMethod());
                table.addCell("S/. " + String.format("%.2f", ped.getTotal()));
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }
}
