import { useState } from "react";
import { router } from "expo-router";
import { ActivityIndicator, Text, TextInput, TouchableOpacity, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import ScreenLayout from "../../components/ScreenLayout";
import { useApp } from "../../models/AppContext";
import { useApiResource } from "../../hooks/use-api-resource";
import { listProducts } from "../../services/products";

export default function ProductsScreen() {
  const { colors, user } = useApp();
  const [search, setSearch] = useState("");
  const { data, error, loading, reload } = useApiResource(listProducts);
  const products = data?.filter(product => `${product.nombre} ${product.codigo}`.toLowerCase().includes(search.toLowerCase()));
  return <ScreenLayout title="Productos" subtitle="Consulta y administra el catálogo">
    {user?.role === "admin" && <TouchableOpacity style={{ backgroundColor: "#F58220", borderRadius: 12, padding: 16, marginBottom: 20 }} onPress={() => router.push("/nuevo-producto")}>
      <Text style={{ color: "#FFFFFF", fontWeight: "bold", textAlign: "center" }}>Nuevo producto</Text>
    </TouchableOpacity>}
    {loading && <ActivityIndicator color="#F58220" />}
    {error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text><TouchableOpacity onPress={() => void reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {!loading && !error && data && <>
      <TextInput accessibilityLabel="Buscar producto" placeholder="Buscar producto..." placeholderTextColor={colors.secondary} value={search} onChangeText={setSearch}
        style={{ backgroundColor: colors.surface, borderColor: colors.border, color: colors.text, borderWidth: 1, borderRadius: 12, padding: 15, marginBottom: 20 }} />
      <Text style={{ color: colors.text, marginBottom: 15 }}>Productos registrados: {data.length}</Text>
      {products?.map(product => <TouchableOpacity key={product.id} onPress={() => router.push({ pathname: "/producto/[id]", params: { id: product.id } })}
        style={{ backgroundColor: colors.surface, padding: 20, marginBottom: 15, borderRadius: 14, gap: 10 }}>
        <Ionicons name="cube-outline" size={25} color="#F58220" />
        <Text style={{ color: colors.text, fontSize: 18, fontWeight: "bold" }}>{product.nombre}</Text>
        <Text style={{ color: colors.secondary }}>Código: {product.codigo}</Text>
        <Text style={{ color: colors.text }}>Costo unitario: {product.costoUnitario.toFixed(2)}</Text>
        <Text style={{ color: colors.secondary }}>{product.estado}</Text>
      </TouchableOpacity>)}
      {products?.length === 0 && <Text style={{ color: colors.secondary }}>{data.length === 0 ? "No hay productos registrados." : "No se encontraron productos."}</Text>}
    </>}

    <View style={{ height: 12 }} />
  </ScreenLayout>;
}
