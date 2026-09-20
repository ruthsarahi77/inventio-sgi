package com.ruth.inventio.controller;

import java.util.List;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.service.QuoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {
    private final QuoteService service;
    public QuoteController(QuoteService service) { this.service=service; }

    @GetMapping
    public List<ProformaResponse> listar() { return service.listar(); }

    @GetMapping("/{id}")
    public ProformaResponse obtener(@PathVariable Long id) { return service.obtener(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProformaResponse crear(@Valid @RequestBody ProformaRequest request) { return service.crear(request); }

    @PatchMapping("/{id}/cancel")
    public ProformaResponse anular(@PathVariable Long id) { return service.anular(id); }

}
