import { createContext, ReactNode, useContext, useState } from "react";
import * as LocalAuthentication from "expo-local-authentication";

export type UserRole = "admin" | "vendedor";
export type Currency = "PEN" | "USD";
export type ThemeMode = "light" | "dark";

export interface AppUser {
  name: string;
  username: string;
  email: string;
  role: UserRole;
  photoUri?: string;
}

interface AppContextValue {
  user: AppUser | null;
  themeMode: ThemeMode;
  currency: Currency;
  biometricEnabled: boolean;
  signIn: (role: UserRole, credentials?: Partial<AppUser>) => void;
  signOut: () => void;
  updateProfile: (changes: Partial<AppUser>) => void;
  setThemeMode: (mode: ThemeMode) => void;
  setCurrency: (currency: Currency) => void;
  authenticateBiometric: () => Promise<boolean>;
  toggleBiometric: () => Promise<boolean>;
  formatMoney: (amount: number, valueCurrency?: Currency) => string;
  convertToBaseCurrency: (amount: number, amountCurrency: Currency) => number;
  exchangeRate: number;
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

export function AppProvider({ children }: { children: ReactNode }) {
  const exchangeRate = 3.75;
  const [user, setUser] = useState<AppUser | null>(null);
  const [themeMode, setThemeMode] = useState<ThemeMode>("light");
  const [currency, setCurrency] = useState<Currency>("PEN");
  const [biometricEnabled, setBiometricEnabled] = useState(false);
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

  const signIn = (role: UserRole, credentials: Partial<AppUser> = {}) => {
    setUser({
      name: role === "admin" ? "Administrador Inventio" : "Vendedor REPSOL",
      username: role === "admin" ? "admin" : "vendedor",
      email: role === "admin" ? "admin@inventio.pe" : "vendedor@inventio.pe",
      role,
      ...credentials,
    });
  };

  const authenticateBiometric = async () => {
    const compatible = await LocalAuthentication.hasHardwareAsync();
    const enrolled = await LocalAuthentication.isEnrolledAsync();
    if (!compatible || !enrolled) return false;
    const result = await LocalAuthentication.authenticateAsync({
      promptMessage: "Confirma tu identidad para continuar",
      fallbackLabel: "Usar contraseña",
    });
    return result.success;
  };

  const toggleBiometric = async () => {
    if (biometricEnabled) {
      setBiometricEnabled(false);
      return true;
    }
    const authenticated = await authenticateBiometric();
    if (authenticated) setBiometricEnabled(true);
    return authenticated;
  };

  return (
    <AppContext.Provider
      value={{
        user,
        themeMode,
        currency,
        biometricEnabled,
        signIn,
        signOut: () => setUser(null),
        updateProfile: (changes) =>
          setUser((current) =>
            current ? { ...current, ...changes } : current,
          ),
        setThemeMode,
        setCurrency,
        authenticateBiometric,
        toggleBiometric,
        formatMoney: (amount, valueCurrency = currency) => {
          const converted =
            valueCurrency === "USD" ? amount / exchangeRate : amount;
          return `${valueCurrency === "PEN" ? "S/" : "$"} ${converted.toFixed(2)}`;
        },
        convertToBaseCurrency: (amount, amountCurrency) =>
          amountCurrency === "USD" ? amount * exchangeRate : amount,
        exchangeRate,
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
