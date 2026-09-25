import { Platform } from "react-native";
import * as SecureStore from "expo-secure-store";

const KEY = "inventio.accessToken";
// Web has no SecureStore: keep its session in memory, never in localStorage.
let webToken: string | null = null;
let pending: Promise<void> = Promise.resolve();

export async function readToken(): Promise<string | null> {
  await pending;
  return Platform.OS === "web" ? webToken : SecureStore.getItemAsync(KEY);
}

export function writeToken(token: string | null): Promise<void> {
  const operation = pending.catch(() => {}).then(async () => {
    if (Platform.OS === "web") { webToken = token; return; }
    if (token === null) await SecureStore.deleteItemAsync(KEY);
    else await SecureStore.setItemAsync(KEY, token, {
      keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
    });
  });
  pending = operation;
  return operation;
}
