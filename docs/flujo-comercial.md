# Flujo comercial y seguridad de Inventio

Spring Boot 4.1.1, Java 21 y Spring Security **7.1.1**, conservados. Se agregaron
el starter oficial `spring-boot-starter-security-oauth2-resource-server` y
`spring-security-test` para pruebas. Supabase sigue siendo PostgreSQL; esta API
autentica usuarios propios de `usuarios`, no usuarios de Supabase Auth.

## Configuracion y primer acceso

Variables obligatorias: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`.
JWT_SECRET es Base64 de al menos 32 bytes aleatorios y no tiene valor por defecto.
No utilizar publishable/secret keys de Supabase para firmar estos tokens.

Opcionales:

| Variable | Valor por defecto / uso |
| --- | --- |
| JWT_ISSUER | `inventio-api` |
| JWT_AUDIENCE | `inventio-clients` |
| JWT_TTL_SECONDS | `900`; rango 60-3600 segundos |
| CORS_ALLOWED_ORIGINS | Vacio; origenes exactos separados por coma, p. ej. `http://localhost:4200` |
| INITIAL_ADMIN_NAME | Nombre del ADMIN inicial, maximo 200 caracteres |
| INITIAL_ADMIN_EMAIL | Email del ADMIN inicial |
| INITIAL_ADMIN_PASSWORD | Clave inicial, 12-72 caracteres y maximo 72 bytes UTF-8 |

Para generar una clave JWT en PowerShell sin imprimirla:

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtBytes)
$jwtRng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
```

Guardar la clave en un gestor de secretos y conservarla entre reinicios. Rotarla
invalida los JWT anteriores. `.env.example` sigue siendo una plantilla; Spring
Boot no carga archivos .env automaticamente.

Se conserva la configuracion JPA existente (`ddl-auto=update`); este cambio de
autenticacion no agrega ni modifica tablas. Si existen las tablas del modelo anterior, revisar
y aplicar manualmente [inventario-ajustes.sql](sql/inventario-ajustes.sql) y
[comercial-seguridad.sql](sql/comercial-seguridad.sql), en ese orden. Este ultimo
agrega token_version, ventas.proforma_id, indices unicos y los tres roles.
Si no hay tablas, `clean verify` genera `target/inventio-schema-preview.sql`
como borrador para crear el esquema actualizado: revisarlo antes de aplicarlo.
Los scripts no se ejecutan al iniciar y no contienen usuarios ni contrasenas.

Para provisionar el ADMIN inicial, definir las tres variables de entorno:

```powershell
$env:INITIAL_ADMIN_NAME = 'Administrador'
$env:INITIAL_ADMIN_EMAIL = 'admin@example.com'
$adminCredential = Get-Credential -UserName $env:INITIAL_ADMIN_EMAIL -Message 'Clave inicial del ADMIN'
$env:INITIAL_ADMIN_PASSWORD = $adminCredential.GetNetworkCredential().Password
.\mvnw.cmd spring-boot:run
```

El bootstrap lee exclusivamente estas variables del entorno del proceso y crea
los roles faltantes y un ADMIN ACTIVO con BCrypt si ese email no existe, aunque
existan otros usuarios. Normaliza el email y lo compara sin distinguir mayusculas.
Si falta alguna variable o esta en blanco, el arranque continua sin crear el ADMIN.
Si el email existe, no cambia nombre, clave, roles ni estado. Los reinicios con
el mismo email no duplican el usuario. Una configuracion completa pero invalida
detiene el arranque con un mensaje sin credenciales. Retirar las tres variables
despues del primer arranque; crear nuevas cuentas desde `/api/users`, no cambiando
el email inicial. No existe registro publico. Los usuarios existentes
deben tener hashes BCrypt: no se convierten ni aceptan claves almacenadas en texto plano.

## Autenticacion y permisos

`POST /api/auth/login` es publico. El resto requiere:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Se utiliza NimbusJwtEncoder/NimbusJwtDecoder y el filtro Bearer de Spring Security,
sin filtros JWT artesanales. Se fija HS256 y se validan firma, expiracion, emisor
y audiencia. No se crea sesion de servidor ni se usan cookies de autenticacion.
CSRF se desactiva para esta API con Bearer explicito. CORS permite solo los
origenes configurados; React Native puede enviar el mismo token.

Cada token contiene el ID de usuario y tokenVersion. Cada peticion comprueba
usuario activo/version vigente y obtiene los roles actuales desde PostgreSQL;
no confia en roles proporcionados por el cliente. Actualizar un usuario o cambiar
su estado incrementa tokenVersion, invalidando todos sus tokens previos. No hay
refresh tokens en este MVP: tras vencer el acceso se requiere login nuevo.

| Operacion | ADMIN | SUPERVISOR | VENDEDOR |
| --- | --- | --- | --- |
| Consultar productos, clientes y stock | Si | Si | Si |
| Crear/editar productos y cambiar estado | Si | No | No |
| Registrar entradas/ajustes | Si | No | No |
| Consultar kardex | Si | Si | No |
| Crear/editar clientes | Si | No | Si |
| Consultar proformas y ventas | Todas | Todas | Solo propias |
| Crear proformas y ventas | Si | No | Si |
| Anular proformas/ventas | Si | No | No |
| Consultar recibos | Todos | Todos | De sus propias ventas |
| Registrar recibos | Si | No | Sobre sus propias ventas |
| Administrar usuarios y roles | Si | No | No |

Los permisos se aplican en la cadena de Spring Security y en metodos de escritura.
Los servicios restringen consultas por propietario, incluidas las consultas por ID.
Si un usuario tiene varios roles, los permisos se acumulan: ADMIN/SUPERVISOR pueden
consultar todas las operaciones. Los roles disponibles son fijos; se administran
sus registros y asignaciones a usuarios, no se redefinen permisos arbitrariamente.
No se permite que un administrador desactive su propia cuenta o retire su propio rol ADMIN.

## Todos los endpoints

Los POST de creacion devuelven 201; login, GET, PUT y PATCH devuelven 200.

| Metodo | Ruta |
| --- | --- |
| POST | `/api/auth/login` |
| GET | `/api/auth/me` |
| GET, POST | `/api/products` |
| GET, PUT | `/api/products/{id}` |
| PATCH | `/api/products/{id}/status` |
| GET, POST | `/api/customers` |
| GET, PUT | `/api/customers/{id}` |
| GET | `/api/inventory` |
| GET | `/api/inventory/{productId}` |
| GET | `/api/inventory/kardex/{productId}` |
| POST | `/api/inventory/entries` |
| POST | `/api/inventory/adjustments/in` |
| POST | `/api/inventory/adjustments/out` |
| GET, POST | `/api/quotes` |
| GET | `/api/quotes/{id}` |
| PATCH | `/api/quotes/{id}/cancel` |
| GET, POST | `/api/sales` |
| GET | `/api/sales/{id}` |
| PATCH | `/api/sales/{id}/cancel` |
| GET, POST | `/api/receipts` |
| GET | `/api/receipts/{id}` |
| GET, POST | `/api/users` |
| PUT | `/api/users/{id}` |
| PATCH | `/api/users/{id}/status` |
| GET, POST | `/api/roles` |

Las listas no llevan cuerpo ni paginacion en esta version. No hay DELETE ni
salida manual publica; tampoco actualizacion generica de ventas, recibos o
movimientos historicos. Los cambios de stock son nuevos movimientos trazables.

## Ejemplos JSON y flujo de prueba

1. **POST /api/auth/login** con el usuario provisionado:

```json
{"email":"admin@example.com","password":"<TU_PASSWORD>"}
```

Respuesta: `accessToken`, `tokenType: "Bearer"`, `user` con `id`, `nombre`, `email`
y `rol`. Por compatibilidad tambien se conservan `expiresIn: 900` y `usuario`
sin password ni hash. Usar el token en todas las peticiones siguientes.

2. **POST /api/products** como ADMIN:

```json
{"codigo":"P001","nombre":"Producto ejemplo","presentacion":"Caja","unidad":"unidad","costoUnitario":2.50}
```

Conservar el ID devuelto (los ejemplos siguientes suponen 1). Descripcion y volumen
son opcionales. El PUT recibe los mismos campos. No hay campo stock en el contrato.
Para inactivar: **PATCH /api/products/1/status** con `{"estado":"INACTIVO"}`.
Los codigos son unicos; el producto nuevo inicia ACTIVO.

3. **POST /api/customers**:

```json
{"identificacion":"0012345678","nombre":"Cliente ejemplo","telefono":"0990000000","email":"cliente@example.com","direccion":"Direccion de entrega"}
```

Conservar el ID devuelto (aqui 1). El PUT recibe los mismos campos; la identificacion
es texto y debe ser unica. No existe eliminacion fisica de clientes.

4. **POST /api/inventory/entries** como ADMIN para disponer de existencias:

```json
{"productoId":1,"cantidad":10,"documentoOrigen":"COMPRA-001","observacion":"Ingreso inicial"}
```

El actor se obtiene del token; usuarioId enviado en HTTP no suplanta a otro usuario.

5. **POST /api/quotes**:

```json
{"clienteId":1,"detalles":[{"productoId":1,"cantidad":2,"precioUnitario":5.00}],"observacion":"Cotizacion inicial"}
```

Se crea EMITIDA, vendedor = usuario autenticado, subtotal = 10.00 y total = 10.00.
No reserva ni cambia stock. Puede anularse solo si no esta convertida en venta.

6. **POST /api/sales**, usando la proforma devuelta (aqui 1):

```json
{"proformaId":1}
```

El servidor copia cliente, productos, cantidades y precios de la proforma; no se
envian clienteId ni detalles junto con proformaId. Se permite una sola conversion
por proforma, incluso si despues se anula la venta. Vendedores solo convierten
sus propias proformas; ADMIN puede convertir cualquiera que este EMITIDA.

Tambien puede crearse una venta directa, sin proforma:

```json
{"clienteId":1,"detalles":[{"productoId":1,"cantidad":2,"precioUnitario":5.00}]}
```

Estos son caminos alternativos; ejecutar ambos crea dos ventas diferentes.
Los DTO no reciben vendedor, total, subtotal, totalAbonado, saldo ni stock.
La venta tiene total 10.00, totalAbonado 0.00, saldo 10.00 y estado PENDIENTE.
Genera una SALIDA de 2 unidades y stock final 8. Su numero se usa como documentoOrigen.

7. **POST /api/receipts**, usando el ID real de la venta (aqui 1):

```json
{"ventaId":1,"monto":4.00,"observacion":"Primer abono"}
```

La venta pasa a PARCIAL, totalAbonado 4.00 y saldo 6.00. Luego:

```json
{"ventaId":1,"monto":6.00,"observacion":"Pago final"}
```

Pasa a PAGADA y saldo cero. Consultar **GET /api/sales/1** para ver el resultado.
Monto mayor al saldo devuelve 409 sin guardar recibo.

8. **PATCH /api/sales/{id}/cancel**, sin body, como ADMIN: solo funciona para
ventas sin recibos. Devuelve ANULADA y genera AJUSTE_ENTRADA por cada detalle,
con el mismo numero de venta como origen y observacion de compensacion. Repetir
la anulacion o intentar anular la venta pagada del ejemplo devuelve 409.

Para crear un vendedor como ADMIN, **POST /api/users**:

```json
{"nombre":"Vendedor ejemplo","email":"vendedor@example.com","password":"<PASSWORD-DE-12-O-MAS-CARACTERES>","roles":["VENDEDOR"]}
```

PUT /api/users/{id} recibe nombre, email, roles y password opcional; omitir password
conserva el hash actual. PATCH /api/users/{id}/status recibe `{"estado":"INACTIVO"}`
o ACTIVO. Ambos invalidan los tokens anteriores. GET /api/roles muestra el catalogo;
POST /api/roles recibe `{"nombre":"VENDEDOR"}` si ese registro aun no existe (si existe, 409).

## Consistencia y decisiones comerciales

- El precio comercial se introduce manualmente y es independiente de costoUnitario.
  Cantidad y precio deben ser positivos; cada subtotal se redondea a dos decimales
  con HALF_UP y el total es la suma de esos subtotales. Se rechazan subtotales que
  redondeen a cero e importes fuera de numeric(19,2). No se calculan impuestos.
- Se generan numeros `PRO-<UUID>`, `VEN-<UUID>` y `REC-<UUID>`, con unicidad en BD;
  no se utiliza MAX(numero)+1 ni se recibe numeracion arbitraria desde el cliente.
- Las ventas y proformas requieren cliente y productos activos. El vendedor se
  obtiene del usuario autenticado activo. Todas las fechas se asignan en backend.
- SalesService ejecuta validacion, calculos, cabecera, detalles y movimientos en
  una sola transaccion READ_COMMITTED. Una falla en cualquier salida revierte
  tambien cabecera, detalles y salidas anteriores de esa misma venta.
- InventoryService sigue siendo la unica logica de disponibilidad y movimientos.
  Bloquea productos en orden ascendente de ID antes de procesar ventas con varios
  productos, evitando ciclos de bloqueos. Luego cada salida recalcula disponibilidad
  bajo el bloqueo existente. Detalles repetidos del mismo producto consumen el
  saldo acumulativamente; si no alcanza, se revierte toda la venta.
- ReceiptService bloquea la venta antes de consultar SUM(recibos.monto). Verifica
  monto <= total - sumatoria, guarda el recibo y vuelve a sumar para actualizar
  totalAbonado, saldo y estado. Pagos simultaneos se serializan sobre esa fila.
- La anulacion toma el mismo bloqueo de venta que los pagos, valida ausencia de
  recibos y compensa inventario dentro de la transaccion. Nunca borra movimientos.
  No hay anulacion/devolucion de recibos en este MVP: todos los recibos son validos.
- En ANULADA se conservan total, totalAbonado y saldo como datos historicos;
  ANULADA no representa una cuenta cobrable. Se rechazan nuevos pagos sobre ella.
- No hay idempotencia general de POST: repetir una creacion exitosa crea otra
  operacion. La conversion de proforma y la anulacion si impiden repeticion.

El mecanismo utiliza [bloqueos PostgreSQL](https://www.postgresql.org/docs/17/explicit-locking.html)
y transacciones Spring. No hay otra formula de stock en SalesService.
Las reglas protegen las operaciones de esta API; SQL administrativo ejecutado
directamente no pasa por los servicios.

## Errores

Se reutiliza ApiError con timestamp, status, error, message y path:
400 para datos invalidos, 401 para autenticacion, 403 para permisos/propiedad,
404 para recursos inexistentes y 409 para duplicados, stock insuficiente,
recibos superiores al saldo, documentos anulados o ventas con recibos que se
intentan anular. Los detalles internos de PostgreSQL y las credenciales no se devuelven.

## Estructura final

```text
com.ruth.inventio
  config       DatabaseConnectionVerifier, AdminBootstrap
  security     SecurityConfig, JwtConfig, JwtUserConverter, CurrentUser,
               UsuarioPrincipal, SecurityErrorHandler
  controller   Auth, Product, Customer, Inventory, Quote, Sales, Receipt, User, Role
  service      AuthService, ProductService, CustomerService, QuoteService,
               SalesService, ReceiptService, UserService, RoleService,
               InventoryService, InventoryApiService, DocumentoCalculator
  repository   Repositorios JPA, consultas agregadas, bloqueos y filtros por vendedor
  entity       Las diez entidades, auditoria; Venta.proforma y Usuario.tokenVersion
  dto          Requests y responses separados, ApiError; nunca entidades HTTP
  mapper       ComercialMapper
  exception    Manejador global, recurso inexistente, stock, codigo y reglas comerciales
  model        Enums existentes
  validation   Paquete reservado
  util         Paquete reservado
```

## Verificacion

```powershell
.\mvnw.cmd clean verify
```

Las pruebas cubren calculos, salidas delegadas, abonos, anulaciones, duplicados,
propiedad, BCrypt, JWT con firma real, emisor/audiencia/expiracion, revocacion,
CORS y permisos de los tres roles, ademas de las pruebas previas.

La suite PostgreSQL condicional se activa con INVENTIO_TEST_DB_URL,
INVENTIO_TEST_DB_USERNAME e INVENTIO_TEST_DB_PASSWORD, sobre una base de pruebas
con permiso de crear esquemas. Crea un esquema aleatorio propio y lo elimina
al finalizar. Prueba rollback real de cabeceras/detalles/movimientos, conversion,
anulacion, pagos y ventas/recibos concurrentes. Sin esas variables se omite.
La prueba de arranque con DB_* requiere tambien JWT_SECRET. Las pruebas sin BD
no acreditan acceso real a Supabase ni la ejecucion de una migracion.

Referencia de seguridad: [JWT Resource Server de Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
