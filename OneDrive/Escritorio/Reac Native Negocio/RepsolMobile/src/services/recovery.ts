// ResetPasswordRequest requires exactly 43 base64url characters.
export function recoveryToken(value: string): string | null {
  const input = value.trim();
  if (/^[A-Za-z0-9_-]{43}$/.test(input)) return input;
  try {
    const values = new URL(input).searchParams.getAll("token");
    return values.length === 1 && /^[A-Za-z0-9_-]{43}$/.test(values[0]) ? values[0] : null;
  } catch { return null; }
}
