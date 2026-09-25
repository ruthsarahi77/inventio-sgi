import { Stack } from "expo-router";
import { AppProvider, useApp } from "../models/AppContext";
import { ActivityIndicator, Button, Text, View } from "react-native";

function SessionNavigator() {
  const { status, user, sessionError, restoreSession, signOut, colors } = useApp();
  if (status === "loading") return (
    <View style={{ flex: 1, justifyContent: "center", padding: 24, backgroundColor: colors.background }}>
      {sessionError ? <><Text style={{ color: colors.text }}>{sessionError}</Text>
        <Button title="Reintentar" onPress={() => void restoreSession()} />
        <Button title="Ir al login" onPress={signOut} /></> : <ActivityIndicator color="#F58220" />}
    </View>
  );
  const authenticated = status === "authenticated";
  return (
    <>
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Screen name="index" />
        <Stack.Protected guard={!authenticated}>
          <Stack.Screen name="forgot-password" />
          <Stack.Screen name="reset-password" />
        </Stack.Protected>
        <Stack.Protected guard={!authenticated}><Stack.Screen name="login" /></Stack.Protected>
        <Stack.Protected guard={authenticated}>
          <Stack.Screen name="tabs" />
          <Stack.Screen name="producto/[id]" />
          <Stack.Screen name="inventario/[id]" />
          <Stack.Screen name="cliente/[id]" />
          <Stack.Screen name="proforma/[id]" />
          <Stack.Screen name="venta/[id]" />
          <Stack.Screen name="recibo/[id]" />
          <Stack.Screen name="perfil" />
          <Stack.Screen name="mis-ventas" />
          <Stack.Screen name="recibos" />
        </Stack.Protected>
        <Stack.Protected guard={authenticated && user?.role === "admin"}>
          <Stack.Screen name="nuevo-producto" />
        </Stack.Protected>
        <Stack.Protected guard={authenticated && (user?.role === "admin" || user?.role === "vendedor")}>
          <Stack.Screen name="nueva-proforma" />
          <Stack.Screen name="nueva-venta" />
          <Stack.Screen name="nuevo-cliente" />
        </Stack.Protected>
      </Stack>
    </>
  );
}

export default function RootLayout() {
  return (
    <AppProvider>
      <SessionNavigator />
    </AppProvider>
  );
}
