import { useLocalSearchParams } from "expo-router";
import CommercialForm from "../components/CommercialForm";

export default function NewQuoteRoute() {
  const { clienteId } = useLocalSearchParams<{ clienteId?: string }>();
  const initialCustomer = typeof clienteId === "string" ? clienteId : "";
  return <CommercialForm key={initialCustomer} initialCustomer={initialCustomer} />;
}
