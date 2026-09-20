package com.ruth.inventio.controller;

import java.util.List;
import com.ruth.inventio.dto.KardexResponse;
import com.ruth.inventio.dto.MovimientoInventarioRequest;
import com.ruth.inventio.dto.MovimientoInventarioResponse;
import com.ruth.inventio.dto.StockProductoResponse;
import com.ruth.inventio.service.InventoryApiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryApiService inventory;

    public InventoryController(InventoryApiService inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    public List<StockProductoResponse> obtenerStockActual() {
        return inventory.obtenerStockActual();
    }

    @GetMapping("/{productId}")
    public StockProductoResponse obtenerStockProducto(@PathVariable Long productId) {
        return inventory.obtenerStockProducto(productId);
    }

    @GetMapping("/kardex/{productId}")
    public KardexResponse obtenerKardexProducto(@PathVariable Long productId) {
        return inventory.obtenerKardexProducto(productId);
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoInventarioResponse registrarEntrada(@Valid @RequestBody MovimientoInventarioRequest request) {
        return inventory.registrarEntrada(request);
    }

    @PostMapping("/adjustments/in")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoInventarioResponse registrarAjusteEntrada(@Valid @RequestBody MovimientoInventarioRequest request) {
        return inventory.registrarAjusteEntrada(request);
    }

    @PostMapping("/adjustments/out")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoInventarioResponse registrarAjusteSalida(@Valid @RequestBody MovimientoInventarioRequest request) {
        return inventory.registrarAjusteSalida(request);
    }
}
