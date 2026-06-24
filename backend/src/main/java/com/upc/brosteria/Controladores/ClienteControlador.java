package com.upc.brosteria.Controladores;

import com.upc.brosteria.DTOs.ClienteDTO;
import com.upc.brosteria.Servicios.ClienteServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteControlador {

    @Autowired
    private ClienteServicio clienteServicio;

    @GetMapping
    public ResponseEntity<List<ClienteDTO>> listarTodos() {
        return ResponseEntity.ok(clienteServicio.listarTodos());
    }

    @PostMapping
    public ResponseEntity<ClienteDTO> crearOActualizar(@RequestBody ClienteDTO clienteDTO) {
        return ResponseEntity.ok(clienteServicio.crearOActualizar(clienteDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/enviar-masivo")
    public ResponseEntity<Void> enviarCorreoMasivo(@RequestBody Map<String, Object> body) {
        List<String> destinatarios = (List<String>) body.get("destinatarios");
        String asunto = (String) body.get("asunto");
        String mensajeHtml = (String) body.get("mensajeHtml");

        clienteServicio.enviarCorreoMasivo(destinatarios, asunto, mensajeHtml);
        return ResponseEntity.ok().build();
    }
}
