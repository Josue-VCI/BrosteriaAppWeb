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
import com.upc.brosteria.Repositorios.PedidoRepositorio;
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

    @Autowired
    private PedidoRepositorio pedidoRepositorio;

    public byte[] generarReporteVentasPdf(
            List<PedidoEntidad> pedidos, 
            Double totalVentas, 
            String formato,
            java.time.LocalDateTime fechaInicio,
            java.time.LocalDateTime fechaFin,
            String tipoPedido,
            String diaSemana) {
        
        boolean isCompact = "compacto".equalsIgnoreCase(formato);
        Document document = new Document(PageSize.A4, 
                isCompact ? 20 : 36, 
                isCompact ? 20 : 36, 
                isCompact ? 20 : 36, 
                isCompact ? 20 : 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Colores corporativos según formato
            java.awt.Color brandOrange = new java.awt.Color(255, 107, 0);
            java.awt.Color darkGray = new java.awt.Color(33, 33, 33);
            java.awt.Color lightGray = new java.awt.Color(245, 245, 245);
            java.awt.Color textDark = new java.awt.Color(50, 50, 50);

            if ("monocromo".equalsIgnoreCase(formato)) {
                brandOrange = new java.awt.Color(50, 50, 50);
                darkGray = new java.awt.Color(90, 90, 90);
                lightGray = new java.awt.Color(240, 240, 240);
            } else if (isCompact) {
                brandOrange = new java.awt.Color(220, 80, 0);
                darkGray = new java.awt.Color(45, 45, 45);
            }

            // Fuentes
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, isCompact ? 14 : 18, Font.BOLD, brandOrange);
            Font fontSubHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, isCompact ? 10 : 12, Font.BOLD, darkGray);
            Font fontText = FontFactory.getFont(FontFactory.HELVETICA, isCompact ? 8 : 9, Font.NORMAL, textDark);
            Font fontBoldText = FontFactory.getFont(FontFactory.HELVETICA_BOLD, isCompact ? 8 : 9, Font.BOLD, textDark);
            Font fontTableHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, isCompact ? 8 : 9, Font.BOLD, java.awt.Color.WHITE);

            // Título Principal
            Paragraph titulo = new Paragraph("LA BROSTERÍA - REPORTE ANALÍTICO DE VENTAS", fontHeader);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(isCompact ? 2 : 5);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("Informe de Métricas de Negocio, Canales y Rendimiento Operativo", FontFactory.getFont(FontFactory.HELVETICA, isCompact ? 8 : 10, Font.ITALIC, darkGray));
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(isCompact ? 8 : 15);
            document.add(subtitulo);

            // Calcular Métricas
            long totalPedidos = pedidos.size();
            long completados = pedidos.stream().filter(p -> "ENTREGADO".equals(p.getStatus())).count();
            long cancelados = pedidos.stream().filter(p -> "CANCELADO".equals(p.getStatus())).count();
            double avgTicket = completados == 0 ? 0.0 : totalVentas / completados;

            // Calcular Comparativas
            boolean hasComparison = false;
            double totalVentasPrev = 0.0;
            long totalPedidosPrev = 0;
            long completadosPrev = 0;
            double avgTicketPrev = 0.0;

            if (fechaInicio != null) {
                java.time.LocalDateTime endRange = (fechaFin != null) ? fechaFin : java.time.LocalDateTime.now();
                long days = java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, endRange);
                if (days <= 0) days = 1;
                final java.time.LocalDateTime startPrev = fechaInicio.minusDays(days);
                final java.time.LocalDateTime endPrev = fechaInicio;
                hasComparison = true;

                List<PedidoEntidad> todosPedidos = pedidoRepositorio.findAll();
                List<PedidoEntidad> pedidosPrev = todosPedidos.stream()
                        .filter(p -> p.getOrderDate().isAfter(startPrev.minusNanos(1)) && p.getOrderDate().isBefore(endPrev.plusNanos(1)))
                        .collect(Collectors.toList());
                
                if (tipoPedido != null && !tipoPedido.isEmpty()) {
                    pedidosPrev = pedidosPrev.stream().filter(p -> p.getType().equalsIgnoreCase(tipoPedido)).collect(Collectors.toList());
                }
                if (diaSemana != null && !diaSemana.isEmpty()) {
                    pedidosPrev = pedidosPrev.stream().filter(p -> p.getOrderDate().getDayOfWeek().name().equalsIgnoreCase(diaSemana)).collect(Collectors.toList());
                }

                totalVentasPrev = pedidosPrev.stream()
                        .filter(p -> "ENTREGADO".equals(p.getStatus()))
                        .mapToDouble(PedidoEntidad::getTotal)
                        .sum();
                totalPedidosPrev = pedidosPrev.size();
                completadosPrev = pedidosPrev.stream().filter(p -> "ENTREGADO".equals(p.getStatus())).count();
                avgTicketPrev = completadosPrev == 0 ? 0.0 : totalVentasPrev / completadosPrev;
            }

            // Formatear deltas comparativos
            String salesDelta = hasComparison ? formatDelta(totalVentas, totalVentasPrev) : "N/D";
            String ordersDelta = hasComparison ? formatDelta(totalPedidos, totalPedidosPrev) : "N/D";
            String ticketDelta = hasComparison ? formatDelta(avgTicket, avgTicketPrev) : "N/D";

            // Tabla 1: Resumen Ejecutivo (KPIs con Comparativas)
            PdfPTable tableKpi = new PdfPTable(4);
            tableKpi.setWidthPercentage(100);
            tableKpi.setSpacingAfter(isCompact ? 8 : 15);
            tableKpi.setWidths(new float[]{1f, 1f, 1f, 1f});

            addKpiCell(tableKpi, "Ventas Totales", "S/. " + String.format(Locale.US, "%.2f", totalVentas), brandOrange, fontBoldText, salesDelta, hasComparison);
            addKpiCell(tableKpi, "Pedidos Totales", String.valueOf(totalPedidos), darkGray, fontBoldText, ordersDelta, hasComparison);
            addKpiCell(tableKpi, "Ticket Promedio", "S/. " + String.format(Locale.US, "%.2f", avgTicket), brandOrange, fontBoldText, ticketDelta, hasComparison);
            addKpiCell(tableKpi, "Completados / Cancel.", completados + " / " + cancelados, darkGray, fontBoldText, "", false);

            document.add(tableKpi);

            // Cargar detalles para estadísticas de productos
            List<Long> pedidoIds = pedidos.stream().map(PedidoEntidad::getId).collect(Collectors.toList());
            List<DetallePedidoEntidad> detalles = pedidoIds.isEmpty() ? new ArrayList<>() 
                    : detallePedidoRepositorio.findByPedidoEntidadIdIn(pedidoIds);

            // Calcular productos más vendidos
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

            // Dos Columnas: Top Productos (Izquierda) y Métodos de Pago (Derecha)
            PdfPTable tableTopRow = new PdfPTable(2);
            tableTopRow.setWidthPercentage(100);
            tableTopRow.setSpacingAfter(isCompact ? 10 : 20);
            tableTopRow.setWidths(new float[]{1.1f, 0.9f});

            // A. Top Productos
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
            for (int i = topProducts.size(); i < 5; i++) {
                tableProducts.addCell(createCell("-", fontText, Element.ALIGN_CENTER, lightGray));
                tableProducts.addCell(createCell("-", fontText, Element.ALIGN_LEFT, lightGray));
                tableProducts.addCell(createCell("-", fontText, Element.ALIGN_CENTER, lightGray));
            }
            cellLeft.addElement(tableProducts);
            tableTopRow.addCell(cellLeft);

            // B. Métodos de Pago
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

            Map<String, Long> payments = pedidos.stream()
                    .collect(Collectors.groupingBy(PedidoEntidad::getPaymentMethod, Collectors.counting()));

            for (Map.Entry<String, Long> entry : payments.entrySet()) {
                double pct = totalPedidos == 0 ? 0.0 : (entry.getValue() * 100.0) / totalPedidos;
                tablePayments.addCell(createCell(entry.getKey(), fontText, Element.ALIGN_LEFT, lightGray));
                tablePayments.addCell(createCell(String.valueOf(entry.getValue()), fontText, Element.ALIGN_CENTER, lightGray));
                tablePayments.addCell(createCell(String.format(Locale.US, "%.1f%%", pct), fontText, Element.ALIGN_CENTER, lightGray));
            }
            if (payments.isEmpty()) {
                tablePayments.addCell(createCell("Sin datos", fontText, Element.ALIGN_LEFT, lightGray));
                tablePayments.addCell(createCell("-", fontText, Element.ALIGN_CENTER, lightGray));
                tablePayments.addCell(createCell("-", fontText, Element.ALIGN_CENTER, lightGray));
            }
            cellRight.addElement(tablePayments);
            tableTopRow.addCell(cellRight);

            document.add(tableTopRow);

            // Segunda Fila: Análisis por Día de la Semana (Izquierda) y Bloques Horarios (Derecha)
            PdfPTable tableBottomRow = new PdfPTable(2);
            tableBottomRow.setWidthPercentage(100);
            tableBottomRow.setSpacingAfter(isCompact ? 10 : 20);
            tableBottomRow.setWidths(new float[]{1f, 1f});

            // C. Análisis por Día de la Semana
            PdfPCell cellBottomLeft = new PdfPCell();
            cellBottomLeft.setBorder(Rectangle.NO_BORDER);
            cellBottomLeft.setPaddingRight(10);
            Paragraph titleDays = new Paragraph("VENTAS POR DÍA DE LA SEMANA", fontSubHeader);
            titleDays.setSpacingAfter(8);
            cellBottomLeft.addElement(titleDays);

            PdfPTable tableDays = new PdfPTable(3);
            tableDays.setWidthPercentage(100);
            tableDays.setWidths(new float[]{0.8f, 0.7f, 1.5f});
            
            addTableHeader(tableDays, "Día", brandOrange, fontTableHeader);
            addTableHeader(tableDays, "Monto", brandOrange, fontTableHeader);
            addTableHeader(tableDays, "Distribución", brandOrange, fontTableHeader);

            Map<java.time.DayOfWeek, Double> ventasDia = pedidos.stream()
                    .filter(p -> "ENTREGADO".equals(p.getStatus()))
                    .collect(Collectors.groupingBy(p -> p.getOrderDate().getDayOfWeek(), Collectors.summingDouble(PedidoEntidad::getTotal)));

            List<java.time.DayOfWeek> diasSemana = Arrays.asList(
                    java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY,
                    java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY,
                    java.time.DayOfWeek.SUNDAY
            );

            for (java.time.DayOfWeek day : diasSemana) {
                double v = ventasDia.getOrDefault(day, 0.0);
                double pct = totalVentas == 0 ? 0.0 : (v * 100.0) / totalVentas;
                
                tableDays.addCell(createCell(getDayNameSpanish(day), fontText, Element.ALIGN_LEFT, lightGray));
                tableDays.addCell(createCell("S/. " + String.format(Locale.US, "%.1f", v), fontText, Element.ALIGN_RIGHT, lightGray));
                tableDays.addCell(createProgressBarCell(pct, brandOrange, new java.awt.Color(230, 230, 230)));
            }
            cellBottomLeft.addElement(tableDays);
            tableBottomRow.addCell(cellBottomLeft);

            // D. Análisis de Bloques Horarios
            PdfPCell cellBottomRight = new PdfPCell();
            cellBottomRight.setBorder(Rectangle.NO_BORDER);
            cellBottomRight.setPaddingLeft(10);
            Paragraph titleHours = new Paragraph("TENDENCIA HORARIA (PEDIDOS PICO)", fontSubHeader);
            titleHours.setSpacingAfter(8);
            cellBottomRight.addElement(titleHours);

            PdfPTable tableHours = new PdfPTable(3);
            tableHours.setWidthPercentage(100);
            tableHours.setWidths(new float[]{1.0f, 0.5f, 1.5f});
            
            addTableHeader(tableHours, "Bloque Horario", darkGray, fontTableHeader);
            addTableHeader(tableHours, "Cant.", darkGray, fontTableHeader);
            addTableHeader(tableHours, "Frecuencia", darkGray, fontTableHeader);

            int aCount = 0; int tCount = 0; int cCount = 0; int ciCount = 0; int oCount = 0;
            for (PedidoEntidad p : pedidos) {
                int hour = p.getOrderDate().getHour();
                if (hour >= 12 && hour < 15) aCount++;
                else if (hour >= 15 && hour < 18) tCount++;
                else if (hour >= 18 && hour < 21) cCount++;
                else if (hour >= 21 && hour < 24) ciCount++;
                else oCount++;
            }

            long totalHoras = totalPedidos;
            tableHours.addCell(createCell("Almuerzo (12-15h)", fontText, Element.ALIGN_LEFT, lightGray));
            tableHours.addCell(createCell(String.valueOf(aCount), fontText, Element.ALIGN_CENTER, lightGray));
            tableHours.addCell(createProgressBarCell(totalHoras == 0 ? 0.0 : (aCount * 100.0) / totalHoras, darkGray, new java.awt.Color(230, 230, 230)));

            tableHours.addCell(createCell("Tarde (15-18h)", fontText, Element.ALIGN_LEFT, lightGray));
            tableHours.addCell(createCell(String.valueOf(tCount), fontText, Element.ALIGN_CENTER, lightGray));
            tableHours.addCell(createProgressBarCell(totalHoras == 0 ? 0.0 : (tCount * 100.0) / totalHoras, darkGray, new java.awt.Color(230, 230, 230)));

            tableHours.addCell(createCell("Cena Pico (18-21h)", fontText, Element.ALIGN_LEFT, lightGray));
            tableHours.addCell(createCell(String.valueOf(cCount), fontText, Element.ALIGN_CENTER, lightGray));
            tableHours.addCell(createProgressBarCell(totalHoras == 0 ? 0.0 : (cCount * 100.0) / totalHoras, brandOrange, new java.awt.Color(230, 230, 230)));

            tableHours.addCell(createCell("Cierre (21-24h)", fontText, Element.ALIGN_LEFT, lightGray));
            tableHours.addCell(createCell(String.valueOf(ciCount), fontText, Element.ALIGN_CENTER, lightGray));
            tableHours.addCell(createProgressBarCell(totalHoras == 0 ? 0.0 : (ciCount * 100.0) / totalHoras, darkGray, new java.awt.Color(230, 230, 230)));

            tableHours.addCell(createCell("Otros Horarios", fontText, Element.ALIGN_LEFT, lightGray));
            tableHours.addCell(createCell(String.valueOf(oCount), fontText, Element.ALIGN_CENTER, lightGray));
            tableHours.addCell(createProgressBarCell(totalHoras == 0 ? 0.0 : (oCount * 100.0) / totalHoras, darkGray, new java.awt.Color(230, 230, 230)));

            cellBottomRight.addElement(tableHours);
            tableBottomRow.addCell(cellBottomRight);

            document.add(tableBottomRow);

            // Tercera Fila: Top Clientes y Canales de Venta
            PdfPTable tableThreeRow = new PdfPTable(2);
            tableThreeRow.setWidthPercentage(100);
            tableThreeRow.setSpacingAfter(isCompact ? 10 : 20);
            tableThreeRow.setWidths(new float[]{1.1f, 0.9f});

            // E. Top 5 Clientes Estrella
            PdfPCell cellThreeLeft = new PdfPCell();
            cellThreeLeft.setBorder(Rectangle.NO_BORDER);
            cellThreeLeft.setPaddingRight(10);
            Paragraph titleClientes = new Paragraph("TOP 5 CLIENTES ESTRELLA (DE MAYOR CONSUMO)", fontSubHeader);
            titleClientes.setSpacingAfter(8);
            cellThreeLeft.addElement(titleClientes);

            PdfPTable tableClientes = new PdfPTable(3);
            tableClientes.setWidthPercentage(100);
            tableClientes.setWidths(new float[]{0.3f, 1.4f, 0.5f});
            
            addTableHeader(tableClientes, "Rank", brandOrange, fontTableHeader);
            addTableHeader(tableClientes, "Cliente / Nombre", brandOrange, fontTableHeader);
            addTableHeader(tableClientes, "Consumo", brandOrange, fontTableHeader);

            Map<String, Double> clientesVentas = pedidos.stream()
                    .filter(p -> "ENTREGADO".equals(p.getStatus()))
                    .collect(Collectors.groupingBy(p -> p.getCustomerName() != null ? p.getCustomerName() : "Desconocido",
                            Collectors.summingDouble(PedidoEntidad::getTotal)));

            List<Map.Entry<String, Double>> topClientes = clientesVentas.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .collect(Collectors.toList());

            int cRank = 1;
            for (Map.Entry<String, Double> entry : topClientes) {
                tableClientes.addCell(createCell(String.valueOf(cRank++), fontText, Element.ALIGN_CENTER, lightGray));
                tableClientes.addCell(createCell(entry.getKey(), fontText, Element.ALIGN_LEFT, lightGray));
                tableClientes.addCell(createCell("S/. " + String.format(Locale.US, "%.2f", entry.getValue()), fontText, Element.ALIGN_RIGHT, lightGray));
            }
            for (int i = topClientes.size(); i < 5; i++) {
                tableClientes.addCell(createCell("-", fontText, Element.ALIGN_CENTER, lightGray));
                tableClientes.addCell(createCell("-", fontText, Element.ALIGN_LEFT, lightGray));
                tableClientes.addCell(createCell("-", fontText, Element.ALIGN_CENTER, lightGray));
            }
            cellThreeLeft.addElement(tableClientes);
            tableThreeRow.addCell(cellThreeLeft);

            // F. Canales de Pedidos
            PdfPCell cellThreeRight = new PdfPCell();
            cellThreeRight.setBorder(Rectangle.NO_BORDER);
            cellThreeRight.setPaddingLeft(10);
            Paragraph titleChannels = new Paragraph("RENDIMIENTO POR CANAL DE VENTA", fontSubHeader);
            titleChannels.setSpacingAfter(8);
            cellThreeRight.addElement(titleChannels);

            PdfPTable tableChannels = new PdfPTable(3);
            tableChannels.setWidthPercentage(100);
            tableChannels.setWidths(new float[]{1.0f, 0.6f, 0.6f});
            
            addTableHeader(tableChannels, "Canal", darkGray, fontTableHeader);
            addTableHeader(tableChannels, "Pedidos", darkGray, fontTableHeader);
            addTableHeader(tableChannels, "Ingresos", darkGray, fontTableHeader);

            double vDelivery = 0.0; int cDelivery = 0;
            double vPickup = 0.0; int cPickup = 0;
            for (PedidoEntidad p : pedidos) {
                if (!"ENTREGADO".equals(p.getStatus())) continue;
                if ("DELIVERY".equalsIgnoreCase(p.getType())) {
                    vDelivery += p.getTotal();
                    cDelivery++;
                } else {
                    vPickup += p.getTotal();
                    cPickup++;
                }
            }

            tableChannels.addCell(createCell("DELIVERY", fontText, Element.ALIGN_LEFT, lightGray));
            tableChannels.addCell(createCell(String.valueOf(cDelivery), fontText, Element.ALIGN_CENTER, lightGray));
            tableChannels.addCell(createCell("S/. " + String.format(Locale.US, "%.2f", vDelivery), fontText, Element.ALIGN_RIGHT, lightGray));

            tableChannels.addCell(createCell("RETIRO EN LOCAL", fontText, Element.ALIGN_LEFT, lightGray));
            tableChannels.addCell(createCell(String.valueOf(cPickup), fontText, Element.ALIGN_CENTER, lightGray));
            tableChannels.addCell(createCell("S/. " + String.format(Locale.US, "%.2f", vPickup), fontText, Element.ALIGN_RIGHT, lightGray));

            cellThreeRight.addElement(tableChannels);
            tableThreeRow.addCell(cellThreeRight);

            document.add(tableThreeRow);

            // Pie de página de generación
            Paragraph footer = new Paragraph("\nReporte Analítico emitido por La Brostería CRM el " + 
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

    private void addKpiCell(PdfPTable table, String label, String value, java.awt.Color colorHeader, Font fontText, String delta, boolean hasComparison) {
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

        if (hasComparison) {
            java.awt.Color deltaColor = delta.startsWith("+") ? new java.awt.Color(0, 150, 0) : new java.awt.Color(200, 0, 0);
            if (delta.equals("+0.0%") || delta.equals("0.0%") || delta.equals("-0.0%")) {
                deltaColor = java.awt.Color.GRAY;
            }
            Paragraph pDelta = new Paragraph(delta + " vs ant.", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, Font.NORMAL, deltaColor));
            pDelta.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(pDelta);
        }

        table.addCell(cell);
    }

    private String formatDelta(double current, double previous) {
        if (previous <= 0.0) {
            return current > 0.0 ? "+100%" : "0.0%";
        }
        double pct = ((current - previous) / previous) * 100.0;
        return String.format(Locale.US, "%+.1f%%", pct);
    }

    private String getDayNameSpanish(java.time.DayOfWeek day) {
        switch (day) {
            case MONDAY: return "Lunes";
            case TUESDAY: return "Martes";
            case WEDNESDAY: return "Miércoles";
            case THURSDAY: return "Jueves";
            case FRIDAY: return "Viernes";
            case SATURDAY: return "Sábado";
            case SUNDAY: return "Domingo";
            default: return "";
        }
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

    private PdfPCell createProgressBarCell(double percentage, java.awt.Color barColor, java.awt.Color bgColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);

        if (percentage <= 0.0) percentage = 1.0;
        if (percentage > 100.0) percentage = 100.0;

        PdfPTable barTable = new PdfPTable(2);
        barTable.setWidthPercentage(100);
        try {
            barTable.setWidths(new float[]{(float) percentage, (float) (100.0 - percentage)});
        } catch (Exception e) {
            // Ignorar
        }

        PdfPCell filledCell = new PdfPCell();
        filledCell.setBackgroundColor(barColor);
        filledCell.setBorder(Rectangle.NO_BORDER);
        filledCell.setFixedHeight(6);
        barTable.addCell(filledCell);

        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(bgColor);
        emptyCell.setBorder(Rectangle.NO_BORDER);
        emptyCell.setFixedHeight(6);
        barTable.addCell(emptyCell);

        cell.addElement(barTable);
        return cell;
    }
}
