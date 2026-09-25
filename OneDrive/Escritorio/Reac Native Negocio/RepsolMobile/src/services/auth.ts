import type { AuthUserResponse, LoginResponse } from "../models/api";
import { request } from "./http";

export class UnsupportedRoleError extends Error {
  constructor() { super("Esta aplicación solo permite cuentas ADMIN o VENDEDOR."); }
}

export function validateUser(user: AuthUserResponse): AuthUserResponse {
  if (!user || (user.rol !== "ADMIN" && user.rol !== "VENDEDOR")) throw new UnsupportedRoleError();
  if (!Number.isInteger(user.id) || typeof user.nombre !== "string" || typeof user.email !== "string") {
    throw new Error("Respuesta de usuario inválida.");
  }
  return user;
}

export async function login(email: string, password: string) {
  const response = await request<LoginResponse>("/api/auth/login", {
    method: "POST", authenticated: false, body: { email: email.trim(), password },
  });
  validateUser(response.user);
  return response;
}

export async function me() {
  return validateUser(await request<AuthUserResponse>("/api/auth/me"));
}

export function forgotPassword(email: string) {
  return request<{ message: string }>("/api/auth/forgot-password", {
    method: "POST", authenticated: false, body: { email: email.trim() },
  });
}

export function resetPassword(token: string, newPassword: string) {
  return request<{ message: string }>("/api/auth/reset-password", {
    method: "POST", authenticated: false, body: { token, newPassword },
  });
}
