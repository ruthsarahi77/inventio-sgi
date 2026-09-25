import { useCallback } from "react";
import { router, useLocalSearchParams } from "expo-router";
import { ActivityIndicator, Text, TouchableOpacity, View } from "react-native";
import ScreenLayout from "../../components/ScreenLayout";
import { useApp } from "../../models/AppContext";
import { useApiResource } from "../../hooks/use-api-resource";
import { getReceipt } from "../../services/receipts";
import { getSale } from "../../services/sales";
import { getCustomer } from "../../services/customers";
import { resourceId } from "../../services/product-validation";

export default function ReceiptDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { colors } = useApp();
  const { data, loading, error, reload } = useApiResource(useCallback(async (signal: AbortSignal) => {
    const receipt = await getReceipt(resourceId(id), signal);
    const sale = await getSale(receipt.ventaId, signal);
    const customer = await getCustomer(sale.clienteId, signal);
    return { receipt, sale, customer };
  }, [id]));
  return <ScreenLayout title="Recibo" subtitle="Comprobante del abono registrado">
    {loading && <ActivityIndicator color="#F58220" />}
    {error && <><Text accessibilityRole="alert" style={{ color: "#D94343" }}>{error}</Text><TouchableOpacity onPress={() => void reload()}><Text style={{ color: colors.text }}>Reintentar</Text></TouchableOpacity></>}
    {data && !loading && !error && <View style={{ backgroundColor: colors.surface, padding: 20, borderRadius: 14, gap: 16 }}>
      <Text style={{ color: colors.text, fontSize: 18, fontWeight: "bold" }}>{data.receipt.numero}</Text>
      <Text style={{ color: colors.secondary }}>{new Date(data.receipt.fecha).toLocaleString()}</Text>
      <Text style={{ color: colors.text }}>Cliente: {data.customer.nombre} · {data.customer.identificacion}</Text>
      <Text style={{ color: "#F58220", fontSize: 24, fontWeight: "bold" }}>Monto: {data.receipt.monto.toFixed(2)}</Text>
      {!!data.receipt.observacion && <Text style={{ color: colors.text }}>{data.receipt.observacion}</Text>}
      <TouchableOpacity onPress={() => router.push({ pathname: "/venta/[id]", params: { id: data.sale.id } })}><Text style={{ color: "#F58220" }}>Ver venta: {data.sale.numero}</Text></TouchableOpacity>
      <Text style={{ color: colors.text }}>Estado actual de la venta: {data.sale.estado}</Text>
      <Text style={{ color: colors.text }}>Total abonado: {data.sale.totalAbonado.toFixed(2)} · Saldo actual: {data.sale.saldo.toFixed(2)}</Text>
    </View>}
  </ScreenLayout>;
}
