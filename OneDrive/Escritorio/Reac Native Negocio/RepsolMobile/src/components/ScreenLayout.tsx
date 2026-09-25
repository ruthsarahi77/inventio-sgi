import FormScroll from "./FormScroll";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
} from "react-native";

import { router, usePathname } from "expo-router";
import type { ReactNode } from "react";
import { Ionicons } from "@expo/vector-icons";
import { useApp } from "../models/AppContext";

interface ScreenLayoutProps {
  title: string;
  subtitle: string;
  children: ReactNode;
  showBack?: boolean;
}

export default function ScreenLayout({
  title,
  subtitle,
  children,
  showBack = true,
}: ScreenLayoutProps) {
  const { colors } = useApp();
  const pathname = usePathname();
  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      {/* ENCABEZADO */}
      <View style={styles.header}>
        <View style={styles.headerTop}>
          {showBack && (
            <TouchableOpacity
              style={styles.backButton}
              accessibilityLabel="Regresar"
              onPress={() => {
                if (router.canGoBack()) router.back();
                else router.replace("/");
              }}
            >
              <Text style={styles.backText}>‹</Text>
            </TouchableOpacity>
          )}

          <View style={styles.headerTextContainer}>
            <Text style={styles.headerTitle}>{title}</Text>
            <Text style={styles.headerSubtitle}>{subtitle}</Text>
          </View>

          {pathname !== "/perfil" && <TouchableOpacity
            style={styles.profileButton}
            accessibilityLabel="Mi perfil"
            onPress={() => router.push("/perfil")}
          >
            <Ionicons name="person-outline" size={20} color="#F58220" />
          </TouchableOpacity>}
        </View>
      </View>

      {/* CONTENIDO */}
      <FormScroll
        style={styles.scroll}
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        {children}
      </FormScroll>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: "#F5F5F5",
  },

  header: {
    backgroundColor: "#F58220",
    paddingTop: 55,
    paddingBottom: 25,
    paddingHorizontal: 20,
  },

  headerTop: {
    flexDirection: "row",
    alignItems: "center",
  },

  brand: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    marginTop: 18,
  },

  brandLogo: {
    width: 22,
    height: 22,
    marginRight: 2,
  },

  brandText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "800",
    letterSpacing: 1,
  },

  profileButton: {
    width: 38,
    height: 38,
    borderRadius: 12,
    backgroundColor: "#FFFFFF",
    justifyContent: "center",
    alignItems: "center",
  },

  backButton: {
    width: 40,
    height: 40,
    borderRadius: 12,
    backgroundColor: "#FFFFFF",
    justifyContent: "center",
    alignItems: "center",
    marginRight: 12,
  },

  backText: {
    fontSize: 32,
    color: "#F58220",
    marginTop: -4,
  },

  headerTextContainer: {
    flex: 1,
  },

  headerTitle: {
    fontSize: 24,
    fontWeight: "bold",
    color: "#FFFFFF",
  },

  headerSubtitle: {
    fontSize: 13,
    color: "#FFF3E8",
    marginTop: 4,
  },

  scroll: {
    flex: 1,
  },

  content: {
    padding: 20,
    paddingBottom: 40,
  },
});
