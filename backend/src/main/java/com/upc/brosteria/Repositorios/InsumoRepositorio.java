package com.upc.brosteria.Repositorios;

import com.upc.brosteria.Entidades.InsumoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InsumoRepositorio extends JpaRepository<InsumoEntidad, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE insumos SET quantity = GREATEST(0, quantity - :cantidad), updated_at = NOW() WHERE id = :id", nativeQuery = true)
    int descontarStockAtomico(@Param("id") Long id, @Param("cantidad") Double cantidad);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE insumos SET quantity = quantity + :cantidad, updated_at = NOW() WHERE id = :id", nativeQuery = true)
    int registrarIngresoAtomico(@Param("id") Long id, @Param("cantidad") Double cantidad);
}
