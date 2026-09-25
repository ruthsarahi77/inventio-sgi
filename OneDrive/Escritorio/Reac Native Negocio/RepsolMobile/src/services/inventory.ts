import { request } from "./http";
import type { KardexResponse, MovimientoInventarioRequest, MovimientoInventarioResponse, StockProductoResponse } from "../models/api";

export const listInventory = (signal?: AbortSignal) => request<StockProductoResponse[]>("/api/inventory", { signal });
export const getStock = (id: number, signal?: AbortSignal) => request<StockProductoResponse>(`/api/inventory/${id}`, { signal });
export const getKardex = (id: number, signal?: AbortSignal) => request<KardexResponse>(`/api/inventory/kardex/${id}`, { signal });
export type InventoryOperation = "entries" | "adjustments/in" | "adjustments/out";
export const registerMovement = (operation: InventoryOperation, body: MovimientoInventarioRequest) =>
  request<MovimientoInventarioResponse>(`/api/inventory/${operation}`, { method: "POST", body });
