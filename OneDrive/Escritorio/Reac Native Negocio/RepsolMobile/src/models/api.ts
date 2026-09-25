export type Role = "ADMIN" | "VENDEDOR";

export interface AuthUserResponse {
  id: number;
  nombre: string;
  email: string;
  rol: Role;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUserResponse;
  usuario: {
    id: number;
    nombre: string;
    email: string;
    estado: "ACTIVO" | "INACTIVO";
    roles: Role[];
    rol: Role;
  };
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export interface StockProductoResponse {
  idProducto: number;
  codigo: string;
  nombre: string;
  presentacion: string | null;
  unidad: string | null;
  costoUnitario: number;
  stockActual: number;
  valorInventario: number;
}

export type EstadoRegistro = "ACTIVO" | "INACTIVO";
export interface ProductoRequest {
  codigo: string;
  nombre: string;
  descripcion: string | null;
  presentacion: string | null;
  volumen: number | null;
  unidad: string | null;
  costoUnitario: number;
}
export interface ProductoResponse extends ProductoRequest {
  id: number;
  estado: EstadoRegistro;
}
export interface MovimientoInventarioRequest {
  productoId: number;
  cantidad: number;
  documentoOrigen: string | null;
  observacion: string | null;
}
export interface MovimientoInventarioResponse {
  idMovimiento: number;
  idProducto: number;
  tipoMovimiento: "ENTRADA" | "SALIDA" | "AJUSTE_ENTRADA" | "AJUSTE_SALIDA";
  cantidad: number;
  fecha: string;
  usuario: { id: number; nombre: string } | null;
  documentoOrigen: string | null;
  observacion: string | null;
  saldoAcumulado: number;
}
export interface KardexResponse {
  idProducto: number;
  codigo: string;
  nombre: string;
  stockActual: number;
  movimientos: MovimientoInventarioResponse[];
}

export interface ClienteRequest {
  identificacion: string;
  nombre: string;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
}
export interface ClienteResponse extends ClienteRequest {
  id: number;
  estado: EstadoRegistro;
}
export interface DetalleComercialRequest {
  productoId: number;
  cantidad: number;
  precioUnitario: number;
}
export interface DetalleComercialResponse extends DetalleComercialRequest {
  id: number;
  subtotal: number;
}
export interface ProformaRequest {
  clienteId: number;
  detalles: DetalleComercialRequest[];
  observacion: string | null;
}
export interface ProformaResponse {
  id: number;
  numero: string;
  fecha: string;
  clienteId: number;
  vendedorId: number;
  estado: "EMITIDA" | "ANULADA";
  total: number;
  observacion: string | null;
  detalles: DetalleComercialResponse[];
}

export type VentaRequest =
  | { clienteId: number; detalles: DetalleComercialRequest[]; proformaId?: never }
  | { proformaId: number; clienteId?: never; detalles?: never };
export interface VentaResponse {
  id: number;
  numero: string;
  fecha: string;
  clienteId: number;
  vendedorId: number;
  proformaId: number | null;
  estado: "PENDIENTE" | "PARCIAL" | "PAGADA" | "ANULADA";
  total: number;
  totalAbonado: number;
  saldo: number;
  detalles: DetalleComercialResponse[];
}
export interface ReciboRequest {
  ventaId: number;
  monto: number;
  observacion: string | null;
}
export interface ReciboResponse extends ReciboRequest {
  id: number;
  numero: string;
  fecha: string;
  usuarioId: number;
}

export interface DashboardStatsResponse {
  litrosVendidosMes: number | null;
  metaRepsol: number | null;
  cumplimientoMeta: number | null;
  totalVentasMes: number | null;
  totalVentasUSD: number | null;
  saldoPendienteTotal: number;
  productosStockBajo: number | null;
  totalProductos: number;
  ventasHoy: number;
  recibosHoy: number;
}
