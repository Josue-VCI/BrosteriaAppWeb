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
}
