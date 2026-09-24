# ADMIN inicial y autenticacion

Consultar el [contrato final](contrato-autenticacion-usuarios.md) para recuperacion
de contrasena, gestion ADMIN y todos los endpoints de autenticacion.

Se reutilizan `usuarios`, `roles`, `usuarios_roles`, los repositorios existentes,
BCrypt (coste 12), JWT HS256 y la gestion de usuarios protegida para ADMIN.
No se cambian Java, dependencias, conexion ni esquema. Se conserva SUPERVISOR
por compatibilidad con los modulos existentes. En cuentas multirrol, `rol` muestra
ADMIN, luego SUPERVISOR, luego VENDEDOR; los permisos efectivos siempre se
obtienen de todos los roles actuales en PostgreSQL.

## Configuracion

Obligatorias para arrancar: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`
(Base64 de al menos 32 bytes aleatorios, estable entre reinicios).

Para crear la cuenta inicial: `INITIAL_ADMIN_NAME`, `INITIAL_ADMIN_EMAIL`,
`INITIAL_ADMIN_PASSWORD`. Si falta cualquiera, no se crea la cuenta y el backend
continua. La clave conserva la politica existente: 12-72 caracteres y hasta
72 bytes UTF-8. No hay valores predeterminados ni contrasenas en logs.
Las antiguas variables `BOOTSTRAP_ADMIN_*` ya no se utilizan.

Opcional: `CORS_ALLOWED_ORIGINS=http://localhost:4200` (varios origenes separados
por comas). React Native nativo usa Bearer sin necesitar CORS. `.env.example`
es una plantilla; exportar variables en la terminal o configurarlas en IntelliJ.

## Contrato HTTP

`POST /api/auth/login`, publico:

```json
{"email":"admin@inventio.com","password":"<clave-configurada>"}
```

La respuesta incluye:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "user": {"id": 1, "nombre": "Administrador", "email": "admin@inventio.com", "rol": "ADMIN"}
}
```

Por compatibilidad se conservan tambien `expiresIn` y `usuario` (con `estado`
y `roles`). Ninguna respuesta contiene el hash. ADMIN y VENDEDOR pueden entrar;
credenciales incorrectas, cuenta inexistente o inactiva producen 401.

`GET /api/auth/me` con `Authorization: Bearer <jwt>` devuelve directamente
el objeto `user` anterior. Sin JWT, con firma invalida, vencido, revocado o una
cuenta inactiva devuelve 401. Se usa el ID firmado, no un ID enviado por el cliente.

Gestion existente, solo ADMIN:

| Metodo | Endpoint |
| --- | --- |
| GET / POST | `/api/users` |
| PUT | `/api/users/{id}` |
| PATCH | `/api/users/{id}/status` |

Para crear: `nombre`, `email`, `password`, `roles: ["ADMIN"]` o
`roles: ["VENDEDOR"]`. VENDEDOR recibe 403 al intentar administrar usuarios o
roles. No existe `/register`.

## Verificacion

Ejecutar `./mvnw.cmd clean verify` con Java 21. Las pruebas de bootstrap cubren
BCrypt, cuenta activa, email existente, otras cuentas, variables incompletas y
repeticion del inicializador. Las pruebas HTTP con MockMvc usan el encoder,
decoder y filtros reales, con repositorio simulado.

Para verificar persistencia real: configurar PostgreSQL y las variables iniciales,
arrancar, hacer login y `/me`, detener y volver a arrancar con el mismo email.
Comprobar en `usuarios` que existe una sola fila para ese email y que el hash
y el ID permanecen iguales. No publicar credenciales ni tokens en capturas/logs.

Referencia: [JWT Bearer en Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
