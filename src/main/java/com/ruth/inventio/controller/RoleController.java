package com.ruth.inventio.controller;

import java.util.List;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.service.RoleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
public class RoleController {
    private final RoleService service;
    public RoleController(RoleService service) { this.service=service; }

    @GetMapping
    public List<RolResponse> listar() { return service.listar(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolResponse crear(@Valid @RequestBody RolRequest request) { return service.crear(request); }

}
