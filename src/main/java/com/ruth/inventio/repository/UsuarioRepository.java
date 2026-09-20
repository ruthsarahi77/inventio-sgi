package com.ruth.inventio.repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

import com.ruth.inventio.entity.Usuario;
import java.util.Optional;

public interface UsuarioRepository extends BaseRepository<Usuario> {
@Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Usuario e where e.id = :id")
    Optional<Usuario> buscarParaActualizar(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = {"roles"})
    Optional<Usuario> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"roles"})
    List<Usuario> findAll();

    @EntityGraph(attributePaths = "roles")
    Optional<Usuario> findByEmailIgnoreCase(String email);

    long count();

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
