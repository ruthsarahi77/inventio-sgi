import { Stack } from "expo-router";
import { ProductProvider } from "../models/ProductContext";
import { AppProvider } from "../models/AppContext";

export default function RootLayout() {
  return (
    <AppProvider>
      <ProductProvider>
        <Stack screenOptions={{ headerShown: false }} />
      </ProductProvider>
    </AppProvider>
  );
}
