package com.ruth.inventio.service;

import java.util.List;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class InventoryApiService {
    private final InventoryService inventory;
    private final CurrentUser current;
    public InventoryApiService(InventoryService inventory,CurrentUser current) { this.inventory=inventory; this.current=current; }
    public List<StockProductoResponse> obtenerStockActual() { return inventory.obtenerStockActual(); }
    public StockProductoResponse obtenerStockProducto(Long id) { return inventory.obtenerStockProducto(id); }
    public KardexResponse obtenerKardexProducto(Long id) { return inventory.obtenerKardexProducto(id); }
    @PreAuthorize("hasRole('ADMIN')")
    public MovimientoInventarioResponse registrarEntrada(MovimientoInventarioRequest r) { return inventory.registrarEntrada(identificar(r)); }
    @PreAuthorize("hasRole('ADMIN')")
    public MovimientoInventarioResponse registrarAjusteEntrada(MovimientoInventarioRequest r) { return inventory.registrarAjusteEntrada(identificar(r)); }
    @PreAuthorize("hasRole('ADMIN')")
    public MovimientoInventarioResponse registrarAjusteSalida(MovimientoInventarioRequest r) { return inventory.registrarAjusteSalida(identificar(r)); }
    private MovimientoInventarioRequest identificar(MovimientoInventarioRequest r) {
        return new MovimientoInventarioRequest(r.productoId(),r.cantidad(),current.id(),r.documentoOrigen(),r.observacion());
    }
}
