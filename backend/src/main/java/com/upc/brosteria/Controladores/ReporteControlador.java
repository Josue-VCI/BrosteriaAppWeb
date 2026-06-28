package com.upc.brosteria.Controladores;

import com.upc.brosteria.Entidades.DetallePedidoEntidad;
import com.upc.brosteria.Entidades.PedidoEntidad;
import com.upc.brosteria.Repositorios.DetallePedidoRepositorio;
import com.upc.brosteria.Repositorios.PedidoRepositorio;
import com.upc.brosteria.Servicios.PdfServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteControlador {

    private static final LocalDateTime INICIO_HISTORICO = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    @Autowired
    private PedidoRepositorio pedidoRepositorio;

    @Autowired
    private DetallePedidoRepositorio detallePedidoRepositorio;

    @Autowired
    private PdfServicio pdfServicio;

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> obtenerResumenVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) String tipoPedido,
            @RequestParam(required = false) String diaSemana) {

        PedidoRepositorio.ResumenReporte resumen = pedidoRepositorio.obtenerResumenReporte(
                inicio(fechaInicio), fin(fechaFin), tipo(tipoPedido), dia(diaSemana));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ventasTotales", resumen.getVentasTotales());
        response.put("totalPedidos", resumen.getTotalPedidos());
        response.put("completados", resumen.getCompletados());
        response.put("cancelados", resumen.getCancelados());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/datos-grafico")
    public ResponseEntity<Map<String, Object>> obtenerDatosGrafico(
            @RequestParam(required = false) String filtroRango,
            @RequestParam(required = false) String diaSemana,
            @RequestParam(required = false) String tipoPedido) {

        LocalDateTime fin = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime inicio = switch (filtroRango == null ? "trimestre" : filtroRango.toLowerCase(Locale.ROOT)) {
            case "semana" -> fin.minusWeeks(1);
            case "mes" -> fin.minusMonths(1);
            default -> fin.minusMonths(3);
        };
        String tipo = tipo(tipoPedido);
        int dia = dia(diaSemana);

        List<PedidoRepositorio.EtiquetaMonto> ventas = pedidoRepositorio.ventasPorFecha(inicio, fin, tipo, dia);
        Map<String, Long> pagos = pedidoRepositorio.pagosReporte(inicio, fin, tipo, dia).stream()
                .collect(Collectors.toMap(PedidoRepositorio.EtiquetaConteo::getEtiqueta,
                        PedidoRepositorio.EtiquetaConteo::getCantidad,
                        (a, b) -> a,
                        LinkedHashMap::new));

        int[] pedidosPorHora = new int[24];
        pedidoRepositorio.pedidosPorHora(inicio, fin, tipo, dia)
                .forEach(item -> pedidosPorHora[item.getHora()] = item.getCantidad().intValue());

        Map<String, Long> distritos = pedidoRepositorio.distritosReporte(inicio, fin, tipo, dia).stream()
                .collect(Collectors.toMap(PedidoRepositorio.EtiquetaConteo::getEtiqueta,
                        PedidoRepositorio.EtiquetaConteo::getCantidad,
                        (a, b) -> a,
                        LinkedHashMap::new));

        List<Map<String, Object>> topProductos = pedidoRepositorio.topProductosReporte(inicio, fin, tipo, dia).stream()
                .map(item -> Map.<String, Object>of("nombre", item.getNombre(), "cantidad", item.getCantidad()))
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("fechas", ventas.stream().map(PedidoRepositorio.EtiquetaMonto::getEtiqueta).toList());
        response.put("montos", ventas.stream().map(PedidoRepositorio.EtiquetaMonto::getMonto).toList());
        response.put("metodosPago", pagos);
        response.put("pedidosPorHora", pedidosPorHora);
        response.put("distritos", distritos);
        response.put("topProductos", topProductos);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/descargar-pdf")
    public ResponseEntity<byte[]> descargarReportePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) String diaSemana,
            @RequestParam(required = false) String tipoPedido,
            @RequestParam(required = false, defaultValue = "naranja") String formato) {

        LocalDateTime inicio = inicio(fechaInicio);
        LocalDateTime fin = fin(fechaFin);
        String tipo = tipo(tipoPedido);
        int dia = dia(diaSemana);
        List<PedidoEntidad> pedidos = pedidoRepositorio.buscarParaReporte(inicio, fin, tipo, dia);
        BigDecimal totalVentas = pedidos.stream()
                .filter(p -> "ENTREGADO".equals(p.getStatus()))
                .map(PedidoEntidad::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        byte[] pdfBytes = pdfServicio.generarReporteVentasPdf(
                pedidos, totalVentas, formato, inicio, fin, tipoPedido, diaSemana);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("reporte_ventas_filtrado.pdf").build());
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/descargar-csv")
    public ResponseEntity<byte[]> descargarReporteCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {

        List<PedidoEntidad> pedidos = pedidoRepositorio.buscarParaReporte(
                inicio(fechaInicio), fin(fechaFin), "", 0);
        List<Long> pedidoIds = pedidos.stream().map(PedidoEntidad::getId).toList();
        List<DetallePedidoEntidad> detalles = pedidoIds.isEmpty()
                ? new ArrayList<>()
                : detallePedidoRepositorio.findByPedidoEntidadIdIn(pedidoIds);
        Map<Long, List<DetallePedidoEntidad>> detallesPorPedido = detalles.stream()
                .collect(Collectors.groupingBy(d -> d.getPedidoEntidad().getId()));

        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff');
        csv.append("ID Pedido,Cliente,Telefono,Direccion,Fecha,Canal,Metodo Pago,Total,Estado,Detalle Productos\n");

        for (PedidoEntidad pedido : pedidos) {
            String productos = detallesPorPedido.getOrDefault(pedido.getId(), List.of()).stream()
                    .map(d -> d.getQuantity() + "x " + d.getProductoEntidad().getName())
                    .collect(Collectors.joining(" | "));

            csv.append(pedido.getId()).append(",")
                    .append(escapeCsvField(pedido.getCustomerName())).append(",")
                    .append(escapeCsvField(pedido.getCustomerPhone())).append(",")
                    .append(escapeCsvField(pedido.getCustomerAddress())).append(",")
                    .append(horaLima(pedido.getOrderDate()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append(",")
                    .append(pedido.getType()).append(",")
                    .append(pedido.getPaymentMethod()).append(",")
                    .append(pedido.getTotal().setScale(2)).append(",")
                    .append(pedido.getStatus()).append(",")
                    .append(escapeCsvField(productos)).append("\n");
        }

        byte[] csvBytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename("respaldo_pedidos.csv").build());
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    private LocalDateTime inicio(LocalDateTime valor) {
        return valor == null ? INICIO_HISTORICO : limaAUtc(valor);
    }

    private LocalDateTime fin(LocalDateTime valor) {
        return valor == null ? LocalDateTime.now(ZoneOffset.UTC) : limaAUtc(valor);
    }

    private LocalDateTime limaAUtc(LocalDateTime valor) {
        return valor.atZone(ZONA_LIMA).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private LocalDateTime horaLima(LocalDateTime valorUtc) {
        return valorUtc.atZone(ZoneOffset.UTC).withZoneSameInstant(ZONA_LIMA).toLocalDateTime();
    }

    private String tipo(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private int dia(String valor) {
        if (valor == null || valor.isBlank()) return 0;
        try {
            return DayOfWeek.valueOf(valor.trim().toUpperCase(Locale.ROOT)).getValue();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("El dia de semana no es valido");
        }
    }

    private String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
