package com.ruth.inventio.service;

import java.util.List;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.entity.Cliente;
import com.ruth.inventio.exception.*;
import com.ruth.inventio.mapper.ComercialMapper;
import com.ruth.inventio.repository.ClienteRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class CustomerService {
    private final ClienteRepository clientes;
    public CustomerService(ClienteRepository clientes) { this.clientes=clientes; }
    public List<ClienteResponse> listar() { return clientes.findAll().stream().map(ComercialMapper::cliente).toList(); }
    public ClienteResponse obtener(Long id) { return ComercialMapper.cliente(buscar(id)); }
    @Transactional @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ClienteResponse crear(ClienteRequest r) { return guardar(new Cliente(),r); }
    @Transactional @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ClienteResponse actualizar(Long id,ClienteRequest r) { return guardar(buscar(id),r); }
    private Cliente buscar(Long id) { return clientes.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: "+id)); }
    private ClienteResponse guardar(Cliente c,ClienteRequest r) {
        clientes.findByIdentificacion(r.identificacion().trim()).filter(e -> !e.getId().equals(c.getId()))
                .ifPresent(e -> { throw new ReglaNegocioException("Identificacion de cliente duplicada."); });
        c.setIdentificacion(r.identificacion().trim()); c.setNombre(r.nombre().trim());
        c.setTelefono(r.telefono()); c.setEmail(r.email()); c.setDireccion(r.direccion());
        return ComercialMapper.cliente(clientes.save(c));
    }
}
