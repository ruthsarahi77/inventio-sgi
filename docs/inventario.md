# API de inventario

Documentacion del modulo de inventario, integrado ahora con el flujo comercial.
Controller -> InventoryService -> Repository -> PostgreSQL.
La [guia comercial](flujo-comercial.md) describe ventas, recibos y JWT.

## Preparacion para probar

1. Configurar JDK 25 y DB_URL, DB_USERNAME, DB_PASSWORD como indica el README.
2. Las tablas del dominio y al menos un producto deben existir. No se agrega
   un CRUD de productos en esta tarea.
3. Si las tablas se crearon con el modelo anterior, revisar y aplicar
   [inventario-ajustes.sql](sql/inventario-ajustes.sql): permite usuario_id nulo
   en movimientos y crea el indice (producto_id, fecha, id). No elimina datos.
   Si aun no existen tablas, el borrador `target/inventio-schema-preview.sql`
   generado por las pruebas refleja el modelo actualizado; requiere revision
   y aplicacion separada. No ejecutar el borrador sobre tablas ya existentes.
4. Ejecutar `.\mvnw.cmd clean verify` y `.\mvnw.cmd spring-boot:run`.

Se conserva `ddl-auto=none`: ningun ajuste SQL se aplica automaticamente.
Todas las rutas requieren Bearer JWT. ADMIN registra entradas/ajustes;
ADMIN, SUPERVISOR y VENDEDOR consultan stock; solo ADMIN/SUPERVISOR consultan kardex.
El usuario del movimiento se obtiene del token autenticado. El usuarioId enviado
por HTTP no permite atribuir una operacion a otro usuario.

## Endpoints

| Metodo | Ruta | Resultado |
| --- | --- | --- |
| GET | `/api/inventory` | 200; lista de productos con stock y valor de inventario |
| GET | `/api/inventory/{productId}` | 200; stock de un producto |
| GET | `/api/inventory/kardex/{productId}` | 200; movimientos ordenados y saldos acumulados |
| POST | `/api/inventory/entries` | 201; nueva ENTRADA |
| POST | `/api/inventory/adjustments/in` | 201; nuevo AJUSTE_ENTRADA |
| POST | `/api/inventory/adjustments/out` | 201; nuevo AJUSTE_SALIDA |

No hay endpoints PUT/PATCH/DELETE de movimientos ni POST de salida manual.
`registrarSalida` solo esta disponible como metodo del servicio para la futura
integracion interna; SalesService ya lo utiliza en una transaccion atomica.

## Ejemplos JSON

Usar `Content-Type: application/json` en POST. Los ejemplos suponen producto 1
existente, codigo P001, nombre Producto, costo 2.50 y stock inicial cero.
Reemplazar ese ID por el de un producto real. Los IDs de movimiento y fechas
de las respuestas los asigna el servidor.

**POST /api/inventory/entries**

```json
{"productoId":1,"cantidad":10}
```

**POST /api/inventory/adjustments/in**

```json
{"productoId":1,"cantidad":2,"observacion":"Diferencia positiva en conteo"}
```

**POST /api/inventory/adjustments/out**

```json
{"productoId":1,"cantidad":3,"observacion":"Diferencia negativa en conteo"}
```

Los tres POST aceptan tambien `usuarioId` y `documentoOrigen` opcionales:

```json
{
  "productoId":1,
  "cantidad":1.250,
  "usuarioId":7,
  "documentoOrigen":"COMPRA-001",
  "observacion":"Recepcion parcial"
}
```

En HTTP se usa siempre el usuario autenticado, aunque se envie usuarioId.
Las operaciones internas conservan compatibilidad con movimientos historicos sin usuario.
El tipo se decide por la ruta; la fecha se asigna en el servicio. No se recibe
un ID de movimiento ni un stock para actualizar registros previos.

Respuesta ilustrativa del tercer POST, despues de los dos primeros:

```json
{
  "idMovimiento":3,
  "idProducto":1,
  "tipoMovimiento":"AJUSTE_SALIDA",
  "cantidad":3.000,
  "fecha":"2026-09-14T18:02:00Z",
  "usuario":null,
  "documentoOrigen":null,
  "observacion":"Diferencia negativa en conteo",
  "saldoAcumulado":9.000
}
```

**GET /api/inventory** no lleva body. Despues de las tres operaciones:

```json
[
  {
    "idProducto":1,
    "codigo":"P001",
    "nombre":"Producto",
    "presentacion":null,
    "unidad":"unidad",
    "costoUnitario":2.50,
    "stockActual":9.000,
    "valorInventario":22.50000
  }
]
```

**GET /api/inventory/1** no lleva body; devuelve el objeto individual:

```json
{
  "idProducto":1,
  "codigo":"P001",
  "nombre":"Producto",
  "presentacion":null,
  "unidad":"unidad",
  "costoUnitario":2.50,
  "stockActual":9.000,
  "valorInventario":22.50000
}
```

**GET /api/inventory/kardex/1** no lleva body:

```json
{
  "idProducto":1,
  "codigo":"P001",
  "nombre":"Producto",
  "stockActual":9.000,
  "movimientos":[
    {"idMovimiento":1,"idProducto":1,"fecha":"2026-09-14T18:00:00Z","tipoMovimiento":"ENTRADA","cantidad":10.000,"documentoOrigen":null,"observacion":null,"usuario":null,"saldoAcumulado":10.000},
    {"idMovimiento":2,"idProducto":1,"fecha":"2026-09-14T18:01:00Z","tipoMovimiento":"AJUSTE_ENTRADA","cantidad":2.000,"documentoOrigen":null,"observacion":"Diferencia positiva en conteo","usuario":null,"saldoAcumulado":12.000},
    {"idMovimiento":3,"idProducto":1,"fecha":"2026-09-14T18:02:00Z","tipoMovimiento":"AJUSTE_SALIDA","cantidad":3.000,"documentoOrigen":null,"observacion":"Diferencia negativa en conteo","usuario":null,"saldoAcumulado":9.000}
  ]
}
```

Cuando existe usuario, se devuelve `{"id":7,"nombre":"Operador"}`. No se
devuelven email, password ni entidades JPA. Productos sin movimientos tienen
stock cero y kardex vacio. La consulta general incluye productos activos e
inactivos, sin omitir saldos historicos. Las listas son completas, sin paginacion
en esta primera version.

## Stock, valor y concurrencia

```text
stockActual = SUM(ENTRADA + AJUSTE_ENTRADA - SALIDA - AJUSTE_SALIDA)
valorInventario = costoUnitario * stockActual
```

El stock se consulta mediante `SUM(CASE...)` y `COALESCE`, agrupando los
movimientos por producto con LEFT JOIN. La lista completa se obtiene en una
consulta agregada; no se cargan colecciones ni se ejecuta una consulta por
producto. El valor se calcula en el servicio con BigDecimal, sin redondear.
Producto no contiene stock editable.

Cada operacion de escritura ejecuta una transaccion READ_COMMITTED:

1. Valida el DTO tambien cuando la llamada viene de otro servicio.
2. Bloquea el producto con PESSIMISTIC_WRITE (bloqueo PostgreSQL de fila).
3. Consulta el stock despues de obtener el bloqueo.
4. Aplica el signo del movimiento y rechaza saldo menor que cero.
5. Inserta un movimiento nuevo y mantiene el bloqueo hasta commit/rollback.

Se bloquea Producto, no la suma ni una lista de movimientos: funciona aunque
todavia no exista historial. Entradas y ajustes usan el mismo bloqueo, y afecta
tambien a otras instancias del backend conectadas a esa base. Si dos retiros de
7 compiten por stock 10, uno deja 3 y el segundo recibe 409 sin insertar nada.
La espera de [bloqueos PostgreSQL](https://www.postgresql.org/docs/17/explicit-locking.html)
y el uso de [READ_COMMITTED](https://www.postgresql.org/docs/15/applevel-consistency.html)
permiten recalcular con los movimientos confirmados despues de esperar.

La garantia exige que todas las escrituras de inventario pasen por este servicio.
La futura operacion de ventas debera invocarlo como bean Spring y usar una
transaccion READ_COMMITTED; no debe insertar movimientos directamente ni
invocarlo dentro de una transaccion de aislamiento diferente. SQL externo y
herramientas administrativas no pasan por estas reglas.

El kardex se ordena por fecha ASC, id ASC y su saldo se calcula recorriendo esa
misma lista. La nueva fecha es la hora del servidor, o un microsegundo posterior
a la ultima fecha si fuera necesario. No se admiten fechas retroactivas desde
la API que alteren el saldo historico. Hibernate marca los movimientos como
inmutables, y un callback impide su eliminacion mediante EntityManager.remove.
Las correcciones son movimientos nuevos de ajuste.

## Validaciones y errores

- Cantidad obligatoria, mayor que cero, hasta 16 digitos enteros y 3 decimales.
- Producto obligatorio; usuario opcional, validado si se informa.
- DocumentoOrigen maximo 100 caracteres; observacion maximo 2000.
- Producto o usuario inexistente: 404.
- JSON invalido, cantidad cero/negativa o precision invalida: 400.
- Stock insuficiente: 409 Conflict, de forma uniforme para SALIDA y AJUSTE_SALIDA.
- Conflicto de bloqueo traducido por Spring: 409 con indicacion de reintento.

Ejemplo: con stock 9, enviar cantidad 10 a adjustments/out:

```json
{
  "timestamp":"2026-09-14T18:03:00Z",
  "status":409,
  "error":"Conflict",
  "message":"Stock insuficiente para el producto 1. Disponible: 9.000; solicitado: 10.",
  "path":"/api/inventory/adjustments/out"
}
```

No se agrego idempotencia: repetir un POST exitoso crea otro movimiento. Este
modulo valida stock; SalesService y ReceiptService coordinan ventas y pagos.

## Pruebas

`InventoryServiceTests` cubre signos, cantidades, recursos inexistentes, retiro
exacto, exceso, orden bloqueo-consulta-insercion, trazabilidad y saldos del kardex.
`InventoryControllerTests` ejercita JSON, estados HTTP y la cadena real de
Spring Security mediante MockMvc. `DomainMappingTests` valida las consultas JPA
y genera el borrador SQL sin conexion.

`InventoryPostgresTests` usa PostgreSQL real (incluyendo ventas y pagos) y requiere variables separadas:

```text
INVENTIO_TEST_DB_URL=jdbc:postgresql://<HOST>:5432/<BASE-DE-PRUEBAS>?sslmode=require
INVENTIO_TEST_DB_USERNAME=<USUARIO-DE-PRUEBAS>
INVENTIO_TEST_DB_PASSWORD=<CONTRASENA-DE-PRUEBAS>
```

Usar una base de pruebas cuyo usuario pueda crear esquemas. La prueba crea un
esquema aleatorio `inventio_test_<uuid>`, crea sus tablas, prueba agregados y dos
retiros concurrentes, y elimina exclusivamente ese esquema al terminar. No usa
las variables DB_* de la aplicacion. Se ejecuta con `clean verify` o con
`.\mvnw.cmd -Dtest=InventoryPostgresTests test`. Sin sus variables, ambas pruebas
se omiten: las pruebas unitarias no acreditan concurrencia real en PostgreSQL.
