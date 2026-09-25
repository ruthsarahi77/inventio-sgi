import { createContext, ReactNode, useCallback, useContext, useEffect, useRef, useState } from "react";
import { AppState } from "react-native";
import * as auth from "../services/auth";
import { HttpError, setAccessToken, setUnauthorizedHandler } from "../services/http";
import { readToken, writeToken } from "../storage/session";
import type { AuthUserResponse } from "./api";

export type UserRole = "admin" | "vendedor";
export type ThemeMode = "light" | "dark";

export interface AppUser {
  id: number;
  name: string;
  email: string;
  role: UserRole;
}

interface AppContextValue {
  status: "loading" | "authenticated" | "unauthenticated";
  sessionError: string | null;
  restoreSession: () => Promise<void>;
  user: AppUser | null;
  themeMode: ThemeMode;
  signIn: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  setThemeMode: (mode: ThemeMode) => void;
  colors: {
    background: string;
    surface: string;
    text: string;
    secondary: string;
    border: string;
    mutedSurface: string;
  };
}

const AppContext = createContext<AppContextValue | undefined>(undefined);

function appUser(value: AuthUserResponse): AppUser {
  const roles = { ADMIN: "admin", VENDEDOR: "vendedor" } as const;
  if (!value || !roles[value.rol] || !Number.isInteger(value.id)) {
    throw new Error("La API devolvió un usuario o rol no válido.");
  }
  return { id: value.id, name: value.nombre, email: value.email, role: roles[value.rol] };
}

export function AppProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AppUser | null>(null);
  const [status, setStatus] = useState<AppContextValue["status"]>("loading");
  const [sessionError, setSessionError] = useState<string | null>(null);
  const generation = useRef(0);
  const [themeMode, setThemeMode] = useState<ThemeMode>("light");
  const colors =
    themeMode === "dark"
      ? {
          background: "#17191C",
          surface: "#23262B",
          text: "#F5F5F5",
          secondary: "#B0B4BA",
          border: "#363A42",
          mutedSurface: "#3A2A1E",
        }
      : {
          background: "#F6F7F8",
          surface: "#FFFFFF",
          text: "#202124",
          secondary: "#6B7280",
          border: "#E5E7EB",
          mutedSurface: "#FFF0E2",
        };

  const signOut = useCallback(async () => {
    generation.current++;
    setAccessToken(null);
    setUser(null);
    setStatus("unauthenticated");
    setSessionError(null);
    await writeToken(null).catch(() => setSessionError("No se pudo eliminar la sesión del almacenamiento seguro. Intenta cerrar sesión nuevamente."));
  }, []);

  const restoreSession = useCallback(async () => {
    const attempt = ++generation.current;
    try {
      const token = await readToken();
      if (attempt !== generation.current) return;
      setSessionError(null);
      if (!token) { setStatus("unauthenticated"); return; }
      setAccessToken(token);
      const restored = appUser(await auth.me());
      if (attempt !== generation.current) return;
      setUser(restored);
      setStatus("authenticated");
    } catch (error) {
      if (attempt !== generation.current) return;
      if ((error instanceof HttpError && error.status === 401) || error instanceof auth.UnsupportedRoleError) {
        signOut();
        if (error instanceof auth.UnsupportedRoleError) setSessionError(error.message);
        return;
      }
      setAccessToken(null);
      // A network/server failure must not erase a potentially valid stored token.
      setSessionError(error instanceof Error ? error.message : "No se pudo validar la sesión.");
    }
  }, [signOut]);

  useEffect(() => {
    const lifecycle = generation;
    let active = true;
    setUnauthorizedHandler(signOut);
    // Start after mounting; a discarded Strict Mode mount must not restore a session.
    void Promise.resolve().then(() => { if (active) return restoreSession(); });
    return () => { active = false; lifecycle.current++; setUnauthorizedHandler(); setAccessToken(null); };
  }, [restoreSession, signOut]);

  useEffect(() => {
    const subscription = AppState.addEventListener("change", (next) => {
      if (next === "active" && status === "authenticated") {
        setStatus("loading");
        void restoreSession();
      }
    });
    return () => subscription.remove();
  }, [status, restoreSession]);

  const signIn = async (email: string, password: string) => {
    const attempt = ++generation.current;
    setSessionError(null);
    const result = await auth.login(email, password);
    if (attempt !== generation.current) return;
    const signedIn = appUser(result.user);
    if (!result.accessToken || result.tokenType !== "Bearer") throw new Error("Respuesta de autenticación inválida.");
    await writeToken(result.accessToken);
    if (attempt !== generation.current) return;
    setAccessToken(result.accessToken);
    setUser(signedIn);
    setStatus("authenticated");
  };

  return (
    <AppContext.Provider
      value={{
        status,
        sessionError,
        restoreSession,
        user,
        themeMode,
        signIn,
        signOut,
        setThemeMode,
        colors,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const context = useContext(AppContext);
  if (!context) throw new Error("useApp debe utilizarse dentro de AppProvider");
  return context;
}
