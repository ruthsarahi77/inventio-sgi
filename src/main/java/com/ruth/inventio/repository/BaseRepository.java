package com.ruth.inventio.repository;

import java.util.Optional;
import java.util.List;

import com.ruth.inventio.entity.EntidadCreada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;


@NoRepositoryBean
public interface BaseRepository<T extends EntidadCreada> extends Repository<T, Long> {

    <S extends T> S save(S entity);

    Optional<T> findById(Long id);

    Page<T> findAll(Pageable pageable);

    List<T> findAll();

    boolean existsById(Long id);
}
