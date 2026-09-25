import { useCallback, useState } from "react";
import { router, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, Text, TouchableOpacity, View } from "react-native";
import ScreenLayout from "../../components/ScreenLayout";
import CustomerForm from "../../components/CustomerForm";
import { useApiResource } from "../../hooks/use-api-resource";
import { useApp } from "../../models/AppContext";
import { getCustomer, updateCustomer } from "../../services/customers";

export default function CustomerDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { colors, user } = useApp();
  const allowed = user?.role === "admin" || user?.role === "vendedor";
  const [editing, setEditing] = useState(false);
  const { data, loading, error, reload } = useApiResource(useCallback(async (signal: AbortSignal) => {
    if (!/^\d+$/.test(id) || !Number.isSafeInteger(Number(id)) || Number(id) <= 0) throw new Error("Cliente inválido.");
    return getCustomer(Number(id), signal);
  }, [id]));
  return <ScreenLayout title="Cliente" subtitle="Información del cliente">
    {loading && <ActivityIndicator color="#F58220" />}
    {error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text><TouchableOpacity onPress={() => void reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {data && !loading && !error && (editing && allowed ? <CustomerForm key={data.id} initial={data} onCancel={() => setEditing(false)} onSave={async value => {
      await updateCustomer(data.id, value); setEditing(false); await reload();
    }} /> : <View style={{ backgroundColor: colors.surface, padding: 20, borderRadius: 14, gap: 16 }}>
      <Text style={{ color: colors.text, fontSize: 20, fontWeight: "bold" }}>{data.nombre}</Text>
      <Text style={{ color: colors.text }}>Identificación: {data.identificacion}</Text>
      <Text style={{ color: colors.text }}>Estado: {data.estado}</Text>
      <Text style={{ color: colors.text }}>Teléfono: {data.telefono || "No registrado"}</Text>
      <Text style={{ color: colors.text }}>Correo: {data.email || "No registrado"}</Text>
      <Text style={{ color: colors.text }}>Dirección: {data.direccion || "No registrada"}</Text>
      {allowed && <TouchableOpacity onPress={() => setEditing(true)}><Text style={{ color: "#F58220" }}>Editar cliente</Text></TouchableOpacity>}
      {allowed && data.estado === "ACTIVO" && <TouchableOpacity onPress={() => router.push({ pathname: "/nueva-proforma", params: { clienteId: data.id } })}><Text style={{ color: "#F58220" }}>Crear proforma para este cliente</Text></TouchableOpacity>}
    </View>)}
  </ScreenLayout>;
}
