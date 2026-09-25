import { request } from "./http";
import type { ClienteRequest, ClienteResponse } from "../models/api";

export const listCustomers = (signal?: AbortSignal) => request<ClienteResponse[]>("/api/customers", { signal });
export const getCustomer = (id: number, signal?: AbortSignal) => request<ClienteResponse>(`/api/customers/${id}`, { signal });
export const createCustomer = (body: ClienteRequest) => request<ClienteResponse>("/api/customers", { method: "POST", body });
export const updateCustomer = (id: number, body: ClienteRequest) => request<ClienteResponse>(`/api/customers/${id}`, { method: "PUT", body });
