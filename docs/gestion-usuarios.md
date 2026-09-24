# Fase 2: contrato de gestion de usuarios

La Fase 3 y el contrato consolidado estan en [autenticacion y usuarios](contrato-autenticacion-usuarios.md).

Angular y React Native deben enviar `Authorization: Bearer <accessToken>` en
todas las operaciones siguientes. Solo ADMIN puede utilizarlas. Los cuerpos
JSON usan `Content-Type: application/json`.

| Metodo | Endpoint | JWT | Rol | Exito |
| --- | --- | --- | --- | --- |
| GET | `/api/usuarios` | Si | ADMIN | 200, array de usuarios |
| POST | `/api/usuarios` | Si | ADMIN | 201, usuario creado |
| PUT | `/api/usuarios/{id}` | Si | ADMIN | 200, usuario actualizado |
| PATCH | `/api/usuarios/{id}/estado` | Si | ADMIN | 200, usuario actualizado |

Se mantienen `/api/users` y `/{id}/status` como rutas compatibles. Ambos nombres
usan el mismo controlador, servicio, repositorio, entidades y autorizacion.
No hay eliminacion fisica, registro publico ni recuperacion de contrasena.

## Listar

`GET /api/usuarios` no lleva cuerpo ni parametros. Incluye cuentas activas e
inactivas para administracion. Respuesta de ejemplo:

```json
[
  {
    "id": 2,
    "nombre": "Juan Pérez",
    "email": "juan@empresa.com",
    "rol": "VENDEDOR",
    "estado": "ACTIVO",
    "roles": ["VENDEDOR"]
  }
]
```

`roles` se conserva para clientes y cuentas multirrol existentes. `rol` es el
principal para la pantalla actual, con precedencia ADMIN > SUPERVISOR > VENDEDOR.
Los permisos efectivos siempre se obtienen de la base de datos. SUPERVISOR
conserva sus permisos anteriores en los otros modulos; no puede administrar usuarios.

## Crear

`POST /api/usuarios`:

```json
{
  "nombre": "Juan Pérez",
  "email": "juan@empresa.com",
  "password": "<contrasena-inicial>",
  "rol": "VENDEDOR",
  "estado": "ACTIVO"
}
```

Respuesta 201: un objeto con los mismos campos publicos del ejemplo de listado
(sin el array exterior). Nunca incluye `password`, `passwordHash` ni tokenVersion.

Validaciones:

- Nombre obligatorio, maximo 200 caracteres; se recortan espacios exteriores.
- Email obligatorio, valido, maximo 254 caracteres y unico sin distinguir
  mayusculas; se almacena normalizado en minusculas.
- Contrasena obligatoria, 12-72 caracteres y maximo 72 bytes UTF-8. Se rechazan
  hashes BCrypt enviados como contrasena. El backend genera BCrypt con coste 12.
- Rol obligatorio: ADMIN, VENDEDOR o SUPERVISOR (compatibilidad existente).
- Estado ACTIVO o INACTIVO. Si se omite o es null, se usa ACTIVO para conservar
  el contrato anterior. Puede crearse una cuenta INACTIVA expresamente.

El contrato anterior `"roles": ["VENDEDOR"]` tambien es valido. Usar `rol` o
`roles`, no ambos. Para editar cuentas multirrol sin perder asignaciones, enviar
el array `roles` completo; `rol` asigna un solo rol.

## Editar sin reenviar contrasena

`PUT /api/usuarios/2`:

```json
{
  "nombre": "Juan Pérez Actualizado",
  "email": "juan.actualizado@empresa.com",
  "rol": "VENDEDOR"
}
```

Respuesta 200: objeto publico actualizado. `nombre`, `email` y `rol` (o `roles`)
son obligatorios. Omitir `password` o enviarlo null conserva el hash actual.
El servicio existente permite reemplazar la contrasena enviando una nueva clave
valida; nunca se recibe o edita directamente un hash. El estado se cambia con PATCH.

Las actualizaciones conservan la invalidacion de tokens existente: el usuario
editado debera iniciar sesion otra vez. El ADMIN no puede quitarse su propio rol ADMIN.

## Activar o desactivar

`PATCH /api/usuarios/2/estado`:

```json
{"estado": "INACTIVO"}
```

Para reactivar: `{"estado":"ACTIVO"}`. Respuesta 200: objeto publico actualizado.
Una cuenta INACTIVA no puede iniciar sesion y sus JWT anteriores dejan de servir.
Reactivar permite un nuevo login, pero no restaura tokens revocados. El ADMIN
no puede desactivar su propia cuenta.

## Errores

| Codigo | Motivo |
| --- | --- |
| 400 | Campos invalidos, rol/estado desconocido, clave corta, mas de 72 bytes o hash BCrypt como entrada |
| 401 | JWT ausente, invalido, vencido o revocado; usuario inactivo |
| 403 | VENDEDOR o SUPERVISOR intenta administrar usuarios |
| 404 | Usuario a editar/desactivar inexistente; rol valido aun no provisionado en `roles` |
| 409 | Email duplicado, conflicto de integridad, intento de quitarse ADMIN o desactivar la propia cuenta |

Se conserva el formato de errores existente:

```json
{
  "timestamp": "2026-09-23T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Email de usuario duplicado.",
  "path": "/api/usuarios"
}
```

## Configuracion y pruebas

No hay variables nuevas para esta fase. Se utiliza la configuracion de
[autenticacion inicial](autenticacion-inicial.md), incluido `CORS_ALLOWED_ORIGINS`
para Angular. JWT, `/api/auth/login`, `/api/auth/me` y bootstrap se reutilizan.

`UserManagementIntegrationTests` recorre login ADMIN, listado, creacion BCrypt,
login VENDEDOR, edicion sin clave, desactivacion, revocacion, reactivacion,
duplicados, validaciones, respuestas sin secretos y rechazos 401/403.
Los filtros, validacion MVC, servicios y criptografia son reales; los repositorios
estan simulados y estas pruebas no acceden a Supabase.

Validacion ejecutada: `mvnw.cmd -B -o -Dmaven.repo.local=C:/Users/ABALT/.m2/repository verify`
con Java 21: BUILD SUCCESS, 102 pruebas contabilizadas, 92 aprobadas, 10 omitidas,
0 fallos y 0 errores. Las omitidas requieren variables de PostgreSQL. No se ha
verificado el arranque ni la persistencia tras reinicio contra Supabase en esta
terminal, donde no estan configuradas las credenciales de base de datos.

Se corrigio una expectativa desactualizada en `JwtConfigTests`: exigia
`ddl-auto=none`, aunque el proyecto ya tenia `update`. Se conserva la configuracion
de produccion y no se cambia el esquema para hacer pasar la prueba.

## Archivos de esta fase

- Modificados: `controller/UserController.java`, `service/UserService.java`,
  `dto/UsuarioRequest.java`, `dto/UsuarioUpdateRequest.java`, `dto/UsuarioResponse.java`
  y `security/SecurityConfig.java`, bajo `src/main/java/com/ruth/inventio`.
- Creado: `src/test/java/com/ruth/inventio/security/UserManagementIntegrationTests.java`.
- Ajustado: `src/test/java/com/ruth/inventio/security/JwtConfigTests.java`.
- Documentacion: este archivo y `README.md`.

Los cambios previos de la Fase 1 permanecen en el arbol de trabajo: `AdminBootstrap`,
`AuthController`, `AuthService`, `LoginResponse`, el nuevo `AuthUserResponse`,
`application.properties`, `.env.example`, `AdminBootstrapTests`,
`SecurityIntegrationTests`, `docs/autenticacion-inicial.md` y `docs/flujo-comercial.md`.
