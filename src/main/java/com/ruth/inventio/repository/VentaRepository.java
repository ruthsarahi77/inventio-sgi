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
    @Query("""
            select coalesce(sum(v.total), 0) as total,
                   coalesce(sum(case when v.estado in (
                       com.ruth.inventio.model.EstadoVenta.PENDIENTE,
                       com.ruth.inventio.model.EstadoVenta.PARCIAL)
                       then v.saldo else 0 end), 0) as saldo,
                   count(case when v.fecha >= :inicioHoy and v.fecha < :finHoy then 1 else null end) as ventasHoy
            from Venta v
            where v.fecha >= :inicio and v.fecha < :fin
              and v.estado <> com.ruth.inventio.model.EstadoVenta.ANULADA
            """)
    DashboardVentasProjection resumirDashboard(@Param("inicio") java.time.Instant inicio,
            @Param("fin") java.time.Instant fin, @Param("inicioHoy") java.time.Instant inicioHoy,
            @Param("finHoy") java.time.Instant finHoy);

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
