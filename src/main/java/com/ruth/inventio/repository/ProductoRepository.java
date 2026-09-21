package com.ruth.inventio.repository;

import com.ruth.inventio.entity.Producto;
import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends BaseRepository<Producto> {
    Long countByEstado(com.ruth.inventio.model.EstadoRegistro estado);

    Optional<Producto> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> buscarParaActualizar(@Param("id") Long id);

    String STOCK_SELECT = """
            select p.id as idProducto, p.codigo as codigo, p.nombre as nombre,
                   p.presentacion as presentacion, p.unidad as unidad,
                   p.costoUnitario as costoUnitario,
            """ + MovimientoInventarioRepository.SALDO_EXPRESSION + " as stockActual ";

    String STOCK_GROUP = """
             group by p.id, p.codigo, p.nombre, p.presentacion, p.unidad, p.costoUnitario
            """;

    @Query(STOCK_SELECT + "from Producto p left join p.movimientos m" + STOCK_GROUP + " order by p.id")
    List<StockProductoProjection> consultarStocks();

    @Query(STOCK_SELECT + "from Producto p left join p.movimientos m where p.id = :id" + STOCK_GROUP)
    Optional<StockProductoProjection> consultarStock(@Param("id") Long id);
}
