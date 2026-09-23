import {
  View,
  Text,
  TextInput,
  Image,
  StyleSheet,
  TouchableOpacity,
  Alert,
  Switch,
} from "react-native";

import { useState } from "react";
import { router } from "expo-router";
import * as ImagePicker from "expo-image-picker";
import { Ionicons } from "@expo/vector-icons";
import ScreenLayout from "../components/ScreenLayout";
import { useApp } from "../models/AppContext";

export default function PerfilScreen() {
  const {
    user,
    themeMode,
    currency,
    setThemeMode,
    setCurrency,
    biometricEnabled,
    toggleBiometric,
    updateProfile,
    signOut,
    colors,
    exchangeRate,
  } = useApp();
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(user?.name ?? "");
  const [email, setEmail] = useState(user?.email ?? "");
  const [username, setUsername] = useState(user?.username ?? "");
  const [password, setPassword] = useState("");

  const pickPhoto = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ["images"],
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.8,
    });
    if (!result.canceled) updateProfile({ photoUri: result.assets[0].uri });
  };

  const saveProfile = () => {
    updateProfile({ name, email, username });
    setEditing(false);
  };

  const handleLogout = () => {
    Alert.alert("Cerrar sesión", "¿Deseas cerrar tu sesión?", [
      {
        text: "Cancelar",
        style: "cancel",
      },
      {
        text: "Cerrar sesión",
        onPress: () => {
          signOut();
          router.replace("/login");
        },
      },
    ]);
  };

  return (
    <ScreenLayout title="Mi perfil" subtitle="Información de tu cuenta">
      <View style={[styles.profileCard, { backgroundColor: colors.surface }]}>
        <TouchableOpacity style={styles.avatar} onPress={pickPhoto}>
          {user?.photoUri ? (
            <Image source={{ uri: user.photoUri }} style={styles.avatarImage} />
          ) : (
            <Text style={styles.avatarText}>
              {(user?.name ?? "U").charAt(0)}
            </Text>
          )}
          <View style={styles.camera}>
            <Ionicons name="camera-outline" size={14} color="#FFFFFF" />
          </View>
        </TouchableOpacity>

        <Text style={[styles.name, { color: colors.text }]}>{user?.name}</Text>
        <Text style={styles.role}>
          {user?.role === "admin" ? "Administrador" : "Vendedor"}
        </Text>
        <View
          style={[styles.accessBadge, { backgroundColor: colors.mutedSurface }]}
        >
          <Ionicons
            name={
              user?.role === "admin"
                ? "shield-checkmark-outline"
                : "person-outline"
            }
            size={15}
            color="#F58220"
          />
          <Text style={styles.accessBadgeText}>
            {user?.role === "admin"
              ? "Acceso total a la empresa"
              : "Acceso a mis operaciones"}
          </Text>
        </View>
      </View>

      <Text style={[styles.sectionTitle, { color: colors.text }]}>
        Información personal
      </Text>

      <View style={[styles.infoCard, { backgroundColor: colors.surface }]}>
        <Text style={styles.label}>Nombre completo</Text>
        {editing ? (
          <TextInput style={styles.input} value={name} onChangeText={setName} />
        ) : (
          <Text style={[styles.value, { color: colors.text }]}>
            {user?.name}
          </Text>
        )}

        <View style={styles.divider} />

        <Text style={styles.label}>Correo electrónico</Text>
        {editing ? (
          <TextInput
            style={styles.input}
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
          />
        ) : (
          <Text style={[styles.value, { color: colors.text }]}>
            {user?.email}
          </Text>
        )}

        <View style={styles.divider} />

        <Text style={styles.label}>Usuario</Text>
        {editing ? (
          <TextInput
            style={styles.input}
            value={username}
            onChangeText={setUsername}
          />
        ) : (
          <Text style={[styles.value, { color: colors.text }]}>
            {user?.username}
          </Text>
        )}
        {editing && (
          <>
            <View style={styles.divider} />
            <Text style={styles.label}>Nueva contraseña</Text>
            <TextInput
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              placeholder="Dejar vacío para conservarla"
              secureTextEntry
            />
          </>
        )}
      </View>

      <TouchableOpacity
        style={styles.editButton}
        onPress={editing ? saveProfile : () => setEditing(true)}
      >
        <Ionicons
          name={editing ? "checkmark-outline" : "create-outline"}
          size={19}
          color="#FFFFFF"
        />
        <Text style={styles.editText}>
          {editing ? "Guardar cambios" : "Editar perfil"}
        </Text>
      </TouchableOpacity>

      <Text style={[styles.sectionTitle, { color: colors.text }]}>
        Preferencias y seguridad
      </Text>
      <View style={[styles.settingsCard, { backgroundColor: colors.surface }]}>
        <View style={styles.settingRow}>
          <Ionicons name="moon-outline" size={21} color="#F58220" />
          <Text style={[styles.settingText, { color: colors.text }]}>
            Tema oscuro
          </Text>
          <Switch
            value={themeMode === "dark"}
            onValueChange={(value) => setThemeMode(value ? "dark" : "light")}
          />
        </View>
        <View style={styles.settingRow}>
          <Ionicons name="finger-print-outline" size={21} color="#F58220" />
          <Text style={[styles.settingText, { color: colors.text }]}>
            Seguridad biométrica
          </Text>
          <Switch
            value={biometricEnabled}
            onValueChange={() => {
              void toggleBiometric();
            }}
          />
        </View>
        <View style={styles.currencyRow}>
          <Text style={[styles.settingText, { color: colors.text }]}>
            Moneda principal
          </Text>
          <View style={styles.currencyOptions}>
            {(["PEN", "USD"] as const).map((item) => (
              <TouchableOpacity
                key={item}
                onPress={() => setCurrency(item)}
                style={[
                  styles.currency,
                  currency === item && styles.currencyActive,
                ]}
              >
                <Text
                  style={
                    currency === item
                      ? styles.currencyActiveText
                      : styles.currencyText
                  }
                >
                  {item}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      </View>

      <Text style={[styles.exchangeRate, { color: colors.secondary }]}>
        Tipo de cambio referencial: 1 USD = S/ {exchangeRate.toFixed(2)}
      </Text>

      <TouchableOpacity style={styles.logoutButton} onPress={handleLogout}>
        <Ionicons name="log-out-outline" size={20} color="#D94343" />
        <Text style={styles.logoutText}>Cerrar sesión</Text>
      </TouchableOpacity>
    </ScreenLayout>
  );
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
