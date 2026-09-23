import {
  View,
  Text,
  TextInput,
  StyleSheet,
  TouchableOpacity,
} from "react-native";

import { useState } from "react";
import ScreenLayout from "../../components/ScreenLayout";
import { Ionicons } from "@expo/vector-icons";
import { useApp } from "../../models/AppContext";
import { router } from "expo-router";

interface Product {
  id: number;
  name: string;
  code: string;
  stock: number;
  price: number;
}

const products: Product[] = [
  {
    id: 1,
    name: "Aceite de motor",
    code: "REP-001",
    stock: 25,
    price: 18.5,
  },
  {
    id: 2,
    name: "Lubricante industrial",
    code: "REP-002",
    stock: 12,
    price: 24.75,
  },
  {
    id: 3,
    name: "Grasa multipropósito",
    code: "REP-003",
    stock: 5,
    price: 12.0,
  },
];

export default function StockScreen() {
  const [search, setSearch] = useState("");
  const { colors, formatMoney } = useApp();

  const filteredProducts = products.filter(
    (product) =>
      product.name.toLowerCase().includes(search.toLowerCase()) ||
      product.code.toLowerCase().includes(search.toLowerCase()),
  );

  return (
    <ScreenLayout
      title="Productos"
      subtitle="Consulta y administra el inventario"
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
        placeholder="Buscar producto..."
        value={search}
        onChangeText={setSearch}
      />

      {/* RESUMEN */}
      <View style={[styles.summary, { backgroundColor: colors.surface }]}>
        <Text style={[styles.summaryTitle, { color: colors.secondary }]}>
          Productos registrados
        </Text>
        <Text style={[styles.summaryNumber, { color: colors.text }]}>
          {products.length}
        </Text>
      </View>

      {/* LISTA */}
      <Text style={[styles.sectionTitle, { color: colors.text }]}>
        Inventario disponible
      </Text>

      {filteredProducts.map((product) => (
        <View
          key={product.id}
          style={[styles.card, { backgroundColor: colors.surface }]}
        >
          <View style={styles.cardHeader}>
            <View style={styles.iconContainer}>
              <Ionicons name="cube-outline" size={25} color="#F58220" />
            </View>

            <View style={styles.productInfo}>
              <Text style={[styles.productName, { color: colors.text }]}>
                {product.name}
              </Text>
              <Text style={styles.code}>Código: {product.code}</Text>
            </View>
          </View>

          <View style={styles.divider} />

          <View style={styles.cardFooter}>
            <View>
              <Text style={styles.label}>Stock disponible</Text>
              <Text
                style={[styles.stock, product.stock <= 5 && styles.lowStock]}
              >
                {product.stock} unidades
              </Text>
            </View>

            <View style={styles.priceContainer}>
              <Text style={styles.label}>Precio</Text>
              <Text style={styles.price}>{formatMoney(product.price)}</Text>
            </View>
          </View>

          {product.stock <= 5 && (
            <Text style={styles.warning}>⚠ Stock bajo</Text>
          )}
        </View>
      ))}

      {filteredProducts.length === 0 && (
        <Text style={styles.empty}>No se encontraron productos.</Text>
      )}

      <TouchableOpacity
        style={styles.addButton}
        onPress={() => router.push("/nuevo-producto")}
      >
        <Ionicons name="add-circle-outline" size={20} color="#FFFFFF" />
        <Text style={styles.addButtonText}>Nuevo producto</Text>
      </TouchableOpacity>
    </ScreenLayout>
  );
}

const styles = StyleSheet.create({
  search: {
    backgroundColor: "#FFFFFF",
    borderWidth: 1,
    borderColor: "#E0E0E0",
    borderRadius: 12,
    paddingHorizontal: 15,
    height: 50,
    fontSize: 15,
    marginBottom: 20,
  },

  summary: {
    backgroundColor: "#FFFFFF",
    padding: 20,
    borderRadius: 14,
    marginBottom: 25,
    borderLeftWidth: 5,
    borderLeftColor: "#F58220",
  },

  summaryTitle: {
    color: "#777777",
    fontSize: 14,
  },

  summaryNumber: {
    fontSize: 30,
    fontWeight: "bold",
    color: "#222222",
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
    width: 48,
    height: 48,
    borderRadius: 12,
    backgroundColor: "#FFF0E2",
    justifyContent: "center",
    alignItems: "center",
    marginRight: 12,
  },

  icon: {
    fontSize: 25,
  },

  productInfo: {
    flex: 1,
  },

  productName: {
    fontSize: 16,
    fontWeight: "bold",
    color: "#222222",
  },

  code: {
    fontSize: 12,
    color: "#888888",
    marginTop: 5,
  },

  divider: {
    height: 1,
    backgroundColor: "#EEEEEE",
    marginVertical: 15,
  },

  cardFooter: {
    flexDirection: "row",
    justifyContent: "space-between",
  },

  label: {
    fontSize: 12,
    color: "#888888",
  },

  stock: {
    fontSize: 16,
    fontWeight: "bold",
    color: "#27864A",
    marginTop: 5,
  },

  lowStock: {
    color: "#D94343",
  },

  priceContainer: {
    alignItems: "flex-end",
  },

  price: {
    fontSize: 17,
    fontWeight: "bold",
    color: "#F58220",
    marginTop: 5,
  },

  warning: {
    marginTop: 12,
    color: "#D94343",
    fontSize: 12,
    fontWeight: "bold",
  },

  empty: {
    textAlign: "center",
    color: "#888888",
    marginTop: 30,
  },

  addButton: {
    backgroundColor: "#F58220",
    borderRadius: 12,
    padding: 16,
    alignItems: "center",
    flexDirection: "row",
    justifyContent: "center",
    gap: 8,
    marginTop: 10,
  },

  addButtonText: {
    color: "#FFFFFF",
    fontWeight: "bold",
    fontSize: 16,
  },
});
