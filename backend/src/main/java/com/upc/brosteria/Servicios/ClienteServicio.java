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
import java.util.Optional;
import java.math.BigDecimal;
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

    public List<ClienteDTO> listarTodos(int limite) {
        int limiteSeguro = Math.max(1, Math.min(limite, 500));
        return clienteRepositorio.findAll(org.springframework.data.domain.PageRequest.of(0, limiteSeguro)).stream()
                .map(c -> modelMapper.map(c, ClienteDTO.class))
                .collect(Collectors.toList());
    }

    public Optional<ClienteDTO> buscarPorTelefono(String telefono) {
        return clienteRepositorio.findFirstByPhoneOrderByIdAsc(telefono.trim())
                .map(cliente -> modelMapper.map(cliente, ClienteDTO.class));
    }

    @org.springframework.transaction.annotation.Transactional
    public ClienteDTO crearOActualizar(ClienteDTO clienteDTO) {
        ClienteEntidad cliente;
        if (clienteDTO.getId() != null) {
            cliente = clienteRepositorio.findById(clienteDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            cliente.setName(clienteDTO.getName().trim());
            cliente.setEmail(clienteDTO.getEmail() == null || clienteDTO.getEmail().isBlank()
                    ? null : clienteDTO.getEmail().trim().toLowerCase());
            cliente.setPhone(clienteDTO.getPhone().trim());
            cliente.setAddress(clienteDTO.getAddress() == null ? null : clienteDTO.getAddress().trim());
        } else {
            cliente = new ClienteEntidad();
            cliente.setName(clienteDTO.getName().trim());
            cliente.setEmail(clienteDTO.getEmail() == null || clienteDTO.getEmail().isBlank()
                    ? null : clienteDTO.getEmail().trim().toLowerCase());
            cliente.setPhone(clienteDTO.getPhone().trim());
            cliente.setAddress(clienteDTO.getAddress() == null ? null : clienteDTO.getAddress().trim());
            cliente.setTotalOrders(0);
            cliente.setTotalSpent(BigDecimal.ZERO);
            cliente.setPoints(0);
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
        pedidoRepositorio.vincularPedidosPorTelefono(cliente.getId(), phone);
        PedidoRepositorio.EstadisticasCliente stats = pedidoRepositorio.obtenerEstadisticasCliente(phone);
        BigDecimal totalSpent = stats.getTotalGastado();

        cliente.setTotalOrders(stats.getTotalPedidos().intValue());
        cliente.setTotalSpent(totalSpent);
        cliente.setPoints(totalSpent.divideToIntegralValue(BigDecimal.TEN).intValue());
        clienteRepositorio.save(cliente);
    }

    @org.springframework.transaction.annotation.Transactional
    public void eliminar(Long id) {
        pedidoRepositorio.detachCliente(id);
        clienteRepositorio.deleteById(id);
    }

    public void enviarCorreoMasivo(List<String> destinatarios, String asunto, String mensajeHtml) {
        if (destinatarios == null || destinatarios.isEmpty() || destinatarios.size() > 200) {
            throw new IllegalArgumentException("La lista debe contener entre 1 y 200 destinatarios");
        }
        if (asunto == null || asunto.isBlank() || asunto.length() > 150) {
            throw new IllegalArgumentException("El asunto no es valido");
        }
        if (mensajeHtml == null || mensajeHtml.isBlank() || mensajeHtml.length() > 20000) {
            throw new IllegalArgumentException("El contenido del correo no es valido");
        }
        for (String email : destinatarios) {
            if (email != null && !email.trim().isEmpty()) {
                emailServicio.enviarCorreoHTML(email, asunto, mensajeHtml);
            }
        }
    }
}
