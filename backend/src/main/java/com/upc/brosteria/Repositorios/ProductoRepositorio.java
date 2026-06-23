package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.ProductoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoRepositorio extends JpaRepository<ProductoEntidad, Long> {
    List<ProductoEntidad> findByActiveTrue();
}
