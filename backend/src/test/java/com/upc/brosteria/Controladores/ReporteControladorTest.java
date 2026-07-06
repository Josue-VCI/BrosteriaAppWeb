package com.upc.brosteria.Controladores;

import com.upc.brosteria.Repositorios.DetallePedidoRepositorio;
import com.upc.brosteria.Repositorios.PedidoRepositorio;
import com.upc.brosteria.Servicios.PdfServicio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteControladorTest {

    @Mock private PedidoRepositorio pedidoRepositorio;
    @Mock private DetallePedidoRepositorio detallePedidoRepositorio;
    @Mock private PdfServicio pdfServicio;

    @InjectMocks private ReporteControlador reporteControlador;

    @Test
    void conservaTopProductosSiOtroGraficoFalla() {
        when(pedidoRepositorio.ventasPorFecha(any(LocalDateTime.class), any(LocalDateTime.class), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Fallo simulado"));
        when(pedidoRepositorio.pagosReporte(any(), any(), anyString(), anyInt())).thenReturn(List.of());
        when(pedidoRepositorio.pedidosPorHora(any(), any(), anyString(), anyInt())).thenReturn(List.of());
        when(pedidoRepositorio.distritosReporte(any(), any(), anyString(), anyInt())).thenReturn(List.of());

        PedidoRepositorio.ProductoConteo producto = mock(PedidoRepositorio.ProductoConteo.class);
        when(producto.getNombre()).thenReturn("Combo El Hincha");
        when(producto.getCantidad()).thenReturn(12L);
        when(pedidoRepositorio.topProductosReporte(any(), any(), anyString(), anyInt()))
                .thenReturn(List.of(producto));

        Map<String, Object> body = reporteControlador
                .obtenerDatosGrafico("semana", "", "")
                .getBody();

        assertEquals(List.of(), body.get("fechas"));
        List<?> top = (List<?>) body.get("topProductos");
        assertEquals(1, top.size());
        assertEquals("Combo El Hincha", ((Map<?, ?>) top.getFirst()).get("nombre"));
    }
}
