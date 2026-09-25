import { router, useLocalSearchParams } from "expo-router";
import ScreenLayout from "../components/ScreenLayout";
import CustomerForm from "../components/CustomerForm";
import { createCustomer } from "../services/customers";

export default function NewCustomer() {
  const { returnTo } = useLocalSearchParams<{ returnTo?: string }>();
  return <ScreenLayout title="Nuevo cliente" subtitle="Datos del cliente">
    <CustomerForm onCancel={() => router.canGoBack() ? router.back() : router.replace("/tabs/clientes")} onSave={async value => {
      const customer = await createCustomer(value);
      if ((returnTo === "quote" || returnTo === "sale") && router.canGoBack()) {
        router.back();
        return;
      }
      router.replace({ pathname: "/cliente/[id]", params: { id: customer.id } });
    }} />
  </ScreenLayout>;
}
