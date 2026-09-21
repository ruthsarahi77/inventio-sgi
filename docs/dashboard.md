# Dashboard

`GET /api/dashboard/stats` o `GET /api/dashboard/stats?year=2026&month=3`.
JWT obligatorio; ADMIN y SUPERVISOR pueden consultar métricas globales. VENDEDOR
no puede consultar cifras de otros vendedores a través de este endpoint (403).
La respuesta contiene siempre los diez campos del contrato, incluso si valen null.
`null` significa no determinable; cero es un resultado numérico conocido.

## Período y consultas

year y month se envían juntos o se omiten ambos. year: 1–9999; month: 1–12.
Los parámetros inválidos producen 400 usando GlobalExceptionHandler.
El reloj se interpreta en America/Lima por defecto, configurable mediante
`INVENTIO_DASHBOARD_ZONAHORARIA`. Los límites son inicio inclusivo y fin exclusivo,
primer día del mes siguiente. No se aplican MONTH/YEAR a columnas de fecha.

Cuatro consultas agregadas, sin cargar entidades ni colecciones: ventas, detalles,
productos activos y recibos. La transacción de lectura REPEATABLE_READ mantiene una
instantánea consistente entre las consultas. No hay consultas por registro ni N+1.

| Campo | Fuente y alcance |
| --- | --- |
| litrosVendidosMes | SUM(VentaDetalle.cantidad × Producto.volumen), ventas del período no ANULADAS; solo con confirmación de litros y sin volúmenes faltantes |
| metaRepsol | INVENTIO_DASHBOARD_METAREPSOL, BigDecimal no negativo; null si no está configurada |
| cumplimientoMeta | litros / meta × 100, HALF_UP a 2 decimales; 0 con meta 0; null si falta alguno de los datos |
| totalVentasMes | SUM(Venta.total) no ANULADAS del período; PEN solo si se confirma una moneda única PEN |
| totalVentasUSD | Misma suma si se confirma moneda única USD; no hay conversión |
| saldoPendienteTotal | SUM(Venta.saldo) de ventas PENDIENTES/PARCIALES originadas en el período, saldo actual |
| productosStockBajo | null: Producto no tiene stock mínimo |
| totalProductos | COUNT de productos actualmente ACTIVOS, catálogo actual independientemente del período |
| ventasHoy | COUNT de ventas no ANULADAS cuya fecha cae en hoy Y en el período seleccionado |
| recibosHoy | COUNT de recibos de hoy Y del período, excluyendo los de ventas ANULADAS |

Con período histórico, ventasHoy/recibosHoy son 0: el día actual no pertenece a ese
mes. No se cambia el significado de «hoy» a un día arbitrario del mes histórico.
El saldo no es un saldo reconstruido al cierre de mes: el modelo mantiene el saldo
actual. Tampoco existe historial de estados de productos/ventas para reconstruir
su estado a una fecha pasada. La meta configurada es mensual uniforme, no histórica.

## Datos que faltan y configuración explícita

No se agregaron campos, tablas ni valores simulados para ocultar estas limitaciones:

- Venta carece de moneda y tipo de cambio; no se puede separar PEN/USD de forma
  fiable. Ambos totales son null por defecto. Solo si TODAS las ventas pertenecen
  a una misma moneda, configurar `INVENTIO_DASHBOARD_MONEDAUNICA=PEN` o `USD`.
  La otra moneda devuelve 0, porque se confirmó que no existen ventas de ella.
  Esto no convierte dinero. No usar esta opción en una base con monedas mixtas.
- Producto.volumen es opcional y su unidad no está normalizada; Producto.unidad
  es texto libre. `INVENTIO_DASHBOARD_VOLUMENENLITROS=true` confirma explícitamente
  que todos los volúmenes representan litros por presentación. Si alguna línea
  vendida carece de volumen, se devuelve null, evitando una suma parcial engañosa.
  Sin esa confirmación, litros y cumplimiento son null. Los volúmenes provienen
  del catálogo actual, pues tampoco existe una instantánea del volumen vendido.
- Producto no tiene stock mínimo; no se asume que cero sea el umbral. Es necesario
  definir y persistir el mínimo antes de poder calcular productosStockBajo. El stock
  actual sí existe como suma de movimientos, pero no alcanza para esta métrica.
- La meta no existía: se centralizó en DashboardConfig. Configurar
  `INVENTIO_DASHBOARD_METAREPSOL` con litros mensuales, usando punto decimal.

No se reutiliza la moneda del PDF como prueba de la moneda de las ventas.
Los saldos se suman en las unidades monetarias almacenadas; el campo no implica
conversión a PEN. Si hay monedas mixtas debe ampliarse el modelo antes de sumar.

## Verificación

DashboardServiceTests cubre año/mes, febrero bisiesto, límites de zona horaria,
porcentajes, meta cero y datos no determinables. DashboardControllerTests verifica
JSON, null explícitos y errores 400. SecurityIntegrationTests comprueba roles/JWT.
DomainMappingTests valida las consultas JPQL con Hibernate sin una base de datos.

DashboardDatabaseTests es optativo: habilitar `INVENTIO_DASHBOARD_DB_TEST=true` con
las variables DB_URL, DB_USERNAME, DB_PASSWORD y JWT_SECRET. Fuerza ddl-auto=none,
sin inicialización SQL/bootstrap y conexiones de solo lectura. Exporta la respuesta
real a `target/dashboard-example.json`; no inserta datos ni crea tablas.
