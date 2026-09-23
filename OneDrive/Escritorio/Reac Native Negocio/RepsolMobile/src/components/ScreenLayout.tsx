import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Image,
} from "react-native";

import { router } from "expo-router";
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
  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      {/* ENCABEZADO */}
      <View style={styles.header}>
        <View style={styles.headerTop}>
          {showBack && (
            <TouchableOpacity
              style={styles.backButton}
              onPress={() => router.back()}
            >
              <Text style={styles.backText}>‹</Text>
            </TouchableOpacity>
          )}

          <View style={styles.headerTextContainer}>
            <Text style={styles.headerTitle}>{title}</Text>
            <Text style={styles.headerSubtitle}>{subtitle}</Text>
          </View>

          <TouchableOpacity
            style={styles.profileButton}
            onPress={() => router.push("/perfil")}
          >
            <Ionicons name="person-outline" size={20} color="#F58220" />
          </TouchableOpacity>
        </View>
        {!showBack && (
          <View style={styles.brand}>
            <Image
              source={require("../../assets/images/icon.png")}
              style={styles.brandLogo}
              resizeMode="contain"
            />
            <Text style={styles.brandText}>INVENTIO</Text>
          </View>
        )}
      </View>

      {/* CONTENIDO */}
      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
      >
        {children}
      </ScrollView>
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
