import { useCallback, useRef, useState } from "react";
import { router, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, Text, TouchableOpacity, View } from "react-native";
import ScreenLayout from "../../components/ScreenLayout";
import { useApp } from "../../models/AppContext";
import { useApiResource } from "../../hooks/use-api-resource";
import { getQuote, cancelQuote } from "../../services/quotes";
import { getCustomer } from "../../services/customers";
import { listProducts } from "../../services/products";
import { shareQuotePdf } from "../../services/quote-pdf";
import { createSale } from "../../services/sales";

export default function QuoteDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { colors, user } = useApp();
  const { data, loading, error, reload } = useApiResource(useCallback(async (signal: AbortSignal) => {
    if (!/^\d+$/.test(id) || !Number.isSafeInteger(Number(id)) || Number(id) <= 0) throw new Error("Proforma inválida.");
    const quote = await getQuote(Number(id), signal);
    const [customer, products] = await Promise.all([getCustomer(quote.clienteId, signal), listProducts(signal)]);
    return { quote, customer, products };
  }, [id]));
  const [actionError, setActionError] = useState("");
  const [working, setWorking] = useState(false);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [confirmSale, setConfirmSale] = useState(false);
  const busy = useRef(false);
  const convert = async () => {
    if (!data || busy.current || (user?.role !== "admin" && user?.role !== "vendedor")) return;
    busy.current = true; setWorking(true); setActionError("");
    try {
      const sale = await createSale({ proformaId: data.quote.id });
      setConfirmSale(false);
      router.replace({ pathname: "/venta/[id]", params: { id: sale.id } });
    } catch (cause) { setActionError(cause instanceof Error ? cause.message : "No se pudo crear la venta."); }
    finally { busy.current = false; setWorking(false); }
  };
  const action = async (cancel: boolean) => {
    if (!data || busy.current || (cancel && user?.role !== "admin")) return;
    busy.current = true; setWorking(true); setActionError("");
    try {
      if (cancel) { await cancelQuote(data.quote.id); setConfirmCancel(false); await reload(); }
      else await shareQuotePdf(data.quote.id);
    } catch (cause) { setActionError(cause instanceof Error ? cause.message : "No se pudo completar la operación."); }
    finally { busy.current = false; setWorking(false); }
  };
  return <ScreenLayout title="Proforma" subtitle="Detalle y documento original">
    {loading && <ActivityIndicator color="#F58220" />}
    {error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text><TouchableOpacity onPress={() => void reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {data && !loading && !error && <View style={{ backgroundColor: colors.surface, padding: 20, borderRadius: 14, gap: 16 }}>
      <Text style={{ color: colors.text, fontSize: 18, fontWeight: "bold" }}>{data.quote.numero}</Text>
      <Text style={{ color: colors.secondary }}>{new Date(data.quote.fecha).toLocaleString()} · {data.quote.estado}</Text>
      <Text style={{ color: colors.text }}>Cliente: {data.customer.nombre} · {data.customer.identificacion}</Text>
      {data.quote.detalles.map(line => <View key={line.id} style={{ borderTopWidth: 1, borderColor: colors.border, paddingTop: 12, gap: 8 }}>
        <Text style={{ color: colors.text }}>{data.products.find(product => product.id === line.productoId)?.nombre ?? `Producto #${line.productoId}`}</Text>
        <Text style={{ color: colors.secondary }}>Cantidad: {line.cantidad} · Precio: {line.precioUnitario.toFixed(2)}</Text>
        <Text style={{ color: colors.text }}>Subtotal: {line.subtotal.toFixed(2)}</Text>
      </View>)}
      <Text style={{ color: "#F58220", fontSize: 24, fontWeight: "bold" }}>Total: {data.quote.total.toFixed(2)}</Text>
      {!!data.quote.observacion && <Text style={{ color: colors.text }}>{data.quote.observacion}</Text>}
      {(user?.role === "admin" || user?.role === "vendedor") && data.quote.estado === "EMITIDA" && <>
        {confirmSale && <Text style={{ color: colors.text }}>Se creará una venta con este cliente y sus detalles, descontando existencias. Si ya fue convertida, el servidor rechazará la operación.</Text>}
        <TouchableOpacity disabled={working} onPress={() => confirmSale ? void convert() : setConfirmSale(true)}><Text style={{ color: "#F58220", fontWeight: "bold" }}>{confirmSale ? "Confirmar venta" : "Crear venta desde proforma"}</Text></TouchableOpacity>
        {confirmSale && <TouchableOpacity disabled={working} onPress={() => setConfirmSale(false)}><Text style={{ color: colors.text }}>Cancelar conversión</Text></TouchableOpacity>}
      </>}
      <TouchableOpacity disabled={working} onPress={() => void action(false)} style={{ backgroundColor: "#F58220", padding: 16, borderRadius: 12 }}>
        <Text style={{ color: "#FFFFFF", textAlign: "center", fontWeight: "bold" }}>Abrir / compartir PDF</Text>
      </TouchableOpacity>
      {user?.role === "admin" && data.quote.estado === "EMITIDA" && <>
        {confirmCancel && <Text style={{ color: colors.text }}>¿Confirmas la anulación de esta proforma?</Text>}
        <TouchableOpacity disabled={working} onPress={() => confirmCancel ? void action(true) : setConfirmCancel(true)}>
          <Text style={{ color: "#D94343" }}>{confirmCancel ? "Confirmar anulación" : "Anular proforma"}</Text>
        </TouchableOpacity>
        {confirmCancel && <TouchableOpacity disabled={working} onPress={() => setConfirmCancel(false)}><Text style={{ color: colors.text }}>Conservar proforma</Text></TouchableOpacity>}
      </>}
    </View>}
    {working && <ActivityIndicator color="#F58220" />}
    {!!actionError && <Text accessibilityRole="alert" style={{ color: "#D94343", marginTop: 16 }}>{actionError}</Text>}
  </ScreenLayout>;
}
