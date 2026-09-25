import { request } from "./http";
import type { ReciboRequest, ReciboResponse } from "../models/api";

export const listReceipts = (signal?: AbortSignal) => request<ReciboResponse[]>("/api/receipts", { signal });
export const getReceipt = (id: number, signal?: AbortSignal) => request<ReciboResponse>(`/api/receipts/${id}`, { signal });
export const createReceipt = (body: ReciboRequest) => request<ReciboResponse>("/api/receipts", { method: "POST", body });
