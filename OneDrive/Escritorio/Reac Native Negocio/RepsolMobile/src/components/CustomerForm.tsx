import { useRef, useState } from "react";
import { ActivityIndicator, Text, TextInput, TouchableOpacity, View } from "react-native";
import { useApp } from "../models/AppContext";
import type { ClienteRequest } from "../models/api";

const fields = [["nombre", "Nombre *", 200], ["identificacion", "Identificación *", 50],
  ["telefono", "Teléfono", 30], ["email", "Correo", 254], ["direccion", "Dirección", 500]] as const;

export default function CustomerForm({ initial, onSave, onCancel }: {
  initial?: ClienteRequest; onSave: (value: ClienteRequest) => Promise<void>; onCancel: () => void;
}) {
  const { colors, user } = useApp();
  const allowed = user?.role === "admin" || user?.role === "vendedor";
  const [values, setValues] = useState({ nombre: initial?.nombre ?? "", identificacion: initial?.identificacion ?? "",
    telefono: initial?.telefono ?? "", email: initial?.email ?? "", direccion: initial?.direccion ?? "" });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const busy = useRef(false);
  const save = async () => {
    if (!allowed || busy.current) return;
    busy.current = true; setSaving(true); setError("");
    try {
      if (!values.nombre.trim() || !values.identificacion.trim()) throw new Error("Nombre e identificación son obligatorios.");
      await onSave({ nombre: values.nombre.trim(), identificacion: values.identificacion.trim(),
        telefono: values.telefono.trim() || null, email: values.email.trim() || null, direccion: values.direccion.trim() || null });
    } catch (cause) { setError(cause instanceof Error ? cause.message : "No se pudo guardar el cliente."); }
    finally { busy.current = false; setSaving(false); }
  };
  return <View style={{ backgroundColor: colors.surface, padding: 20, borderRadius: 14, gap: 12 }}>
    {fields.map(([key, label, maxLength]) => <View key={key} style={{ gap: 8 }}>
      <Text style={{ color: colors.text, fontWeight: "bold" }}>{label}</Text>
      <TextInput accessibilityLabel={label} value={values[key]} onChangeText={value => setValues(current => ({ ...current, [key]: value }))}
        maxLength={maxLength} editable={!saving && allowed} autoCapitalize={key === "email" ? "none" : "sentences"}
        keyboardType={key === "email" ? "email-address" : key === "telefono" ? "phone-pad" : "default"}
        style={{ color: colors.text, borderColor: colors.border, borderWidth: 1, borderRadius: 10, padding: 14 }} />
    </View>)}
    {!!error && <Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text>}
    <TouchableOpacity disabled={saving || !allowed} onPress={() => void save()} style={{ backgroundColor: "#F58220", borderRadius: 12, padding: 16, alignItems: "center" }}>
      {saving ? <ActivityIndicator color="#FFFFFF" /> : <Text style={{ color: "#FFFFFF", fontWeight: "bold" }}>Guardar cliente</Text>}
    </TouchableOpacity>
    <TouchableOpacity disabled={saving} onPress={onCancel}><Text style={{ color: colors.text, textAlign: "center" }}>Cancelar</Text></TouchableOpacity>
  </View>;
}
