package com.ruth.inventio.repository;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ruth.inventio.entity.Recibo;
import java.util.Optional;

public interface ReciboRepository extends BaseRepository<Recibo> {
@Override
    @EntityGraph(attributePaths = {"venta", "usuario"})
    Optional<Recibo> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"venta", "usuario"})
    List<Recibo> findAll();

    @EntityGraph(attributePaths = {"venta", "usuario"})
    List<Recibo> findByVentaVendedorId(Long vendedorId);

    boolean existsByVentaId(Long ventaId);

    @Query("select coalesce(sum(r.monto), 0) from Recibo r where r.venta.id = :id")
    BigDecimal sumarAbonos(@Param("id") Long id);

    Optional<Recibo> findByNumero(String numero);

    boolean existsByNumero(String numero);
}
