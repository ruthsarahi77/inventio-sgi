import { useState } from "react";
import { router } from "expo-router";
import { ActivityIndicator, Text, TextInput, TouchableOpacity, View } from "react-native";
import ScreenLayout from "../../components/ScreenLayout";
import { useApp } from "../../models/AppContext";
import { useApiResource } from "../../hooks/use-api-resource";
import { listCustomers } from "../../services/customers";

export default function CustomersScreen() {
  const { colors, user } = useApp();
  const [search, setSearch] = useState("");
  const { data, loading, error, reload } = useApiResource(listCustomers);
  const filtered = data?.filter(item => [item.nombre, item.identificacion, item.email ?? ""].join(" ").toLowerCase().includes(search.toLowerCase()));
  return <ScreenLayout title="Clientes" subtitle="Consulta y administra los clientes">
    {(user?.role === "admin" || user?.role === "vendedor") && <TouchableOpacity onPress={() => router.push("/nuevo-cliente")} style={{ backgroundColor: "#F58220", padding: 16, borderRadius: 12, marginBottom: 20 }}>
      <Text style={{ color: "#FFFFFF", fontWeight: "bold", textAlign: "center" }}>Nuevo cliente</Text>
    </TouchableOpacity>}
    {loading && <ActivityIndicator color="#F58220" />}
    {error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text><TouchableOpacity onPress={() => void reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {data && !loading && !error && <>
      <TextInput accessibilityLabel="Filtrar clientes" placeholder="Buscar nombre, identificación o correo" placeholderTextColor={colors.secondary} value={search} onChangeText={setSearch}
        style={{ color: colors.text, backgroundColor: colors.surface, padding: 15, borderRadius: 12, borderColor: colors.border, borderWidth: 1, marginBottom: 20 }} />
      <Text style={{ color: colors.text, marginBottom: 15 }}>Clientes registrados: {data.length}</Text>
      {filtered?.map(item => <TouchableOpacity key={item.id} onPress={() => router.push({ pathname: "/cliente/[id]", params: { id: item.id } })}
        style={{ backgroundColor: colors.surface, padding: 20, borderRadius: 14, marginBottom: 15, gap: 10 }}>
        <Text style={{ color: colors.text, fontSize: 18, fontWeight: "bold" }}>{item.nombre}</Text>
        <Text style={{ color: colors.secondary }}>{item.identificacion}</Text>
        <Text style={{ color: colors.secondary }}>{item.estado}</Text>
      </TouchableOpacity>)}
      {filtered?.length === 0 && <Text style={{ color: colors.secondary }}>{data.length ? "No se encontraron clientes." : "No hay clientes registrados."}</Text>}
    </>}

    <View style={{ height: 12 }} />
  </ScreenLayout>;
}
