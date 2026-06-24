package com.upc.brosteria.Controladores;

import com.upc.brosteria.Entidades.PedidoEntidad;
import com.upc.brosteria.Entidades.DetallePedidoEntidad;
import com.upc.brosteria.Repositorios.PedidoRepositorio;
import com.upc.brosteria.Repositorios.DetallePedidoRepositorio;
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
public class ReporteControlador {

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
        
        List<PedidoEntidad> pedidos = pedidoRepositorio.findAllWithCliente();

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
        if (diaSemana != null && !diaSemana.isEmpty()) {
            pedidos = pedidos.stream()
                    .filter(p -> p.getOrderDate().getDayOfWeek().name().equalsIgnoreCase(diaSemana))
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
            @RequestParam(required = false) String filtroRango,
            @RequestParam(required = false) String diaSemana) {
        
        List<PedidoEntidad> pedidos = pedidoRepositorio.findAllWithCliente().stream()
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

        // Aplicar filtro por día de la semana si existe
        if (diaSemana != null && !diaSemana.isEmpty()) {
            pedidos = pedidos.stream()
                    .filter(p -> p.getOrderDate().getDayOfWeek().name().equalsIgnoreCase(diaSemana))
                    .collect(Collectors.toList());
        }

        // 1. Agrupar ventas por día/fecha para el gráfico de línea
        Map<String, Double> ventasPorFecha = new LinkedHashMap<>();
        for (PedidoEntidad p : pedidos) {
            String fechaKey = p.getOrderDate().toLocalDate().toString();
            ventasPorFecha.put(fechaKey, ventasPorFecha.getOrDefault(fechaKey, 0.0) + p.getTotal());
        }

        // 2. Agrupar métodos de pago para dona
        Map<String, Long> pagosMap = pedidos.stream()
                .collect(Collectors.groupingBy(PedidoEntidad::getPaymentMethod, Collectors.counting()));

        // 3. Ventas/Pedidos por Hora (0-23)
        int[] pedidosPorHora = new int[24];
        for (PedidoEntidad p : pedidos) {
            int hora = p.getOrderDate().getHour();
            pedidosPorHora[hora]++;
        }

        // 4. Distritos con más pedidos
        Map<String, Long> distritosMap = pedidos.stream()
                .collect(Collectors.groupingBy(p -> extraerDistrito(p.getCustomerAddress()), Collectors.counting()));

        // 5. Top 5 productos vendidos
        List<Long> pedidoIds = pedidos.stream().map(PedidoEntidad::getId).collect(Collectors.toList());
        List<DetallePedidoEntidad> detalles = pedidoIds.isEmpty() ? new ArrayList<>() 
                : detallePedidoRepositorio.findByPedidoEntidadIdIn(pedidoIds);

        Map<String, Integer> prodCounts = new HashMap<>();
        for (DetallePedidoEntidad det : detalles) {
            if (det.getProductoEntidad() != null) {
                String pName = det.getProductoEntidad().getName();
                prodCounts.put(pName, prodCounts.getOrDefault(pName, 0) + det.getQuantity());
            }
        }
        List<Map<String, Object>> topProducts = prodCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("nombre", e.getKey());
                    item.put("cantidad", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("fechas", ventasPorFecha.keySet());
        response.put("montos", ventasPorFecha.values());
        response.put("metodosPago", pagosMap);
        response.put("pedidosPorHora", pedidosPorHora);
        response.put("distritos", distritosMap);
        response.put("topProductos", topProducts);

        return ResponseEntity.ok(response);
    }

    private String extraerDistrito(String direccion) {
        if (direccion == null || direccion.trim().isEmpty() || "Retiro en local".equalsIgnoreCase(direccion)) {
            return "Retiro en Local";
        }
        String dirLower = direccion.toLowerCase();
        if (dirLower.contains("carabayllo")) return "Carabayllo";
        if (dirLower.contains("comas")) return "Comas";
        if (dirLower.contains("surco")) return "Surco";
        if (dirLower.contains("miraflores")) return "Miraflores";
        if (dirLower.contains("san miguel")) return "San Miguel";
        if (dirLower.contains("magdalena")) return "Magdalena";
        return "Otros";
    }

    @GetMapping("/descargar-pdf")
    public ResponseEntity<byte[]> descargarReportePdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) String diaSemana,
            @RequestParam(required = false, defaultValue = "naranja") String formato) {
        
        List<PedidoEntidad> pedidos = pedidoRepositorio.findAllWithCliente();
 
        if (fechaInicio != null) {
            pedidos = pedidos.stream().filter(p -> p.getOrderDate().isAfter(fechaInicio)).collect(Collectors.toList());
        }
        if (fechaFin != null) {
            pedidos = pedidos.stream().filter(p -> p.getOrderDate().isBefore(fechaFin)).collect(Collectors.toList());
        }
        if (diaSemana != null && !diaSemana.isEmpty()) {
            pedidos = pedidos.stream().filter(p -> p.getOrderDate().getDayOfWeek().name().equalsIgnoreCase(diaSemana)).collect(Collectors.toList());
        }
 
        Double totalVentas = pedidos.stream()
                .filter(p -> p.getStatus().equals("ENTREGADO"))
                .mapToDouble(PedidoEntidad::getTotal)
                .sum();
 
        byte[] pdfBytes = pdfServicio.generarReporteVentasPdf(pedidos, totalVentas, formato);
 
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename("reporte_ventas_filtrado.pdf").build());
 
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
