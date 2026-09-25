import { useCallback, useRef, useState } from "react";
import { router, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, Text, TouchableOpacity, View } from "react-native";
import ScreenLayout from "../../components/ScreenLayout";
import ProductForm from "../../components/ProductForm";
import { useApp } from "../../models/AppContext";
import { useApiResource } from "../../hooks/use-api-resource";
import { getProduct, setProductStatus, updateProduct } from "../../services/products";
import { resourceId } from "../../services/product-validation";

export default function ProductDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { colors, user } = useApp();
  const resource = useApiResource(useCallback(async (signal: AbortSignal) => getProduct(resourceId(id), signal), [id]));
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const busy = useRef(false);
  const product = resource.data;
  const changeStatus = async () => {
    if (!product || busy.current || user?.role !== "admin") return;
    busy.current = true; setSaving(true); setError("");
    try { await setProductStatus(product.id, product.estado === "ACTIVO" ? "INACTIVO" : "ACTIVO"); await resource.reload(); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "No se pudo cambiar el estado."); }
    finally { busy.current = false; setSaving(false); }
  };
  return <ScreenLayout title="Producto" subtitle="Detalle del catálogo">
    {resource.loading && <ActivityIndicator color="#F58220" />}
    {resource.error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{resource.error}</Text><TouchableOpacity onPress={() => void resource.reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {product && !resource.loading && !resource.error && (editing && user?.role === "admin" ?
      <ProductForm key={product.id} initial={product} onCancel={() => setEditing(false)} onSave={async value => {
        await updateProduct(product.id, value); setEditing(false); await resource.reload();
      }} /> : <View style={{ backgroundColor: colors.surface, padding: 20, borderRadius: 14, gap: 16 }}>
        <Text style={{ color: colors.text, fontSize: 20, fontWeight: "bold" }}>{product.nombre}</Text>
        <Text style={{ color: colors.text }}>Código: {product.codigo}</Text>
        <Text style={{ color: colors.text }}>Estado: {product.estado}</Text>
        <Text style={{ color: colors.text }}>Costo unitario: {product.costoUnitario.toFixed(2)}</Text>
        <Text style={{ color: colors.text }}>Descripción: {product.descripcion ?? "No especificada"}</Text>
        <Text style={{ color: colors.text }}>Presentación: {product.presentacion ?? "No especificada"}</Text>
        <Text style={{ color: colors.text }}>Volumen: {product.volumen ?? "No especificado"}</Text>
        <Text style={{ color: colors.text }}>Unidad: {product.unidad ?? "No especificada"}</Text>
        <TouchableOpacity disabled={saving} onPress={() => router.push({ pathname: "/inventario/[id]", params: { id: product.id } })}><Text style={{ color: "#F58220" }}>Ver existencias</Text></TouchableOpacity>
        {user?.role === "admin" && <>
          <TouchableOpacity disabled={saving} onPress={() => { setError(""); setEditing(true); }}><Text style={{ color: "#F58220" }}>Editar producto</Text></TouchableOpacity>
          <TouchableOpacity disabled={saving} onPress={() => void changeStatus()}><Text style={{ color: "#F58220" }}>{product.estado === "ACTIVO" ? "Desactivar" : "Activar"} producto</Text></TouchableOpacity>
        </>}
      </View>)}
    {saving && <ActivityIndicator color="#F58220" />}
    {!!error && <Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text>}
  </ScreenLayout>;
}
