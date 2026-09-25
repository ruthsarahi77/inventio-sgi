import { useCallback, useRef, useState } from "react";
import { router } from "expo-router";
import { ActivityIndicator, Text, TextInput, TouchableOpacity, View } from "react-native";
import ScreenLayout from "./ScreenLayout";
import { useApp } from "../models/AppContext";
import { useApiResource } from "../hooks/use-api-resource";
import type { DetalleComercialRequest } from "../models/api";
import { listCustomers } from "../services/customers";
import { listProducts } from "../services/products";
import { createQuote } from "../services/quotes";
import { createSale } from "../services/sales";
import { decimal } from "../services/product-validation";

export default function CommercialForm({ initialCustomer = "", sale = false }: { initialCustomer?: string; sale?: boolean }) {
  const { colors, user } = useApp();
  const allowed = user?.role === "admin" || user?.role === "vendedor";
  const { data, loading, error, reload } = useApiResource(useCallback(async (signal: AbortSignal) => {
    const [customers, products] = await Promise.all([listCustomers(signal), listProducts(signal)]);
    return { customers: customers.filter(item => item.estado === "ACTIVO"), products: products.filter(item => item.estado === "ACTIVO") };
  }, []));
  const [customersOpen, setCustomersOpen] = useState(false);
  const [productsOpen, setProductsOpen] = useState(false);
  const [customerId, setCustomerId] = useState(initialCustomer);
  const [productId, setProductId] = useState("");
  const [customerSearch, setCustomerSearch] = useState("");
  const [productSearch, setProductSearch] = useState("");
  const [quantity, setQuantity] = useState("");
  const [price, setPrice] = useState("");
  const [observation, setObservation] = useState("");
  const [lines, setLines] = useState<(DetalleComercialRequest & { key: number })[]>([]);
  const sequence = useRef(0);
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);
  const busy = useRef(false);
  const selectedCustomer = data?.customers.find(item => String(item.id) === customerId);
  const selectedProduct = data?.products.find(item => String(item.id) === productId);
  const addLine = () => {
    if (saving) return;
    setFormError("");
    try {
      if (!selectedProduct) throw new Error("Selecciona un producto del catálogo.");
      if (lines.length >= 100) throw new Error("El máximo es de 100 detalles por documento.");
      const line = { productoId: selectedProduct.id, cantidad: decimal(quantity, 3, 16), precioUnitario: decimal(price, 2, 17), key: ++sequence.current };
      setLines(current => [...current, line]); setQuantity(""); setPrice(""); setProductId("");
    } catch (cause) { setFormError(cause instanceof Error ? cause.message : "Revisa el detalle."); }
  };
  const save = async () => {
    if (!allowed || busy.current) return;
    setFormError("");
    if (!selectedCustomer) { setFormError("Selecciona un cliente activo de la lista."); return; }
    if (!lines.length) { setFormError("Agrega al menos un producto."); return; }
    if (lines.some(line => !data?.products.some(product => product.id === line.productoId))) {
      setFormError("Un producto ya no está disponible. Retira esa línea y revisa el catálogo."); return;
    }
    busy.current = true; setSaving(true);
    try {
      const detalles = lines.map(line => ({ productoId: line.productoId, cantidad: line.cantidad, precioUnitario: line.precioUnitario }));
      const document = sale
        ? await createSale({ clienteId: selectedCustomer.id, detalles })
        : await createQuote({ clienteId: selectedCustomer.id, observacion: observation.trim() || null, detalles });
      router.replace({ pathname: sale ? "/venta/[id]" : "/proforma/[id]", params: { id: document.id } });
    } catch (cause) { setFormError(cause instanceof Error ? cause.message : "No se pudo guardar el documento."); }
    finally { busy.current = false; setSaving(false); }
  };
  const inputStyle = { backgroundColor: colors.surface, color: colors.text, borderColor: colors.border, borderWidth: 1, borderRadius: 12, padding: 14 };
  return <ScreenLayout title={sale ? "Nueva venta" : "Nueva proforma"} subtitle="Selecciona cliente y productos reales">
    <TouchableOpacity disabled={saving} onPress={() => router.push({ pathname: "/nuevo-cliente", params: { returnTo: sale ? "sale" : "quote" } })} style={{ marginBottom: 16 }}><Text style={{ color: "#F58220", fontWeight: "bold" }}>+ Registrar cliente</Text></TouchableOpacity>
    {loading && <ActivityIndicator color="#F58220" />}
    {error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text><TouchableOpacity onPress={() => void reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {data && !loading && !error && <View style={{ gap: 14 }}>
      <Text style={{ color: colors.text, fontWeight: "bold" }}>Cliente: {selectedCustomer ? selectedCustomer.nombre + " · " + selectedCustomer.identificacion : "Selecciona un cliente"}</Text>
      <TouchableOpacity accessibilityState={{ expanded: customersOpen }} onPress={() => setCustomersOpen(value => !value)}><Text style={{ color: "#F58220", fontWeight: "bold" }}>{customersOpen ? "Cerrar clientes" : "Seleccionar cliente"}</Text></TouchableOpacity>
      {customersOpen && <>
      <TextInput accessibilityLabel="Buscar cliente" style={inputStyle} placeholder="Buscar nombre o identificación" placeholderTextColor={colors.secondary} value={customerSearch} onChangeText={setCustomerSearch} editable={!saving} />
      {data.customers.filter(item => (item.nombre + " " + item.identificacion).toLowerCase().includes(customerSearch.toLowerCase())).map(item =>
        <TouchableOpacity key={item.id} disabled={saving} onPress={() => { setCustomerId(String(item.id)); setCustomersOpen(false); }} style={{ padding: 12, borderRadius: 10, backgroundColor: String(item.id) === customerId ? "#F58220" : colors.surface }}>
          <Text style={{ color: String(item.id) === customerId ? "#FFFFFF" : colors.text }}>{item.nombre} · {item.identificacion}</Text>
        </TouchableOpacity>)}
      {data.customers.length === 0 && <Text style={{ color: colors.secondary }}>No hay clientes activos.</Text>}
      </>}
      <Text style={{ color: colors.text, fontWeight: "bold" }}>Producto: {selectedProduct ? selectedProduct.nombre + " · " + selectedProduct.codigo : "Selecciona un producto"}</Text>
      <TouchableOpacity accessibilityState={{ expanded: productsOpen }} onPress={() => setProductsOpen(value => !value)}><Text style={{ color: "#F58220", fontWeight: "bold" }}>{productsOpen ? "Cerrar productos" : "Seleccionar productos"}</Text></TouchableOpacity>
      {productsOpen && <>
      <TextInput accessibilityLabel="Buscar producto" style={inputStyle} placeholder="Buscar nombre o código" placeholderTextColor={colors.secondary} value={productSearch} onChangeText={setProductSearch} editable={!saving} />
      {data.products.filter(item => (item.nombre + " " + item.codigo).toLowerCase().includes(productSearch.toLowerCase())).map(item =>
        <TouchableOpacity key={item.id} disabled={saving} onPress={() => setProductId(String(item.id))} style={{ padding: 12, borderRadius: 10, backgroundColor: String(item.id) === productId ? "#F58220" : colors.surface }}>
          <Text style={{ color: String(item.id) === productId ? "#FFFFFF" : colors.text }}>{item.nombre} · {item.codigo}</Text>
        </TouchableOpacity>)}
      {data.products.length === 0 && <Text style={{ color: colors.secondary }}>No hay productos activos.</Text>}
      <TextInput accessibilityLabel="Cantidad" style={inputStyle} placeholder="Cantidad" placeholderTextColor={colors.secondary} value={quantity} onChangeText={setQuantity} keyboardType="decimal-pad" maxLength={20} editable={!saving} />
      <TextInput accessibilityLabel="Precio unitario" style={inputStyle} placeholder="Precio unitario de venta" placeholderTextColor={colors.secondary} value={price} onChangeText={setPrice} keyboardType="decimal-pad" maxLength={21} editable={!saving} />
      <TouchableOpacity disabled={saving || !allowed} onPress={addLine}><Text style={{ color: "#F58220", fontWeight: "bold" }}>Agregar producto</Text></TouchableOpacity>
      </>}
      {lines.map(line => <View key={line.key} style={{ backgroundColor: colors.surface, padding: 16, borderRadius: 12, gap: 8 }}>
        <Text style={{ color: colors.text }}>{data.products.find(item => item.id === line.productoId)?.nombre ?? "Producto #" + line.productoId}</Text>
        <Text style={{ color: colors.text }}>Cantidad: {line.cantidad} · Precio: {line.precioUnitario.toFixed(2)}</Text>
        <TouchableOpacity disabled={saving} onPress={() => setLines(current => current.filter(item => item.key !== line.key))}><Text style={{ color: "#D94343" }}>Quitar</Text></TouchableOpacity>
      </View>)}
      {lines.length === 0 && <Text style={{ color: colors.secondary }}>Sin detalles agregados.</Text>}
      {!sale && <TextInput accessibilityLabel="Observación" style={inputStyle} placeholder="Observación (opcional)" placeholderTextColor={colors.secondary} value={observation} onChangeText={setObservation} maxLength={2000} editable={!saving} />}
      <Text style={{ color: colors.secondary }}>Los subtotales y el total definitivo se calculan al guardar y se muestran en el detalle. El precio ingresado no se toma del costo del producto.</Text>
      {!!formError && <Text accessibilityRole="alert" style={{ color: "#D94343" }}>{formError}</Text>}
      <Text style={{ color: colors.secondary }}>Si se interrumpe la conexión al guardar, consulta el listado antes de repetir la operación.</Text>
      <TouchableOpacity disabled={saving || !allowed} onPress={() => void save()} style={{ backgroundColor: "#F58220", padding: 16, borderRadius: 12, alignItems: "center" }}>
        {saving ? <ActivityIndicator color="#FFFFFF" /> : <Text style={{ color: "#FFFFFF", fontWeight: "bold" }}>{sale ? "Guardar venta" : "Guardar proforma"}</Text>}
      </TouchableOpacity>
    </View>}
  </ScreenLayout>;
}
