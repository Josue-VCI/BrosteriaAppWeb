package com.upc.brosteria.Servicios;

import com.upc.brosteria.DTOs.InsumoDTO;
import com.upc.brosteria.Entidades.InsumoEntidad;
import com.upc.brosteria.Repositorios.InsumoRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsumoServicioTest {

    @Mock private InsumoRepositorio insumoRepositorio;
    @Mock private EmailServicio emailServicio;
    @Mock private ModelMapper modelMapper;

    @InjectMocks private InsumoServicio insumoServicio;

    @Test
    void registraIngresoConActualizacionAtomica() {
        InsumoEntidad insumo = new InsumoEntidad();
        insumo.setId(1L);
        insumo.setQuantity(new BigDecimal("12.000"));
        InsumoDTO dto = new InsumoDTO();
        dto.setId(1L);
        dto.setQuantity(new BigDecimal("12.000"));

        when(insumoRepositorio.registrarIngresoAtomico(1L, new BigDecimal("2.0"))).thenReturn(1);
        when(insumoRepositorio.findById(1L)).thenReturn(Optional.of(insumo));
        when(modelMapper.map(insumo, InsumoDTO.class)).thenReturn(dto);

        InsumoDTO resultado = insumoServicio.registrarIngreso(1L, new BigDecimal("2.0"));

        verify(insumoRepositorio).registrarIngresoAtomico(1L, new BigDecimal("2.0"));
        assertEquals(new BigDecimal("12.000"), resultado.getQuantity());
    }

    @Test
    void enviaUnaAlertaSoloCuandoObtieneElCooldown() {
        InsumoEntidad insumo = new InsumoEntidad();
        insumo.setId(1L);
        insumo.setName("Mayonesa");
        insumo.setUnit("kg");
        insumo.setQuantity(new BigDecimal("2.000"));
        insumo.setMinimumStock(new BigDecimal("3.000"));

        ReflectionTestUtils.setField(insumoServicio, "alertCooldownHours", 12);
        when(insumoRepositorio.descontarStockAtomico(1L, new BigDecimal("1.000"))).thenReturn(1);
        when(insumoRepositorio.reclamarAlertaStock(1L, 12)).thenReturn(1);
        when(insumoRepositorio.findById(1L)).thenReturn(Optional.of(insumo));

        insumoServicio.descontarStock(1L, new BigDecimal("1.000"));

        verify(insumoRepositorio).reclamarAlertaStock(1L, 12);
        verify(emailServicio).notificarStockBajo("Mayonesa", new BigDecimal("2.000"), "kg");
    }
}
