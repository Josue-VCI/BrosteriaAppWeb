package com.upc.brosteria.Servicios;

import com.upc.brosteria.DTOs.InsumoDTO;
import com.upc.brosteria.Entidades.InsumoEntidad;
import com.upc.brosteria.Repositorios.InsumoRepositorio;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InsumoServicio {

    @Autowired
    private InsumoRepositorio insumoRepositorio;

    @Autowired
    private EmailServicio emailServicio;

    @Autowired
    private ModelMapper modelMapper;

    public List<InsumoDTO> listarTodos() {
        return insumoRepositorio.findAll().stream()
                .map(insumo -> modelMapper.map(insumo, InsumoDTO.class))
                .collect(Collectors.toList());
    }

    public InsumoDTO crear(InsumoDTO insumoDTO) {
        InsumoEntidad insumo = modelMapper.map(insumoDTO, InsumoEntidad.class);
        insumo = insumoRepositorio.save(insumo);
        return modelMapper.map(insumo, InsumoDTO.class);
    }

    public InsumoDTO actualizar(Long id, InsumoDTO insumoDTO) {
        InsumoEntidad insumo = insumoRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado"));

        insumo.setName(insumoDTO.getName());
        insumo.setQuantity(insumoDTO.getQuantity());
        insumo.setUnit(insumoDTO.getUnit());
        insumo.setMinimumStock(insumoDTO.getMinimumStock());

        insumo = insumoRepositorio.save(insumo);
        return modelMapper.map(insumo, InsumoDTO.class);
    }

    public void eliminar(Long id) {
        insumoRepositorio.deleteById(id);
    }

    @Transactional
    public InsumoDTO registrarIngreso(Long id, Double cantidad) {
        if (insumoRepositorio.registrarIngresoAtomico(id, cantidad) == 0) {
            throw new RuntimeException("Insumo no encontrado");
        }
        InsumoEntidad insumo = insumoRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado"));
        return modelMapper.map(insumo, InsumoDTO.class);
    }

    @Transactional
    public void descontarStock(Long id, Double cantidad) {
        if (insumoRepositorio.descontarStockAtomico(id, cantidad) == 0) {
            throw new RuntimeException("Insumo no encontrado");
        }
        InsumoEntidad insumo = insumoRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado"));

        if (insumo.getQuantity() <= insumo.getMinimumStock()) {
            emailServicio.notificarStockBajo(insumo.getName(), insumo.getQuantity(), insumo.getUnit());
        }
    }
}
