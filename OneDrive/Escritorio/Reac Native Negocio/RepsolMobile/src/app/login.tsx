import { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Image,
} from "react-native";
import { router } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { useApp, UserRole } from "../models/AppContext";

export default function LoginScreen() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<UserRole>("vendedor");
  const { signIn, authenticateBiometric, themeMode, setThemeMode } = useApp();
  const dark = themeMode === "dark";

  const handleLogin = () => {
    signIn(role, email ? { email } : undefined);
    router.replace("/tabs");
  };

  const handleBiometricLogin = async () => {
    if (await authenticateBiometric()) router.replace("/tabs");
  };

  return (
    <View
      style={[
        styles.container,
        { backgroundColor: dark ? "#17191C" : "#F5F5F5" },
      ]}
    >
      <View style={styles.logoContainer}>
        <Image
          source={require("../../assets/images/icon.png")}
          style={styles.logo}
          resizeMode="contain"
        />
      </View>

      <Text style={[styles.title, { color: dark ? "#F5F5F5" : "#222222" }]}>
        Bienvenido
      </Text>

      <Text style={[styles.subtitle, { color: dark ? "#B0B4BA" : "#777777" }]}>
        Ingresa a tu cuenta
      </Text>

      <View style={styles.roleSelector}>
        {(["vendedor", "admin"] as UserRole[]).map((option) => (
          <TouchableOpacity
            key={option}
            style={[
              styles.roleOption,
              role === option && styles.roleOptionActive,
            ]}
            onPress={() => setRole(option)}
          >
            <Ionicons
              name={
                option === "admin"
                  ? "shield-checkmark-outline"
                  : "person-outline"
              }
              size={18}
              color={role === option ? "#FFFFFF" : "#F58220"}
            />
            <Text
              style={[
                styles.roleText,
                role === option && styles.roleTextActive,
              ]}
            >
              {option === "admin" ? "Admin" : "Vendedor"}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <View style={styles.form}>
        <Text style={[styles.label, { color: dark ? "#F5F5F5" : "#333333" }]}>
          Correo electrónico
        </Text>

        <TextInput
          style={[
            styles.input,
            {
              backgroundColor: dark ? "#23262B" : "#FFFFFF",
              borderColor: dark ? "#363A42" : "#DDDDDD",
              color: dark ? "#F5F5F5" : "#222222",
            },
          ]}
          placeholder="Ingresa tu correo"
          placeholderTextColor="#999999"
          value={email}
          onChangeText={setEmail}
          keyboardType="email-address"
          autoCapitalize="none"
        />

        <Text style={[styles.label, { color: dark ? "#F5F5F5" : "#333333" }]}>
          Contraseña
        </Text>

        <TextInput
          style={[
            styles.input,
            {
              backgroundColor: dark ? "#23262B" : "#FFFFFF",
              borderColor: dark ? "#363A42" : "#DDDDDD",
              color: dark ? "#F5F5F5" : "#222222",
            },
          ]}
          placeholder="Ingresa tu contraseña"
          placeholderTextColor="#999999"
          value={password}
          onChangeText={setPassword}
          secureTextEntry
        />

        <TouchableOpacity style={styles.button} onPress={handleLogin}>
          <Text style={styles.buttonText}>Iniciar sesión</Text>
        </TouchableOpacity>
      </View>

      <TouchableOpacity
        style={styles.biometricButton}
        onPress={handleBiometricLogin}
      >
        <Ionicons name="finger-print-outline" size={21} color="#F58220" />
        <Text style={styles.biometricText}>Ingresar con biometría</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={styles.themeButton}
        onPress={() => setThemeMode(dark ? "light" : "dark")}
      >
        <Ionicons
          name={dark ? "sunny-outline" : "moon-outline"}
          size={18}
          color="#F58220"
        />
        <Text style={styles.biometricText}>
          {dark ? "Modo claro" : "Modo oscuro"}
        </Text>
      </TouchableOpacity>

      <Text style={[styles.footer, { color: dark ? "#9CA3AF" : "#999999" }]}>
        Sistema de Inventario REPSOL
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F5F5F5",
    paddingHorizontal: 25,
    justifyContent: "center",
  },

  logoContainer: {
    alignItems: "center",
    justifyContent: "center",
    marginTop: 4,
    marginBottom: 10,
    paddingVertical: 0,
  },

  logo: {
    width: 150,
    height: 150,
    maxWidth: "68%",
    aspectRatio: 1,
    resizeMode: "contain",
    alignSelf: "center",
  },

  title: {
    fontSize: 30,
    fontWeight: "bold",
    textAlign: "center",
    color: "#222222",
  },

  subtitle: {
    fontSize: 16,
    textAlign: "center",
    color: "#777777",
    marginTop: 8,
    marginBottom: 30,
  },

  roleSelector: {
    flexDirection: "row",
    gap: 10,
    marginBottom: 22,
  },

  roleOption: {
    flex: 1,
    height: 44,
    borderWidth: 1,
    borderColor: "#F58220",
    borderRadius: 10,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 7,
  },

  roleOptionActive: { backgroundColor: "#F58220" },
  roleText: { color: "#F58220", fontWeight: "600" },
  roleTextActive: { color: "#FFFFFF" },

  form: {
    width: "100%",
  },

  label: {
    fontSize: 14,
    fontWeight: "600",
    color: "#333333",
    marginBottom: 8,
  },

  input: {
    height: 50,
    backgroundColor: "#FFFFFF",
    borderWidth: 1,
    borderColor: "#DDDDDD",
    borderRadius: 10,
    paddingHorizontal: 15,
    fontSize: 15,
    marginBottom: 20,
  },

  button: {
    height: 52,
    backgroundColor: "#F58220",
    borderRadius: 10,
    justifyContent: "center",
    alignItems: "center",
    marginTop: 5,
  },

  buttonText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "bold",
  },

  biometricButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    marginTop: 16,
  },

  biometricText: { color: "#F58220", fontWeight: "600" },

  themeButton: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 8,
    marginTop: 18,
  },

  footer: {
    textAlign: "center",
    color: "#999999",
    fontSize: 12,
    marginTop: 30,
  },
});
