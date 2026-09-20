package com.ruth.inventio.controller;

import java.util.List;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service=service; }

    @GetMapping
    public List<ProductoResponse> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) { return service.crear(request); }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable Long id,@Valid @RequestBody ProductoRequest request) {
        return service.actualizar(id,request);
    }

    @PatchMapping("/{id}/status")
    public ProductoResponse estado(@PathVariable Long id,@Valid @RequestBody EstadoRequest request) {
        return service.estado(id,request);
    }

}
