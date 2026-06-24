package com.upc.brosteria.Servicios;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.*;
import com.upc.brosteria.Entidades.PedidoEntidad;
import com.upc.brosteria.Entidades.DetallePedidoEntidad;
import com.upc.brosteria.Repositorios.DetallePedidoRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfServicio {

    @Autowired
    private DetallePedidoRepositorio detallePedidoRepositorio;

    public byte[] generarReporteVentasPdf(List<PedidoEntidad> pedidos, Double totalVentas) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Colores corporativos
            java.awt.Color brandOrange = new java.awt.Color(255, 107, 0);
            java.awt.Color darkGray = new java.awt.Color(33, 33, 33);
            java.awt.Color lightGray = new java.awt.Color(245, 245, 245);
            java.awt.Color textDark = new java.awt.Color(50, 50, 50);

            // Fuentes
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD, brandOrange);
            Font fontSubHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, darkGray);
            Font fontText = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, textDark);
            Font fontBoldText = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, textDark);
            Font fontTableHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, java.awt.Color.WHITE);

            // Título Principal
            Paragraph titulo = new Paragraph("LA BROSTERÍA - REPORTE DE VENTAS", fontHeader);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(5);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("Métricas de Control de Caja y Análisis Operativo", FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC, darkGray));
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(15);
            document.add(subtitulo);

            // Calcular Métricas
            long totalPedidos = pedidos.size();
            long completados = pedidos.stream().filter(p -> "ENTREGADO".equals(p.getStatus())).count();
            long cancelados = pedidos.stream().filter(p -> "CANCELADO".equals(p.getStatus())).count();
            double avgTicket = completados == 0 ? 0.0 : totalVentas / completados;

            // Tabla 1: Resumen Ejecutivo (Tarjetas)
            PdfPTable tableKpi = new PdfPTable(4);
            tableKpi.setWidthPercentage(100);
            tableKpi.setSpacingAfter(15);
            tableKpi.setWidths(new float[]{1f, 1f, 1f, 1f});

            addKpiCell(tableKpi, "Ventas Totales", "S/. " + String.format("%.2f", totalVentas), brandOrange, fontBoldText);
            addKpiCell(tableKpi, "Pedidos Totales", String.valueOf(totalPedidos), darkGray, fontBoldText);
            addKpiCell(tableKpi, "Ticket Promedio", "S/. " + String.format("%.2f", avgTicket), brandOrange, fontBoldText);
            addKpiCell(tableKpi, "Completados / Cancelados", completados + " / " + cancelados, darkGray, fontBoldText);

            document.add(tableKpi);

            // Cargar detalles para estadísticas de productos
            List<Long> pedidoIds = pedidos.stream().map(PedidoEntidad::getId).collect(Collectors.toList());
            List<DetallePedidoEntidad> detalles = pedidoIds.isEmpty() ? new ArrayList<>() 
                    : detallePedidoRepositorio.findByPedidoEntidadIdIn(pedidoIds);

            // Calcular platos más vendidos
            Map<String, Integer> prodCounts = new HashMap<>();
            for (DetallePedidoEntidad det : detalles) {
                if (det.getProductoEntidad() != null) {
                    String pName = det.getProductoEntidad().getName();
                    prodCounts.put(pName, prodCounts.getOrDefault(pName, 0) + det.getQuantity());
                }
            }
            List<Map.Entry<String, Integer>> topProducts = prodCounts.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .collect(Collectors.toList());

            // Calcular Métodos de Pago
            Map<String, Long> payments = pedidos.stream()
                    .collect(Collectors.groupingBy(PedidoEntidad::getPaymentMethod, Collectors.counting()));

            // Dos Columnas: Top Productos (Izquierda) y Métodos de Pago (Derecha)
            PdfPTable tableSections = new PdfPTable(2);
            tableSections.setWidthPercentage(100);
            tableSections.setSpacingAfter(20);
            tableSections.setWidths(new float[]{1.1f, 0.9f});

            // Sección Izquierda: Top Productos
            PdfPCell cellLeft = new PdfPCell();
            cellLeft.setBorder(Rectangle.NO_BORDER);
            cellLeft.setPaddingRight(10);
            Paragraph titleProducts = new Paragraph("TOP 5 PRODUCTOS MÁS VENDIDOS", fontSubHeader);
            titleProducts.setSpacingAfter(8);
            cellLeft.addElement(titleProducts);

            PdfPTable tableProducts = new PdfPTable(3);
            tableProducts.setWidthPercentage(100);
            tableProducts.setWidths(new float[]{0.3f, 1.4f, 0.5f});
            
            addTableHeader(tableProducts, "Pos", brandOrange, fontTableHeader);
            addTableHeader(tableProducts, "Producto / Combo", brandOrange, fontTableHeader);
            addTableHeader(tableProducts, "Cant.", brandOrange, fontTableHeader);

            int rank = 1;
            for (Map.Entry<String, Integer> entry : topProducts) {
                tableProducts.addCell(createCell(String.valueOf(rank++), fontText, Element.ALIGN_CENTER, lightGray));
                tableProducts.addCell(createCell(entry.getKey(), fontText, Element.ALIGN_LEFT, lightGray));
                tableProducts.addCell(createCell(String.valueOf(entry.getValue()), fontText, Element.ALIGN_CENTER, lightGray));
            }
            // Rellenar si hay menos de 5
            for (int i = topProducts.size(); i < 5; i++) {
                tableProducts.addCell(createCell("-", fontText, Element.ALIGN_CENTER, lightGray));
                tableProducts.addCell(createCell("-", fontText, Element.ALIGN_LEFT, lightGray));
                tableProducts.addCell(createCell("-", fontText, Element.ALIGN_CENTER, lightGray));
            }
            cellLeft.addElement(tableProducts);
            tableSections.addCell(cellLeft);

            // Sección Derecha: Métodos de Pago
            PdfPCell cellRight = new PdfPCell();
            cellRight.setBorder(Rectangle.NO_BORDER);
            cellRight.setPaddingLeft(10);
            Paragraph titlePayments = new Paragraph("VENTAS POR MÉTODO DE PAGO", fontSubHeader);
            titlePayments.setSpacingAfter(8);
            cellRight.addElement(titlePayments);

            PdfPTable tablePayments = new PdfPTable(3);
            tablePayments.setWidthPercentage(100);
            tablePayments.setWidths(new float[]{1f, 0.6f, 0.6f});

            addTableHeader(tablePayments, "Método", darkGray, fontTableHeader);
            addTableHeader(tablePayments, "Pedidos", darkGray, fontTableHeader);
            addTableHeader(tablePayments, "Porcent.", darkGray, fontTableHeader);

            for (Map.Entry<String, Long> entry : payments.entrySet()) {
                double pct = totalPedidos == 0 ? 0.0 : (entry.getValue() * 100.0) / totalPedidos;
                tablePayments.addCell(createCell(entry.getKey(), fontText, Element.ALIGN_LEFT, lightGray));
                tablePayments.addCell(createCell(String.valueOf(entry.getValue()), fontText, Element.ALIGN_CENTER, lightGray));
                tablePayments.addCell(createCell(String.format("%.1f%%", pct), fontText, Element.ALIGN_CENTER, lightGray));
            }
            // Rellenar si está vacío
            if (payments.isEmpty()) {
                tablePayments.addCell(createCell("Sin datos", fontText, Element.ALIGN_LEFT, lightGray));
                tablePayments.addCell(createCell("-", fontText, Element.ALIGN_CENTER, lightGray));
                tablePayments.addCell(createCell("-", fontText, Element.ALIGN_CENTER, lightGray));
            }
            cellRight.addElement(tablePayments);
            tableSections.addCell(cellRight);

            document.add(tableSections);

            // Listado de Transacciones
            Paragraph titleOrders = new Paragraph("HISTORIAL COMPLETO DE PEDIDOS FILTRADOS", fontSubHeader);
            titleOrders.setSpacingAfter(10);
            document.add(titleOrders);

            PdfPTable tableOrders = new PdfPTable(6);
            tableOrders.setWidthPercentage(100);
            tableOrders.setWidths(new float[]{0.6f, 2.0f, 1.2f, 1.0f, 1.0f, 1.2f});

            addTableHeader(tableOrders, "ID", brandOrange, fontTableHeader);
            addTableHeader(tableOrders, "Cliente / Nombre", brandOrange, fontTableHeader);
            addTableHeader(tableOrders, "Fecha", brandOrange, fontTableHeader);
            addTableHeader(tableOrders, "Canal", brandOrange, fontTableHeader);
            addTableHeader(tableOrders, "Pago", brandOrange, fontTableHeader);
            addTableHeader(tableOrders, "Total", brandOrange, fontTableHeader);

            for (PedidoEntidad ped : pedidos) {
                tableOrders.addCell(createCell("#" + ped.getId(), fontText, Element.ALIGN_CENTER, null));
                tableOrders.addCell(createCell(ped.getCustomerName(), fontText, Element.ALIGN_LEFT, null));
                tableOrders.addCell(createCell(ped.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), fontText, Element.ALIGN_CENTER, null));
                tableOrders.addCell(createCell(ped.getType(), fontText, Element.ALIGN_CENTER, null));
                tableOrders.addCell(createCell(ped.getPaymentMethod(), fontText, Element.ALIGN_CENTER, null));
                tableOrders.addCell(createCell("S/. " + String.format("%.2f", ped.getTotal()), fontText, Element.ALIGN_RIGHT, null));
            }

            document.add(tableOrders);

            // Pie de página de generación
            Paragraph footer = new Paragraph("\nReporte emitido automáticamente por La Brostería CRM el " + 
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), 
                    FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, java.awt.Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private void addKpiCell(PdfPTable table, String label, String value, java.awt.Color colorHeader, Font fontText) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new java.awt.Color(245, 245, 245));
        cell.setBorderWidth(1);
        cell.setBorderColor(new java.awt.Color(220, 220, 220));
        cell.setPadding(8);

        Paragraph pLabel = new Paragraph(label.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Font.NORMAL, java.awt.Color.GRAY));
        pLabel.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pLabel);

        Paragraph pValue = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, colorHeader));
        pValue.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(pValue);

        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String title, java.awt.Color bgColor, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(title, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorderColor(new java.awt.Color(220, 220, 220));
        table.addCell(cell);
    }

    private PdfPCell createCell(String text, Font font, int alignment, java.awt.Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        if (bgColor != null) {
            cell.setBackgroundColor(bgColor);
        }
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        cell.setBorderColor(new java.awt.Color(235, 235, 235));
        return cell;
    }
}
