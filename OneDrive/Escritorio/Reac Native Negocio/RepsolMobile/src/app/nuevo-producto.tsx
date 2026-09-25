import { router } from "expo-router";
import ScreenLayout from "../components/ScreenLayout";
import ProductForm from "../components/ProductForm";
import { createProduct } from "../services/products";

export default function NuevoProductoScreen() {
  return <ScreenLayout title="Nuevo producto" subtitle="Registra un producto en el catálogo">
    <ProductForm onCancel={() => router.canGoBack() ? router.back() : router.replace("/tabs/productos")} onSave={async value => {
      const product = await createProduct(value);
      router.replace({ pathname: "/producto/[id]", params: { id: product.id } });
    }} />
  </ScreenLayout>;
}
