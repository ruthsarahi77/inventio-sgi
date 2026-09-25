# Contrato final: autenticacion y usuarios de Inventio

Este es el contrato unificado para Angular y React Native. La API usa JSON y
`Authorization: Bearer <accessToken>`; no utiliza cookies ni sesiones de servidor.
No existe registro publico. Se conservan ADMIN, VENDEDOR y SUPERVISOR.

## Endpoints

| Metodo | Endpoint | Autenticacion | Rol | Exito | Errores principales |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/auth/login` | Publico | Cualquier cuenta activa con roles | 200 | 400, 401 |
| GET | `/api/auth/me` | JWT | Cualquier cuenta activa con roles | 200 | 401 |
| POST | `/api/auth/forgot-password` | Publico | Ninguno | 200 generico | 400 |
| POST | `/api/auth/reset-password` | Token de recuperacion en JSON, sin JWT | Ninguno | 200 | 400, 429 |
| GET | `/api/usuarios` | JWT | ADMIN | 200 | 401, 403 |
| POST | `/api/usuarios` | JWT | ADMIN | 201 | 400, 401, 403, 404, 409 |
| PUT | `/api/usuarios/{id}` | JWT | ADMIN | 200 | 400, 401, 403, 404, 409 |
| PATCH | `/api/usuarios/{id}/estado` | JWT | ADMIN | 200 | 400, 401, 403, 404, 409 |
| GET | `/api/roles` | JWT | ADMIN | 200 | 401, 403 |
| POST | `/api/roles` | JWT | ADMIN | 201 | 400, 401, 403, 409 |

Se conservan los alias `/api/users` y `/{id}/status`, con los mismos permisos.
Las rutas publicas deben invocarse sin agregar un JWT viejo: el filtro Bearer
rechaza un token invalido enviado expresamente, incluso en una ruta publica.

## Login y sesion

`POST /api/auth/login`:

```json
{"email":"admin@inventio.com","password":"<contrasena>"}
```

Respuesta 200:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {"id":1,"nombre":"Administrador","email":"admin@inventio.com","rol":"ADMIN"},
  "usuario": {"id":1,"nombre":"Administrador","email":"admin@inventio.com","estado":"ACTIVO","roles":["ADMIN"],"rol":"ADMIN"}
}
```

`expiresIn` depende de `JWT_TTL_SECONDS`. `usuario` y `roles` permanecen por
compatibilidad. En cuentas multirrol, `rol` muestra ADMIN > SUPERVISOR > VENDEDOR;
los permisos efectivos siempre usan los roles actuales de la base de datos.
Credenciales incorrectas o cuenta inexistente/inactiva producen 401 generico.

`GET /api/auth/me`, sin cuerpo y con Bearer, responde:

```json
{"id":1,"nombre":"Administrador","email":"admin@inventio.com","rol":"ADMIN"}
```

Flujo: **Login → JWT → sesion**. Ante 401, eliminar la sesion local y volver al
login. El backend no implementa refresh tokens. VENDEDOR y SUPERVISOR reciben
403 al intentar administrar cuentas, aunque manipulen peticiones o roles del cliente.

## Solicitar recuperacion

`POST /api/auth/forgot-password`:

```json
{"email":"usuario@empresa.com"}
```

Respuesta 200, igual para una cuenta activa, inactiva, inexistente o solicitud
limitada por abuso:

```json
{"message":"Si el correo está registrado, recibirás instrucciones para restablecer tu contraseña."}
```

Un email vacio, mal formado o de mas de 254 caracteres produce 400. El resultado
generico no confirma entrega: consulta y SMTP se procesan en segundo plano.
Solo las cuentas activas reciben correo; nunca se reactiva una cuenta.

El correo contiene un enlace a `FRONTEND_RESET_PASSWORD_URL` con `?token=<token>`
(o `&token=` si ya hay parametros), el plazo de expiracion y la indicacion de
ignorar una solicitud no realizada. Nunca incluye una contrasena. La URL procede
exclusivamente de configuracion, nunca de `Host`, `Origin` ni de un redirect
enviado por el cliente. Se puede configurar una ruta Angular o un universal link
para una app movil sin cambiar la logica del backend.

## Establecer nueva contrasena

`POST /api/auth/reset-password`:

```json
{"token":"<token-del-enlace>","newPassword":"<nueva-contrasena>"}
```

Respuesta 200:

```json
{"message":"Contraseña restablecida. Inicia sesión con tu nueva contraseña."}
```

El token es independiente del JWT, tiene 256 bits aleatorios, formato Base64URL
sin padding (43 caracteres) y expira en 20 minutos por defecto. Solo su hash
SHA-256 se guarda en `password_reset_tokens`. Una fila unica por usuario limita
la persistencia y reemplaza cualquier recuperacion anterior cuando se procesa
una nueva solicitud. `used_at` impide reutilizarla; el limite de expiracion es
exclusivo: al llegar a la hora exacta de vencimiento ya no sirve.

El cambio BCrypt, la marca de uso y el incremento de `tokenVersion` se confirman
en una sola transaccion. El bloqueo de la fila del usuario serializa solicitudes
y restablecimientos concurrentes. Los JWT anteriores dejan de autenticar. La
version guardada al emitir el enlace tambien invalida recuperaciones pendientes
si un ADMIN edita la cuenta, cambia su clave o su estado. No hay blacklist.

Token inexistente, reemplazado, usado, expirado, revocado o de cuenta inactiva:
400 con mensaje `El enlace de recuperación es inválido o ha expirado.`. Datos
JSON/formato de token invalidos usan el error 400 de validacion existente.
Una clave rechazada no consume un token valido. No se inicia sesion automaticamente.

Flujo: **Olvide mi contrasena → correo → enlace → Nueva contrasena → Login**.
El frontend debe recoger el token, confirmar localmente la nueva clave y enviarla
por POST. Evitar analitica/logs con la URL, usar `Referrer-Policy: no-referrer`
en la pagina de recuperacion y retirar el token de la URL una vez recogido.

## Gestion ADMIN

`GET /api/usuarios`, sin cuerpo: array de objetos publicos (incluye inactivos).

`POST /api/usuarios`:

```json
{"nombre":"Juan Pérez","email":"juan@empresa.com","password":"<clave-inicial>","rol":"VENDEDOR","estado":"ACTIVO"}
```

`PUT /api/usuarios/2`, sin necesidad de reenviar la clave:

```json
{"nombre":"Juan Pérez Actualizado","email":"juan.nuevo@empresa.com","rol":"VENDEDOR"}
```

`PATCH /api/usuarios/2/estado`:

```json
{"estado":"INACTIVO"}
```

Enviar `ACTIVO` para reactivar. Creacion, edicion y cambio de estado responden
con un objeto como este (listado devuelve un array de estos objetos):

```json
{"id":2,"nombre":"Juan Pérez","email":"juan@empresa.com","rol":"VENDEDOR","estado":"ACTIVO","roles":["VENDEDOR"]}
```

Nombre obligatorio, hasta 200 caracteres; email obligatorio, valido, hasta 254,
normalizado y unico sin distinguir mayusculas. Estado omitido/null al crear
conserva ACTIVO. En PUT, omitir `password` o enviarlo null conserva la clave;
enviarlo con valor la reemplaza aplicando la politica comun. El estado se cambia
por PATCH. Se acepta `roles: ["VENDEDOR"]` para clientes anteriores: usar `rol`
o `roles`, no ambos; al editar cuentas multirrol, enviar el array completo.

No se permite al ADMIN desactivarse ni quitarse su propio rol ADMIN (409).
Editar cuentas/inactivar invalida sus JWT existentes. Nunca hay eliminacion fisica.

Catalogo existente: GET `/api/roles` devuelve
`[{"id":1,"nombre":"ADMIN"},{"id":2,"nombre":"VENDEDOR"}]` (incluye los roles
provisionados, tambien SUPERVISOR). POST recibe `{"nombre":"VENDEDOR"}` y
responde 201 `{"id":2,"nombre":"VENDEDOR"}`; 409 si ya existe.

Flujo: **ADMIN → Gestion de usuarios → Crear / Editar / Activar / Desactivar**.

## Politica y errores

`PasswordPolicy` centraliza las reglas para crear cuentas desde la UI y para
cambiar/restablecer claves: 12-72 caracteres, maximo 72 bytes UTF-8, no vacia,
no repeticion de un unico caracter, ni algunas claves triviales conocidas ni
un hash BCrypt suministrado por el cliente. Se admiten frases y no se exige
mezclar numeros, simbolos y mayusculas. No se cambia la validacion del login
ni el aprovisionamiento inicial del ADMIN.

Ninguna respuesta devuelve password, passwordHash o tokens de recuperacion. Solo
login entrega el JWT de acceso. Errores mantienen este formato:

```json
{"timestamp":"2026-09-23T12:00:00Z","status":400,"error":"Bad Request","message":"El enlace de recuperación es inválido o ha expirado.","path":"/api/auth/reset-password"}
```

400: datos/clave/token invalidos; 401: autenticacion invalida; 403: permisos;
404: usuario/rol no encontrado; 409: email duplicado o regla administrativa;
429: demasiados intentos de restablecimiento. Fallos internos usan 500 generico.

## Variables de entorno y despliegue

Se mantienen `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` y las variables
opcionales existentes de JWT, CORS y ADMIN inicial. No se agregan secretos al repo.

| Variable nueva | Uso / valor predeterminado |
| --- | --- |
| `MAIL_HOST` | Servidor SMTP; configurar para enviar correo |
| `MAIL_PORT` | Puerto SMTP, 587 |
| `MAIL_USERNAME` | Usuario SMTP |
| `MAIL_PASSWORD` | Contrasena SMTP, desde gestor de secretos |
| `MAIL_FROM` | Remitente autorizado por el proveedor |
| `FRONTEND_RESET_PASSWORD_URL` | URL HTTPS de recuperacion, sin credenciales ni fragmento; HTTP solo localhost |
| `PASSWORD_RESET_TTL_MINUTES` | 20, admite 15-30 |
| `MAIL_SMTP_AUTH` | true |
| `MAIL_STARTTLS_ENABLE` / `MAIL_STARTTLS_REQUIRED` | true / true |
| `MAIL_SSL_ENABLE` | false; para TLS implicito/465 usar true y STARTTLS false/false |

SMTP tiene timeouts de conexion/lectura/escritura de 5 segundos y debug desactivado.
No configurar logging de cuerpos HTTP, parametros SQL ni trazas SMTP con secretos.
Sin URL configurada no se emiten recuperaciones. Sin SMTP/remitente configurado
no se envia correo; el backend conserva login/usuarios y la respuesta publica
generica. Los fallos de entrega se registran sin destinatario, token ni excepcion
del proveedor. La solicitud puede repetirse dentro de los limites indicados.

`CORS_ALLOWED_ORIGINS` sigue aceptando origenes web exactos separados por comas
(ejemplo Angular: `http://localhost:4200`); React Native nativo envia Bearer sin
necesitar CORS. No hay nuevas dependencias salvo `spring-boot-starter-mail`,
administrada por la version actual de Spring Boot.

Se agrega una sola tabla, sin modificar entidades/tablas comerciales ni usuarios
existentes: [SQL aditivo](sql/recuperacion-password.sql). La configuracion existente
`ddl-auto=update` puede crearla; si se administra el esquema manualmente, revisar
y aplicar ese script. No se ejecutaron migraciones contra Supabase desde esta tarea.

## Resend mediante SMTP

La implementacion Spring Mail existente admite Resend sin SDK ni cambios de endpoints.
Configurar en las variables del proceso Spring Boot (IDE, terminal o gestor de secretos):

```dotenv
MAIL_HOST=smtp.resend.com
MAIL_PORT=465
MAIL_USERNAME=resend
MAIL_PASSWORD=<API_KEY_RESEND>
MAIL_FROM=onboarding@resend.dev
MAIL_SMTP_AUTH=true
MAIL_SSL_ENABLE=true
MAIL_STARTTLS_ENABLE=false
MAIL_STARTTLS_REQUIRED=false
```

Conservar el valor existente de `FRONTEND_RESET_PASSWORD_URL`. Debe ser HTTPS;
HTTP se admite solo para localhost. No configurar literalmente los placeholders.
La API key se proporciona exclusivamente como `MAIL_PASSWORD` en el entorno,
nunca en application.properties, archivos versionados ni configuracion del movil.

Spring Boot no carga `.env` automaticamente: `.env.example` es una plantilla,
no una configuracion activa. Cargar las variables anteriores en el proceso y
reiniciar el backend. Los tres flags TLS son obligatorios para esta configuracion:
465 utiliza TLS implicito, no STARTTLS. Se conservan los defaults genericos de
application.properties y todos los overrides MAIL_* para otros proveedores.

`onboarding@resend.dev` es el remitente de pruebas: Resend restringe los destinatarios
permitidos con ese dominio. Para enviar a usuarios finales, verificar un dominio
propio y configurar un remitente autorizado mediante MAIL_FROM.

La respuesta generica de forgot-password no acredita entrega SMTP. Comprobar la
entrega en Resend y en el buzon destinatario; despues verificar el enlace de un solo
uso y el login con la nueva contrasena. No activar debug SMTP ni registrar tokens.

Referencia: https://resend.com/docs/send-with-smtp

## Proteccion contra abuso y limites operativos

- Ventana de 15 minutos: hasta 5 solicitudes por email normalizado y 20 por IP.
  El exceso devuelve el mismo 200 generico, sin generar/enviar otro token.
- Reset: hasta 30 intentos validos de formulario por IP/15 minutos, luego 429.
- Memoria acotada a 10.000 claves, 2 trabajadores y 100 solicitudes en cola.
  Consulta/SMTP ocurren fuera del hilo HTTP. Si se llena la cola se conserva
  la respuesta generica; nunca se ejecuta SMTP sincrono como fallback.
- Estos limites son locales al proceso y se reinician al reiniciar el backend.
  En produccion con replicas, agregar limites compartidos en proxy/gateway.
  Se utiliza la IP de la conexion; no confiar ciegamente en `X-Forwarded-For`.
  Configurar proxies confiables para no agrupar a todos los clientes en una IP.
- La cola no es persistente: un corte del proceso puede perder envios pendientes.
  Solicitar otro enlace en ese caso. No hay reintentos SMTP automaticos ni garantia
  de entrega. Para garantias mayores, evolucionar a una cola persistente/outbox.

## Pruebas y archivos

Verificacion final ejecutada con Java 21:
`mvnw.cmd -B -o -Dmaven.repo.local=C:/Users/ABALT/.m2/repository verify`.
Resultado: **BUILD SUCCESS**, 119 pruebas contabilizadas, 108 aprobadas,
0 fallos, 0 errores y 11 omitidas por falta de variables de PostgreSQL.
Se genero el JAR ejecutable. La configuracion de recuperacion tambien arranca
en pruebas con y sin SMTP, manteniendo el executor general de Spring Boot.
El arranque completo contra Supabase, la concurrencia PostgreSQL real y la
entrega SMTP real no se comprobaron en esta terminal sin esas variables.

Las pruebas HTTP usan filtros, servicios, BCrypt y JWT reales, repositorios
simulados y `PasswordResetMailer` mockeado. Verifican login ADMIN/VENDEDOR,
inactivos, respuesta generica, hash/entropia, uso unico, expiracion, reemplazo,
BCrypt, cambio de login, revocacion JWT, permisos y ausencia de secretos.
Las pruebas SMTP inspeccionan el mensaje con `JavaMailSender` mockeado. No se
envian correos reales. Los mapeos/repositorios nuevos se validan con Hibernate.

`PasswordRecoveryPostgresTests` permite verificar reemplazo persistido y dos usos
concurrentes del token en PostgreSQL real: usa `INVENTIO_TEST_DB_URL`,
`INVENTIO_TEST_DB_USERNAME`, `INVENTIO_TEST_DB_PASSWORD`, crea y elimina solo
su esquema aleatorio. Sin esas variables se omite, igual que las pruebas de BD
existentes. No usar credenciales de produccion para ejecutar pruebas de BD.

Archivos agregados, bajo `src/main/java/com/ruth/inventio`:

- `config/PasswordRecoveryConfig.java`, `config/PasswordRecoveryProperties.java`.
- `controller/PasswordRecoveryController.java`.
- `dto/ForgotPasswordRequest.java`, `dto/ResetPasswordRequest.java`, `dto/MessageResponse.java`.
- `entity/PasswordResetToken.java`, `repository/PasswordResetTokenRepository.java`.
- `security/PasswordPolicy.java`, `security/RecoveryTokens.java`, `security/RecoveryRateLimiter.java`.
- `service/PasswordRecoveryService.java`, `service/PasswordRecoveryDispatcher.java`,
  `service/PasswordResetMailer.java`, `service/PasswordResetMail.java`, `service/SmtpPasswordResetMailer.java`.
- `validation/ValidPassword.java`, `validation/PasswordValidator.java`.

Modificados: `UsuarioRepository`, `SecurityConfig`, `UserService`,
`UsuarioRequest`, `UsuarioUpdateRequest`, `application.properties`, `pom.xml` y
`.env.example`. Pruebas: ampliados `UserManagementIntegrationTests` y
`DomainMappingTests`; agregados `PasswordRecoverySupportTests` y
`PasswordRecoveryPostgresTests`, ademas de `PasswordRecoveryConfigurationTests`
para verificar configuracion y aislamiento del executor. Se agregan este contrato y el SQL aditivo,
con enlaces desde README y guias anteriores.

Referencias de implementacion:
[Spring Mail](https://docs.spring.io/spring-boot/reference/io/email.html) y
[OWASP: recuperacion de contrasena](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).
