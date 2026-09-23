import { Ionicons } from "@expo/vector-icons";
import { router } from "expo-router";
import {
  Image,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { useState } from "react";
import { useApp } from "../../models/AppContext";
import { useProducts } from "../../models/ProductContext";

export default function HomeScreen() {
  const { formatMoney, themeMode } = useApp();
  const { products } = useProducts();
  const [period, setPeriod] = useState<
    "Este mes" | "Este año" | "Últimos 7 días"
  >("Este mes");
  const dark = themeMode === "dark";
  const colors = {
    background: dark ? "#17191C" : "#F4F6F7",
    surface: dark ? "#23262B" : "#FFFFFF",
    text: dark ? "#F5F5F5" : "#202124",
    secondary: dark ? "#AEB4BC" : "#69727D",
    border: dark ? "#363A42" : "#E6E9EC",
  };
  const lowStock = products.filter((product) => product.stock <= 5).length;
  const chartValues =
    period === "Este año"
      ? [58, 67, 61, 78, 72, 88, 82, 96]
      : period === "Últimos 7 días"
        ? [35, 52, 44, 69, 58, 76, 68]
        : [42, 58, 50, 70, 62, 84, 76, 95];

  const cyclePeriod = () => {
    setPeriod((current) =>
      current === "Este mes"
        ? "Este año"
        : current === "Este año"
          ? "Últimos 7 días"
          : "Este mes",
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.content}
      >
        <View style={styles.topBar}>
          <View style={styles.brandRow}>
            <Image
              source={require("../../../assets/images/icon.png")}
              style={styles.brandLogo}
              resizeMode="contain"
            />
            <View>
              <Text style={styles.brand}>INVENTIO</Text>
              <Text style={styles.brandCaption}>DISTRIBUIDOR REPSOL</Text>
            </View>
          </View>
          <View style={styles.topActions}>
            <TouchableOpacity>
              <Ionicons
                name="notifications-outline"
                size={23}
                color="#FFFFFF"
              />
            </TouchableOpacity>
            <TouchableOpacity
              style={styles.avatar}
              onPress={() => router.push("/perfil")}
            >
              <Ionicons name="person-outline" size={18} color="#F58220" />
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.headingRow}>
          <View>
            <Text style={[styles.screenTitle, { color: colors.text }]}>
              Resumen Comercial
            </Text>
            <Text style={[styles.date, { color: colors.secondary }]}>
              Hoy, 21 de septiembre de 2026
            </Text>
          </View>
          <TouchableOpacity
            style={[
              styles.periodButton,
              { backgroundColor: colors.surface, borderColor: colors.border },
            ]}
            onPress={cyclePeriod}
          >
            <Text style={[styles.periodText, { color: colors.text }]}>
              {period}
            </Text>
            <Ionicons name="chevron-down" size={15} color={colors.secondary} />
          </TouchableOpacity>
        </View>

        <View style={styles.metricGrid}>
          <Metric
            icon="water-outline"
            label="Litros vendidos"
            value="48.650 L"
            colors={colors}
            onPress={() => router.push("/tabs/ventas")}
          />
          <Metric
            icon="cash-outline"
            label="Ventas"
            value={formatMoney(284750)}
            colors={colors}
            onPress={() => router.push("/tabs/ventas")}
          />
          <TouchableOpacity
            style={[styles.goalCard, { backgroundColor: "#F58220" }]}
            onPress={() => router.push("/tabs/ventas")}
          >
            <Text style={styles.goalLabel}>Cumplimiento Meta</Text>
            <Text style={styles.goalValue}>88,5%</Text>
            <Text style={styles.goalSmall}>48.650 L / 55.000 L</Text>
            <View style={styles.goalTrack}>
              <View style={styles.goalProgress} />
            </View>
          </TouchableOpacity>
        </View>

        <View
          style={[
            styles.panel,
            { backgroundColor: colors.surface, borderColor: colors.border },
          ]}
        >
          <View style={styles.panelHeader}>
            <View>
              <Text style={[styles.panelTitle, { color: colors.text }]}>
                Litros vendidos
              </Text>
              <Text style={[styles.panelSubtitle, { color: colors.secondary }]}>
                {period}
              </Text>
            </View>
            <TouchableOpacity onPress={() => router.push("/tabs/ventas")}>
              <Ionicons name="trending-up-outline" size={21} color="#27864A" />
            </TouchableOpacity>
          </View>
          <View style={styles.chart}>
            {chartValues.map((height, index) => (
              <View key={index} style={styles.chartColumn}>
                <View style={[styles.chartBar, { height: height * 0.72 }]} />
                <Text style={[styles.chartLabel, { color: colors.secondary }]}>
                  {
                    ["Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep"][
                      index
                    ]
                  }
                </Text>
              </View>
            ))}
          </View>
        </View>

        <View style={styles.twoColumns}>
          <TouchableOpacity
            style={[
              styles.smallMetric,
              { backgroundColor: colors.surface, borderColor: colors.border },
            ]}
            onPress={() => router.push("/tabs/recibos")}
          >
            <View style={styles.smallIcon}>
              <Ionicons name="wallet-outline" size={19} color="#27864A" />
            </View>
            <Text style={[styles.smallLabel, { color: colors.secondary }]}>
              Cobrado este mes
            </Text>
            <Text style={styles.greenValue}>{formatMoney(148650)}</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[
              styles.smallMetric,
              { backgroundColor: colors.surface, borderColor: colors.border },
            ]}
            onPress={() => router.push("/tabs/recibos")}
          >
            <View style={styles.smallIconRed}>
              <Ionicons name="time-outline" size={19} color="#D94343" />
            </View>
            <Text style={[styles.smallLabel, { color: colors.secondary }]}>
              Pendiente de cobro
            </Text>
            <Text style={styles.redValue}>{formatMoney(86420)}</Text>
          </TouchableOpacity>
        </View>

        <View
          style={[
            styles.panel,
            { backgroundColor: colors.surface, borderColor: colors.border },
          ]}
        >
          <View style={styles.panelHeader}>
            <Text style={[styles.panelTitle, { color: colors.text }]}>
              Últimas actividades
            </Text>
            <TouchableOpacity onPress={() => router.push("/tabs/proformas")}>
              <Text style={styles.link}>Ver todas</Text>
            </TouchableOpacity>
          </View>
          <Activity
            icon="document-text-outline"
            title="Proforma #P-248"
            detail="Cliente: Distribuidora del Norte"
            time="Hace 2 h"
            colors={colors}
            onPress={() => router.push("/tabs/proformas")}
          />
          <Activity
            icon="wallet-outline"
            title="Recibo #R-156"
            detail="Cliente: Inversiones Paredes"
            time="Hace 4 h"
            colors={colors}
            onPress={() => router.push("/tabs/recibos")}
          />
          <Activity
            icon="cube-outline"
            title={`${products.length} productos registrados`}
            detail={`${lowStock} con stock bajo`}
            time="Hoy"
            colors={colors}
            onPress={() => router.push("/tabs/stock")}
          />
        </View>
      </ScrollView>
    </View>
  );
}

function Metric({
  icon,
  label,
  value,
  colors,
  onPress,
}: {
  icon: "water-outline" | "cash-outline";
  label: string;
  value: string;
  colors: { surface: string; text: string; secondary: string; border: string };
  onPress: () => void;
}) {
  return (
    <TouchableOpacity
      style={[
        styles.metric,
        { backgroundColor: colors.surface, borderColor: colors.border },
      ]}
      onPress={onPress}
    >
      <View style={styles.metricIcon}>
        <Ionicons name={icon} size={21} color="#F58220" />
      </View>
      <Text style={[styles.metricLabel, { color: colors.secondary }]}>
        {label}
      </Text>
      <Text style={[styles.metricValue, { color: colors.text }]}>{value}</Text>
    </TouchableOpacity>
  );
}

function Activity({
  icon,
  title,
  detail,
  time,
  colors,
  onPress,
}: {
  icon: "document-text-outline" | "wallet-outline" | "cube-outline";
  title: string;
  detail: string;
  time: string;
  colors: { text: string; secondary: string };
  onPress: () => void;
}) {
  return (
    <TouchableOpacity style={styles.activity} onPress={onPress}>
      <View style={styles.activityIcon}>
        <Ionicons name={icon} size={17} color="#F58220" />
      </View>
      <View style={styles.activityText}>
        <Text style={[styles.activityTitle, { color: colors.text }]}>
          {title}
        </Text>
        <Text style={[styles.activityDetail, { color: colors.secondary }]}>
          {detail}
        </Text>
      </View>
      <Text style={[styles.activityTime, { color: colors.secondary }]}>
        {time}
      </Text>
      <Ionicons name="chevron-forward" size={16} color={colors.secondary} />
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { paddingBottom: 28 },
  topBar: {
    backgroundColor: "#1D2226",
    paddingTop: 51,
    paddingBottom: 18,
    paddingHorizontal: 20,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  brandRow: { flexDirection: "row", alignItems: "center", gap: 9 },
  brandLogo: {
    width: 26,
    height: 26,
    marginRight: 2,
    resizeMode: "contain",
  },
  brand: {
    color: "#F58220",
    fontSize: 18,
    fontWeight: "900",
    letterSpacing: 0.5,
  },
  brandCaption: {
    color: "#B9C0C6",
    fontSize: 8,
    marginTop: 1,
    letterSpacing: 0.6,
  },
  topActions: { flexDirection: "row", alignItems: "center", gap: 18 },
  avatar: {
    width: 34,
    height: 34,
    borderRadius: 17,
    backgroundColor: "#FFFFFF",
    alignItems: "center",
    justifyContent: "center",
  },
  headingRow: {
    paddingHorizontal: 20,
    paddingTop: 23,
    paddingBottom: 17,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  screenTitle: { fontSize: 21, fontWeight: "800" },
  date: { fontSize: 11, marginTop: 5 },
  periodButton: {
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 10,
    paddingVertical: 8,
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
  },
  periodText: { fontSize: 11, fontWeight: "600" },
  metricGrid: { paddingHorizontal: 20, flexDirection: "row", gap: 8 },
  metric: {
    flex: 1,
    minHeight: 119,
    borderWidth: 1,
    borderRadius: 11,
    padding: 12,
  },
  metricIcon: {
    width: 35,
    height: 35,
    borderRadius: 18,
    backgroundColor: "#FFF0E2",
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 8,
  },
  metricLabel: { fontSize: 10 },
  metricValue: { fontSize: 16, fontWeight: "800", marginTop: 4 },
  goalCard: { flex: 1.15, minHeight: 119, borderRadius: 11, padding: 12 },
  goalLabel: { color: "#FFFFFF", fontSize: 10, fontWeight: "700" },
  goalValue: {
    color: "#FFFFFF",
    fontSize: 27,
    fontWeight: "900",
    marginTop: 9,
  },
  goalSmall: { color: "#FFF4EA", fontSize: 9, marginTop: 2 },
  goalTrack: {
    height: 5,
    backgroundColor: "#D8640C",
    borderRadius: 3,
    marginTop: 8,
  },
  goalProgress: {
    width: "88.5%",
    height: 5,
    backgroundColor: "#FFFFFF",
    borderRadius: 3,
  },
  panel: {
    marginHorizontal: 20,
    marginTop: 14,
    borderWidth: 1,
    borderRadius: 12,
    padding: 15,
  },
  panelHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  panelTitle: { fontSize: 15, fontWeight: "800" },
  panelSubtitle: { fontSize: 11, marginTop: 3 },
  link: { color: "#F58220", fontSize: 12, fontWeight: "700" },
  chart: {
    height: 145,
    flexDirection: "row",
    alignItems: "flex-end",
    justifyContent: "space-between",
    paddingTop: 18,
  },
  chartColumn: {
    alignItems: "center",
    justifyContent: "flex-end",
    height: "100%",
    gap: 7,
  },
  chartBar: { width: 18, borderRadius: 5, backgroundColor: "#F58220" },
  chartLabel: { fontSize: 9 },
  twoColumns: {
    flexDirection: "row",
    gap: 10,
    marginHorizontal: 20,
    marginTop: 14,
  },
  smallMetric: { flex: 1, borderWidth: 1, borderRadius: 12, padding: 13 },
  smallIcon: {
    width: 31,
    height: 31,
    borderRadius: 16,
    backgroundColor: "#E8F6ED",
    alignItems: "center",
    justifyContent: "center",
  },
  smallIconRed: {
    width: 31,
    height: 31,
    borderRadius: 16,
    backgroundColor: "#FFF0F0",
    alignItems: "center",
    justifyContent: "center",
  },
  smallLabel: { fontSize: 10, marginTop: 9 },
  greenValue: {
    color: "#27864A",
    fontSize: 16,
    fontWeight: "800",
    marginTop: 4,
  },
  redValue: { color: "#D94343", fontSize: 16, fontWeight: "800", marginTop: 4 },
  activity: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 11,
    borderBottomWidth: 1,
    borderBottomColor: "#EEF0F2",
  },
  activityIcon: {
    width: 32,
    height: 32,
    borderRadius: 9,
    backgroundColor: "#FFF0E2",
    alignItems: "center",
    justifyContent: "center",
  },
  activityText: { flex: 1, marginLeft: 10 },
  activityTitle: { fontSize: 12, fontWeight: "700" },
  activityDetail: { fontSize: 10, marginTop: 3 },
  activityTime: { fontSize: 10 },
});
