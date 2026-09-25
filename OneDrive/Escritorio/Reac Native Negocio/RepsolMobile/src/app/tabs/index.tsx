import FormScroll from "../../components/FormScroll";
import { Ionicons } from "@expo/vector-icons";
import { Redirect, router } from "expo-router";
import { Image, StyleSheet, Text, TextInput, TouchableOpacity, View, ActivityIndicator, useWindowDimensions } from "react-native";
import { useCallback, useState } from "react";
import { useApp } from "../../models/AppContext";
import { useApiResource } from "../../hooks/use-api-resource";
import { getDashboard } from "../../services/dashboard";

export default function HomeScreen() {
  const { user } = useApp();
  return user?.role === "admin" ? <Dashboard /> : <Redirect href="/tabs/stock" />;
}
function Dashboard() {
  const { colors } = useApp();
  const { width } = useWindowDimensions();
  const [year, setYear] = useState(String(new Date().getFullYear()));
  const [month, setMonth] = useState(String(new Date().getMonth() + 1));
  // Omit both parameters initially: Spring Boot chooses its current month/time zone.
  const [period, setPeriod] = useState<{ year: number; month: number }>();
  const [filterError, setFilterError] = useState("");
  const { data, loading, error, reload } = useApiResource(useCallback((signal: AbortSignal) => getDashboard(period, signal), [period]));
  const apply = () => {
    if (!/^\d{1,4}$/.test(year) || !/^\d{1,2}$/.test(month) || Number(year) < 1 || Number(month) < 1 || Number(month) > 12) {
      setFilterError("Introduce un mes de 1 a 12 y un año de 1 a 9999."); return;
    }
    setFilterError(""); setPeriod({ year: Number(year), month: Number(month) });
  };
  const cards = data ? [
    { label: "Litros vendidos", value: data.litrosVendidosMes, suffix: " L", route: "/tabs/ventas" },
    { label: "Meta Repsol", value: data.metaRepsol, suffix: " L", route: "/tabs/ventas" },
    { label: "Cumplimiento de meta", value: data.cumplimientoMeta, suffix: "%", route: "/tabs/ventas" },
    { label: "Ventas PEN", value: data.totalVentasMes, suffix: " PEN", route: "/tabs/ventas" },
    { label: "Ventas USD", value: data.totalVentasUSD, suffix: " USD", route: "/tabs/ventas" },
    { label: "Saldo pendiente de ventas del mes", value: data.saldoPendienteTotal, suffix: "", route: "/tabs/ventas" },
    { label: "Productos activos (actual)", value: data.totalProductos, suffix: "", route: "/tabs/productos" },
    { label: "Productos con stock bajo", value: data.productosStockBajo, suffix: "", route: "/tabs/stock" },
    { label: "Ventas de hoy en el mes seleccionado", value: data.ventasHoy, suffix: "", route: "/tabs/ventas" },
    { label: "Recibos de hoy en el mes seleccionado", value: data.recibosHoy, suffix: "", route: "/tabs/recibos" },
  ] as const : [];
  return <View style={[styles.container, { backgroundColor: colors.background }]}><FormScroll contentContainerStyle={styles.content}>
    <View style={styles.topBar}><View style={styles.brandRow}><Image source={require("../../../assets/images/icon.png")} style={styles.brandLogo} /><Text style={styles.brand}>INVENTIO</Text></View><TouchableOpacity accessibilityLabel="Mi perfil" style={styles.avatar} onPress={() => router.push("/perfil")}><Ionicons name="person-outline" size={18} color="#F58220" /></TouchableOpacity></View>
    <Text style={[styles.screenTitle, { color: colors.text }]}>Resumen Comercial</Text>
    <Text style={[styles.date, { color: colors.secondary }]}>{period ? "Mes " + period.month + " / " + period.year : "Mes vigente del servidor"}</Text>
    <View style={{ margin: 20, padding: 16, borderRadius: 12, backgroundColor: colors.surface, borderColor: colors.border, borderWidth: 1, gap: 14 }}>
      <Text style={{ color: colors.text, fontWeight: "bold", fontSize: 16 }}>Período del resumen</Text>
      <View style={{ flexDirection: "row", gap: 12 }}>
        <View style={{ flex: 1, gap: 6 }}><Text style={{ color: colors.secondary }}>Mes (1–12)</Text><TextInput accessibilityLabel="Mes" value={month} onChangeText={setMonth} keyboardType="number-pad" maxLength={2} style={{ color: colors.text, borderColor: colors.border, borderWidth: 1, borderRadius: 10, padding: 12, minHeight: 48 }} /></View>
        <View style={{ flex: 1, gap: 6 }}><Text style={{ color: colors.secondary }}>Año</Text><TextInput accessibilityLabel="Año" value={year} onChangeText={setYear} keyboardType="number-pad" maxLength={4} style={{ color: colors.text, borderColor: colors.border, borderWidth: 1, borderRadius: 10, padding: 12, minHeight: 48 }} /></View>
      </View>
      <TouchableOpacity onPress={apply} style={{ backgroundColor: "#F58220", padding: 14, borderRadius: 10, alignItems: "center" }}><Text style={{ color: "white", fontWeight: "bold" }}>Aplicar período</Text></TouchableOpacity>
      <TouchableOpacity onPress={() => { const now = new Date(); setYear(String(now.getFullYear())); setMonth(String(now.getMonth() + 1)); setFilterError(""); if (period) setPeriod(undefined); else void reload(); }} style={{ padding: 8, alignItems: "center" }}><Text style={styles.link}>Mes vigente</Text></TouchableOpacity>
    </View>
    {!!filterError && <Text accessibilityRole="alert" style={{ color: "#D94343" }}>{filterError}</Text>}
    {loading && <ActivityIndicator color="#F58220" />}
    {error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text><TouchableOpacity onPress={() => void reload()}><Text style={styles.link}>Reintentar</Text></TouchableOpacity></>}
    {data && !loading && !error && <View style={styles.metricGrid}>{cards.filter(card => card.value !== null).map(card => <Metric key={card.label} icon="cash-outline" label={card.label} value={card.value!.toLocaleString(undefined, { maximumFractionDigits: 2 }) + card.suffix} colors={colors} wide={width >= 420} onPress={() => router.push(card.route)} />)}</View>}
    {data && !loading && !error && <Text style={{ color: colors.secondary }}>Solo se muestran los indicadores disponibles del servidor, incluidos sus valores cero.</Text>}
  </FormScroll></View>;
}
function Metric({
  wide,
  icon,
  label,
  value,
  colors,
  onPress,
}: {
  wide: boolean;
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
        { width: wide ? "48%" : "100%" },
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
  metricGrid: { paddingHorizontal: 20, flexDirection: "row", flexWrap: "wrap", gap: 12 },
  metric: {
    flexGrow: 1,
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
  metricLabel: { fontSize: 14, lineHeight: 20, flexShrink: 1 },
  metricValue: { fontSize: 23, fontWeight: "800", marginTop: 4 },
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
