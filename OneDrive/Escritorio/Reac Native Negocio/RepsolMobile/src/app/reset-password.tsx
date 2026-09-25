import PasswordInput from "../components/PasswordInput";
import FormScroll from "../components/FormScroll";
import { useRef, useState } from "react";
import { router, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, Text, TextInput, TouchableOpacity } from "react-native";
import { useApp } from "../models/AppContext";
import { resetPassword } from "../services/auth";
import { recoveryToken } from "../services/recovery";

export default function ResetPasswordScreen() {
  const params = useLocalSearchParams<{ token?: string | string[] }>();
  const { colors, signOut } = useApp();
  const [link, setLink] = useState("");
  const [password, setPassword] = useState("");
  const [confirmation, setConfirmation] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const busy = useRef(false);
  const token = recoveryToken(link || (typeof params.token === "string" ? params.token : ""));
  const submit = async () => {
    if (busy.current) return;
    if (!token) { setError("Abre o pega un enlace de recuperación válido."); return; }
    if (password.trim().length < 12 || password.length > 72) { setError("La contraseña debe tener entre 12 y 72 caracteres."); return; }
    if (password !== confirmation) { setError("Las contraseñas no coinciden."); return; }
    busy.current = true; setLoading(true); setError("");
    try {
      const result = await resetPassword(token, password);
      setPassword(""); setConfirmation(""); setLink("");
      router.setParams({ token: undefined });
      setMessage(result.message);
      await signOut();
      router.dismissAll();
      router.replace({ pathname: "/login", params: { passwordReset: "success" } });
    } catch (cause) { setError(cause instanceof Error ? cause.message : "No se pudo restablecer la contraseña."); }
    finally { busy.current = false; setLoading(false); }
  };
  const inputStyle = { backgroundColor: colors.surface, color: colors.text, padding: 15, borderRadius: 12 };
  return (
    <FormScroll style={{ backgroundColor: colors.background }} contentContainerStyle={{ padding: 25, paddingTop: 70, gap: 18 }} keyboardShouldPersistTaps="handled">
      <Text style={{ color: colors.text, fontSize: 24, fontWeight: "bold" }}>Nueva contraseña</Text>
      {message ? <Text accessibilityRole="alert" style={{ color: colors.text }}>{message}</Text> : <>
        <Text style={{ color: colors.secondary }}>Usa el enlace recibido por correo. La contraseña debe tener 12–72 caracteres, hasta 72 bytes UTF-8, y no ser trivial ni un hash. El servidor valida estas reglas.</Text>
        {!params.token && <TextInput accessibilityLabel="Enlace de recuperación" placeholder="Pega el enlace del correo" placeholderTextColor={colors.secondary}
          value={link} onChangeText={setLink} autoCapitalize="none" autoCorrect={false} secureTextEntry editable={!loading} style={inputStyle} />}
        {params.token && !token ? <Text style={{ color: "#D94343" }}>El enlace no es válido. Solicita uno nuevo.</Text> : null}
        <PasswordInput accessibilityLabel="Nueva contraseña" placeholder="Nueva contraseña" placeholderTextColor={colors.secondary}
          value={password} onChangeText={setPassword} secureTextEntry autoCapitalize="none" autoCorrect={false} maxLength={72} editable={!loading} style={inputStyle} />
        <PasswordInput accessibilityLabel="Confirmar contraseña" placeholder="Confirmar contraseña" placeholderTextColor={colors.secondary}
          value={confirmation} onChangeText={setConfirmation} secureTextEntry autoCapitalize="none" autoCorrect={false} maxLength={72} editable={!loading} style={inputStyle} />
        {!!error && <Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text>}
        <TouchableOpacity disabled={loading} onPress={() => void submit()} style={{ backgroundColor: "#F58220", padding: 16, borderRadius: 12, alignItems: "center" }}>
          {loading ? <ActivityIndicator color="#FFFFFF" /> : <Text style={{ color: "#FFFFFF", fontWeight: "bold" }}>Guardar contraseña</Text>}
        </TouchableOpacity>
        <TouchableOpacity onPress={() => router.replace("/forgot-password")}><Text style={{ color: "#F58220" }}>Solicitar otro enlace</Text></TouchableOpacity>
      </>}
      <TouchableOpacity onPress={() => router.replace("/")}><Text style={{ color: colors.text }}>{message ? "Iniciar sesión" : "Volver"}</Text></TouchableOpacity>
    </FormScroll>
  );
}
