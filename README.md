# Inventio: API REST de inventario y flujo comercial

API Spring Boot 4.1.1 / Java 21, paquete raiz `com.ruth.inventio`.
Arquitectura prevista: Controller -> Service -> Repository -> PostgreSQL.
`entity` contiene las entidades JPA, `model` sus enums y `repository` los
repositorios. `config`, `security`, `dto` y `exception` conservan la infraestructura
inicial. `controller` y `service` implementan inventario, catalogos, proformas,
ventas, recibos y administracion de usuarios/roles con JWT.
Ver [modelo de dominio](docs/modelo-dominio.md) para relaciones y tablas, y
[API de inventario](docs/inventario.md) para endpoints, ejemplos JSON y concurrencia.
La guia vigente para configuracion de seguridad, todos los endpoints y el flujo
completo es [flujo comercial y autenticacion](docs/flujo-comercial.md).

## Configuracion

Instalar o seleccionar un JDK 21 (`JAVA_HOME` debe apuntar a ese JDK).
Spring Boot lee las variables de entorno de forma nativa; no necesita dotenv.
Se corrigio en `mvnw.cmd` el acceso a un destino nulo de directorio en PowerShell.
`.env.example` es una plantilla y no se carga automaticamente.

En Supabase, abrir **Connect** y copiar los parametros de PostgreSQL.
Para redes IPv4, usar **Session pooler**, puerto 5432, con el host exacto
mostrado en el panel y el usuario `postgres.<PROJECT-ID>`.
La conexion directa usa `db.<PROJECT-ID>.supabase.co:5432` y usuario `postgres`;
normalmente requiere IPv6. No usar la URL HTTPS del proyecto como URL JDBC.

Variables obligatorias:

| Variable | Contenido |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://<HOST>:5432/postgres?sslmode=require` sin usuario ni contrasena |
| `DB_USERNAME` | Usuario PostgreSQL indicado en Connect |
| `DB_PASSWORD` | Contrasena de la base de datos |
| `JWT_SECRET` | Base64 de una clave aleatoria de al menos 32 bytes; no es una clave de Supabase |

Las claves publishable/secret y la URL JWKS no se utilizan para JDBC.
No guardar credenciales en archivos versionados. Si una clave secreta se ha
compartido fuera de su almacenamiento seguro, reemplazarla en Supabase.

Ejemplo para PowerShell (reemplazar host y usuario):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
$env:DB_URL = 'jdbc:postgresql://<HOST>:5432/postgres?sslmode=require'
$env:DB_USERNAME = 'postgres.<PROJECT-ID>'
$dbCredential = Get-Credential -UserName $env:DB_USERNAME -Message 'Contrasena PostgreSQL de Supabase'
$env:DB_PASSWORD = $dbCredential.GetNetworkCredential().Password
# Configurar tambien JWT_SECRET con una clave estable desde el gestor de secretos.
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Configurar opcionalmente `$env:SPRING_PROFILES_ACTIVE = 'dev'` para ver SQL,
sin logging de valores de parametros. SSL se exige tambien en el datasource.
JPA detecta PostgreSQL a traves del driver y los metadatos de conexion.
`ddl-auto=none` y `spring.sql.init.mode=never` evitan cambios automaticos del esquema.

## Verificacion

Al iniciar, `DatabaseConnectionVerifier` obtiene una conexion, ejecuta `SELECT 1`
y cierra los recursos. El mensaje de exito es:

```text
Conexion con PostgreSQL verificada correctamente (SELECT 1).
```

Si falla la conexion, el inicio falla: no basta con ver el puerto HTTP abierto.
No se crean tablas ni endpoints de diagnostico automaticamente. Solo el login
es publico; el resto requiere JWT y los roles permitidos. Los ajustes SQL para
tablas existentes y la provision inicial del ADMIN se describen en la guia comercial.

`clean verify` ejecuta pruebas del verificador, validaciones, auditoria,
serializacion y construccion de mapeos/repositorios sin base de datos. Genera
`target/inventio-schema-preview.sql` como borrador del DDL PostgreSQL, sin ejecutarlo.
La prueba de contexto
se ejecuta solamente cuando existe `DB_URL` y requiere tambien `DB_USERNAME`
`DB_PASSWORD` y `JWT_SECRET`: arranca JPA y el verificador contra esa base de datos. Con
`ddl-auto=none`, esta prueba no comprueba que existan las tablas del dominio.
Sin variables, esa prueba se omite; las pruebas unitarias no prueban acceso real a Supabase.
Tambien se prueban reglas comerciales, contratos HTTP, BCrypt, JWT y permisos.
`InventoryPostgresTests` habilita pruebas adicionales de SQL, rollback y concurrencia real
con `INVENTIO_TEST_DB_URL`, `INVENTIO_TEST_DB_USERNAME` y `INVENTIO_TEST_DB_PASSWORD`;
ver la guia de inventario antes de ejecutarlas.

`GlobalExceptionHandler` normaliza errores MVC y errores inesperados con
`timestamp`, `status`, `error`, `message` y `path`, sin devolver detalles internos.
Los rechazos de los filtros de seguridad usan `SecurityErrorHandler` y el mismo
formato de error que `@RestControllerAdvice`.

Fuentes: [conexion PostgreSQL/Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres)
y [JPA en Spring Boot](https://docs.spring.io/spring-boot/reference/data/sql.html).

## Arranque en IntelliJ (Java 21)

En **Run > Edit Configurations > InventioApplication**, seleccionar JRE 21.
En **Modify options > Environment variables**, agregar `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD` y `JWT_SECRET` con los formatos de la tabla anterior. No incluir
comillas alrededor de los valores. Las variables de una terminal no se heredan
por un IntelliJ que ya estaba abierto. `.env.example` no se carga automaticamente.

Para generar `JWT_SECRET` una sola vez en PowerShell:

```powershell
$keyBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($keyBytes)
[Convert]::ToBase64String($keyBytes)
$rng.Dispose()
```

Guardar el resultado en el gestor de secretos y en la variable de entorno del IDE;
conservar la misma clave entre reinicios. No usar una contrasena de texto ni una
clave de Supabase. `JWT_ISSUER`, `JWT_AUDIENCE` y `JWT_TTL_SECONDS` son opcionales
(valores predeterminados: `inventio-api`, `inventio-clients`, `900`). El TTL debe
estar entre 60 y 3600 segundos. `PORT` es opcional y vale 8080 por defecto.