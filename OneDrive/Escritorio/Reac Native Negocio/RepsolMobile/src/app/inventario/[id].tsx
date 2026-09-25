import { useCallback, useRef, useState } from "react";
import { router, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, Text, TextInput, TouchableOpacity, View } from "react-native";
import ScreenLayout from "../../components/ScreenLayout";
import { useApp } from "../../models/AppContext";
import { useApiResource } from "../../hooks/use-api-resource";
import { getKardex, getStock, registerMovement, type InventoryOperation } from "../../services/inventory";
import { decimal, resourceId } from "../../services/product-validation";

const operations: { value: InventoryOperation; label: string }[] = [
  { value: "entries", label: "Entrada" },
  { value: "adjustments/in", label: "Ajuste de entrada" },
  { value: "adjustments/out", label: "Ajuste de salida" },
];

export default function InventoryDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { colors, user } = useApp();
  const admin = user?.role === "admin";
  const resource = useApiResource(useCallback(async (signal: AbortSignal) => {
    const productId = resourceId(id);
    const [stock, kardex] = await Promise.all([getStock(productId, signal), admin ? getKardex(productId, signal) : Promise.resolve(null)]);
    return { stock, kardex };
  }, [id, admin]));
  const [operation, setOperation] = useState<InventoryOperation>("entries");
  const [quantity, setQuantity] = useState("");
  const [document, setDocument] = useState("");
  const [observation, setObservation] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const busy = useRef(false);
  const save = async () => {
    if (!admin || busy.current) return;
    busy.current = true; setSaving(true); setError(""); setMessage("");
    try {
      const movement = await registerMovement(operation, {
        productoId: resourceId(id), cantidad: decimal(quantity, 3, 16),
        documentoOrigen: document.trim() || null, observacion: observation.trim() || null,
      });
      setQuantity(""); setDocument(""); setObservation("");
      setMessage(`Movimiento ${movement.idMovimiento} registrado. Saldo resultante: ${movement.saldoAcumulado}.`);
      await resource.reload();
    } catch (cause) { setError(cause instanceof Error ? cause.message : "No se pudo registrar el movimiento."); }
    finally { busy.current = false; setSaving(false); }
  };
  const input = { color: colors.text, backgroundColor: colors.surface, borderColor: colors.border, borderWidth: 1, borderRadius: 10, padding: 14 };
  return <ScreenLayout title="Inventario" subtitle="Existencias y movimientos del producto">
    {resource.loading && <ActivityIndicator color="#F58220" />}
    {resource.error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{resource.error}</Text><TouchableOpacity onPress={() => void resource.reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {!!message && <Text accessibilityRole="alert" style={{ color: colors.text, marginVertical: 12 }}>{message}</Text>}
    {resource.data && !resource.loading && !resource.error && <>
      <View style={{ backgroundColor: colors.surface, padding: 20, borderRadius: 14, gap: 12, marginBottom: 20 }}>
        <Text style={{ color: colors.text, fontSize: 20, fontWeight: "bold" }}>{resource.data.stock.nombre}</Text>
        <Text style={{ color: colors.secondary }}>Código: {resource.data.stock.codigo}</Text>
        <Text style={{ color: colors.text }}>Stock: {resource.data.stock.stockActual} {resource.data.stock.unidad ?? ""}</Text>
        <Text style={{ color: colors.text }}>Valor de inventario: {resource.data.stock.valorInventario.toFixed(2)}</Text>
        <TouchableOpacity onPress={() => router.push({ pathname: "/producto/[id]", params: { id: resource.data!.stock.idProducto } })}><Text style={{ color: "#F58220" }}>Ver producto</Text></TouchableOpacity>
      </View>
      {admin && <>
        <View style={{ gap: 12, marginBottom: 25 }}>
          <Text style={{ color: colors.text, fontSize: 18, fontWeight: "bold" }}>Registrar movimiento</Text>
          {operations.map(item => <TouchableOpacity key={item.value} disabled={saving} onPress={() => setOperation(item.value)}
            style={{ borderWidth: 1, borderColor: "#F58220", borderRadius: 10, padding: 12, backgroundColor: operation === item.value ? "#F58220" : colors.surface }}>
            <Text style={{ color: operation === item.value ? "#FFFFFF" : colors.text }}>{item.label}</Text>
          </TouchableOpacity>)}
          <TextInput accessibilityLabel="Cantidad" placeholder="Cantidad (hasta 3 decimales)" placeholderTextColor={colors.secondary} keyboardType="decimal-pad" value={quantity} onChangeText={setQuantity} maxLength={20} editable={!saving} style={input} />
          <TextInput accessibilityLabel="Documento de origen" placeholder="Documento de origen (opcional)" placeholderTextColor={colors.secondary} value={document} onChangeText={setDocument} maxLength={100} editable={!saving} style={input} />
          <TextInput accessibilityLabel="Observación" placeholder="Observación (opcional)" placeholderTextColor={colors.secondary} value={observation} onChangeText={setObservation} maxLength={2000} editable={!saving} style={input} />
          {!!error && <Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text>}
          <Text style={{ color: colors.secondary }}>Si se interrumpe la conexión al guardar, consulta los movimientos antes de repetir la operación.</Text>
          <TouchableOpacity disabled={saving} onPress={() => void save()} style={{ backgroundColor: "#F58220", borderRadius: 12, padding: 16, alignItems: "center" }}>
            {saving ? <ActivityIndicator color="#FFFFFF" /> : <Text style={{ color: "#FFFFFF", fontWeight: "bold" }}>Registrar movimiento</Text>}
          </TouchableOpacity>
        </View>
        <Text style={{ color: colors.text, fontSize: 18, fontWeight: "bold", marginBottom: 15 }}>Kardex</Text>
        {resource.data.kardex?.movimientos.length === 0 && <Text style={{ color: colors.secondary }}>No hay movimientos registrados.</Text>}
        {resource.data.kardex?.movimientos.map(movement => <View key={movement.idMovimiento} style={{ backgroundColor: colors.surface, padding: 16, borderRadius: 12, marginBottom: 12, gap: 8 }}>
          <Text style={{ color: colors.text }}>{movement.tipoMovimiento}: {movement.cantidad}</Text>
          <Text style={{ color: colors.secondary }}>{new Date(movement.fecha).toLocaleString()}</Text>
          <Text style={{ color: colors.text }}>Saldo: {movement.saldoAcumulado}</Text>
          {movement.usuario && <Text style={{ color: colors.text }}>{movement.usuario.nombre}</Text>}
          {movement.documentoOrigen && <Text style={{ color: colors.text }}>{movement.documentoOrigen}</Text>}
          {movement.observacion && <Text style={{ color: colors.text }}>{movement.observacion}</Text>}
        </View>)}
      </>}
    </>}
  </ScreenLayout>;
}
