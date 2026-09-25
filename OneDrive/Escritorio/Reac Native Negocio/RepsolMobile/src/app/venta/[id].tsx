import { useCallback, useRef, useState } from "react";
import { router, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, Text, TextInput, TouchableOpacity, View } from "react-native";
import ScreenLayout from "../../components/ScreenLayout";
import { useApp } from "../../models/AppContext";
import { useApiResource } from "../../hooks/use-api-resource";
import { getSale, cancelSale } from "../../services/sales";
import { listReceipts, createReceipt } from "../../services/receipts";
import { getCustomer } from "../../services/customers";
import { listProducts } from "../../services/products";
import { decimal, resourceId } from "../../services/product-validation";

export default function SaleRoute() {
  const { id } = useLocalSearchParams<{ id: string }>();
  return <SaleDetail key={id} id={id} />;
}

function SaleDetail({ id }: { id: string }) {
  const { colors, user } = useApp();
  const { data, loading, error, reload } = useApiResource(useCallback(async (signal: AbortSignal) => {
    const sale = await getSale(resourceId(id), signal);
    const [customer, products, receipts] = await Promise.all([
      getCustomer(sale.clienteId, signal), listProducts(signal), listReceipts(signal),
    ]);
    return { sale, customer, products, receipts: receipts.filter(receipt => receipt.ventaId === sale.id) };
  }, [id]));
  const [amount, setAmount] = useState("");
  const [observation, setObservation] = useState("");
  const [actionError, setActionError] = useState("");
  const [working, setWorking] = useState(false);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const busy = useRef(false);
  const canPay = user?.role === "admin" || user?.role === "vendedor";
  const action = async (cancel: boolean) => {
    if (!data || busy.current || (cancel ? user?.role !== "admin" : !canPay)) return;
    busy.current = true; setWorking(true); setActionError("");
    try {
      if (cancel) {
        await cancelSale(data.sale.id);
        setConfirmCancel(false);
        await reload();
      } else {
        const receipt = await createReceipt({ ventaId: data.sale.id, monto: decimal(amount, 2, 17), observacion: observation.trim() || null });
        setAmount(""); setObservation("");
        // Fetch the authoritative balance and receipts again, without local arithmetic.
        await reload();
        router.push({ pathname: "/recibo/[id]", params: { id: receipt.id } });
      }
    } catch (cause) { setActionError(cause instanceof Error ? cause.message : "No se pudo completar la operación."); }
    finally { busy.current = false; setWorking(false); }
  };
  const inputStyle = { color: colors.text, borderColor: colors.border, borderWidth: 1, borderRadius: 10, padding: 14 };
  return <ScreenLayout title="Venta" subtitle="Detalle, saldo y abonos">
    {loading && <ActivityIndicator color="#F58220" />}
    {error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text><TouchableOpacity onPress={() => void reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {data && !loading && !error && <View style={{ backgroundColor: colors.surface, padding: 20, borderRadius: 14, gap: 16 }}>
      <Text style={{ color: colors.text, fontSize: 18, fontWeight: "bold" }}>{data.sale.numero}</Text>
      <Text style={{ color: colors.secondary }}>{new Date(data.sale.fecha).toLocaleString()} · {data.sale.estado}</Text>
      <Text style={{ color: colors.text }}>Cliente: {data.customer.nombre} · {data.customer.identificacion}</Text>
      {data.sale.proformaId !== null && <TouchableOpacity onPress={() => router.push({ pathname: "/proforma/[id]", params: { id: data.sale.proformaId! } })}><Text style={{ color: "#F58220" }}>Proforma #{data.sale.proformaId}</Text></TouchableOpacity>}
      {data.sale.detalles.map(line => <View key={line.id} style={{ borderTopWidth: 1, borderColor: colors.border, paddingTop: 12, gap: 8 }}>
        <Text style={{ color: colors.text }}>{data.products.find(product => product.id === line.productoId)?.nombre ?? `Producto #${line.productoId}`}</Text>
        <Text style={{ color: colors.secondary }}>Cantidad: {line.cantidad} · Precio: {line.precioUnitario.toFixed(2)}</Text>
        <Text style={{ color: colors.text }}>Subtotal: {line.subtotal.toFixed(2)}</Text>
      </View>)}
      <Text style={{ color: "#F58220", fontSize: 24, fontWeight: "bold" }}>Total: {data.sale.total.toFixed(2)}</Text>
      <Text style={{ color: colors.text }}>Abonado: {data.sale.totalAbonado.toFixed(2)} · Saldo: {data.sale.saldo.toFixed(2)}</Text>
      <TouchableOpacity onPress={() => router.push("/tabs/stock")}><Text style={{ color: "#F58220" }}>Consultar inventario actualizado</Text></TouchableOpacity>
      <Text style={{ color: colors.text, fontWeight: "bold" }}>Recibos / abonos</Text>
      {data.receipts.length === 0 && <Text style={{ color: colors.secondary }}>No hay abonos registrados.</Text>}
      {data.receipts.map(receipt => <TouchableOpacity key={receipt.id} onPress={() => router.push({ pathname: "/recibo/[id]", params: { id: receipt.id } })}>
        <Text style={{ color: colors.text }}>{receipt.numero} · {receipt.monto.toFixed(2)}</Text>
      </TouchableOpacity>)}
      {canPay && data.sale.estado !== "ANULADA" && data.sale.saldo > 0 && <>
        <TextInput accessibilityLabel="Monto del abono" style={inputStyle} placeholder="Monto del abono" placeholderTextColor={colors.secondary} keyboardType="decimal-pad" maxLength={21} value={amount} onChangeText={setAmount} editable={!working} />
        <TextInput accessibilityLabel="Observación del abono" style={inputStyle} placeholder="Observación (opcional)" placeholderTextColor={colors.secondary} maxLength={2000} value={observation} onChangeText={setObservation} editable={!working} />
        <TouchableOpacity disabled={working} onPress={() => void action(false)} style={{ backgroundColor: "#F58220", padding: 16, borderRadius: 12 }}><Text style={{ color: "white", fontWeight: "bold" }}>Registrar abono</Text></TouchableOpacity>
        <Text style={{ color: colors.secondary }}>Si se interrumpe la conexión al registrar, revisa los recibos antes de repetir el abono.</Text>
      </>}
      {user?.role === "admin" && data.sale.estado !== "ANULADA" && data.receipts.length === 0 && data.sale.totalAbonado === 0 && <>
        {confirmCancel && <Text style={{ color: colors.text }}>¿Anular esta venta y devolver sus existencias al inventario?</Text>}
        <TouchableOpacity disabled={working} onPress={() => confirmCancel ? void action(true) : setConfirmCancel(true)}><Text style={{ color: "#D94343" }}>{confirmCancel ? "Confirmar anulación" : "Anular venta"}</Text></TouchableOpacity>
        {confirmCancel && <TouchableOpacity disabled={working} onPress={() => setConfirmCancel(false)}><Text style={{ color: colors.text }}>Conservar venta</Text></TouchableOpacity>}
      </>}
    </View>}
    {working && <ActivityIndicator color="#F58220" />}
    {!!actionError && <Text accessibilityRole="alert" style={{ color: "#D94343", marginTop: 16 }}>{actionError}</Text>}
  </ScreenLayout>;
}
