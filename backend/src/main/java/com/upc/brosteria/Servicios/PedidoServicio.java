package com.upc.brosteria.Servicios;

import com.upc.brosteria.DTOs.DetallePedidoDTO;
import com.upc.brosteria.DTOs.PedidoDTO;
import com.upc.brosteria.Entidades.ClienteEntidad;
import com.upc.brosteria.Entidades.DetallePedidoEntidad;
import com.upc.brosteria.Entidades.PedidoEntidad;
import com.upc.brosteria.Entidades.ProductoEntidad;
import com.upc.brosteria.Repositorios.ClienteRepositorio;
import com.upc.brosteria.Repositorios.DetallePedidoRepositorio;
import com.upc.brosteria.Repositorios.PedidoRepositorio;
import com.upc.brosteria.Repositorios.ProductoRepositorio;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.web.util.HtmlUtils;

@Service
public class PedidoServicio {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private static final java.util.Set<String> ESTADOS_VALIDOS = java.util.Set.of(
            "PENDIENTE", "PREPARANDO", "ENVIADO", "ENTREGADO", "CANCELADO");
    private static final java.util.Set<String> ESTADOS_PAGO_VALIDOS = java.util.Set.of("PENDIENTE", "PAGADO");
    private static final BigDecimal PRECIO_EXTRA_CHAUFA = new BigDecimal("4.00");
    private static final java.util.Set<Long> PRODUCTOS_CON_CHAUFA = java.util.Set.of(
            1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 14L, 16L, 20L, 28L, 29L, 30L);

    @Autowired
    private PedidoRepositorio pedidoRepositorio;

    @Autowired
    private DetallePedidoRepositorio detallePedidoRepositorio;

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Autowired
    private ClienteRepositorio clienteRepositorio;

    @Autowired
    private InsumoServicio insumoServicio;

    @Autowired
    private EmailServicio emailServicio;

    @Autowired
    private ModelMapper modelMapper;

    public List<PedidoDTO> listarPorEstado(String status, int limite) {
        int limiteSeguro = Math.max(1, Math.min(limite, 500));
        List<PedidoEntidad> pedidos = pedidoRepositorio.findByStatusWithCliente(
                status, org.springframework.data.domain.PageRequest.of(0, limiteSeguro));
        return mappedPedidos(pedidos);
    }

    public List<PedidoDTO> listarTodos(int limite) {
        int limiteSeguro = Math.max(1, Math.min(limite, 500));
        List<PedidoEntidad> pedidos = pedidoRepositorio.findAllWithCliente(
                org.springframework.data.domain.PageRequest.of(0, limiteSeguro));
        return mappedPedidos(pedidos);
    }

    public List<PedidoDTO> listarActivos() {
        LocalDateTime todayStart = inicioDiaLimaUtc();
        List<PedidoEntidad> pedidos = pedidoRepositorio.findActiveWithCliente(todayStart);
        return mappedPedidos(pedidos);
    }

    public List<PedidoDTO> listarEntregadosHoy() {
        LocalDateTime inicio = inicioDiaLimaUtc();
        return mappedPedidos(pedidoRepositorio.findByStatusAndRangeWithCliente(
                "ENTREGADO", inicio, inicio.plusDays(1)));
    }

    public List<PedidoDTO> listarRecientes(int limite) {
        int limiteSeguro = Math.max(1, Math.min(limite, 100));
        List<PedidoEntidad> pedidos = pedidoRepositorio.findRecentWithCliente(org.springframework.data.domain.PageRequest.of(0, limiteSeguro));
        return mappedPedidos(pedidos);
    }

    private List<PedidoDTO> mappedPedidos(List<PedidoEntidad> pedidos) {
        if (pedidos.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> pedidoIds = pedidos.stream().map(PedidoEntidad::getId).collect(Collectors.toList());
        List<DetallePedidoEntidad> allDetalles = detallePedidoRepositorio.findByPedidoEntidadIdIn(pedidoIds);
        Map<Long, List<DetallePedidoEntidad>> detallesByPedidoId = allDetalles.stream()
                .collect(Collectors.groupingBy(d -> d.getPedidoEntidad().getId()));
        return pedidos.stream()
                .map(p -> convertirADTO(p, detallesByPedidoId.getOrDefault(p.getId(), new ArrayList<>())))
                .collect(Collectors.toList());
    }

    @Transactional
    public PedidoDTO crear(PedidoDTO pedidoDTO) {
        if (pedidoDTO.getDetalles() == null || pedidoDTO.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe incluir al menos un producto");
        }

        String requestId = normalizarRequestId(pedidoDTO.getRequestId());
        if (requestId != null) {
            java.util.Optional<PedidoEntidad> existente = pedidoRepositorio.findByRequestId(requestId);
            if (existente.isPresent()) {
                return convertirADTO(existente.get());
            }
        }

        PedidoEntidad pedido = new PedidoEntidad();
        pedido.setRequestId(requestId);
        String name = (pedidoDTO.getCustomerName() == null || pedidoDTO.getCustomerName().trim().isEmpty())
                ? "Anonimo"
                : pedidoDTO.getCustomerName().trim();
        String phone = (pedidoDTO.getCustomerPhone() == null || pedidoDTO.getCustomerPhone().trim().isEmpty())
                ? "000000000"
                : pedidoDTO.getCustomerPhone().trim();
        String address = (pedidoDTO.getCustomerAddress() == null || pedidoDTO.getCustomerAddress().trim().isEmpty())
                ? "Sin Direccion"
                : pedidoDTO.getCustomerAddress().trim();

        pedido.setCustomerName(name);
        pedido.setCustomerPhone(phone);
        pedido.setCustomerAddress(address);
        pedido.setDeliveryCost(pedidoDTO.getDeliveryCost() != null ? pedidoDTO.getDeliveryCost() : BigDecimal.ZERO);
        pedido.setType(pedidoDTO.getType() != null ? pedidoDTO.getType() : "DELIVERY");
        pedido.setPaymentMethod(pedidoDTO.getPaymentMethod() != null ? pedidoDTO.getPaymentMethod() : "EFECTIVO");
        pedido.setPaymentStatus(normalizarEstadoPago(pedidoDTO.getPaymentStatus()));
        pedido.setStatus("PREPARANDO");
        pedido.setOrderDate(LocalDateTime.now(ZoneOffset.UTC));
        if ("PAGADO".equals(pedido.getPaymentStatus())) {
            pedido.setPaidAt(pedido.getOrderDate());
        }

        String requestedEmail = normalizarEmail(pedidoDTO.getCustomerEmail());

        if (pedidoDTO.getClienteId() != null) {
            ClienteEntidad cliente = clienteRepositorio.findById(pedidoDTO.getClienteId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            completarDatosDesdeCliente(pedido, pedidoDTO, cliente, requestedEmail);
            pedido.setClienteEntidad(cliente);
        } else {
            java.util.Optional<ClienteEntidad> optCliente = clienteRepositorio.findFirstByPhoneOrderByIdAsc(phone);
            if (optCliente.isPresent()) {
                ClienteEntidad cliente = optCliente.get();
                completarDatosDesdeCliente(pedido, pedidoDTO, cliente, requestedEmail);
                pedido.setClienteEntidad(cliente);
            } else {
                ClienteEntidad nuevoCliente = new ClienteEntidad();
                nuevoCliente.setName(name);
                nuevoCliente.setPhone(phone);
                nuevoCliente.setAddress(address);
                nuevoCliente.setEmail(emailDisponibleParaCliente(requestedEmail, null) ? requestedEmail : null);
                nuevoCliente.setTotalOrders(0);
                nuevoCliente.setTotalSpent(BigDecimal.ZERO);
                nuevoCliente.setPoints(0);
                nuevoCliente = clienteRepositorio.save(nuevoCliente);
                pedido.setClienteEntidad(nuevoCliente);
            }
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<DetallePedidoEntidad> detalles = new ArrayList<>();

        for (DetallePedidoDTO detDTO : pedidoDTO.getDetalles()) {
            ProductoEntidad prod = productoRepositorio.findById(detDTO.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID " + detDTO.getProductoId()));

            DetallePedidoEntidad det = new DetallePedidoEntidad();
            det.setPedidoEntidad(pedido);
            det.setProductoEntidad(prod);
            det.setQuantity(detDTO.getQuantity());
            boolean extraChaufa = validarExtraChaufa(prod.getId(), detDTO.getExtraChaufa());
            BigDecimal precioUnitario = prod.getPrice().add(extraChaufa ? PRECIO_EXTRA_CHAUFA : BigDecimal.ZERO);
            BigDecimal sub = precioUnitario
                    .multiply(BigDecimal.valueOf(detDTO.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            det.setSubtotal(sub);
            det.setCreams(detDTO.getCreams());
            det.setExtraChaufa(extraChaufa);
            subtotal = subtotal.add(sub);
            detalles.add(det);

            // Descontar inventario de forma simulada/aproximada por producto vendido
            descontarInventarioAsociado(prod.getId(), detDTO.getQuantity());
        }

        pedido.setTotal(subtotal.add(pedido.getDeliveryCost()).setScale(2, RoundingMode.HALF_UP));
        final PedidoEntidad guardado = pedidoRepositorio.save(pedido);

        for (DetallePedidoEntidad det : detalles) {
            detallePedidoRepositorio.save(det);
        }

        // Recalcular estadisticas del cliente si esta registrado
        if (pedido.getClienteEntidad() != null) {
            recalcularYGuardarStatsCliente(pedido.getClienteEntidad());
        }

        return convertirADTO(guardado);
    }

    @Transactional
    public PedidoDTO actualizar(Long id, PedidoDTO pedidoDTO) {
        if (pedidoDTO.getDetalles() == null || pedidoDTO.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("El pedido debe incluir al menos un producto");
        }

        PedidoEntidad pedido = pedidoRepositorio.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        if ("ENTREGADO".equals(pedido.getStatus()) || "CANCELADO".equals(pedido.getStatus())) {
            throw new IllegalStateException("Un pedido entregado o cancelado ya no puede modificarse");
        }

        // Calcular diferencias de inventario por diferencia neta
        List<DetallePedidoEntidad> detallesAnteriores = detallePedidoRepositorio.findByPedidoEntidadId(id);
        Map<Long, Integer> oldQuantities = detallesAnteriores.stream().collect(Collectors.toMap(
                d -> d.getProductoEntidad().getId(),
                d -> d.getQuantity(),
                (q1, q2) -> q1 + q2
        ));

        Map<Long, Integer> newQuantities = pedidoDTO.getDetalles().stream().collect(Collectors.toMap(
                d -> d.getProductoId(),
                d -> d.getQuantity(),
                (q1, q2) -> q1 + q2
        ));

        java.util.Set<Long> allProductIds = new java.util.HashSet<>(oldQuantities.keySet());
        allProductIds.addAll(newQuantities.keySet());

        for (Long prodId : allProductIds) {
            int oldQty = oldQuantities.getOrDefault(prodId, 0);
            int newQty = newQuantities.getOrDefault(prodId, 0);
            int diff = newQty - oldQty;
            if (diff != 0) {
                descontarInventarioAsociado(prodId, diff);
            }
        }

        detallePedidoRepositorio.deleteAll(detallesAnteriores);
        detallePedidoRepositorio.flush();

        String name = (pedidoDTO.getCustomerName() == null || pedidoDTO.getCustomerName().trim().isEmpty())
                ? "Anonimo"
                : pedidoDTO.getCustomerName().trim();
        String phone = (pedidoDTO.getCustomerPhone() == null || pedidoDTO.getCustomerPhone().trim().isEmpty())
                ? "000000000"
                : pedidoDTO.getCustomerPhone().trim();
        String address = (pedidoDTO.getCustomerAddress() == null || pedidoDTO.getCustomerAddress().trim().isEmpty())
                ? "Sin Direccion"
                : pedidoDTO.getCustomerAddress().trim();

        ClienteEntidad clienteAnterior = pedido.getClienteEntidad();
        String phoneAnterior = (clienteAnterior != null) ? clienteAnterior.getPhone().trim() : "";

        if (!phone.equals(phoneAnterior)) {
            String requestedEmail = normalizarEmail(pedidoDTO.getCustomerEmail());
            java.util.Optional<ClienteEntidad> optCliente = clienteRepositorio.findFirstByPhoneOrderByIdAsc(phone);
            ClienteEntidad nuevoCliente;
            if (optCliente.isPresent()) {
                nuevoCliente = optCliente.get();
            } else {
                nuevoCliente = new ClienteEntidad();
                nuevoCliente.setName(name);
                nuevoCliente.setPhone(phone);
                nuevoCliente.setAddress(address);
                nuevoCliente.setEmail(emailDisponibleParaCliente(requestedEmail, null) ? requestedEmail : null);
                nuevoCliente.setTotalOrders(0);
                nuevoCliente.setTotalSpent(BigDecimal.ZERO);
                nuevoCliente.setPoints(0);
                nuevoCliente = clienteRepositorio.save(nuevoCliente);
            }
            completarDatosDesdeCliente(pedido, pedidoDTO, nuevoCliente, requestedEmail);
            pedido.setClienteEntidad(nuevoCliente);
        } else if (clienteAnterior != null) {
            String requestedEmail = normalizarEmail(pedidoDTO.getCustomerEmail());
            completarDatosDesdeCliente(pedido, pedidoDTO, clienteAnterior, requestedEmail);
        }

        pedido.setCustomerName(name);
        pedido.setCustomerPhone(phone);
        pedido.setCustomerAddress(address);
        pedido.setDeliveryCost(pedidoDTO.getDeliveryCost() != null ? pedidoDTO.getDeliveryCost() : BigDecimal.ZERO);
        pedido.setType(pedidoDTO.getType());
        pedido.setPaymentMethod(pedidoDTO.getPaymentMethod());

        BigDecimal subtotal = BigDecimal.ZERO;
        List<DetallePedidoEntidad> detallesNuevos = new ArrayList<>();
        for (DetallePedidoDTO detDTO : pedidoDTO.getDetalles()) {
            ProductoEntidad producto = productoRepositorio.findById(detDTO.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID " + detDTO.getProductoId()));
            DetallePedidoEntidad detalle = new DetallePedidoEntidad();
            detalle.setPedidoEntidad(pedido);
            detalle.setProductoEntidad(producto);
            detalle.setQuantity(detDTO.getQuantity());
            boolean extraChaufa = validarExtraChaufa(producto.getId(), detDTO.getExtraChaufa());
            BigDecimal precioUnitario = producto.getPrice().add(extraChaufa ? PRECIO_EXTRA_CHAUFA : BigDecimal.ZERO);
            BigDecimal importe = precioUnitario
                    .multiply(BigDecimal.valueOf(detDTO.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            detalle.setSubtotal(importe);
            detalle.setCreams(detDTO.getCreams());
            detalle.setExtraChaufa(extraChaufa);
            subtotal = subtotal.add(importe);
            detallesNuevos.add(detalle);
        }

        BigDecimal nuevoTotal = subtotal.add(pedido.getDeliveryCost()).setScale(2, RoundingMode.HALF_UP);

        String nuevoEstadoPago = normalizarEstadoPago(pedidoDTO.getPaymentStatus());
        if ("PAGADO".equals(nuevoEstadoPago)) {
            if (pedido.getTotal() != null && pedido.getTotal().compareTo(nuevoTotal) != 0) {
                nuevoEstadoPago = "PENDIENTE";
                pedido.setPaidAt(null);
            } else if (!"PAGADO".equals(pedido.getPaymentStatus())) {
                pedido.setPaidAt(LocalDateTime.now(ZoneOffset.UTC));
            }
        } else {
            pedido.setPaidAt(null);
        }
        pedido.setPaymentStatus(nuevoEstadoPago);
        pedido.setTotal(nuevoTotal);

        PedidoEntidad guardado = pedidoRepositorio.save(pedido);
        detallePedidoRepositorio.saveAll(detallesNuevos);

        if (pedido.getClienteEntidad() != null) {
            recalcularYGuardarStatsCliente(pedido.getClienteEntidad());
        }
        if (clienteAnterior != null && pedido.getClienteEntidad() != null && !clienteAnterior.getId().equals(pedido.getClienteEntidad().getId())) {
            recalcularYGuardarStatsCliente(clienteAnterior);
        }

        return convertirADTO(guardado, detallesNuevos);
    }

    @Transactional
    public PedidoDTO actualizarEstado(Long id, String nuevoEstado) {
        String estadoNormalizado = nuevoEstado == null ? "" : nuevoEstado.trim().toUpperCase(java.util.Locale.ROOT);
        if (!ESTADOS_VALIDOS.contains(estadoNormalizado)) {
            throw new IllegalArgumentException("El estado del pedido no es valido");
        }
        PedidoEntidad pedido = pedidoRepositorio.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        if (estadoNormalizado.equals(pedido.getStatus())) {
            return convertirADTO(pedido);
        }

        pedido.setStatus(estadoNormalizado);
        pedido = pedidoRepositorio.save(pedido);

        // Si se entrega y el cliente tiene correo, enviar comprobante
        if ("ENTREGADO".equals(estadoNormalizado) && pedido.getClienteEntidad() != null && pedido.getClienteEntidad().getEmail() != null) {
            enviarComprobantePorCorreo(pedido);
        }

        // Recalcular estadisticas del cliente
        if (pedido.getClienteEntidad() != null) {
            recalcularYGuardarStatsCliente(pedido.getClienteEntidad());
        }

        return convertirADTO(pedido);
    }

    @Transactional
    public PedidoDTO actualizarPago(Long id, String nuevoEstado) {
        String estadoPago = nuevoEstado == null ? "" : nuevoEstado.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS_PAGO_VALIDOS.contains(estadoPago)) {
            throw new IllegalArgumentException("El estado de pago no es valido");
        }
        PedidoEntidad pedido = pedidoRepositorio.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));
        
        if ("PAGADO".equals(estadoPago)) {
            if (!"PAGADO".equals(pedido.getPaymentStatus())) {
                pedido.setPaidAt(LocalDateTime.now(ZoneOffset.UTC));
            }
        } else {
            pedido.setPaidAt(null);
        }
        pedido.setPaymentStatus(estadoPago);
        return convertirADTO(pedidoRepositorio.save(pedido));
    }

    private void descontarInventarioAsociado(Long productoId, int cantidad) {
        double cantDouble = (double) cantidad;
            if (productoId == 1) { // Combo El Hincha
                insumoServicio.descontarStock(1L, 1.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.2 * cantDouble);
                insumoServicio.descontarStock(8L, 1.0 * cantDouble);
            } else if (productoId == 2) { // Combo La Jugada Familiar
                insumoServicio.descontarStock(1L, 6.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.6 * cantDouble);
                insumoServicio.descontarStock(8L, 1.0 * cantDouble);
            } else if (productoId == 3) { // Combo Gol de Media Cancha
                insumoServicio.descontarStock(1L, 3.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.25 * cantDouble);
                insumoServicio.descontarStock(8L, 1.0 * cantDouble);
            } else if (productoId == 4) { // Combo Tiempo Extra
                insumoServicio.descontarStock(1L, 8.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.8 * cantDouble);
                insumoServicio.descontarStock(8L, 1.0 * cantDouble);
            } else if (productoId >= 5 && productoId <= 7) { // Clasicos por piezas
                insumoServicio.descontarStock(1L, 1.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.2 * cantDouble);
            } else if (productoId == 8) { // 1/4 Pollo
                insumoServicio.descontarStock(1L, 1.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.25 * cantDouble);
                insumoServicio.descontarStock(8L, 1.0 * cantDouble);
            } else if (productoId == 9) { // 1/2 Pollo
                insumoServicio.descontarStock(1L, 2.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.5 * cantDouble);
                insumoServicio.descontarStock(8L, 1.0 * cantDouble);
            } else if (productoId == 10) { // Pollo Entero
                insumoServicio.descontarStock(1L, 4.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.8 * cantDouble);
            } else if (productoId == 11) { // Salchipapa Personal
                insumoServicio.descontarStock(2L, 0.25 * cantDouble);
            } else if (productoId == 12) { // Salchipapa Familiar
                insumoServicio.descontarStock(2L, 0.5 * cantDouble);
            } else if (productoId == 13) { // Papas Solas
                insumoServicio.descontarStock(2L, 0.3 * cantDouble);
            } else if (productoId == 14) { // Brosteipapa
                insumoServicio.descontarStock(1L, 1.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.3 * cantDouble);
            } else if (productoId == 15) { // Burger Clasica
                insumoServicio.descontarStock(2L, 0.1 * cantDouble);
            } else if (productoId == 16) { // Burger Broster
                insumoServicio.descontarStock(1L, 1.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.1 * cantDouble);
            } else if (productoId == 17) { // Burger Doble
                insumoServicio.descontarStock(2L, 0.1 * cantDouble);
            } else if (productoId >= 18 && productoId <= 20) { // Alitas
                insumoServicio.descontarStock(1L, 1.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.2 * cantDouble);
            } else if (productoId >= 21 && productoId <= 24) { // Bebidas
                insumoServicio.descontarStock(8L, 1.0 * cantDouble);
            } else if (productoId == 25) { // Extras Papas
                insumoServicio.descontarStock(2L, 0.3 * cantDouble);
            } else if (productoId == 28) { // Promo Duo
                insumoServicio.descontarStock(1L, 1.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.5 * cantDouble);
                insumoServicio.descontarStock(8L, 2.0 * cantDouble);
            } else if (productoId == 29) { // Mega Balde
                insumoServicio.descontarStock(1L, 10.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.8 * cantDouble);
                insumoServicio.descontarStock(8L, 1.0 * cantDouble);
            } else if (productoId == 30) { // Promo Burger Lover
                insumoServicio.descontarStock(1L, 2.0 * cantDouble);
                insumoServicio.descontarStock(2L, 0.5 * cantDouble);
                insumoServicio.descontarStock(8L, 2.0 * cantDouble);
        }
    }

    private String normalizarRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) return null;
        return requestId.trim();
    }

    private String normalizarEstadoPago(String estadoPago) {
        if (estadoPago == null || estadoPago.isBlank()) return "PENDIENTE";
        String normalizado = estadoPago.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS_PAGO_VALIDOS.contains(normalizado)) {
            throw new IllegalArgumentException("El estado de pago no es valido");
        }
        return normalizado;
    }

    private boolean validarExtraChaufa(Long productoId, Boolean solicitado) {
        boolean extraChaufa = Boolean.TRUE.equals(solicitado);
        if (extraChaufa && !PRODUCTOS_CON_CHAUFA.contains(productoId)) {
            throw new IllegalArgumentException("El extra de chaufa no esta disponible para este producto");
        }
        return extraChaufa;
    }

    private String textoConDefault(String valor, String valorDefault) {
        return valor == null || valor.isBlank() ? valorDefault : valor.trim();
    }

    private LocalDateTime inicioDiaLimaUtc() {
        return java.time.ZonedDateTime.now(ZONA_LIMA)
                .toLocalDate()
                .atStartOfDay(ZONA_LIMA)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    private String normalizarEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean emailDisponibleParaCliente(String email, Long clienteIdActual) {
        if (email == null) {
            return false;
        }
        return clienteRepositorio.findByEmail(email)
                .map(cliente -> clienteIdActual != null && cliente.getId().equals(clienteIdActual))
                .orElse(true);
    }

    private void completarDatosDesdeCliente(PedidoEntidad pedido,
                                             PedidoDTO pedidoDTO,
                                             ClienteEntidad cliente,
                                             String requestedEmail) {
        boolean clienteModificado = false;

        if (pedidoDTO.getCustomerName() == null || pedidoDTO.getCustomerName().isBlank()) {
            pedido.setCustomerName(cliente.getName());
        }
        if ((pedidoDTO.getCustomerAddress() == null || pedidoDTO.getCustomerAddress().isBlank())
                && cliente.getAddress() != null && !cliente.getAddress().isBlank()) {
            pedido.setCustomerAddress(cliente.getAddress());
        }
        if ((cliente.getEmail() == null || cliente.getEmail().isBlank())
                && emailDisponibleParaCliente(requestedEmail, cliente.getId())) {
            cliente.setEmail(requestedEmail);
            clienteModificado = true;
        }
        if ((cliente.getAddress() == null || cliente.getAddress().isBlank())
                && pedidoDTO.getCustomerAddress() != null && !pedidoDTO.getCustomerAddress().isBlank()) {
            cliente.setAddress(pedidoDTO.getCustomerAddress().trim());
            clienteModificado = true;
        }
        if (clienteModificado) {
            clienteRepositorio.save(cliente);
        }
    }

    private void enviarComprobantePorCorreo(PedidoEntidad pedido) {
        String destinatario = pedido.getClienteEntidad().getEmail();
        String asunto = "Pedido #" + pedido.getId() + " entregado - La Brosteria";

        StringBuilder itemsHtml = new StringBuilder();
        List<DetallePedidoEntidad> detalles = detallePedidoRepositorio.findByPedidoEntidadId(pedido.getId());
        for (DetallePedidoEntidad det : detalles) {
            String cremas = det.getCreams() == null || det.getCreams().isBlank()
                    ? ""
                    : " - Cremas: " + HtmlUtils.htmlEscape(det.getCreams());
            String chaufa = Boolean.TRUE.equals(det.getExtraChaufa()) ? " - Con chaufa" : "";
            itemsHtml.append("<li>%d x %s%s%s</li>"
                    .formatted(det.getQuantity(),
                            HtmlUtils.htmlEscape(det.getProductoEntidad().getName()),
                            cremas,
                            chaufa));
        }

        String html = """
            <div style="font-family:Arial,sans-serif;max-width:520px;padding:24px;border:1px solid #eee;border-radius:8px">
                <h2 style="color:#FF6B00;margin:0 0 8px">Pedido entregado</h2>
                <p>Gracias, %s. Tu pedido #%d ya fue entregado.</p>
                <ul style="padding-left:20px">%s</ul>
                <p style="font-size:18px"><strong>Total: S/. %.2f</strong></p>
                <p>Pago: <strong>%s</strong> con %s</p>
                <a href="https://brosteria.vci.pe/#menu-seccion-anchor" style="display:inline-block;background:#FF6B00;color:#fff;text-decoration:none;padding:12px 18px;border-radius:6px;font-weight:bold">Volver a pedir</a>
            </div>
            """.formatted(HtmlUtils.htmlEscape(pedido.getCustomerName()), pedido.getId(), itemsHtml.toString(),
                    pedido.getTotal(), HtmlUtils.htmlEscape(pedido.getPaymentStatus()),
                    HtmlUtils.htmlEscape(pedido.getPaymentMethod()));

        emailServicio.enviarCorreoHTML(destinatario, asunto, html);
    }

    private PedidoDTO convertirADTO(PedidoEntidad entity) {
        List<DetallePedidoEntidad> detalles = detallePedidoRepositorio.findByPedidoEntidadId(entity.getId());
        return convertirADTO(entity, detalles);
    }

    private PedidoDTO convertirADTO(PedidoEntidad entity, List<DetallePedidoEntidad> detalles) {
        PedidoDTO dto = modelMapper.map(entity, PedidoDTO.class);
        dto.setPaidAt(entity.getPaidAt());
        if (entity.getClienteEntidad() != null) {
            dto.setClienteId(entity.getClienteEntidad().getId());
            dto.setCustomerEmail(entity.getClienteEntidad().getEmail());
        }
        dto.setDetalles(detalles.stream().map(d -> {
            DetallePedidoDTO dDto = new DetallePedidoDTO();
            dDto.setId(d.getId());
            dDto.setProductoId(d.getProductoEntidad().getId());
            dDto.setProductoName(d.getProductoEntidad().getName());
            dDto.setProductoPrice(d.getProductoEntidad().getPrice());
            dDto.setQuantity(d.getQuantity());
            dDto.setSubtotal(d.getSubtotal());
            dDto.setCreams(d.getCreams());
            dDto.setExtraChaufa(Boolean.TRUE.equals(d.getExtraChaufa()));
            return dDto;
        }).collect(Collectors.toList()));
        return dto;
    }

    private void recalcularYGuardarStatsCliente(ClienteEntidad cliente) {
        if (cliente == null || cliente.getPhone() == null) return;
        PedidoRepositorio.EstadisticasCliente stats = pedidoRepositorio
                .obtenerEstadisticasCliente(cliente.getPhone().trim());
        BigDecimal totalSpent = stats.getTotalGastado();
        cliente.setTotalOrders(stats.getTotalPedidos().intValue());
        cliente.setTotalSpent(totalSpent);
        cliente.setPoints(totalSpent.divideToIntegralValue(BigDecimal.TEN).intValue());
        clienteRepositorio.save(cliente);
    }
}
