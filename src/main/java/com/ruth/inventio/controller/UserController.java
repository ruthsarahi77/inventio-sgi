package com.ruth.inventio.controller;

import java.util.List;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/users", "/api/usuarios"})
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service=service; }

    @GetMapping
    public List<UsuarioResponse> listar() { return service.listar(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) { return service.crear(request); }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id,@Valid @RequestBody UsuarioUpdateRequest request) {
        return service.actualizar(id,request);
    }

    @PatchMapping({"/{id}/status", "/{id}/estado"})
    public UsuarioResponse estado(@PathVariable Long id,@Valid @RequestBody EstadoRequest request) {
        return service.estado(id,request);
    }

}
