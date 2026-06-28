package com.upc.brosteria.Controladores;

import com.upc.brosteria.DTOs.InsumoDTO;
import com.upc.brosteria.Servicios.InsumoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/insumos")
@Validated
public class InsumoControlador {

    @Autowired
    private InsumoServicio insumoServicio;

    @GetMapping
    public ResponseEntity<List<InsumoDTO>> listarTodos() {
        return ResponseEntity.ok(insumoServicio.listarTodos());
    }

    @PostMapping
    public ResponseEntity<InsumoDTO> crear(@Valid @RequestBody InsumoDTO insumoDTO) {
        return new ResponseEntity<>(insumoServicio.crear(insumoDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InsumoDTO> actualizar(@PathVariable Long id, @Valid @RequestBody InsumoDTO insumoDTO) {
        return ResponseEntity.ok(insumoServicio.actualizar(id, insumoDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        insumoServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/ingreso")
    public ResponseEntity<InsumoDTO> registrarIngreso(@PathVariable Long id, @RequestParam @Positive BigDecimal cantidad) {
        return ResponseEntity.ok(insumoServicio.registrarIngreso(id, cantidad));
    }
}
