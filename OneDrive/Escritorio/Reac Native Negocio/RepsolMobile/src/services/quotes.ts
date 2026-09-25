import { request } from "./http";
import type { ProformaRequest, ProformaResponse } from "../models/api";

export const listQuotes = (signal?: AbortSignal) => request<ProformaResponse[]>("/api/quotes", { signal });
export const getQuote = (id: number, signal?: AbortSignal) => request<ProformaResponse>(`/api/quotes/${id}`, { signal });
export const createQuote = (body: ProformaRequest) => request<ProformaResponse>("/api/quotes", { method: "POST", body });
export const cancelQuote = (id: number) => request<ProformaResponse>(`/api/quotes/${id}/cancel`, { method: "PATCH" });
export const getQuotePdf = (id: number) => request<Uint8Array>(`/api/proformas/${id}/pdf`, { responseType: "pdf" });
