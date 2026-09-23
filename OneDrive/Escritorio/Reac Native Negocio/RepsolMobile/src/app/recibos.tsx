import {View,Text,TextInput,TouchableOpacity,Alert,StyleSheet,} from "react-native";
import { useState } from "react";
import ScreenLayout from "../components/ScreenLayout";
import { Ionicons } from "@expo/vector-icons";
import { useApp } from "../models/AppContext";

interface Receipt {
  id: number;
  number: string;
  client: string;
  date: string;
  amount: number;
  method: string;
}

const initialReceipts: Receipt[] = [
  {
    id: 1,
    number: "REC-001",
    client: "Comercial Andina",
    date: "18/09/2026",
    amount: 100.0,
    method: "Transferencia",
  },
  {
    id: 2,
    number: "REC-002",
    client: "Distribuidora del Norte",
    date: "18/09/2026",
    amount: 200.0,
    method: "Efectivo",
  },
  {
    id: 3,
    number: "REC-003",
    client: "Comercial Andina",
    date: "17/09/2026",
    amount: 75.5,
    method: "Transferencia",
  },
];

export default function RecibosScreen() {
  const [search, setSearch] = useState("");
  const [receipts, setReceipts] = useState(initialReceipts);
  const { formatMoney, colors } = useApp();

  const filteredReceipts = receipts.filter(
    (receipt) =>
      receipt.number.toLowerCase().includes(search.toLowerCase()) ||
      receipt.client.toLowerCase().includes(search.toLowerCase()),
  );

  const totalCollected = receipts.reduce(
    (total, receipt) => total + receipt.amount,
    0,
  );

  const addPayment = (receipt: Receipt) => {
    Alert.alert(
      "Registrar abono",
      `¿Registrar un abono de ${formatMoney(50)} para ${receipt.client}?`,
      [
        { text: "Cancelar", style: "cancel" },
        {
          text: "Registrar",
          onPress: () =>
            setReceipts((current) =>
              current.map((item) =>
                item.id === receipt.id
                  ? { ...item, amount: item.amount + 50 }
                  : item,
              ),
            ),
        },
      ],
    );
  };

  return (
    <ScreenLayout title="Recibos" subtitle="Consulta los pagos recibidos">
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
        placeholder="Buscar recibo o cliente..."
        value={search}
        onChangeText={setSearch}
      />

      {/* RESUMEN */}
      <View style={[styles.summary, { backgroundColor: colors.surface }]}>
        <Text style={[styles.summaryLabel, { color: colors.secondary }]}>
          Total de recibos
        </Text>

        <Text style={styles.summaryNumber}>{receipts.length}</Text>

        <Text style={[styles.summaryLabel, { color: colors.secondary }]}>
          Total recaudado
        </Text>

        <Text style={styles.summaryTotal}>{formatMoney(totalCollected)}</Text>
      </View>

      {/* LISTADO */}
      <Text style={[styles.sectionTitle, { color: colors.text }]}>
        Historial de recibos
      </Text>

      {filteredReceipts.map((receipt) => (
        <View
          key={receipt.id}
          style={[styles.card, { backgroundColor: colors.surface }]}
        >
          <View style={styles.cardHeader}>
            <View style={styles.iconContainer}>
              <Text style={styles.icon}>🧾</Text>
            </View>

            <View style={styles.receiptInfo}>
              <Text style={styles.receiptNumber}>{receipt.number}</Text>

              <Text style={styles.date}>{receipt.date}</Text>
            </View>

            <Text style={styles.amount}>{formatMoney(receipt.amount)}</Text>
          </View>

          <View style={styles.divider} />

          <Text style={styles.label}>Cliente</Text>

          <Text style={[styles.client, { color: colors.text }]}>
            {receipt.client}
          </Text>

          <View style={styles.methodContainer}>
            <Text style={styles.label}>Método de pago</Text>

            <Text style={styles.method}>{receipt.method}</Text>
          </View>

          <TouchableOpacity
            style={styles.paymentButton}
            onPress={() => addPayment(receipt)}
          >
            <Ionicons name="add-circle-outline" size={17} color="#F58220" />
            <Text style={styles.paymentButtonText}>Registrar abono</Text>
          </TouchableOpacity>
        </View>
      ))}

      {filteredReceipts.length === 0 && (
        <Text style={styles.empty}>No se encontraron recibos.</Text>
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
