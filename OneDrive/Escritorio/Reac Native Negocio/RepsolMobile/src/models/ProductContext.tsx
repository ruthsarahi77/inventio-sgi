import React, {createContext,useContext,useState,ReactNode,} from 'react';

export interface Product {
  id: number;
  name: string;
  code: string;
  stock: number;
  price: number;
}

interface ProductContextType {
  products: Product[];
  addProduct: (product: Omit<Product, 'id'>) => void;
}

const ProductContext = createContext<ProductContextType | undefined>(
  undefined
);

interface ProductProviderProps {
  children: ReactNode;
}

export function ProductProvider({ children }: ProductProviderProps) {
  const [products, setProducts] = useState<Product[]>([
    {
      id: 1,
      name: 'Aceite de motor',
      code: 'REP-001',
      stock: 25,
      price: 18.5,
    },
    {
      id: 2,
      name: 'Lubricante industrial',
      code: 'REP-002',
      stock: 12,
      price: 24.75,
    },
    {
      id: 3,
      name: 'Grasa multipropósito',
      code: 'REP-003',
      stock: 5,
      price: 12,
    },
  ]);

  const addProduct = (product: Omit<Product, 'id'>) => {
    const newProduct: Product = {
      ...product,
      id: Date.now(),
    };

    setProducts((currentProducts) => [
      ...currentProducts,
      newProduct,
    ]);
  };

  return (
    <ProductContext.Provider value={{ products, addProduct }}>
      {children}
    </ProductContext.Provider>
  );
}

export function useProducts() {
  const context = useContext(ProductContext);

  if (!context) {
    throw new Error(
      'useProducts debe utilizarse dentro de ProductProvider'
    );
  }

  return context;
}