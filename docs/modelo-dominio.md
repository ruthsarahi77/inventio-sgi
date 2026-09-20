# Modelo de dominio inicial

Se mantienen Spring Boot 4.1.1, Java 25, el paquete `com.ruth.inventio` y la
infraestructura previa. No se agregaron dependencias ni se cambio la configuracion
de conexion. El modulo de [inventario](inventario.md) agrega consultas y registro
de movimientos; el [flujo comercial](flujo-comercial.md) incorpora servicios y JWT.
No hay CRUD generico de movimientos ni borrados.

## Estructura

```text
com.ruth.inventio
|-- entity
|   |-- EntidadCreada          (id, createdAt; no crea tabla)
|   |-- EntidadAuditable      (agrega updatedAt; no crea tabla)
|   |-- Usuario
|   |-- Rol
|   |-- Cliente
|   |-- Producto
|   |-- MovimientoInventario
|   |-- Proforma
|   |-- ProformaDetalle
|   |-- Venta
|   |-- VentaDetalle
|   `-- Recibo
|-- model
|   |-- EstadoRegistro        ACTIVO, INACTIVO
|   |-- TipoMovimiento       ENTRADA, SALIDA, AJUSTE_ENTRADA, AJUSTE_SALIDA
|   |-- EstadoProforma       EMITIDA, ANULADA
|   |-- EstadoVenta          PENDIENTE, PARCIAL, PAGADA, ANULADA
|   `-- NombreRol            ADMIN, SUPERVISOR, VENDEDOR
`-- repository
    |-- BaseRepository
    `-- Una interfaz <Entidad>Repository por cada una de las 10 entidades
```

## Relaciones y tablas

| Tabla | Entidad / relaciones principales |
| --- | --- |
| `usuarios` | Usuario; un usuario puede registrar muchas ventas, proformas, movimientos y recibos |
| `roles` | Rol; nombre unico de tipo NombreRol |
| `usuarios_roles` | Union muchos a muchos entre Usuario y Rol |
| `clientes` | Cliente; uno a muchos con proformas y ventas |
| `productos` | Producto; uno a muchos con movimientos y ambos tipos de detalle |
| `movimientos_inventario` | MovimientoInventario; pertenece a un producto; usuario opcional |
| `proformas` | Proforma; pertenece a un cliente y un usuario; tiene muchos detalles |
| `proforma_detalles` | ProformaDetalle; pertenece a una proforma y referencia un producto |
| `ventas` | Venta; pertenece a un cliente y un vendedor; tiene muchos detalles y recibos |
| `venta_detalles` | VentaDetalle; pertenece a una venta y referencia un producto |
| `recibos` | Recibo; pertenece a una venta y un usuario; cliente accesible mediante la venta |

Son **10 entidades y 11 tablas**, contando la union `usuarios_roles`. Los enums
se almacenan con `EnumType.STRING`, sin tablas de estados. `Rol` si es una entidad.
Los valores ADMIN, SUPERVISOR y VENDEDOR estan definidos; no se insertan filas
automaticamente ni se crean usuarios de prueba.

Todas las asociaciones son LAZY. Las relaciones muchos a uno son obligatorias,
salvo el usuario de MovimientoInventario, ahora opcional. Las obligatorias usan
`@NotNull` y claves foraneas no nulas. La columna del vendedor en
ventas es `vendedor_id`; en proformas, movimientos y recibos se usa `usuario_id`.
La relacion Usuario -> Rol es unidireccional y requiere al menos un rol mediante
Bean Validation. Las colecciones inversas usan `mappedBy` y se inicializan vacias.

El lado propietario es el campo `@ManyToOne` de cada hijo, y `Usuario.roles`
para la tabla de union. Al construir relaciones en memoria se deben mantener
ambos lados coherentes; agregar solo a una coleccion inversa no establece la FK.
No hay cascadas: los futuros servicios deberan guardar explicitamente padres,
roles y detalles en una transaccion. No se usa `orphanRemoval` ni eliminacion
en cascada al quitar elementos de colecciones.

## Decisiones de diseno

- IDs `Long` generados por identidad PostgreSQL, sin setters de ID.
- Todos los importes son `BigDecimal` / `numeric(19,2)`. Cantidad y volumen usan
  `BigDecimal` / `numeric(19,3)` para admitir fracciones. Bean Validation rechaza
  valores fuera de precision/escala; esta etapa no aplica redondeos.
- Cantidades y monto de recibo son estrictamente positivos. Costos, precios,
  subtotales, totales, total abonado y saldo son no negativos. Se declaran
  restricciones Bean Validation y SQL `CHECK` en los mapeos JPA.
- Producto no contiene stock. Volumen es opcional y, si existe, positivo;
  presentacion y unidad son texto libre. No se convierte entre unidades.
- Cliente usa `nombre` tanto para nombre personal como razon social.
  `identificacion` es texto obligatorio y unico, conservando ceros iniciales.
- Se eligio email obligatorio y unico como identificador de Usuario. La unicidad
  es la de PostgreSQL sobre el texto almacenado, sin normalizacion automatica
  de mayusculas/minusculas. El campo password queda reservado para un hash;
  UserService y el bootstrap codifican contrasenas con BCrypt. tokenVersion
  invalida JWT previos cuando cambia el usuario.
- Codigo de producto, identificacion, email de usuario, nombre de rol y numero
  de cada tipo de documento son unicos en sus respectivas tablas. No se genera
  numeracion automaticamente y no se impone unicidad entre tipos de documento.
- `fecha` e instantes de auditoria usan `Instant` (UTC). `fecha` es obligatoria
  y corresponde al documento/movimiento; es independiente de `createdAt`.
- Callbacks JPA en dos `@MappedSuperclass` registran `createdAt` y `updatedAt`.
  MovimientoInventario y Recibo tienen solo createdAt; las demas entidades,
  incluidos detalles y Rol, tienen ambos campos. No hay setters de auditoria.
  Estas fechas se mantienen en operaciones JPA, no en SQL ejecutado por fuera.
  El servicio de inventario asigna la fecha al agregar movimientos al final
  del kardex; no recibe fechas retroactivas desde HTTP.
- EstadoRegistro se comparte entre Usuario, Cliente y Producto. Sus estados
  iniciales son ACTIVO; Proforma inicia EMITIDA y Venta PENDIENTE.
- DocumentoCalculator calcula subtotales y totales en el backend. ReceiptService
  deriva totalAbonado de recibos y saldo del total menos los abonos. Los servicios
  exigen al menos un detalle. El precio comercial es independiente del costo.
- Venta tiene una referencia opcional unica a Proforma: una proforma puede
  convertirse una sola vez y sus datos se copian en el servidor.
- No se usa Lombok `@Data`, `@ToString` ni igualdad basada en relaciones.
  `@JsonIgnore` protege password y todas las asociaciones frente a serializacion
  accidental; los futuros controllers deberan usar DTO de todas formas.

## Repositorios y eliminacion

`BaseRepository` expone solamente `save`, `findById`, `findAll(Pageable)` y
`existsById`, siguiendo la [exposicion selectiva de Spring Data](https://docs.spring.io/spring-data/jpa/reference/repositories/definition.html).
Ningun repositorio expone metodos `delete`. No se implementa un mecanismo de
eliminacion fisica o logica; los estados ya modelan anulacion/inactivacion.
Esto limita la API de repositorios, no los privilegios de un administrador que
ejecute SQL directamente ni el uso futuro de EntityManager.remove.

Busquedas adicionales: codigo en Producto, email en Usuario, identificacion
en Cliente, nombre en Rol y numero en Proforma/Venta/Recibo; cada una con
`findBy...` y `existsBy...`. Detalles solo exponen los metodos base. Producto y
MovimientoInventario agregan las consultas de stock, bloqueo y kardex descritas
en la guia del inventario. No existe un CRUD HTTP generico para movimientos.

## Estado del esquema y pruebas

`spring.jpa.hibernate.ddl-auto=none` y `spring.sql.init.mode=never` se conservan.
**Esta tarea no crea ni modifica tablas en Supabase.** La lista anterior describe
el esquema que resultara de aplicar los mapeos mediante una futura migracion
revisada contra el esquema existente. Los `CHECK`, las FK y las restricciones
unicas solo se haran efectivos en PostgreSQL al crear/aplicar ese esquema.

Ejecutar con JDK 25:

```powershell
.\mvnw.cmd clean verify
```

`DomainMappingTests` construye Hibernate y los 10 repositorios sin conectar a una
BD, verifica consultas derivadas y genera `target/inventio-schema-preview.sql`.
Ese archivo es un borrador de DDL para revision, no una migracion ejecutada.
`DomainValidationTests` comprueba cantidades, dinero, relaciones obligatorias,
auditoria y serializacion. Las pruebas previas de conexion se conservan.
Sin variables DB_* no se prueba persistencia real ni acceso a Supabase.
