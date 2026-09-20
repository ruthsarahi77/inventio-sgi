package com.ruth.inventio.repository;

import com.ruth.inventio.entity.MovimientoInventario;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoInventarioRepository extends BaseRepository<MovimientoInventario> {

    String SALDO_EXPRESSION = """
            coalesce(sum(case when m.tipoMovimiento in (
                com.ruth.inventio.model.TipoMovimiento.ENTRADA,
                com.ruth.inventio.model.TipoMovimiento.AJUSTE_ENTRADA)
                then m.cantidad else -m.cantidad end), 0)
            """;

    @Query("select " + SALDO_EXPRESSION + " from MovimientoInventario m where m.producto.id = :productoId")
    BigDecimal calcularStock(@Param("productoId") Long productoId);

    @Query("select max(m.fecha) from MovimientoInventario m where m.producto.id = :productoId")
    Instant obtenerUltimaFecha(@Param("productoId") Long productoId);

    @EntityGraph(attributePaths = "usuario")
    List<MovimientoInventario> findByProductoIdOrderByFechaAscIdAsc(Long productoId);
}
