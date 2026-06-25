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
import java.util.stream.Collectors;

@Service
public class PedidoServicio {

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
        List<PedidoEntidad> pedidos = pedidoRepositorio.findActiveWithCliente();
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
        PedidoEntidad pedido = new PedidoEntidad();
        pedido.setCustomerName(pedidoDTO.getCustomerName());
        pedido.setCustomerPhone(pedidoDTO.getCustomerPhone());
        pedido.setCustomerAddress(pedidoDTO.getCustomerAddress());
        pedido.setDeliveryCost(pedidoDTO.getDeliveryCost());
        pedido.setType(pedidoDTO.getType());
        pedido.setPaymentMethod(pedidoDTO.getPaymentMethod());
        pedido.setStatus("PENDIENTE");
        pedido.setOrderDate(LocalDateTime.now());

        if (pedidoDTO.getClienteId() != null) {
            ClienteEntidad cliente = clienteRepositorio.findById(pedidoDTO.getClienteId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            pedido.setClienteEntidad(cliente);
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

        // Si el cliente está registrado, sumar estadísticas
        if (pedido.getClienteEntidad() != null) {
            ClienteEntidad cliente = pedido.getClienteEntidad();
            cliente.setTotalOrders(cliente.getTotalOrders() + 1);
            cliente.setTotalSpent(cliente.getTotalSpent() + pedido.getTotal());
            cliente.setPoints(cliente.getPoints() + (int) (pedido.getTotal() / 10)); // 1 punto por cada S/.10
            clienteRepositorio.save(cliente);
        }

        return convertirADTO(guardado);
    }

    @Transactional
    public PedidoDTO actualizarEstado(Long id, String nuevoEstado) {
        PedidoEntidad pedido = pedidoRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

        pedido.setStatus(nuevoEstado);
        pedido = pedidoRepositorio.save(pedido);

        // Si se entrega y el cliente tiene correo, enviar comprobante
        if ("ENTREGADO".equals(nuevoEstado) && pedido.getClienteEntidad() != null && pedido.getClienteEntidad().getEmail() != null) {
            enviarComprobantePorCorreo(pedido);
        }

        return convertirADTO(pedido);
    }

    private void descontarInventarioAsociado(Long productoId, int cantidad) {
        try {
            // Descuentos estimados basados en IDs de productos insertados en data.sql
            if (productoId == 1) { // Combo El Hincha
                insumoServicio.descontarStock(1L, (double) cantidad); // 1 pollo
                insumoServicio.descontarStock(2L, 0.2 * cantidad); // 200g papas
                insumoServicio.descontarStock(8L, (double) cantidad); // 1 bebida
            } else if (productoId == 2) { // Combo La Jugada Familiar
                insumoServicio.descontarStock(1L, 3.0 * cantidad); // 3 pollos
                insumoServicio.descontarStock(2L, 0.4 * cantidad); // 400g papas
                insumoServicio.descontarStock(8L, (double) cantidad); // 1 gaseosa familiar
            } else if (productoId >= 5 && productoId <= 7) { // Clásicos por piezas individuales
                insumoServicio.descontarStock(1L, (double) cantidad); // 1 pollo
                insumoServicio.descontarStock(2L, 0.2 * cantidad); // 200g papas
            } else if (productoId == 11) { // Salchipapa Personal
                insumoServicio.descontarStock(2L, 0.25 * cantidad); // 250g papas
            } else if (productoId == 12) { // Salchipapa Familiar
                insumoServicio.descontarStock(2L, 0.6 * cantidad); // 600g papas
            }
        } catch (Exception e) {
            System.err.println("No se pudo descontar stock de inventario: " + e.getMessage());
        }
    }

    private void enviarComprobantePorCorreo(PedidoEntidad pedido) {
        String destinatario = pedido.getClienteEntidad().getEmail();
        String asunto = "🍗 ¡Gracias por tu compra en La Brostería! - Pedido #" + pedido.getId();

        StringBuilder itemsHtml = new StringBuilder();
        List<DetallePedidoEntidad> detalles = detallePedidoRepositorio.findByPedidoEntidadId(pedido.getId());
        for (DetallePedidoEntidad det : detalles) {
            itemsHtml.append("<li>%d x %s - S/. %.2f (Cremas: %s)</li>"
                    .formatted(det.getQuantity(), det.getProductoEntidad().getName(), det.getSubtotal(), det.getCreams()));
        }

        String html = """
            <div style="font-family: Arial, sans-serif; border: 1px solid #FF6B00; border-radius: 8px; padding: 20px; max-width: 600px;">
                <h2 style="color: #FF6B00; margin-top: 0;">¡Hola %s! Aquí tienes el detalle de tu orden</h2>
                <p>Tu pedido ha sido entregado con éxito. ¡Que lo disfrutes!</p>
                <hr style="border: 0; border-top: 1px solid #eee; margin: 15px 0;">
                <h3 style="color: #333;">Detalle del Pedido #%d</h3>
                <ul>
                    %s
                </ul>
                <p><strong>Costo de Delivery:</strong> S/. %.2f</p>
                <h3 style="color: #FF6B00;">Total Pagado: S/. %.2f (%s)</h3>
                <p style="font-size: 12px; color: #888;">Dirección de Entrega: %s</p>
                <hr style="border: 0; border-top: 1px solid #eee; margin-top: 20px;">
                <p style="font-size: 12px; color: #888; text-align: center;">La Brostería - Sabor Crujiente Premium</p>
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
}
