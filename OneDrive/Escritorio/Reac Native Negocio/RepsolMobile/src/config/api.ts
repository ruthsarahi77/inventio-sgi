export function apiUrl(path: string): string {
  const base = process.env.EXPO_PUBLIC_API_URL?.trim().replace(/\/+$/, "");
  if (!base || !/^https?:\/\//i.test(base)) {
    throw new Error("Configura EXPO_PUBLIC_API_URL con la URL HTTP(S) de Spring Boot, sin /api.");
  }
  return `${base}${path}`;
}
