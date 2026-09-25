import { useCallback, useState } from "react";
import { router } from "expo-router";
import { ActivityIndicator, Text, TextInput, TouchableOpacity, StyleSheet } from "react-native";
import ScreenLayout from "../../components/ScreenLayout";
import { useApp } from "../../models/AppContext";
import { useApiResource } from "../../hooks/use-api-resource";
import { listSales } from "../../services/sales";
import { listCustomers } from "../../services/customers";

export default function SalesScreen({ mine = false }: { mine?: boolean }) {
  const { colors, user } = useApp();
  const [search, setSearch] = useState("");
  const { data, loading, error, reload } = useApiResource(useCallback(async (signal: AbortSignal) => {
    const [sales, customers] = await Promise.all([listSales(signal), listCustomers(signal)]);
    return { sales: mine ? sales.filter(sale => sale.vendedorId === user?.id) : sales, customers };
  }, [mine, user?.id]));
  const customerName = (id: number) => data?.customers.find(customer => customer.id === id)?.nombre ?? "Cliente #" + id;
  const rows = data?.sales.slice().sort((a, b) => Date.parse(b.fecha) - Date.parse(a.fecha)).filter(sale => (sale.numero + " " + customerName(sale.clienteId)).toLowerCase().includes(search.toLowerCase())) ?? [];
  return <ScreenLayout title={mine ? "Mis ventas" : "Ventas"} subtitle="Consulta y administra las ventas">
    <TouchableOpacity onPress={() => router.push("/nueva-venta")} style={{ backgroundColor: "#F58220", padding: 16, borderRadius: 12, marginBottom: 16 }}><Text style={{ color: "white", fontWeight: "bold" }}>Nueva venta</Text></TouchableOpacity>
    <TextInput style={[styles.search, { color: colors.text, backgroundColor: colors.surface, borderColor: colors.border }]} placeholder="Buscar venta o cliente" placeholderTextColor={colors.secondary} value={search} onChangeText={setSearch} />
    {loading && <ActivityIndicator color="#F58220" />}
    {error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text><TouchableOpacity onPress={() => void reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {data && !loading && !error && <>
      {rows.length === 0 && <Text style={{ color: colors.secondary }}>No se encontraron ventas.</Text>}
      {rows.map(sale => <TouchableOpacity key={sale.id} onPress={() => router.push({ pathname: "/venta/[id]", params: { id: sale.id } })} style={[styles.card, { backgroundColor: colors.surface }]}>
        <Text style={[styles.saleNumber, { color: colors.text }]}>{sale.numero}</Text>
        <Text style={styles.date}>{new Date(sale.fecha).toLocaleString()} · {sale.estado}</Text>
        <Text style={[styles.client, { color: colors.text }]}>{customerName(sale.clienteId)}</Text>
        <Text style={styles.total}>Total: {sale.total.toFixed(2)}</Text>
        <Text style={{ color: colors.text }}>Abonado: {sale.totalAbonado.toFixed(2)} · Saldo: {sale.saldo.toFixed(2)}</Text>
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
    justifyContent: "space-between",
    alignItems: "center",
  },

  saleNumber: {
    fontSize: 17,
    fontWeight: "bold",
    color: "#222222",
  },

  date: {
    fontSize: 12,
    color: "#888888",
    marginTop: 5,
  },

  status: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
  },

  paid: {
    backgroundColor: "#E2F5E8",
  },

  pending: {
    backgroundColor: "#FFF0D9",
  },

  statusText: {
    fontSize: 11,
    fontWeight: "bold",
    color: "#555555",
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

  totalContainer: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginTop: 18,
  },

  total: {
    fontSize: 20,
    fontWeight: "bold",
    color: "#F58220",
  },

  empty: {
    textAlign: "center",
    color: "#888888",
    marginTop: 30,
  },
});
