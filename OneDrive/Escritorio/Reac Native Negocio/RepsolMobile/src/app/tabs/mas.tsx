import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import { StyleSheet, Text, TouchableOpacity, View } from "react-native";
import ScreenLayout from "../../components/ScreenLayout";
import { useApp } from "../../models/AppContext";

export default function MasScreen() {
  const { colors, user } = useApp();
  const options = [
    { label: "Productos", icon: "cube-outline" as const, route: "/tabs/productos" },
    { label: "Inventario", icon: "layers-outline" as const, route: "/tabs/stock" },
    {
      label: "Clientes",
      icon: "people-outline" as const,
      route: "/tabs/clientes",
    },
    {
      label: "Ventas",
      icon: "receipt-outline" as const,
      route: "/tabs/ventas",
    },
    {
      label: "Mis ventas",
      icon: "stats-chart-outline" as const,
      route: "/mis-ventas",
    },
    { label: "Mi perfil", icon: "person-outline" as const, route: "/perfil" },
  ] as const;
  return (
    <ScreenLayout title="Más" subtitle="Accesos y administración">
      <View style={[styles.roleCard, { backgroundColor: colors.surface }]}>
        <Ionicons
          name={
            user?.role === "admin"
              ? "shield-checkmark-outline"
              : "person-circle-outline"
          }
          size={27}
          color="#F58220"
        />
        <View>
          <Text style={[styles.roleTitle, { color: colors.text }]}>
            {user?.role === "admin" ? "Administrador" : "Vendedor"}
          </Text>
          <Text style={[styles.roleText, { color: colors.secondary }]}>
            {user?.role === "admin"
              ? "Administra tus operaciones"
              : "Gestiona tus operaciones"}
          </Text>
        </View>
      </View>
      {options.map((option) => (
        <TouchableOpacity
          key={option.label}
          style={[
            styles.option,
            { backgroundColor: colors.surface, borderColor: colors.border },
          ]}
          onPress={() => router.push(option.route)}
        >
          <View style={styles.optionIcon}>
            <Ionicons name={option.icon} size={21} color="#F58220" />
          </View>
          <Text style={[styles.optionLabel, { color: colors.text }]}>
            {option.label}
          </Text>
          <Ionicons name="chevron-forward" size={19} color={colors.secondary} />
        </TouchableOpacity>
      ))}
    </ScreenLayout>
  );
}

const styles = StyleSheet.create({
  roleCard: {
    borderRadius: 13,
    padding: 17,
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    marginBottom: 18,
    elevation: 2,
  },
  roleTitle: { fontSize: 16, fontWeight: "800" },
  roleText: { fontSize: 12, marginTop: 4 },
  option: {
    minHeight: 58,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 14,
    marginBottom: 10,
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
  },
  optionIcon: {
    width: 35,
    height: 35,
    borderRadius: 10,
    backgroundColor: "#FFF0E2",
    alignItems: "center",
    justifyContent: "center",
  },
  optionLabel: { flex: 1, fontSize: 14, fontWeight: "700" },
});
