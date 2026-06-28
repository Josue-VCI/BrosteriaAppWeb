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

import java.util.Optional;

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
        insumo.setQuantity(12.0);
        InsumoDTO dto = new InsumoDTO();
        dto.setId(1L);
        dto.setQuantity(12.0);

        when(insumoRepositorio.registrarIngresoAtomico(1L, 2.0)).thenReturn(1);
        when(insumoRepositorio.findById(1L)).thenReturn(Optional.of(insumo));
        when(modelMapper.map(insumo, InsumoDTO.class)).thenReturn(dto);

        InsumoDTO resultado = insumoServicio.registrarIngreso(1L, 2.0);

        verify(insumoRepositorio).registrarIngresoAtomico(1L, 2.0);
        assertEquals(12.0, resultado.getQuantity());
    }
}
