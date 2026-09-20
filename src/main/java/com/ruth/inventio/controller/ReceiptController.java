package com.ruth.inventio.controller;

import java.util.List;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.service.ReceiptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/receipts")
public class ReceiptController {
    private final ReceiptService service;
    public ReceiptController(ReceiptService service) { this.service=service; }

    @GetMapping
    public List<ReciboResponse> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public ReciboResponse obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReciboResponse crear(@Valid @RequestBody ReciboRequest request) { return service.crear(request); }

}
