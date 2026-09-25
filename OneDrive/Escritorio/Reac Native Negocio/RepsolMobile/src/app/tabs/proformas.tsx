import { useCallback, useState } from "react";
import { router } from "expo-router";
import { ActivityIndicator, StyleSheet, Text, TextInput, TouchableOpacity, View } from "react-native";
import ScreenLayout from "../../components/ScreenLayout";
import { useApp } from "../../models/AppContext";
import { useApiResource } from "../../hooks/use-api-resource";
import { listQuotes } from "../../services/quotes";
import { listCustomers } from "../../services/customers";

export default function QuotesScreen() {
  const { colors, user } = useApp();
  const [search, setSearch] = useState("");
  const { data, error, loading, reload } = useApiResource(useCallback(async (signal: AbortSignal) => {
    const [quotes, customers] = await Promise.all([listQuotes(signal), listCustomers(signal)]);
    return { quotes, customers };
  }, []));
  const customerName = (id: number) => data?.customers.find(item => item.id === id)?.nombre ?? "Cliente #" + id;
  const filtered = data?.quotes.filter(quote => (quote.numero + " " + customerName(quote.clienteId)).toLowerCase().includes(search.toLowerCase()));
  return <ScreenLayout title="Proformas" subtitle="Consulta y administra las proformas">
    {(user?.role === "admin" || user?.role === "vendedor") && <TouchableOpacity style={styles.addButton} onPress={() => router.push("/nueva-proforma")}>
      <Text style={styles.addButtonText}>Nueva proforma</Text>
    </TouchableOpacity>}
    {loading && <ActivityIndicator color="#F58220" />}
    {error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text><TouchableOpacity onPress={() => void reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {data && !loading && !error && <>
      <TextInput style={[styles.search, { color: colors.text, backgroundColor: colors.surface, borderColor: colors.border }]} placeholder="Buscar por número o cliente..."
        placeholderTextColor={colors.secondary} value={search} onChangeText={setSearch} />
      <View style={[styles.summary, { backgroundColor: colors.surface }]}>
        <Text style={{ color: colors.secondary }}>Proformas registradas</Text><Text style={styles.summaryNumber}>{data.quotes.length}</Text>
      </View>
      {filtered?.map(quote => <View key={quote.id} style={[styles.card, { backgroundColor: colors.surface }]}>
        <Text style={[styles.proformaNumber, { color: colors.text }]}>{quote.numero}</Text>
        <Text style={styles.date}>{new Date(quote.fecha).toLocaleString()}</Text>
        <Text style={{ color: colors.secondary }}>{quote.estado}</Text>
        <View style={styles.divider} />
        <Text style={[styles.client, { color: colors.text }]}>{customerName(quote.clienteId)}</Text>
        <Text style={styles.total}>Total: {quote.total.toFixed(2)}</Text>
        <TouchableOpacity style={styles.detailButton} onPress={() => router.push({ pathname: "/proforma/[id]", params: { id: quote.id } })}>
          <Text style={styles.detailButtonText}>Ver detalle y PDF</Text>
        </TouchableOpacity>
      </View>)}
      {filtered?.length === 0 && <Text style={{ color: colors.secondary }}>{data.quotes.length ? "No se encontraron proformas." : "No hay proformas registradas."}</Text>}
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
    padding: 20,
    borderRadius: 14,
    marginBottom: 25,
    borderLeftWidth: 5,
    borderLeftColor: "#F58220",
  },

  summaryLabel: {
    fontSize: 14,
    color: "#777777",
  },

  summaryNumber: {
    fontSize: 30,
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

  proformaNumber: {
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
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 20,
  },

  approved: {
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

  detailButton: {
    borderWidth: 1,
    borderColor: "#F58220",
    borderRadius: 10,
    padding: 12,
    alignItems: "center",
    marginTop: 15,
    flexDirection: "row",
    justifyContent: "center",
    gap: 7,
  },

  detailButtonText: {
    color: "#F58220",
    fontWeight: "bold",
  },

  pdfButton: {
    backgroundColor: "#F58220",
    borderRadius: 10,
    padding: 12,
    alignItems: "center",
    marginTop: 9,
    flexDirection: "row",
    justifyContent: "center",
    gap: 7,
  },
  pdfButtonText: { color: "#FFFFFF", fontWeight: "bold" },

  empty: {
    textAlign: "center",
    color: "#888888",
    marginVertical: 25,
  },

  addButton: {
    backgroundColor: "#F58220",
    borderRadius: 12,
    padding: 16,
    alignItems: "center",
    marginTop: 5,
    marginBottom: 20,
  },

  addButtonText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "bold",
  },
});
