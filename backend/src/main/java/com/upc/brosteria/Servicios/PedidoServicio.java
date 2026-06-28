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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class PedidoServicio {

    private static final java.util.Set<String> ESTADOS_VALIDOS = java.util.Set.of(
            "PENDIENTE", "PREPARANDO", "ENVIADO", "ENTREGADO", "CANCELADO");

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

    public List<PedidoDTO> listarPorEstado(String status) {
        List<PedidoEntidad> pedidos = pedidoRepositorio.findByStatusWithCliente(status);
        return mappedPedidos(pedidos);
    }

    public List<PedidoDTO> listarTodos() {
        List<PedidoEntidad> pedidos = pedidoRepositorio.findAllWithCliente();
        return mappedPedidos(pedidos);
    }

    public List<PedidoDTO> listarActivos() {
        java.time.LocalDateTime todayStart = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Lima"))
                .toLocalDate().atStartOfDay();
        List<PedidoEntidad> pedidos = pedidoRepositorio.findActiveWithCliente(todayStart);
        return mappedPedidos(pedidos);
    }

    public List<PedidoDTO> listarRecientes(int limite) {
        List<PedidoEntidad> pedidos = pedidoRepositorio.findRecentWithCliente(org.springframework.data.domain.PageRequest.of(0, limite));
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

        PedidoEntidad pedido = new PedidoEntidad();
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
        pedido.setDeliveryCost(pedidoDTO.getDeliveryCost() != null ? pedidoDTO.getDeliveryCost() : 0.0);
        pedido.setType(pedidoDTO.getType() != null ? pedidoDTO.getType() : "DELIVERY");
        pedido.setPaymentMethod(pedidoDTO.getPaymentMethod() != null ? pedidoDTO.getPaymentMethod() : "EFECTIVO");
        pedido.setStatus("PENDIENTE");
        pedido.setOrderDate(LocalDateTime.now());

        String requestedEmail = normalizarEmail(pedidoDTO.getCustomerEmail());

        if (pedidoDTO.getClienteId() != null) {
            ClienteEntidad cliente = clienteRepositorio.findById(pedidoDTO.getClienteId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            pedido.setClienteEntidad(cliente);
        } else {
            java.util.Optional<ClienteEntidad> optCliente = clienteRepositorio.findFirstByPhoneOrderByIdAsc(phone);
            if (optCliente.isPresent()) {
                ClienteEntidad cliente = optCliente.get();
                if ((cliente.getEmail() == null || cliente.getEmail().trim().isEmpty())
                        && emailDisponibleParaCliente(requestedEmail, cliente.getId())) {
                    cliente.setEmail(requestedEmail);
                    clienteRepositorio.save(cliente);
                }
                pedido.setClienteEntidad(cliente);
            } else {
                ClienteEntidad nuevoCliente = new ClienteEntidad();
                nuevoCliente.setName(name);
                nuevoCliente.setPhone(phone);
                nuevoCliente.setAddress(address);
                nuevoCliente.setEmail(emailDisponibleParaCliente(requestedEmail, null) ? requestedEmail : null);
                nuevoCliente.setTotalOrders(0);
                nuevoCliente.setTotalSpent(0.0);
                nuevoCliente.setPoints(0);
                nuevoCliente = clienteRepositorio.save(nuevoCliente);
                pedido.setClienteEntidad(nuevoCliente);
            }
        }

        double subtotal = 0.0;
        List<DetallePedidoEntidad> detalles = new ArrayList<>();

        for (DetallePedidoDTO detDTO : pedidoDTO.getDetalles()) {
            ProductoEntidad prod = productoRepositorio.findById(detDTO.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID " + detDTO.getProductoId()));

            DetallePedidoEntidad det = new DetallePedidoEntidad();
            det.setPedidoEntidad(pedido);
            det.setProductoEntidad(prod);
            det.setQuantity(detDTO.getQuantity());
            double sub = prod.getPrice() * detDTO.getQuantity();
            det.setSubtotal(sub);
            det.setCreams(detDTO.getCreams());
            subtotal += sub;
            detalles.add(det);

            // Descontar inventario de forma simulada/aproximada por producto vendido
            descontarInventarioAsociado(prod.getId(), detDTO.getQuantity());
        }

        pedido.setTotal(subtotal + pedido.getDeliveryCost());
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
    public PedidoDTO actualizarEstado(Long id, String nuevoEstado) {
        String estadoNormalizado = nuevoEstado == null ? "" : nuevoEstado.trim().toUpperCase(java.util.Locale.ROOT);
        if (!ESTADOS_VALIDOS.contains(estadoNormalizado)) {
            throw new IllegalArgumentException("El estado del pedido no es valido");
        }
        PedidoEntidad pedido = pedidoRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

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

    private void descontarInventarioAsociado(Long productoId, int cantidad) {
        try {
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
        } catch (Exception e) {
            System.err.println("No se pudo descontar stock de inventario: " + e.getMessage());
        }
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

    private void enviarComprobantePorCorreo(PedidoEntidad pedido) {
        String destinatario = pedido.getClienteEntidad().getEmail();
        String asunto = "Gracias por tu compra en La Brosteria - Pedido #" + pedido.getId();

        StringBuilder itemsHtml = new StringBuilder();
        List<DetallePedidoEntidad> detalles = detallePedidoRepositorio.findByPedidoEntidadId(pedido.getId());
        for (DetallePedidoEntidad det : detalles) {
            itemsHtml.append("<li>%d x %s - S/. %.2f (Cremas: %s)</li>"
                    .formatted(det.getQuantity(), det.getProductoEntidad().getName(), det.getSubtotal(), det.getCreams()));
        }

        String html = """
            <div style="font-family: Arial, sans-serif; border: 1px solid #FF6B00; border-radius: 8px; padding: 20px; max-width: 600px;">
                <h2 style="color: #FF6B00; margin-top: 0;">Hola %s! Aqui tienes el detalle de tu orden</h2>
                <p>Tu pedido ha sido entregado con exito. Que lo disfrutes!</p>
                <hr style="border: 0; border-top: 1px solid #eee; margin: 15px 0;">
                <h3 style="color: #333;">Detalle del Pedido #%d</h3>
                <ul>
                    %s
                </ul>
                <p><strong>Costo de Delivery:</strong> S/. %.2f</p>
                <h3 style="color: #FF6B00;">Total Pagado: S/. %.2f (%s)</h3>
                <p style="font-size: 12px; color: #888;">Direccion de Entrega: %s</p>
                <hr style="border: 0; border-top: 1px solid #eee; margin-top: 20px;">
                <p style="font-size: 12px; color: #888; text-align: center;">La Brosteria - Sabor Crujiente Premium</p>
            </div>
            """.formatted(pedido.getCustomerName(), pedido.getId(), itemsHtml.toString(), pedido.getDeliveryCost(), pedido.getTotal(), pedido.getPaymentMethod(), pedido.getCustomerAddress());

        emailServicio.enviarCorreoHTML(destinatario, asunto, html);
    }

    private PedidoDTO convertirADTO(PedidoEntidad entity) {
        List<DetallePedidoEntidad> detalles = detallePedidoRepositorio.findByPedidoEntidadId(entity.getId());
        return convertirADTO(entity, detalles);
    }

    private PedidoDTO convertirADTO(PedidoEntidad entity, List<DetallePedidoEntidad> detalles) {
        PedidoDTO dto = modelMapper.map(entity, PedidoDTO.class);
        if (entity.getClienteEntidad() != null) {
            dto.setClienteId(entity.getClienteEntidad().getId());
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
            return dDto;
        }).collect(Collectors.toList()));
        return dto;
    }

    private void recalcularYGuardarStatsCliente(ClienteEntidad cliente) {
        if (cliente == null || cliente.getPhone() == null) return;
        List<PedidoEntidad> orders = pedidoRepositorio.findByCustomerPhone(cliente.getPhone().trim());
        int totalOrders = 0;
        double totalSpent = 0.0;
        for (PedidoEntidad order : orders) {
            if ("ENTREGADO".equalsIgnoreCase(order.getStatus())) {
                totalOrders++;
                totalSpent += order.getTotal();
            }
        }
        cliente.setTotalOrders(totalOrders);
        cliente.setTotalSpent(totalSpent);
        cliente.setPoints((int) (totalSpent / 10));
        clienteRepositorio.save(cliente);
    }
}
