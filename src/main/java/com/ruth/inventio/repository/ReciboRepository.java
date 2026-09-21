package com.ruth.inventio.repository;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ruth.inventio.entity.Recibo;
import java.util.Optional;

public interface ReciboRepository extends BaseRepository<Recibo> {
    @Query("""
            select count(r) from Recibo r
            where r.fecha >= :inicio and r.fecha < :fin
              and r.fecha >= :inicioHoy and r.fecha < :finHoy
              and r.venta.estado <> com.ruth.inventio.model.EstadoVenta.ANULADA
            """)
    Long contarHoyDashboard(@Param("inicio") java.time.Instant inicio, @Param("fin") java.time.Instant fin,
            @Param("inicioHoy") java.time.Instant inicioHoy, @Param("finHoy") java.time.Instant finHoy);

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
