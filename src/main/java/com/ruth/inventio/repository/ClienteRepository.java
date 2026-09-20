package com.ruth.inventio.repository;

import com.ruth.inventio.entity.Cliente;
import java.util.Optional;

public interface ClienteRepository extends BaseRepository<Cliente> {

    Optional<Cliente> findByIdentificacion(String identificacion);

    boolean existsByIdentificacion(String identificacion);
}
