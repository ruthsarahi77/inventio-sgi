import { View, Text, TextInput, StyleSheet } from "react-native";

import { useState } from "react";
import ScreenLayout from "../../components/ScreenLayout";
import { useApp } from "../../models/AppContext";

interface Sale {
  id: number;
  number: string;
  client: string;
  date: string;
  total: number;
  status: string;
}

const sales: Sale[] = [
  {
    id: 1,
    number: "VEN-001",
    client: "Comercial Andina",
    date: "18/09/2026",
    total: 250.5,
    status: "Pagada",
  },
  {
    id: 2,
    number: "VEN-002",
    client: "Distribuidora del Norte",
    date: "18/09/2026",
    total: 480.0,
    status: "Pendiente",
  },
  {
    id: 3,
    number: "VEN-003",
    client: "Comercial Andina",
    date: "17/09/2026",
    total: 125.75,
    status: "Pendiente",
  },
];

export default function VentasScreen() {
  const [search, setSearch] = useState("");
  const { formatMoney, colors } = useApp();

  const filteredSales = sales.filter(
    (sale) =>
      sale.number.toLowerCase().includes(search.toLowerCase()) ||
      sale.client.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <ScreenLayout title="Ventas" subtitle="Consulta y administra las ventas">
      {/* BUSCADOR */}
      <TextInput
        style={[
          styles.search,
          {
            backgroundColor: colors.surface,
            borderColor: colors.border,
            color: colors.text,
          },
        ]}
        placeholder="Buscar por número o cliente..."
        value={search}
        onChangeText={setSearch}
      />

      {/* RESUMEN */}
      <View style={[styles.summary, { backgroundColor: colors.surface }]}>
        <Text style={styles.summaryLabel}>Ventas registradas</Text>

        <Text style={styles.summaryNumber}>{sales.length}</Text>

        <Text style={styles.summaryLabel}>Total vendido</Text>

        <Text style={styles.summaryTotal}>
          {formatMoney(sales.reduce((total, sale) => total + sale.total, 0))}
        </Text>
      </View>

      {/* LISTADO */}
      <Text style={styles.sectionTitle}>Historial de ventas</Text>

      {filteredSales.map((sale) => (
        <View key={sale.id} style={styles.card}>
          <View style={styles.cardHeader}>
            <View>
              <Text style={styles.saleNumber}>{sale.number}</Text>

              <Text style={styles.date}>{sale.date}</Text>
            </View>

            <View
              style={[
                styles.status,
                sale.status === "Pagada" ? styles.paid : styles.pending,
              ]}
            >
              <Text style={styles.statusText}>{sale.status}</Text>
            </View>
          </View>

          <View style={styles.divider} />

          <Text style={styles.label}>Cliente</Text>

          <Text style={[styles.client, { color: colors.text }]}>
            {sale.client}
          </Text>

          <View style={styles.totalContainer}>
            <Text style={styles.label}>Total de venta</Text>

            <Text style={styles.total}>{formatMoney(sale.total)}</Text>
          </View>
        </View>
      ))}

      {filteredSales.length === 0 && (
        <Text style={styles.empty}>No se encontraron ventas.</Text>
      )}
    </ScreenLayout>
  );
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
