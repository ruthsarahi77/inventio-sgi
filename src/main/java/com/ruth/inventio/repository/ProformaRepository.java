package com.ruth.inventio.repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

import com.ruth.inventio.entity.Proforma;
import java.util.Optional;

public interface ProformaRepository extends BaseRepository<Proforma> {
@Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Proforma e where e.id = :id")
    Optional<Proforma> buscarParaActualizar(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"cliente", "usuario", "detalles", "detalles.producto"})
    Optional<Proforma> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"cliente", "usuario", "detalles", "detalles.producto"})
    List<Proforma> findAll();

    @EntityGraph(attributePaths = {"cliente", "usuario", "detalles", "detalles.producto"})
    List<Proforma> findByUsuarioId(Long usuarioId);

    Optional<Proforma> findByNumero(String numero);

    boolean existsByNumero(String numero);
}
