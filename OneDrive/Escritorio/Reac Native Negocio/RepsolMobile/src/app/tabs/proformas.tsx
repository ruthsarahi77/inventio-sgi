import {
  View,
  Text,
  TextInput,
  StyleSheet,
  TouchableOpacity,
  Alert,
} from "react-native";

import { useState } from "react";
import { router } from "expo-router";
import ScreenLayout from "../../components/ScreenLayout";
import { Ionicons } from "@expo/vector-icons";
import * as Print from "expo-print";
import * as Sharing from "expo-sharing";
import { useApp } from "../../models/AppContext";

interface Proforma {
  id: number;
  number: string;
  client: string;
  date: string;
  total: number;
  status: string;
}

const initialProformas: Proforma[] = [
  {
    id: 1,
    number: "PRO-001",
    client: "Comercial Andina",
    date: "18/09/2026",
    total: 250.5,
    status: "Pendiente",
  },
  {
    id: 2,
    number: "PRO-002",
    client: "Distribuidora del Norte",
    date: "17/09/2026",
    total: 480.0,
    status: "Aprobada",
  },
];

export default function ProformasScreen() {
  const [search, setSearch] = useState("");
  const { formatMoney, colors } = useApp();

  const printProforma = async (proforma: Proforma) => {
    const html = `<html><body style="font-family:Arial;padding:32px"><h1 style="color:#F58220">INVENTIO</h1><h2>${proforma.number}</h2><p>Fecha: ${proforma.date}</p><p>Cliente: ${proforma.client}</p><hr/><h2>Total: ${formatMoney(proforma.total)}</h2></body></html>`;
    const { uri } = await Print.printToFileAsync({ html });
    if (await Sharing.isAvailableAsync())
      await Sharing.shareAsync(uri, {
        mimeType: "application/pdf",
        dialogTitle: "Compartir proforma",
      });
    else Alert.alert("PDF generado", uri);
  };

  const filteredProformas = initialProformas.filter(
    (proforma) =>
      proforma.number.toLowerCase().includes(search.toLowerCase()) ||
      proforma.client.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <ScreenLayout
      title="Proformas"
      subtitle="Consulta y administra las proformas"
    >
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
        <Text style={[styles.summaryLabel, { color: colors.secondary }]}>
          Proformas registradas
        </Text>

        <Text style={styles.summaryNumber}>{initialProformas.length}</Text>
      </View>

      {/* TÍTULO */}
      <Text style={[styles.sectionTitle, { color: colors.text }]}>
        Listado de proformas
      </Text>

      {/* LISTA */}
      {filteredProformas.map((proforma) => (
        <View
          key={proforma.id}
          style={[styles.card, { backgroundColor: colors.surface }]}
        >
          <View style={styles.cardHeader}>
            <View>
              <Text style={[styles.proformaNumber, { color: colors.text }]}>
                {proforma.number}
              </Text>

              <Text style={styles.date}>{proforma.date}</Text>
            </View>

            <View
              style={[
                styles.status,
                proforma.status === "Aprobada"
                  ? styles.approved
                  : styles.pending,
              ]}
            >
              <Text style={styles.statusText}>{proforma.status}</Text>
            </View>
          </View>

          <View style={styles.divider} />

          <Text style={styles.label}>Cliente</Text>
          <Text style={[styles.client, { color: colors.text }]}>
            {proforma.client}
          </Text>

          <View style={styles.totalContainer}>
            <Text style={styles.label}>Total</Text>

            <Text style={styles.total}>{formatMoney(proforma.total)}</Text>
          </View>

          <TouchableOpacity
            style={styles.detailButton}
            onPress={() =>
              Alert.alert(
                "Detalle de proforma",
                `${proforma.number}\nCliente: ${proforma.client}\nTotal: ${formatMoney(proforma.total)}`,
              )
            }
          >
            <Ionicons name="eye-outline" size={17} color="#F58220" />
            <Text style={styles.detailButtonText}>Ver detalles</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.pdfButton}
            onPress={() => void printProforma(proforma)}
          >
            <Ionicons name="print-outline" size={17} color="#FFFFFF" />
            <Text style={styles.pdfButtonText}>PDF / Imprimir</Text>
          </TouchableOpacity>
        </View>
      ))}

      {filteredProformas.length === 0 && (
        <Text style={styles.empty}>No se encontraron proformas.</Text>
      )}

      {/* NUEVA PROFORMA */}
      <TouchableOpacity
        style={styles.addButton}
        onPress={() => router.push("/nueva-proforma")}
      >
        <Ionicons name="add-circle-outline" size={20} color="#FFFFFF" />
        <Text style={styles.addButtonText}>Nueva proforma</Text>
      </TouchableOpacity>
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
  },

  addButtonText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "bold",
  },
});
