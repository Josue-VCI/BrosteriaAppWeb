package com.upc.brosteria.Servicios;

import com.upc.brosteria.DTOs.ClienteDTO;
import com.upc.brosteria.Entidades.ClienteEntidad;
import com.upc.brosteria.Repositorios.ClienteRepositorio;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClienteServicio {

    @Autowired
    private ClienteRepositorio clienteRepositorio;

    @Autowired
    private EmailServicio emailServicio;

    @Autowired
    private ModelMapper modelMapper;

    public List<ClienteDTO> listarTodos() {
        return clienteRepositorio.findAll().stream()
                .map(c -> modelMapper.map(c, ClienteDTO.class))
                .collect(Collectors.toList());
    }

    public ClienteDTO crearOActualizar(ClienteDTO clienteDTO) {
        ClienteEntidad cliente;
        if (clienteDTO.getId() != null) {
            cliente = clienteRepositorio.findById(clienteDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            cliente.setName(clienteDTO.getName());
            cliente.setEmail(clienteDTO.getEmail());
            cliente.setPhone(clienteDTO.getPhone());
            cliente.setAddress(clienteDTO.getAddress());
        } else {
            cliente = modelMapper.map(clienteDTO, ClienteEntidad.class);
        }
        cliente = clienteRepositorio.save(cliente);
        return modelMapper.map(cliente, ClienteDTO.class);
    }

    public void eliminar(Long id) {
        clienteRepositorio.deleteById(id);
    }

    public void enviarCorreoMasivo(List<String> destinatarios, String asunto, String mensajeHtml) {
        for (String email : destinatarios) {
            if (email != null && !email.trim().isEmpty()) {
                emailServicio.enviarCorreoHTML(email, asunto, mensajeHtml);
            }
        }
    }
}
