import { request } from "./http";
import type { VentaRequest, VentaResponse } from "../models/api";

export const listSales = (signal?: AbortSignal) => request<VentaResponse[]>("/api/sales", { signal });
export const getSale = (id: number, signal?: AbortSignal) => request<VentaResponse>(`/api/sales/${id}`, { signal });
export const createSale = (body: VentaRequest) => request<VentaResponse>("/api/sales", { method: "POST", body });
export const cancelSale = (id: number) => request<VentaResponse>(`/api/sales/${id}/cancel`, { method: "PATCH" });
