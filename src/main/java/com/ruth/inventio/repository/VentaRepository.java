package com.ruth.inventio.repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

import com.ruth.inventio.entity.Venta;
import java.util.Optional;

public interface VentaRepository extends BaseRepository<Venta> {
@Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Venta e where e.id = :id")
    Optional<Venta> buscarParaActualizar(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"cliente", "vendedor", "detalles", "detalles.producto", "proforma"})
    Optional<Venta> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"cliente", "vendedor", "detalles", "detalles.producto", "proforma"})
    List<Venta> findAll();

    @EntityGraph(attributePaths = {"cliente", "vendedor", "detalles", "detalles.producto", "proforma"})
    List<Venta> findByVendedorId(Long vendedorId);

    boolean existsByProformaId(Long proformaId);

    Optional<Venta> findByNumero(String numero);

    boolean existsByNumero(String numero);
}
