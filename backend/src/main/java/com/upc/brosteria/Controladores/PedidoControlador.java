package com.upc.brosteria.Controladores;

import com.upc.brosteria.DTOs.PedidoDTO;
import com.upc.brosteria.Servicios.PedidoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoControlador {

    @Autowired
    private PedidoServicio pedidoServicio;

    @GetMapping
    public ResponseEntity<List<PedidoDTO>> listarTodos() {
        return ResponseEntity.ok(pedidoServicio.listarTodos());
    }

    @GetMapping("/activos")
    public ResponseEntity<List<PedidoDTO>> listarActivos() {
        return ResponseEntity.ok(pedidoServicio.listarActivos());
    }

    @GetMapping("/recientes")
    public ResponseEntity<List<PedidoDTO>> listarRecientes(@RequestParam(defaultValue = "5") int limite) {
        return ResponseEntity.ok(pedidoServicio.listarRecientes(limite));
    }

    @GetMapping("/estado/{status}")
    public ResponseEntity<List<PedidoDTO>> listarPorEstado(@PathVariable String status) {
        return ResponseEntity.ok(pedidoServicio.listarPorEstado(status));
    }

    @PostMapping
    public ResponseEntity<PedidoDTO> crear(@RequestBody PedidoDTO pedidoDTO) {
        return new ResponseEntity<>(pedidoServicio.crear(pedidoDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoDTO> actualizarEstado(@PathVariable Long id, @RequestParam String nuevoEstado) {
        return ResponseEntity.ok(pedidoServicio.actualizarEstado(id, nuevoEstado));
    }
}
