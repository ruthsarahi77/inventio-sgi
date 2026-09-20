package com.ruth.inventio.controller;

import java.util.List;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.service.SalesService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
public class SalesController {
    private final SalesService service;
    public SalesController(SalesService service) { this.service=service; }

    @GetMapping
    public List<VentaResponse> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public VentaResponse obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaResponse crear(@Valid @RequestBody VentaRequest request) { return service.crear(request); }

    @PatchMapping("/{id}/cancel")
    public VentaResponse anular(@PathVariable Long id) { return service.anular(id); }

}
