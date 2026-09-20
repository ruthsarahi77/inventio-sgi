package com.ruth.inventio.service;

import java.util.List;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.entity.Rol;
import com.ruth.inventio.exception.ReglaNegocioException;
import com.ruth.inventio.repository.RolRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly=true)
public class RoleService {
    private final RolRepository roles;
    public RoleService(RolRepository roles) { this.roles=roles; }
    public List<RolResponse> listar() {
        return roles.findAll().stream().map(r -> new RolResponse(r.getId(),r.getNombre())).toList();
    }
    @Transactional
    public RolResponse crear(RolRequest request) {
        if (roles.existsByNombre(request.nombre())) throw new ReglaNegocioException("El rol ya existe.");
        var rol=new Rol(); rol.setNombre(request.nombre()); rol=roles.save(rol);
        return new RolResponse(rol.getId(),rol.getNombre());
    }
}
