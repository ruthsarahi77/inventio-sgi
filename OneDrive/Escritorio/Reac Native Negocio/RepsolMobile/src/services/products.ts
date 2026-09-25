import { request } from "./http";
import type { EstadoRegistro, ProductoRequest, ProductoResponse } from "../models/api";

export const listProducts = (signal?: AbortSignal) => request<ProductoResponse[]>("/api/products", { signal });
export const getProduct = (id: number, signal?: AbortSignal) => request<ProductoResponse>(`/api/products/${id}`, { signal });
export const createProduct = (body: ProductoRequest) => request<ProductoResponse>("/api/products", { method: "POST", body });
export const updateProduct = (id: number, body: ProductoRequest) => request<ProductoResponse>(`/api/products/${id}`, { method: "PUT", body });
export const setProductStatus = (id: number, estado: EstadoRegistro) => request<ProductoResponse>(`/api/products/${id}/status`, { method: "PATCH", body: { estado } });
