package com.ruth.inventio.repository;

import com.ruth.inventio.entity.Rol;
import com.ruth.inventio.model.NombreRol;
import java.util.Optional;

public interface RolRepository extends BaseRepository<Rol> {

    Optional<Rol> findByNombre(NombreRol nombre);

    boolean existsByNombre(NombreRol nombre);
}
