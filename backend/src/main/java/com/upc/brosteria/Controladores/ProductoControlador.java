package com.upc.brosteria.Controladores;

import com.upc.brosteria.DTOs.ProductoDTO;
import com.upc.brosteria.Repositorios.ProductoRepositorio;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoControlador {

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<ProductoDTO>> listarTodos() {
        List<ProductoDTO> productos = productoRepositorio.findAll().stream()
                .map(p -> modelMapper.map(p, ProductoDTO.class))
                .collect(Collectors.toList());
        return ResponseEntity.ok(productos);
    }
}
