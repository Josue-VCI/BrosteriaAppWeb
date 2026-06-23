package com.upc.brosteria.Controladores;

import com.upc.brosteria.Entidades.PedidoEntidad;
import com.upc.brosteria.Repositorios.PedidoRepositorio;
import com.upc.brosteria.Servicios.PdfServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/reportes")
@CrossOrigin(origins = "http://localhost:4200")
public class ReporteControlador {

    @Autowired
    private PedidoRepositorio pedidoRepositorio;

    @Autowired
    private PdfServicio pdfServicio;

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> obtenerResumenVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) String tipoPedido) {
        
        List<PedidoEntidad> pedidos = pedidoRepositorio.findAll();

        // Aplicar filtros
        if (fechaInicio != null) {
            pedidos = pedidos.stream()
                    .filter(p -> p.getOrderDate().isAfter(fechaInicio))
                    .collect(Collectors.toList());
        }
        if (fechaFin != null) {
            pedidos = pedidos.stream()
                    .filter(p -> p.getOrderDate().isBefore(fechaFin))
                    .collect(Collectors.toList());
        }
        if (tipoPedido != null && !tipoPedido.isEmpty()) {
            pedidos = pedidos.stream()
                    .filter(p -> p.getType().equalsIgnoreCase(tipoPedido))
                    .collect(Collectors.toList());
        }

        Double totalVentas = pedidos.stream()
                .filter(p -> p.getStatus().equals("ENTREGADO"))
                .mapToDouble(PedidoEntidad::getTotal)
                .sum();

        long totalPedidos = pedidos.size();
        long completados = pedidos.stream().filter(p -> p.getStatus().equals("ENTREGADO")).count();
        long cancelados = pedidos.stream().filter(p -> p.getStatus().equals("CANCELADO")).count();

        Map<String, Object> response = new HashMap<>();
        response.put("ventasTotales", totalVentas);
        response.put("totalPedidos", totalPedidos);
        response.put("completados", completados);
        response.put("cancelados", cancelados);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/datos-grafico")
    public ResponseEntity<Map<String, Object>> obtenerDatosGrafico(
            @RequestParam(required = false) String filtroRango) {
        
        List<PedidoEntidad> pedidos = pedidoRepositorio.findAll().stream()
                .filter(p -> p.getStatus().equals("ENTREGADO"))
                .sorted(Comparator.comparing(PedidoEntidad::getOrderDate))
                .collect(Collectors.toList());

        LocalDateTime limite = LocalDateTime.now();
        if ("semana".equalsIgnoreCase(filtroRango)) {
            limite = LocalDateTime.now().minusWeeks(1);
        } else if ("mes".equalsIgnoreCase(filtroRango)) {
            limite = LocalDateTime.now().minusMonths(1);
        } else {
            limite = LocalDateTime.now().minusMonths(3); // Por defecto últimos 3 meses
        }

        final LocalDateTime finalLimite = limite;
        pedidos = pedidos.stream()
                .filter(p -> p.getOrderDate().isAfter(finalLimite))
                .collect(Collectors.toList());

        // Agrupar ventas por día/fecha para el gráfico de línea
        Map<String, Double> ventasPorFecha = new LinkedHashMap<>();
        for (PedidoEntidad p : pedidos) {
            String fechaKey = p.getOrderDate().toLocalDate().toString();
            ventasPorFecha.put(fechaKey, ventasPorFecha.getOrDefault(fechaKey, 0.0) + p.getTotal());
        }

        // Agrupar métodos de pago para dona
        Map<String, Long> pagosMap = pedidos.stream()
                .collect(Collectors.groupingBy(PedidoEntidad::getPaymentMethod, Collectors.counting()));

        Map<String, Object> response = new HashMap<>();
        response.put("fechas", ventasPorFecha.keySet());
        response.put("montos", ventasPorFecha.values());
        response.put("metodosPago", pagosMap);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/descargar-pdf")
    public ResponseEntity<byte[]> descargarReportePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        
        List<PedidoEntidad> pedidos = pedidoRepositorio.findAll();

        if (fechaInicio != null) {
            pedidos = pedidos.stream().filter(p -> p.getOrderDate().isAfter(fechaInicio)).collect(Collectors.toList());
        }
        if (fechaFin != null) {
            pedidos = pedidos.stream().filter(p -> p.getOrderDate().isBefore(fechaFin)).collect(Collectors.toList());
        }

        Double totalVentas = pedidos.stream()
                .filter(p -> p.getStatus().equals("ENTREGADO"))
                .mapToDouble(PedidoEntidad::getTotal)
                .sum();

        byte[] pdfBytes = pdfServicio.generarReporteVentasPdf(pedidos, totalVentas);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename("reporte_ventas_filtrado.pdf").build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
