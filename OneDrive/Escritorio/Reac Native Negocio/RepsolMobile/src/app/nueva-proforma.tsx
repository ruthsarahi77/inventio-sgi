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
import ScreenLayout from "../components/ScreenLayout";
import { Ionicons } from "@expo/vector-icons";
import { useApp } from "../models/AppContext";

export default function NuevaProformaScreen() {
  const [client, setClient] = useState("");
  const [product, setProduct] = useState("");
  const [quantity, setQuantity] = useState("");
  const [price, setPrice] = useState("");
  const { formatMoney, colors } = useApp();

  const quantityNumber = Number(quantity);
  const priceNumber = Number(price);

  const total =
    quantityNumber > 0 && priceNumber > 0 ? quantityNumber * priceNumber : 0;

  const handleSave = () => {
    if (
      client.trim() === "" ||
      product.trim() === "" ||
      quantityNumber <= 0 ||
      priceNumber <= 0
    ) {
      Alert.alert(
        "Campos incompletos",
        "Completa todos los campos con información válida.",
      );
      return;
    }

    Alert.alert(
      "Proforma creada",
      "La información se validó correctamente. Todavía no se guarda en el backend.",
      [
        {
          text: "Aceptar",
          onPress: () => router.back(),
        },
      ],
    );
  };

  return (
    <ScreenLayout
      title="Nueva proforma"
      subtitle="Registra una proforma para un cliente"
    >
      {/* CLIENTE */}
      <Text style={[styles.label, { color: colors.text }]}>
        Nombre del cliente
      </Text>

      <TextInput
        style={[
          styles.input,
          {
            backgroundColor: colors.surface,
            borderColor: colors.border,
            color: colors.text,
          },
        ]}
        placeholder="Ingresa el nombre del cliente"
        value={client}
        onChangeText={setClient}
      />

      {/* PRODUCTO */}
      <Text style={[styles.label, { color: colors.text }]}>Producto</Text>

      <TextInput
        style={[
          styles.input,
          {
            backgroundColor: colors.surface,
            borderColor: colors.border,
            color: colors.text,
          },
        ]}
        placeholder="Nombre del producto"
        value={product}
        onChangeText={setProduct}
      />

      {/* CANTIDAD */}
      <Text style={[styles.label, { color: colors.text }]}>Cantidad</Text>

      <TextInput
        style={[
          styles.input,
          {
            backgroundColor: colors.surface,
            borderColor: colors.border,
            color: colors.text,
          },
        ]}
        placeholder="Ingresa la cantidad"
        value={quantity}
        onChangeText={setQuantity}
        keyboardType="numeric"
      />

      {/* PRECIO */}
      <Text style={[styles.label, { color: colors.text }]}>
        Precio unitario
      </Text>

      <TextInput
        style={[
          styles.input,
          {
            backgroundColor: colors.surface,
            borderColor: colors.border,
            color: colors.text,
          },
        ]}
        placeholder="Ingresa el precio"
        value={price}
        onChangeText={setPrice}
        keyboardType="decimal-pad"
      />

      {/* TOTAL */}
      <View style={[styles.totalCard, { backgroundColor: colors.surface }]}>
        <View style={styles.totalHeader}>
          <Ionicons name="calculator-outline" size={19} color="#F58220" />
          <Text style={[styles.totalLabel, { color: colors.secondary }]}>
            Total estimado
          </Text>
        </View>

        <Text style={styles.total}>{formatMoney(total)}</Text>
      </View>

      {/* GUARDAR */}
      <TouchableOpacity style={styles.saveButton} onPress={handleSave}>
        <Ionicons name="save-outline" size={20} color="#FFFFFF" />
        <Text style={styles.saveButtonText}>Guardar proforma</Text>
      </TouchableOpacity>
    </ScreenLayout>
  );
}

const styles = StyleSheet.create({
  label: {
    fontSize: 14,
    fontWeight: "600",
    color: "#333333",
    marginBottom: 8,
    marginTop: 12,
  },

  input: {
    height: 52,
    backgroundColor: "#FFFFFF",
    borderWidth: 1,
    borderColor: "#DDDDDD",
    borderRadius: 12,
    paddingHorizontal: 15,
    fontSize: 15,
  },

  totalCard: {
    backgroundColor: "#FFFFFF",
    borderRadius: 14,
    padding: 20,
    marginTop: 25,
    borderLeftWidth: 5,
    borderLeftColor: "#F58220",
  },

  totalLabel: {
    fontSize: 14,
    color: "#777777",
  },

  totalHeader: { flexDirection: "row", alignItems: "center", gap: 8 },

  total: {
    fontSize: 28,
    fontWeight: "bold",
    color: "#F58220",
    marginTop: 8,
  },

  saveButton: {
    height: 52,
    backgroundColor: "#F58220",
    borderRadius: 12,
    justifyContent: "center",
    alignItems: "center",
    flexDirection: "row",
    gap: 8,
    marginTop: 25,
  },

  saveButtonText: {
    color: "#FFFFFF",
    fontSize: 16,
    fontWeight: "bold",
  },
});
