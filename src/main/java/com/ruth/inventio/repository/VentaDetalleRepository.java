package com.ruth.inventio.repository;

import com.ruth.inventio.entity.VentaDetalle;

public interface VentaDetalleRepository extends BaseRepository<VentaDetalle> {
    @org.springframework.data.jpa.repository.Query("""
            select coalesce(sum(d.cantidad * p.volumen), 0) as volumen,
                   count(case when p.volumen is null then 1 else null end) as sinVolumen
            from VentaDetalle d join d.producto p join d.venta v
            where v.fecha >= :inicio and v.fecha < :fin
              and v.estado <> com.ruth.inventio.model.EstadoVenta.ANULADA
            """)
    DashboardVolumenProjection resumirVolumenDashboard(
            @org.springframework.data.repository.query.Param("inicio") java.time.Instant inicio,
            @org.springframework.data.repository.query.Param("fin") java.time.Instant fin);
}
