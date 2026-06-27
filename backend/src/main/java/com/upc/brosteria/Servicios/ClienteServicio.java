package com.upc.brosteria.Servicios;

import com.upc.brosteria.DTOs.ClienteDTO;
import com.upc.brosteria.Entidades.ClienteEntidad;
import com.upc.brosteria.Entidades.PedidoEntidad;
import com.upc.brosteria.Repositorios.ClienteRepositorio;
import com.upc.brosteria.Repositorios.PedidoRepositorio;
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
    private PedidoRepositorio pedidoRepositorio;

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
        
        // Vincular pedidos anteriores y recalcular estadisticas
        linkOrdersAndRecalculateStats(cliente);
        
        return modelMapper.map(cliente, ClienteDTO.class);
    }

    private void linkOrdersAndRecalculateStats(ClienteEntidad cliente) {
        if (cliente.getPhone() == null || cliente.getPhone().trim().isEmpty()) {
            return;
        }
        String phone = cliente.getPhone().trim();
        List<PedidoEntidad> orders = pedidoRepositorio.findByCustomerPhone(phone);
        
        int totalOrders = 0;
        double totalSpent = 0.0;
        
        for (PedidoEntidad order : orders) {
            if (order.getClienteEntidad() == null || !order.getClienteEntidad().getId().equals(cliente.getId())) {
                order.setClienteEntidad(cliente);
                pedidoRepositorio.save(order);
            }
            if ("ENTREGADO".equalsIgnoreCase(order.getStatus())) {
                totalOrders++;
                totalSpent += order.getTotal();
            }
        }
        
        cliente.setTotalOrders(totalOrders);
        cliente.setTotalSpent(totalSpent);
        cliente.setPoints((int) (totalSpent / 10));
        clienteRepositorio.save(cliente);
    }

    @org.springframework.transaction.annotation.Transactional
    public void eliminar(Long id) {
        pedidoRepositorio.detachCliente(id);
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
