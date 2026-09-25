import { useRef, useState } from "react";
import { ActivityIndicator, StyleSheet, Text, TextInput, TouchableOpacity, View } from "react-native";
import { useApp } from "../models/AppContext";
import type { ProductoRequest } from "../models/api";
import { decimal } from "../services/product-validation";

const fields = [
  ["nombre", "Nombre *", 200], ["codigo", "Código *", 100],
  ["costoUnitario", "Costo unitario *", 21], ["descripcion", "Descripción", 2000],
  ["presentacion", "Presentación", 100], ["volumen", "Volumen", 20], ["unidad", "Unidad", 30],
] as const;

export default function ProductForm({ initial, onSave, onCancel }: {
  initial?: ProductoRequest;
  onSave: (value: ProductoRequest) => Promise<void>;
  onCancel?: () => void;
}) {
  const { colors, user } = useApp();
  const [values, setValues] = useState({
    nombre: initial?.nombre ?? "", codigo: initial?.codigo ?? "",
    costoUnitario: initial ? String(initial.costoUnitario) : "",
    descripcion: initial?.descripcion ?? "", presentacion: initial?.presentacion ?? "",
    volumen: initial?.volumen == null ? "" : String(initial.volumen), unidad: initial?.unidad ?? "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const busy = useRef(false);
  const save = async () => {
    if (busy.current || user?.role !== "admin") return;
    busy.current = true; setSaving(true); setError("");
    try {
      if (!values.nombre.trim() || !values.codigo.trim()) throw new Error("Nombre y código son obligatorios.");
      await onSave({
        nombre: values.nombre.trim(), codigo: values.codigo.trim(),
        costoUnitario: decimal(values.costoUnitario, 2, 17, true),
        descripcion: values.descripcion.trim() || null, presentacion: values.presentacion.trim() || null,
        volumen: values.volumen.trim() ? decimal(values.volumen, 3, 16) : null,
        unidad: values.unidad.trim() || null,
      });
    } catch (cause) { setError(cause instanceof Error ? cause.message : "No se pudo guardar el producto."); }
    finally { busy.current = false; setSaving(false); }
  };
  return <View style={[styles.form, { backgroundColor: colors.surface }]}>
    {fields.map(([key, label, maxLength]) => <View key={key}>
      <Text style={[styles.label, { color: colors.text }]}>{label}</Text>
      <TextInput accessibilityLabel={label} value={values[key]} maxLength={maxLength}
        editable={!saving && user?.role === "admin"} onChangeText={value => setValues(current => ({ ...current, [key]: value }))}
        keyboardType={key === "costoUnitario" || key === "volumen" ? "decimal-pad" : "default"}
        style={[styles.input, { color: colors.text, borderColor: colors.border }]} />
    </View>)}
    <Text style={{ color: colors.secondary, marginTop: 12 }}>Las existencias se registran por separado en Inventario. El costo no es el precio de venta.</Text>
    {!!error && <Text accessibilityRole="alert" style={styles.error}>{error}</Text>}
    <TouchableOpacity disabled={saving || user?.role !== "admin"} onPress={() => void save()} style={styles.saveButton}>
      {saving ? <ActivityIndicator color="#FFFFFF" /> : <Text style={styles.saveText}>Guardar producto</Text>}
    </TouchableOpacity>
    {onCancel && <TouchableOpacity disabled={saving} onPress={onCancel} style={{ padding: 14 }}><Text style={{ color: colors.text, textAlign: "center" }}>Cancelar</Text></TouchableOpacity>}
  </View>;
}

const styles = StyleSheet.create({
  form: { borderRadius: 15, padding: 20, marginBottom: 25, elevation: 2 },
  label: { fontSize: 14, fontWeight: "bold", marginBottom: 8, marginTop: 15 },
  input: { borderWidth: 1, borderRadius: 10, paddingHorizontal: 12, height: 52, fontSize: 14 },
  saveButton: { backgroundColor: "#F58220", borderRadius: 12, height: 52, justifyContent: "center", alignItems: "center", marginTop: 25 },
  saveText: { color: "#FFFFFF", fontSize: 15, fontWeight: "bold" },
  error: { color: "#D94343", marginTop: 12 },
});
