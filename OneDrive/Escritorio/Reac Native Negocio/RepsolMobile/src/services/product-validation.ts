// DTO decimal limits; reject values JS cannot safely represent instead of rounding silently.
export function decimal(value: string, fraction: number, integer: number, allowZero = false): number {
  const normalized = value.trim().replace(",", ".");
  if (!new RegExp(`^\\d{1,${integer}}(?:\\.\\d{1,${fraction}})?$`).test(normalized)) {
    throw new Error(`Ingresa un número con hasta ${fraction} decimales.`);
  }
  const number = Number(normalized);
  if (!Number.isFinite(number) || number < 0 || (!allowZero && number === 0) || number * 10 ** fraction > Number.MAX_SAFE_INTEGER) {
    throw new Error("Cantidad fuera del rango admitido por el móvil.");
  }
  return number;
}

export function resourceId(value: string | string[] | undefined): number {
  if (typeof value !== "string" || !/^\d+$/.test(value) || !Number.isSafeInteger(Number(value)) || Number(value) <= 0) {
    throw new Error("Identificador de producto inválido.");
  }
  return Number(value);
}
