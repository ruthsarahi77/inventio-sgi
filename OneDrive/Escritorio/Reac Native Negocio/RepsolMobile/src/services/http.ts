import { apiUrl } from "../config/api";
import type { ApiError } from "../models/api";

let accessToken: string | null = null;
let onUnauthorized: (() => void) | undefined;

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export function setUnauthorizedHandler(handler?: () => void) {
  onUnauthorized = handler;
}

const messages: Record<number, string> = {
  400: "Revisa los datos enviados.",
  401: "Credenciales inválidas o sesión vencida. Inicia sesión nuevamente.",
  403: "No tienes permiso para esta operación.",
  404: "No se encontró el recurso solicitado.",
  409: "La operación tiene un conflicto con los datos actuales.",
  429: "Demasiados intentos. Inténtalo más tarde.",
  500: "Ocurrió un error en el servidor.",
};

export class HttpError extends Error {
  constructor(public readonly status: number, public readonly detail?: ApiError) {
    super(detail?.message || messages[status] || `Error HTTP ${status}.`);
    this.name = "HttpError";
  }
}

type Options = Omit<RequestInit, "body"> & { body?: unknown; authenticated?: boolean; responseType?: "json" | "pdf" };

export async function request<T>(path: string, options: Options = {}): Promise<T> {
  const { body, authenticated = true, responseType = "json", ...init } = options;
  const url = apiUrl(path);
  const token = authenticated ? accessToken : null;
  const headers = new Headers(init.headers);
  headers.set("Accept", responseType === "pdf" ? "application/pdf" : "application/json");
  if (body !== undefined) headers.set("Content-Type", "application/json");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  const controller = new AbortController();
  const abort = () => controller.abort();
  if (init.signal?.aborted) abort();
  init.signal?.addEventListener("abort", abort);
  const timeout = setTimeout(abort, 15000);
  try {
    const response = await fetch(url, {
      ...init, headers, credentials: "omit", signal: controller.signal,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    // A late response from a previous session must not log out a new account.
    if (response.status === 401 && token && token === accessToken) {
      accessToken = null;
      onUnauthorized?.();
    }
    if (response.ok && responseType === "pdf") {
      if (!response.headers.get("Content-Type")?.toLowerCase().startsWith("application/pdf")) {
        throw new Error("El servidor no devolvió un PDF válido.");
      }
      const bytes = new Uint8Array(await response.arrayBuffer());
      if (bytes.length < 5 || String.fromCharCode(...bytes.slice(0, 5)) !== "%PDF-") {
        throw new Error("El documento PDF recibido está vacío o no es válido.");
      }
      return bytes as T;
    }
    const raw = await response.text();
    let data: unknown;
    try { data = raw ? JSON.parse(raw) : undefined; } catch {
      if (response.ok) throw new Error("El servidor no devolvió JSON válido.");
    }
    if (!response.ok) {
      const detail = data && typeof data === "object" && "message" in data
        && typeof data.message === "string" ? data as ApiError : undefined;
      throw new HttpError(response.status, detail);
    }
    return data as T;
  } catch (error) {
    if (controller.signal.aborted && !init.signal?.aborted) {
      throw new Error("El servidor tardó demasiado en responder. Inténtalo nuevamente.");
    }
    if (error instanceof TypeError) {
      throw new Error("No se pudo conectar con Spring Boot. Revisa la conexión y la URL de la API.");
    }
    throw error;
  } finally {
    clearTimeout(timeout);
    init.signal?.removeEventListener("abort", abort);
  }
}
