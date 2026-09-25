import { useState } from "react";
import { router } from "expo-router";
import { ActivityIndicator, Text, TextInput, TouchableOpacity, StyleSheet } from "react-native";
import ScreenLayout from "../components/ScreenLayout";
import { useApp } from "../models/AppContext";
import { useApiResource } from "../hooks/use-api-resource";
import { listReceipts } from "../services/receipts";

export default function ReceiptsScreen() {
  const { colors } = useApp();
  const [search, setSearch] = useState("");
  const { data, loading, error, reload } = useApiResource(listReceipts);
  const rows = data?.slice().sort((a, b) => Date.parse(b.fecha) - Date.parse(a.fecha)).filter(receipt => (receipt.numero + " " + receipt.ventaId).toLowerCase().includes(search.toLowerCase())) ?? [];
  return <ScreenLayout title="Recibos" subtitle="Consulta los pagos recibidos">
    <TouchableOpacity onPress={() => router.push("/tabs/ventas")} style={{ backgroundColor: "#F58220", padding: 16, borderRadius: 12, marginBottom: 16 }}><Text style={{ color: "white", fontWeight: "bold" }}>Seleccionar venta para registrar abono</Text></TouchableOpacity>
    <TextInput style={[styles.search, { color: colors.text, backgroundColor: colors.surface, borderColor: colors.border }]} placeholder="Buscar recibo o ID de venta" placeholderTextColor={colors.secondary} value={search} onChangeText={setSearch} />
    {loading && <ActivityIndicator color="#F58220" />}
    {error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text><TouchableOpacity onPress={() => void reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {data && !loading && !error && <>
      {rows.length === 0 && <Text style={{ color: colors.secondary }}>No se encontraron recibos.</Text>}
      {rows.map(receipt => <TouchableOpacity key={receipt.id} onPress={() => router.push({ pathname: "/recibo/[id]", params: { id: receipt.id } })} style={[styles.card, { backgroundColor: colors.surface }]}>
        <Text style={[styles.receiptNumber, { color: colors.text }]}>{receipt.numero}</Text>
        <Text style={styles.date}>{new Date(receipt.fecha).toLocaleString()}</Text>
        <Text style={{ color: colors.text }}>Venta #{receipt.ventaId}</Text>
        <Text style={styles.amount}>{receipt.monto.toFixed(2)}</Text>
      </TouchableOpacity>)}
    </>}
  </ScreenLayout>;
}

const styles = StyleSheet.create({
  search: {
    height: 50,
    backgroundColor: "#FFFFFF",
    borderWidth: 1,
    borderColor: "#DDDDDD",
    borderRadius: 12,
    paddingHorizontal: 15,
    marginBottom: 20,
    fontSize: 14,
  },

  summary: {
    backgroundColor: "#FFFFFF",
    borderRadius: 14,
    padding: 20,
    marginBottom: 25,
    borderLeftWidth: 5,
    borderLeftColor: "#F58220",
  },

  summaryLabel: {
    fontSize: 13,
    color: "#777777",
  },

  summaryNumber: {
    fontSize: 28,
    fontWeight: "bold",
    color: "#222222",
    marginTop: 5,
    marginBottom: 15,
  },

  summaryTotal: {
    fontSize: 25,
    fontWeight: "bold",
    color: "#F58220",
    marginTop: 5,
  },

  sectionTitle: {
    fontSize: 19,
    fontWeight: "bold",
    color: "#222222",
    marginBottom: 15,
  },

  card: {
    backgroundColor: "#FFFFFF",
    borderRadius: 14,
    padding: 16,
    marginBottom: 15,
    elevation: 2,
  },

  cardHeader: {
    flexDirection: "row",
    alignItems: "center",
  },

  iconContainer: {
    width: 45,
    height: 45,
    borderRadius: 12,
    backgroundColor: "#FFF0E2",
    justifyContent: "center",
    alignItems: "center",
    marginRight: 12,
  },

  icon: {
    fontSize: 23,
  },

  receiptInfo: {
    flex: 1,
  },

  receiptNumber: {
    fontSize: 16,
    fontWeight: "bold",
    color: "#222222",
  },

  date: {
    fontSize: 12,
    color: "#888888",
    marginTop: 5,
  },

  amount: {
    fontSize: 17,
    fontWeight: "bold",
    color: "#F58220",
  },

  divider: {
    height: 1,
    backgroundColor: "#EEEEEE",
    marginVertical: 15,
  },

  label: {
    fontSize: 12,
    color: "#888888",
  },

  client: {
    fontSize: 15,
    fontWeight: "600",
    color: "#333333",
    marginTop: 5,
  },

  methodContainer: {
    marginTop: 15,
  },

  method: {
    fontSize: 14,
    fontWeight: "600",
    color: "#333333",
    marginTop: 5,
  },

  paymentButton: {
    borderWidth: 1,
    borderColor: "#F58220",
    borderRadius: 10,
    padding: 11,
    marginTop: 16,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 7,
  },
  paymentButtonText: { color: "#F58220", fontWeight: "700" },

  empty: {
    textAlign: "center",
    color: "#888888",
    marginTop: 30,
  },
});
