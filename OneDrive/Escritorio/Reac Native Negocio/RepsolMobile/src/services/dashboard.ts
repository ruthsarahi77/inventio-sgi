import { request } from "./http";
import type { DashboardStatsResponse } from "../models/api";

export function getDashboard(period?: { year: number; month: number }, signal?: AbortSignal) {
  if (period && (!Number.isInteger(period.year) || period.year < 1 || period.year > 9999
    || !Number.isInteger(period.month) || period.month < 1 || period.month > 12)) {
    throw new Error("Introduce un año entre 1 y 9999 y un mes entre 1 y 12.");
  }
  const query = period ? `?year=${period.year}&month=${period.month}` : "";
  return request<DashboardStatsResponse>(`/api/dashboard/stats${query}`, { signal });
}
