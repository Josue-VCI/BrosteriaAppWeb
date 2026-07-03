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
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;

@Service
public class InsumoServicio {

    @Autowired
    private InsumoRepositorio insumoRepositorio;

    @Autowired
    private EmailServicio emailServicio;

    @Autowired
    private ModelMapper modelMapper;

    @Value("${stock.alert.cooldown-hours:12}")
    private int alertCooldownHours;

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
    public InsumoDTO registrarIngreso(Long id, BigDecimal cantidad) {
        if (insumoRepositorio.registrarIngresoAtomico(id, cantidad) == 0) {
            throw new RuntimeException("Insumo no encontrado");
        }
        InsumoEntidad insumo = insumoRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Insumo no encontrado"));
        return modelMapper.map(insumo, InsumoDTO.class);
    }

    @Transactional
    public void descontarStock(Long id, BigDecimal cantidad) {
        if (insumoRepositorio.descontarStockAtomico(id, cantidad) == 0) {
            throw new RuntimeException("Insumo no encontrado");
        }
        if (cantidad.signum() > 0 && insumoRepositorio.reclamarAlertaStock(id, alertCooldownHours) == 1) {
            InsumoEntidad insumo = insumoRepositorio.findById(id)
                    .orElseThrow(() -> new RuntimeException("Insumo no encontrado"));
            emailServicio.notificarStockBajo(insumo.getName(), insumo.getQuantity(), insumo.getUnit());
        }
    }

    public void descontarStock(Long id, double cantidad) {
        descontarStock(id, BigDecimal.valueOf(cantidad).setScale(3, RoundingMode.HALF_UP));
    }
}
