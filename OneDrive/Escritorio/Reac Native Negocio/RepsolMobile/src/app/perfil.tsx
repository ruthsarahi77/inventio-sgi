import { View, Text, StyleSheet, TouchableOpacity, Switch, ActivityIndicator } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import ScreenLayout from "../components/ScreenLayout";
import { useApp } from "../models/AppContext";
import { useApiResource } from "../hooks/use-api-resource";
import { me } from "../services/auth";

export default function PerfilScreen() {
  const { themeMode, setThemeMode, signOut, colors } = useApp();
  const { data, loading, error, reload } = useApiResource(me);
  return <ScreenLayout title="Mi perfil" subtitle="Información de tu cuenta">
    {loading && <ActivityIndicator color="#F58220" />}
    {error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text><TouchableOpacity onPress={() => void reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {data && !loading && !error && <>
      <View style={[styles.profileCard, { backgroundColor: colors.surface }]}>
        <View style={styles.avatar}><Text style={styles.avatarText}>{data.nombre.charAt(0)}</Text></View>
        <Text style={[styles.name, { color: colors.text }]}>{data.nombre}</Text>
        <Text style={styles.role}>{data.rol === "ADMIN" ? "Administrador" : "Vendedor"}</Text>
      </View>
      <Text style={[styles.sectionTitle, { color: colors.text }]}>Información personal</Text>
      <View style={[styles.infoCard, { backgroundColor: colors.surface }]}>
        <Text style={styles.label}>Nombre completo</Text><Text style={[styles.value, { color: colors.text }]}>{data.nombre}</Text>
        <View style={styles.divider} /><Text style={styles.label}>Correo electrónico</Text><Text style={[styles.value, { color: colors.text }]}>{data.email}</Text>
        <View style={styles.divider} /><Text style={styles.label}>Rol</Text><Text style={[styles.value, { color: colors.text }]}>{data.rol}</Text>
      </View>
    </>}
    <View style={[styles.settingsCard, { backgroundColor: colors.surface }]}><View style={styles.settingRow}><Ionicons name="moon-outline" size={21} color="#F58220" /><Text style={[styles.settingText, { color: colors.text }]}>Tema oscuro</Text><Switch value={themeMode === "dark"} onValueChange={value => setThemeMode(value ? "dark" : "light")} /></View></View>
    <TouchableOpacity style={styles.logoutButton} onPress={() => void signOut()}><Ionicons name="log-out-outline" size={20} color="#D94343" /><Text style={styles.logoutText}>Cerrar sesión</Text></TouchableOpacity>
  </ScreenLayout>;
}
const styles = StyleSheet.create({
  profileCard: {
    backgroundColor: "#FFFFFF",
    borderRadius: 16,
    padding: 25,
    alignItems: "center",
    marginBottom: 25,
    elevation: 2,
  },

  avatar: {
    width: 85,
    height: 85,
    borderRadius: 45,
    backgroundColor: "#FFF0E2",
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 15,
  },

  avatarText: {
    fontSize: 36,
    fontWeight: "bold",
    color: "#F58220",
  },

  avatarImage: { width: 85, height: 85, borderRadius: 45 },
  camera: {
    position: "absolute",
    right: -2,
    bottom: 0,
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: "#F58220",
    alignItems: "center",
    justifyContent: "center",
  },

  name: {
    fontSize: 20,
    fontWeight: "bold",
    color: "#222222",
  },

  role: {
    fontSize: 14,
    color: "#F58220",
    marginTop: 6,
  },

  accessBadge: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    paddingHorizontal: 10,
    paddingVertical: 7,
    borderRadius: 10,
    marginTop: 10,
  },
  accessBadgeText: { color: "#F58220", fontSize: 12, fontWeight: "700" },

  sectionTitle: {
    fontSize: 19,
    fontWeight: "bold",
    color: "#222222",
    marginBottom: 15,
  },

  infoCard: {
    backgroundColor: "#FFFFFF",
    borderRadius: 14,
    padding: 20,
    elevation: 2,
  },

  label: {
    fontSize: 12,
    color: "#888888",
  },

  value: {
    fontSize: 15,
    fontWeight: "600",
    color: "#333333",
    marginTop: 6,
  },

  input: {
    borderBottomWidth: 1,
    borderBottomColor: "#F58220",
    paddingVertical: 4,
    fontSize: 15,
    color: "#333333",
  },

  editButton: {
    height: 50,
    borderRadius: 12,
    backgroundColor: "#F58220",
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    marginTop: 18,
  },
  editText: { color: "#FFFFFF", fontWeight: "bold", fontSize: 15 },
  settingsCard: {
    backgroundColor: "#FFFFFF",
    borderRadius: 14,
    padding: 16,
    elevation: 2,
  },
  settingRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    paddingVertical: 10,
  },
  settingText: { flex: 1, color: "#333333", fontSize: 14, fontWeight: "600" },
  currencyRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: "#EEEEEE",
  },
  currencyOptions: { flexDirection: "row", gap: 8 },
  currency: {
    paddingHorizontal: 12,
    paddingVertical: 7,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "#F58220",
  },
  currencyActive: { backgroundColor: "#F58220" },
  currencyText: { color: "#F58220", fontWeight: "700" },
  currencyActiveText: { color: "#FFFFFF", fontWeight: "700" },
  exchangeRate: { fontSize: 12, marginTop: 10, textAlign: "center" },

  divider: {
    height: 1,
    backgroundColor: "#EEEEEE",
    marginVertical: 16,
  },

  logoutButton: {
    height: 52,
    backgroundColor: "#FFFFFF",
    borderWidth: 1,
    borderColor: "#D94343",
    borderRadius: 12,
    justifyContent: "center",
    alignItems: "center",
    flexDirection: "row",
    gap: 8,
    marginTop: 25,
  },

  logoutText: {
    color: "#D94343",
    fontSize: 16,
    fontWeight: "bold",
  },
});
