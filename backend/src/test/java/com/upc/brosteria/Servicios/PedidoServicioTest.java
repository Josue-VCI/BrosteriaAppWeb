package com.upc.brosteria.Servicios;

import com.upc.brosteria.DTOs.DetallePedidoDTO;
import com.upc.brosteria.DTOs.PedidoDTO;
import com.upc.brosteria.Entidades.ClienteEntidad;
import com.upc.brosteria.Entidades.PedidoEntidad;
import com.upc.brosteria.Entidades.ProductoEntidad;
import com.upc.brosteria.Repositorios.ClienteRepositorio;
import com.upc.brosteria.Repositorios.DetallePedidoRepositorio;
import com.upc.brosteria.Repositorios.PedidoRepositorio;
import com.upc.brosteria.Repositorios.ProductoRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServicioTest {

    @Mock private PedidoRepositorio pedidoRepositorio;
    @Mock private DetallePedidoRepositorio detallePedidoRepositorio;
    @Mock private ProductoRepositorio productoRepositorio;
    @Mock private ClienteRepositorio clienteRepositorio;
    @Mock private InsumoServicio insumoServicio;
    @Mock private EmailServicio emailServicio;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private PedidoServicio pedidoServicio;

    @Test
    void creaPedidoSinReutilizarEmailDeOtroCliente() {
        ClienteEntidad clienteExistente = new ClienteEntidad();
        clienteExistente.setId(10L);
        clienteExistente.setEmail("cliente@correo.com");

        ProductoEntidad producto = new ProductoEntidad();
        producto.setId(1L);
        producto.setName("Combo El Hincha");
        producto.setPrice(new BigDecimal("15.00"));

        when(clienteRepositorio.findFirstByPhoneOrderByIdAsc("999111222")).thenReturn(Optional.empty());
        when(clienteRepositorio.findByEmail("cliente@correo.com")).thenReturn(Optional.of(clienteExistente));
        when(clienteRepositorio.save(any(ClienteEntidad.class))).thenAnswer(invocation -> {
            ClienteEntidad cliente = invocation.getArgument(0);
            cliente.setId(20L);
            return cliente;
        });
        when(productoRepositorio.findById(1L)).thenReturn(Optional.of(producto));
        when(pedidoRepositorio.save(any(PedidoEntidad.class))).thenAnswer(invocation -> {
            PedidoEntidad pedido = invocation.getArgument(0);
            pedido.setId(30L);
            return pedido;
        });
        PedidoRepositorio.EstadisticasCliente stats = mock(PedidoRepositorio.EstadisticasCliente.class);
        when(stats.getTotalPedidos()).thenReturn(0L);
        when(stats.getTotalGastado()).thenReturn(BigDecimal.ZERO);
        when(pedidoRepositorio.obtenerEstadisticasCliente("999111222")).thenReturn(stats);
        when(detallePedidoRepositorio.findByPedidoEntidadId(30L)).thenReturn(List.of());
        when(modelMapper.map(any(PedidoEntidad.class), eq(PedidoDTO.class))).thenReturn(new PedidoDTO());

        DetallePedidoDTO detalle = new DetallePedidoDTO();
        detalle.setProductoId(1L);
        detalle.setQuantity(1);

        PedidoDTO pedido = new PedidoDTO();
        pedido.setCustomerName("Cliente Nuevo");
        pedido.setCustomerPhone("999111222");
        pedido.setCustomerEmail(" CLIENTE@CORREO.COM ");
        pedido.setType("PICKUP");
        pedido.setPaymentMethod("YAPE");
        pedido.setDetalles(List.of(detalle));

        pedidoServicio.crear(pedido);

        ArgumentCaptor<ClienteEntidad> captor = ArgumentCaptor.forClass(ClienteEntidad.class);
        verify(clienteRepositorio, atLeastOnce()).save(captor.capture());
        assertNull(captor.getValue().getEmail());
        ArgumentCaptor<PedidoEntidad> pedidoCaptor = ArgumentCaptor.forClass(PedidoEntidad.class);
        verify(pedidoRepositorio).save(pedidoCaptor.capture());
        assertEquals("PENDIENTE", pedidoCaptor.getValue().getPaymentStatus());
    }

    @Test
    void reintentoConMismoRequestIdDevuelvePedidoExistente() {
        PedidoEntidad existente = new PedidoEntidad();
        existente.setId(44L);
        existente.setRequestId("req-123");
        PedidoDTO esperado = new PedidoDTO();
        esperado.setId(44L);

        when(pedidoRepositorio.findByRequestId("req-123")).thenReturn(Optional.of(existente));
        when(detallePedidoRepositorio.findByPedidoEntidadId(44L)).thenReturn(List.of());
        when(modelMapper.map(existente, PedidoDTO.class)).thenReturn(esperado);

        PedidoDTO solicitud = new PedidoDTO();
        solicitud.setRequestId("req-123");
        DetallePedidoDTO detalle = new DetallePedidoDTO();
        detalle.setProductoId(1L);
        detalle.setQuantity(1);
        solicitud.setDetalles(List.of(detalle));

        PedidoDTO resultado = pedidoServicio.crear(solicitud);

        assertSame(esperado, resultado);
        verify(productoRepositorio, never()).findById(any());
    }

    @Test
    void falloDeInventarioNoSeOculta() {
        ProductoEntidad producto = new ProductoEntidad();
        producto.setId(1L);
        producto.setName("Combo El Hincha");
        producto.setPrice(new BigDecimal("15.00"));

        when(clienteRepositorio.findFirstByPhoneOrderByIdAsc("999111222")).thenReturn(Optional.empty());
        when(clienteRepositorio.save(any(ClienteEntidad.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoRepositorio.findById(1L)).thenReturn(Optional.of(producto));
        doThrow(new RuntimeException("Stock no disponible"))
                .when(insumoServicio).descontarStock(1L, 1.0);

        DetallePedidoDTO detalle = new DetallePedidoDTO();
        detalle.setProductoId(1L);
        detalle.setQuantity(1);
        PedidoDTO pedido = new PedidoDTO();
        pedido.setCustomerPhone("999111222");
        pedido.setType("PICKUP");
        pedido.setPaymentMethod("YAPE");
        pedido.setDetalles(List.of(detalle));

        assertThrows(RuntimeException.class, () -> pedidoServicio.crear(pedido));
        verify(pedidoRepositorio, never()).save(any(PedidoEntidad.class));
    }

    @Test
    void permiteConfirmarPagoSinCambiarEstadoDelPedido() {
        PedidoEntidad pedido = new PedidoEntidad();
        pedido.setId(50L);
        pedido.setStatus("PREPARANDO");
        pedido.setPaymentStatus("PENDIENTE");

        when(pedidoRepositorio.findByIdForUpdate(50L)).thenReturn(Optional.of(pedido));
        when(pedidoRepositorio.save(pedido)).thenReturn(pedido);
        when(detallePedidoRepositorio.findByPedidoEntidadId(50L)).thenReturn(List.of());
        when(modelMapper.map(pedido, PedidoDTO.class)).thenReturn(new PedidoDTO());

        pedidoServicio.actualizarPago(50L, "PAGADO");

        assertEquals("PAGADO", pedido.getPaymentStatus());
        assertEquals("PREPARANDO", pedido.getStatus());
    }

    @Test
    void noPermiteModificarPedidoEntregado() {
        PedidoEntidad pedido = new PedidoEntidad();
        pedido.setId(60L);
        pedido.setStatus("ENTREGADO");
        when(pedidoRepositorio.findByIdForUpdate(60L)).thenReturn(Optional.of(pedido));

        PedidoDTO cambios = new PedidoDTO();
        DetallePedidoDTO detalle = new DetallePedidoDTO();
        detalle.setProductoId(1L);
        detalle.setQuantity(1);
        cambios.setDetalles(List.of(detalle));

        assertThrows(IllegalStateException.class, () -> pedidoServicio.actualizar(60L, cambios));
        verify(detallePedidoRepositorio, never()).deleteAll(any());
    }

    @Test
    void modificarPedidoVuelveAPendienteSiCambiaElTotal() {
        PedidoEntidad pedido = new PedidoEntidad();
        pedido.setId(70L);
        pedido.setStatus("PREPARANDO");
        pedido.setPaymentStatus("PAGADO");
        pedido.setTotal(new BigDecimal("15.00"));
        pedido.setPaidAt(java.time.LocalDateTime.now());
        pedido.setClienteEntidad(clienteAnonimo());

        ProductoEntidad producto = new ProductoEntidad();
        producto.setId(1L);
        producto.setName("Combo El Hincha");
        producto.setPrice(new BigDecimal("15.00"));

        com.upc.brosteria.Entidades.DetallePedidoEntidad detalleAnterior = new com.upc.brosteria.Entidades.DetallePedidoEntidad();
        detalleAnterior.setId(100L);
        detalleAnterior.setPedidoEntidad(pedido);
        detalleAnterior.setProductoEntidad(producto);
        detalleAnterior.setQuantity(1);
        detalleAnterior.setSubtotal(new BigDecimal("15.00"));

        when(pedidoRepositorio.findByIdForUpdate(70L)).thenReturn(Optional.of(pedido));
        when(detallePedidoRepositorio.findByPedidoEntidadId(70L)).thenReturn(List.of(detalleAnterior));
        when(productoRepositorio.findById(1L)).thenReturn(Optional.of(producto));
        when(pedidoRepositorio.save(any(PedidoEntidad.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.map(any(PedidoEntidad.class), eq(PedidoDTO.class))).thenReturn(new PedidoDTO());
        mockEstadisticasCliente();

        PedidoDTO cambios = new PedidoDTO();
        DetallePedidoDTO detDto = new DetallePedidoDTO();
        detDto.setProductoId(1L);
        detDto.setQuantity(2); // Cambia la cantidad de 1 a 2 -> Total cambia a S/. 30
        cambios.setDetalles(List.of(detDto));
        cambios.setPaymentStatus("PAGADO");
        cambios.setType("PICKUP");
        cambios.setPaymentMethod("YAPE");

        pedidoServicio.actualizar(70L, cambios);

        assertEquals("PENDIENTE", pedido.getPaymentStatus());
        assertNull(pedido.getPaidAt());
    }

    @Test
    void modificarPedidoCalculaInventarioDiferencialCorrectamente() {
        PedidoEntidad pedido = new PedidoEntidad();
        pedido.setId(80L);
        pedido.setStatus("PREPARANDO");
        pedido.setPaymentStatus("PENDIENTE");
        pedido.setTotal(new BigDecimal("30.00"));
        pedido.setClienteEntidad(clienteAnonimo());

        ProductoEntidad producto = new ProductoEntidad();
        producto.setId(1L);
        producto.setName("Combo El Hincha");
        producto.setPrice(new BigDecimal("15.00"));

        com.upc.brosteria.Entidades.DetallePedidoEntidad detalleAnterior = new com.upc.brosteria.Entidades.DetallePedidoEntidad();
        detalleAnterior.setId(101L);
        detalleAnterior.setPedidoEntidad(pedido);
        detalleAnterior.setProductoEntidad(producto);
        detalleAnterior.setQuantity(2); // Tenía 2 unidades del Combo 1
        detalleAnterior.setSubtotal(new BigDecimal("30.00"));

        when(pedidoRepositorio.findByIdForUpdate(80L)).thenReturn(Optional.of(pedido));
        when(detallePedidoRepositorio.findByPedidoEntidadId(80L)).thenReturn(List.of(detalleAnterior));
        when(productoRepositorio.findById(1L)).thenReturn(Optional.of(producto));
        when(pedidoRepositorio.save(any(PedidoEntidad.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.map(any(PedidoEntidad.class), eq(PedidoDTO.class))).thenReturn(new PedidoDTO());
        mockEstadisticasCliente();

        PedidoDTO cambios = new PedidoDTO();
        DetallePedidoDTO detDto = new DetallePedidoDTO();
        detDto.setProductoId(1L);
        detDto.setQuantity(3); // Cambia a 3 unidades -> Diferencia neta es +1 unidad
        cambios.setDetalles(List.of(detDto));
        cambios.setPaymentStatus("PENDIENTE");
        cambios.setType("PICKUP");
        cambios.setPaymentMethod("YAPE");

        pedidoServicio.actualizar(80L, cambios);

        // Combo 1 descuenta 1.0 del insumo 1, 0.2 del insumo 2, y 1.0 del insumo 8
        // Dado que la cantidad subió en 1, el descuento neto debe ser de +1 Combo El Hincha
        verify(insumoServicio).descontarStock(1L, 1.0);
        verify(insumoServicio).descontarStock(2L, 0.2);
        verify(insumoServicio).descontarStock(8L, 1.0);
    }

    private ClienteEntidad clienteAnonimo() {
        ClienteEntidad cliente = new ClienteEntidad();
        cliente.setId(1L);
        cliente.setName("Anonimo");
        cliente.setPhone("000000000");
        cliente.setAddress("Sin Direccion");
        return cliente;
    }

    private void mockEstadisticasCliente() {
        PedidoRepositorio.EstadisticasCliente stats = mock(PedidoRepositorio.EstadisticasCliente.class);
        when(stats.getTotalPedidos()).thenReturn(0L);
        when(stats.getTotalGastado()).thenReturn(BigDecimal.ZERO);
        when(pedidoRepositorio.obtenerEstadisticasCliente("000000000")).thenReturn(stats);
    }
}
