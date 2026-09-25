import FormScroll from "../components/FormScroll";
import { useRef, useState } from "react";
import { router } from "expo-router";
import { ActivityIndicator, Text, TextInput, TouchableOpacity } from "react-native";
import { useApp } from "../models/AppContext";
import { forgotPassword } from "../services/auth";

export default function ForgotPasswordScreen() {
  const { colors } = useApp();
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const busy = useRef(false);
  const submit = async () => {
    if (busy.current) return;
    if (!email.trim() || !email.includes("@")) { setError("Ingresa un correo válido."); return; }
    busy.current = true;
    setLoading(true); setError(""); setMessage("");
    try { setMessage((await forgotPassword(email)).message); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "No se pudo solicitar la recuperación."); }
    finally { busy.current = false; setLoading(false); }
  };
  return (
    <FormScroll style={{ backgroundColor: colors.background }} contentContainerStyle={{ padding: 25, paddingTop: 70, gap: 18 }} keyboardShouldPersistTaps="handled">
      <Text style={{ color: colors.text, fontSize: 24, fontWeight: "bold" }}>Recuperar contraseña</Text>
      <Text style={{ color: colors.secondary }}>Ingresa el correo de tu cuenta para solicitar un enlace de recuperación.</Text>
      <TextInput accessibilityLabel="Correo electrónico" placeholder="Correo electrónico" placeholderTextColor={colors.secondary}
        style={{ backgroundColor: colors.surface, color: colors.text, padding: 15, borderRadius: 12 }}
        value={email} onChangeText={setEmail} maxLength={254} keyboardType="email-address" autoCapitalize="none" autoCorrect={false} editable={!loading} />
      {!!error && <Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text>}
      {!!message && <Text accessibilityRole="alert" style={{ color: colors.text }}>{message}</Text>}
      <TouchableOpacity disabled={loading} onPress={() => void submit()} style={{ backgroundColor: "#F58220", padding: 16, borderRadius: 12, alignItems: "center" }}>
        {loading ? <ActivityIndicator color="#FFFFFF" /> : <Text style={{ color: "#FFFFFF", fontWeight: "bold" }}>Solicitar recuperación</Text>}
      </TouchableOpacity>
      <TouchableOpacity onPress={() => router.push("/reset-password")}><Text style={{ color: "#F58220" }}>Ya tengo el enlace de recuperación</Text></TouchableOpacity>
      <TouchableOpacity onPress={() => router.replace("/")}><Text style={{ color: colors.text }}>Volver</Text></TouchableOpacity>
    </FormScroll>
  );
}
