import { Redirect, Tabs } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { useApp } from "../../models/AppContext";

export default function TabsLayout() {
  const { themeMode, user } = useApp();
  const dark = themeMode === "dark";
  if (!user) return <Redirect href="/login" />;
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: "#F58220",
        tabBarInactiveTintColor: dark ? "#9CA3AF" : "#8A8A8A",
        tabBarStyle: {
          backgroundColor: dark ? "#23262B" : "#FFFFFF",
          height: 65,
          paddingBottom: 8,
          paddingTop: 8,
          borderTopColor: dark ? "#363A42" : "#E5E7EB",
        },
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: "600",
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: "Inicio",
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="home-outline" size={size} color={color} />
          ),
        }}
      />

      <Tabs.Screen
        name="stock"
        options={{
          title: "Stock",
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="cube-outline" size={size} color={color} />
          ),
        }}
      />

      <Tabs.Screen
        name="clientes"
        options={{
          href: null,
        }}
      />

      <Tabs.Screen
        name="proformas"
        options={{
          title: "Proformas",
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="document-text-outline" size={size} color={color} />
          ),
        }}
      />

      <Tabs.Screen
        name="ventas"
        options={{
          href: null,
        }}
      />
      <Tabs.Screen
        name="recibos"
        options={{
          title: "Recibos",
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="wallet-outline" size={size} color={color} />
          ),
        }}
      />
      <Tabs.Screen
        name="mas"
        options={{
          title: "Más",
          tabBarIcon: ({ color, size }) => (
            <Ionicons name="ellipsis-horizontal" size={size} color={color} />
          ),
        }}
      />
    </Tabs>
  );
}
