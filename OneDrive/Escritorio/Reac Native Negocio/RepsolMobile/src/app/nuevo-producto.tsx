import {
  View,
  Text,
  TextInput,
  StyleSheet,
  TouchableOpacity,
  Alert,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
} from "react-native";

import { useState } from "react";
import { router } from "expo-router";
import { Ionicons } from "@expo/vector-icons";

import ScreenLayout from "../components/ScreenLayout";
import { useApp } from "../models/AppContext";
import { useProducts } from "../models/ProductContext";

export default function NuevoProductoScreen() {
  const [nombre, setNombre] = useState("");
  const [codigo, setCodigo] = useState("");
  const [precio, setPrecio] = useState("");
  const [stock, setStock] = useState("");
  const { colors, currency, formatMoney, convertToBaseCurrency } = useApp();
  const { addProduct } = useProducts();

  const registrarProducto = () => {
    if (
      nombre.trim() === "" ||
      codigo.trim() === "" ||
      precio.trim() === "" ||
      stock.trim() === ""
    ) {
      Alert.alert(
        "Campos obligatorios",
        "Por favor, completa todos los campos.",
      );
      return;
    }

    const precioNumero = Number(precio);
    const stockNumero = Number(stock);

    if (!Number.isFinite(precioNumero) || precioNumero <= 0) {
      Alert.alert("Precio inválido", "Ingresa un precio mayor que cero.");
      return;
    }

    if (!Number.isInteger(stockNumero) || stockNumero < 0) {
      Alert.alert(
        "Stock inválido",
        "Ingresa una cantidad entera igual o mayor que cero.",
      );
      return;
    }

    addProduct({
      name: nombre.trim(),
      code: codigo.trim().toUpperCase(),
      price: convertToBaseCurrency(precioNumero, currency),
      stock: stockNumero,
    });

    Alert.alert(
      "Producto registrado",
      `${nombre.trim()} se agregó al inventario.\nPrecio: ${formatMoney(convertToBaseCurrency(precioNumero, currency))}`,
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
      title="Nuevo producto"
      subtitle="Registra un producto en el inventario"
    >
      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : undefined}
      >
        <ScrollView
          showsVerticalScrollIndicator={false}
          keyboardShouldPersistTaps="handled"
        >
          {/* ENCABEZADO DEL FORMULARIO */}
          <View style={styles.introContainer}>
            <View style={styles.iconContainer}>
              <Ionicons name="cube-outline" size={35} color="#F58220" />
            </View>

            <Text style={[styles.introTitle, { color: colors.text }]}>
              Información del producto
            </Text>

            <Text style={[styles.introText, { color: colors.secondary }]}>
              Completa los siguientes datos para registrar un producto.
            </Text>
          </View>

          {/* FORMULARIO */}
          <View style={styles.form}>
            {/* NOMBRE */}
            <Text style={[styles.label, { color: colors.text }]}>
              Nombre del producto *
            </Text>

            <View
              style={[
                styles.inputContainer,
                { backgroundColor: colors.surface, borderColor: colors.border },
              ]}
            >
              <Ionicons name="pricetag-outline" size={20} color="#888888" />

              <TextInput
                style={[styles.input, { color: colors.text }]}
                placeholder="Ej. Aceite de motor"
                placeholderTextColor="#999999"
                value={nombre}
                onChangeText={setNombre}
                autoCapitalize="words"
              />
            </View>

            {/* CÓDIGO */}
            <Text style={[styles.label, { color: colors.text }]}>
              Código del producto *
            </Text>

            <View
              style={[
                styles.inputContainer,
                { backgroundColor: colors.surface, borderColor: colors.border },
              ]}
            >
              <Ionicons name="barcode-outline" size={20} color="#888888" />

              <TextInput
                style={[styles.input, { color: colors.text }]}
                placeholder="Ej. REP-001"
                placeholderTextColor="#999999"
                value={codigo}
                onChangeText={setCodigo}
                autoCapitalize="characters"
              />
            </View>

            {/* PRECIO */}
            <Text style={[styles.label, { color: colors.text }]}>
              Precio unitario ({currency}) *
            </Text>

            <View
              style={[
                styles.inputContainer,
                { backgroundColor: colors.surface, borderColor: colors.border },
              ]}
            >
              <Ionicons name="cash-outline" size={20} color="#888888" />

              <TextInput
                style={[styles.input, { color: colors.text }]}
                placeholder={currency === "PEN" ? "Ej. 18.50" : "Ej. 4.95"}
                placeholderTextColor="#999999"
                value={precio}
                onChangeText={setPrecio}
                keyboardType="decimal-pad"
              />
            </View>

            {/* STOCK */}
            <Text style={[styles.label, { color: colors.text }]}>
              Stock inicial *
            </Text>

            <View
              style={[
                styles.inputContainer,
                { backgroundColor: colors.surface, borderColor: colors.border },
              ]}
            >
              <Ionicons name="layers-outline" size={20} color="#888888" />

              <TextInput
                style={[styles.input, { color: colors.text }]}
                placeholder="Ej. 25"
                placeholderTextColor="#999999"
                value={stock}
                onChangeText={setStock}
                keyboardType="number-pad"
              />
            </View>

            <Text style={styles.helperText}>
              Ingresa la cantidad de unidades disponibles inicialmente.
            </Text>

            {/* BOTÓN REGISTRAR */}
            <TouchableOpacity
              style={styles.saveButton}
              activeOpacity={0.8}
              onPress={registrarProducto}
            >
              <Ionicons name="save-outline" size={22} color="#FFFFFF" />

              <Text style={styles.saveButtonText}>Registrar producto</Text>
            </TouchableOpacity>

            {/* BOTÓN CANCELAR */}
            <TouchableOpacity
              style={styles.cancelButton}
              activeOpacity={0.8}
              onPress={() => router.back()}
            >
              <Text style={styles.cancelButtonText}>Cancelar</Text>
            </TouchableOpacity>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </ScreenLayout>
  );
}

const styles = StyleSheet.create({
  /* INTRODUCCIÓN */
  introContainer: {
    alignItems: "center",
    marginBottom: 25,
  },

  iconContainer: {
    width: 75,
    height: 75,
    borderRadius: 20,
    backgroundColor: "#FFF0E2",
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 15,
  },

  introTitle: {
    fontSize: 20,
    fontWeight: "bold",
    color: "#222222",
    textAlign: "center",
  },

  introText: {
    fontSize: 13,
    color: "#777777",
    textAlign: "center",
    marginTop: 8,
    lineHeight: 20,
  },

  /* FORMULARIO */
  form: {
    backgroundColor: "#FFFFFF",
    borderRadius: 15,
    padding: 20,
    marginBottom: 25,
    elevation: 2,
    shadowColor: "#000000",
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.06,
    shadowRadius: 4,
  },

  label: {
    fontSize: 14,
    fontWeight: "bold",
    color: "#333333",
    marginBottom: 8,
    marginTop: 15,
  },

  inputContainer: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#FAFAFA",
    borderWidth: 1,
    borderColor: "#E0E0E0",
    borderRadius: 10,
    paddingHorizontal: 12,
    height: 52,
  },

  input: {
    flex: 1,
    height: "100%",
    fontSize: 14,
    color: "#222222",
    marginLeft: 10,
  },

  helperText: {
    fontSize: 12,
    color: "#888888",
    marginTop: 8,
    lineHeight: 18,
  },

  /* BOTÓN REGISTRAR */
  saveButton: {
    backgroundColor: "#F58220",
    borderRadius: 12,
    height: 52,
    flexDirection: "row",
    justifyContent: "center",
    alignItems: "center",
    marginTop: 25,
  },

  saveButtonText: {
    color: "#FFFFFF",
    fontSize: 15,
    fontWeight: "bold",
    marginLeft: 8,
  },

  /* BOTÓN CANCELAR */
  cancelButton: {
    backgroundColor: "#FFFFFF",
    borderWidth: 1,
    borderColor: "#DDDDDD",
    borderRadius: 12,
    height: 50,
    justifyContent: "center",
    alignItems: "center",
    marginTop: 12,
  },

  cancelButtonText: {
    color: "#666666",
    fontSize: 15,
    fontWeight: "bold",
  },
});
