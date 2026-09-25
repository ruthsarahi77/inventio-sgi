import {
  View,
  Text,
  TextInput,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
} from "react-native";

import { useState } from "react";
import { Ionicons } from "@expo/vector-icons";
import ScreenLayout from "../../components/ScreenLayout";
import { router } from "expo-router";
import { listInventory } from "../../services/inventory";
import { useApiResource } from "../../hooks/use-api-resource";
import { useApp } from "../../models/AppContext";

export default function StockScreen() {
  const { colors } = useApp();
  const { data, loading, error, reload } = useApiResource(listInventory);
  const products = data ?? [];
  const [search, setSearch] = useState("");

  const filteredProducts = products.filter(
    (product) =>
      product.nombre.toLowerCase().includes(search.toLowerCase()) ||
      product.codigo.toLowerCase().includes(search.toLowerCase()),
  );

  const lowStockCount = products.filter((product) => product.stockActual <= 5).length;

  return (
    <ScreenLayout
      title="Inventario"
      subtitle="Consulta el inventario"
      showBack={false}
    >
      <TouchableOpacity onPress={() => router.push("/tabs/productos")}><Text style={{ color: "#F58220", marginBottom: 16 }}>Ver catálogo de productos</Text></TouchableOpacity>
      {loading && <ActivityIndicator color="#F58220" />}
      {error && <View><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text>
        <TouchableOpacity onPress={() => void reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></View>}
      {!loading && !error && <>
      {/* BUSCADOR */}
      <View
        style={[
          styles.searchContainer,
          { backgroundColor: colors.surface, borderColor: colors.border },
        ]}
      >
        <Ionicons name="search-outline" size={22} color="#888888" />

        <TextInput
          style={[styles.search, { color: colors.text }]}
          placeholder="Buscar producto..."
          placeholderTextColor="#999999"
          value={search}
          onChangeText={setSearch}
        />

        {search.length > 0 && (
          <TouchableOpacity onPress={() => setSearch("")}>
            <Ionicons name="close-circle" size={22} color="#888888" />
          </TouchableOpacity>
        )}
      </View>

      {/* RESUMEN */}
      <View style={[styles.summary, { backgroundColor: colors.surface }]}>
        <View style={styles.summaryIcon}>
          <Ionicons name="cube-outline" size={28} color="#F58220" />
        </View>

        <View style={styles.summaryInfo}>
          <Text style={[styles.summaryTitle, { color: colors.secondary }]}>
            Productos registrados
          </Text>

          <Text style={[styles.summaryNumber, { color: colors.text }]}>
            {products.length}
          </Text>
        </View>
      </View>

      {/* AVISO DE STOCK BAJO */}
      {lowStockCount > 0 && (
        <View style={styles.lowStockBanner}>
          <Ionicons name="alert-circle-outline" size={23} color="#D94343" />

          <View style={styles.lowStockInfo}>
            <Text style={styles.lowStockTitle}>Atención: stock bajo</Text>

            <Text style={styles.lowStockDescription}>
              Hay {lowStockCount} producto(s) con 5 unidades o menos.
            </Text>
          </View>
        </View>
      )}

      {/* TÍTULO DE LA LISTA */}
      <View style={styles.sectionHeader}>
        <Text style={[styles.sectionTitle, { color: colors.text }]}>
          Inventario disponible
        </Text>

        <View style={styles.productCount}>
          <Text style={styles.productCountText}>{filteredProducts.length}</Text>
        </View>
      </View>

      {/* LISTA DE PRODUCTOS */}
      {filteredProducts.map((product) => (
        <TouchableOpacity
          onPress={() => router.push({ pathname: "/inventario/[id]", params: { id: product.idProducto } })}
          key={product.idProducto}
          style={[styles.card, { backgroundColor: colors.surface }]}
        >
          {/* ENCABEZADO DE LA TARJETA */}
          <View style={styles.cardHeader}>
            <View style={styles.iconContainer}>
              <Ionicons name="cube-outline" size={28} color="#F58220" />
            </View>

            <View style={styles.productInfo}>
              <Text style={[styles.productName, { color: colors.text }]}>
                {product.nombre}
              </Text>

              <Text style={styles.code}>Código: {product.codigo}</Text>
            </View>

            <Ionicons name="chevron-forward" size={20} color="#BBBBBB" />
          </View>

          <View style={styles.divider} />

          {/* INFORMACIÓN DEL PRODUCTO */}
          <View style={styles.cardFooter}>
            <View style={styles.stockContainer}>
              <Text style={[styles.label, { color: colors.secondary }]}>
                Stock disponible
              </Text>

              <View style={styles.stockRow}>
                <Ionicons
                  name="layers-outline"
                  size={17}
                  color={product.stockActual <= 5 ? "#D94343" : "#27864A"}
                />

                <Text
                  style={[
                    styles.stock,
                    product.stockActual <= 5 && styles.lowStockText,
                  ]}
                >
                  {product.stockActual} {product.unidad ?? "unidades"}
                </Text>
              </View>
            </View>

            <View style={styles.priceContainer}>
              <Text style={[styles.label, { color: colors.secondary }]}>
                Costo unitario
              </Text>

              <Text style={styles.price}>{product.costoUnitario.toFixed(2)}</Text>
            </View>
          </View>

          {/* ESTADO DEL STOCK */}
          <View
            style={[
              styles.statusBadge,
              product.stockActual <= 5 ? styles.statusLow : styles.statusAvailable,
            ]}
          >
            <Ionicons
              name={
                product.stockActual <= 5
                  ? "alert-circle-outline"
                  : "checkmark-circle-outline"
              }
              size={16}
              color={product.stockActual <= 5 ? "#D94343" : "#27864A"}
            />

            <Text
              style={[
                styles.statusText,
                product.stockActual <= 5
                  ? styles.statusLowText
                  : styles.statusAvailableText,
              ]}
            >
              {product.stockActual <= 5 ? "Stock bajo" : "Disponible"}
            </Text>
          </View>
        </TouchableOpacity>
      ))}

      {/* MENSAJE CUANDO NO HAY RESULTADOS */}
      {filteredProducts.length === 0 && (
        <View style={styles.emptyContainer}>
          <Ionicons name="search-outline" size={45} color="#BBBBBB" />

          <Text style={styles.emptyTitle}>No se encontraron productos</Text>

          <Text style={styles.empty}>
            Intenta buscar con otro nombre o código.
          </Text>
        </View>
      )}

      </>}
    </ScreenLayout>
  );
}

const styles = StyleSheet.create({
  /* BUSCADOR */
  searchContainer: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#FFFFFF",
    borderWidth: 1,
    borderColor: "#E0E0E0",
    borderRadius: 12,
    paddingHorizontal: 14,
    height: 52,
    marginBottom: 20,
  },

  search: {
    flex: 1,
    fontSize: 15,
    color: "#222222",
    marginLeft: 10,
    height: "100%",
  },

  /* RESUMEN */
  summary: {
    backgroundColor: "#FFFFFF",
    padding: 20,
    borderRadius: 14,
    marginBottom: 20,
    borderLeftWidth: 5,
    borderLeftColor: "#F58220",
    flexDirection: "row",
    alignItems: "center",
    elevation: 2,
    shadowColor: "#000000",
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.06,
    shadowRadius: 4,
  },

  summaryIcon: {
    width: 55,
    height: 55,
    borderRadius: 12,
    backgroundColor: "#FFF0E2",
    justifyContent: "center",
    alignItems: "center",
    marginRight: 15,
  },

  summaryInfo: {
    flex: 1,
  },

  summaryTitle: {
    color: "#777777",
    fontSize: 14,
  },

  summaryNumber: {
    fontSize: 30,
    fontWeight: "bold",
    color: "#222222",
    marginTop: 4,
  },

  /* AVISO DE STOCK BAJO */
  lowStockBanner: {
    backgroundColor: "#FFF0F0",
    borderWidth: 1,
    borderColor: "#F5C6C6",
    borderRadius: 12,
    padding: 14,
    marginBottom: 22,
    flexDirection: "row",
    alignItems: "center",
  },

  lowStockInfo: {
    flex: 1,
    marginLeft: 10,
  },

  lowStockTitle: {
    fontSize: 14,
    fontWeight: "bold",
    color: "#D94343",
  },

  lowStockDescription: {
    fontSize: 12,
    color: "#8B4444",
    marginTop: 4,
  },

  /* TÍTULO DE LA LISTA */
  sectionHeader: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 15,
  },

  sectionTitle: {
    fontSize: 19,
    fontWeight: "bold",
    color: "#222222",
  },

  productCount: {
    backgroundColor: "#FFF0E2",
    borderRadius: 15,
    paddingHorizontal: 10,
    paddingVertical: 4,
    marginLeft: 10,
  },

  productCountText: {
    fontSize: 12,
    fontWeight: "bold",
    color: "#F58220",
  },

  /* TARJETAS */
  card: {
    backgroundColor: "#FFFFFF",
    borderRadius: 14,
    padding: 16,
    marginBottom: 15,
    elevation: 2,
    shadowColor: "#000000",
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.06,
    shadowRadius: 4,
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

  /* INFORMACIÓN */
  cardFooter: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },

  stockContainer: {
    flex: 1,
  },

  label: {
    fontSize: 12,
    color: "#888888",
  },

  stockRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 5,
  },

  stock: {
    fontSize: 15,
    fontWeight: "bold",
    color: "#27864A",
    marginLeft: 5,
  },

  lowStockText: {
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

  /* ESTADO */
  statusBadge: {
    flexDirection: "row",
    alignItems: "center",
    alignSelf: "flex-start",
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 20,
    marginTop: 15,
  },

  statusAvailable: {
    backgroundColor: "#E8F5EC",
  },

  statusLow: {
    backgroundColor: "#FFF0F0",
  },

  statusText: {
    fontSize: 12,
    fontWeight: "bold",
    marginLeft: 5,
  },

  statusAvailableText: {
    color: "#27864A",
  },

  statusLowText: {
    color: "#D94343",
  },

  /* SIN RESULTADOS */
  emptyContainer: {
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 35,
  },

  emptyTitle: {
    fontSize: 16,
    fontWeight: "bold",
    color: "#555555",
    marginTop: 12,
  },

  empty: {
    textAlign: "center",
    color: "#888888",
    marginTop: 7,
    fontSize: 13,
  },

  /* BOTÓN */
  addButton: {
    backgroundColor: "#F58220",
    borderRadius: 12,
    padding: 16,
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    marginTop: 10,
    marginBottom: 20,
    elevation: 3,
  },

  addButtonText: {
    color: "#FFFFFF",
    fontWeight: "bold",
    fontSize: 16,
    marginLeft: 8,
  },
});
