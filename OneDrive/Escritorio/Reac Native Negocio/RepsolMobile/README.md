# Welcome to your Expo app 👋

This is an [Expo](https://expo.dev) project created with [`create-expo-app`](https://www.npmjs.com/package/create-expo-app).

## Inventio: fase 1

Ejecutar los comandos desde esta carpeta (`movil/OneDrive/Escritorio/Reac Native Negocio/RepsolMobile`).

1. `npm install`
2. Copiar `.env.example` a `.env.local` y configurar `EXPO_PUBLIC_API_URL` con el origen de Spring Boot, **sin `/api`**.
3. `npx expo start --clear`

Android emulado: `http://10.0.2.2:8080`. Teléfono físico: `http://<IP-LAN-del-equipo>:8080`, con el servidor accesible desde esa red. Web: `http://localhost:8080`; agregar el origen exacto de Expo Web a `CORS_ALLOWED_ORIGINS` del backend. Para servidores remotos utilizar HTTPS. No incluir claves de Supabase, credenciales de BD ni `JWT_SECRET` en el móvil.

El backend debe estar iniciado con su configuración existente y una cuenta activa. El móvil usa únicamente `POST /api/auth/login`, `GET /api/auth/me` y `GET /api/inventory`. El backend continúa gestionando PostgreSQL/Supabase y los permisos.

Android/iOS guardan el token con `expo-secure-store`; se valida con `/me` al abrir la aplicación. Web mantiene el token únicamente en memoria y requiere otro login al recargar. Un 401 elimina la sesión; un fallo de red durante la restauración permite reintentar sin borrar el token. No hay refresh token. La biometría no sustituye el login JWT.

El inicio autenticado abre Stock (consulta real). Los demás módulos conservan su implementación local anterior y **no están integrados en esta fase**. Las rutas privadas están protegidas y se restringen las acciones por el rol principal devuelto por el backend. La edición del perfil no modifica datos de la cuenta.

Validación:

```sh
npx tsc --noEmit
npx expo lint
node --test tests/api.test.cjs
```

Las pruebas automatizadas usan respuestas controladas: no acreditan una conexión real con Spring Boot/BD. Validar manualmente en dispositivo: login correcto/incorrecto, inventario contra la BD, reinicio con sesión, logout, token vencido/revocado, permisos ADMIN/VENDEDOR y recuperación tras desconexión.

## Fase 2: autenticación y recuperación

El móvil admite exclusivamente ADMIN y VENDEDOR. Cualquier otro rol recibido en login o `/api/auth/me` se rechaza; durante la restauración se elimina la sesión no admitida. No se modificaron los roles ni permisos del backend.

Desde “¿Olvidaste tu contraseña?” se llama a `POST /api/auth/forgot-password` con `{ email }`. La confirmación genérica del servidor **no garantiza entrega del correo**. La ruta pública `/reset-password?token=...` llama a `POST /api/auth/reset-password` con `{ token, newPassword }`. Un token inválido, vencido o usado muestra el error del servidor; tras éxito se elimina la sesión local y se vuelve al login. También se puede pegar el enlace recibido dentro del móvil. El token y las contraseñas no se guardan ni se registran en logs.

Configurar exclusivamente en el entorno de **Spring Boot**:

| Variable | Configuración |
| --- | --- |
| `MAIL_HOST` | Servidor SMTP del proveedor |
| `MAIL_PORT` | Puerto SMTP; predeterminado 587 |
| `MAIL_USERNAME` | Usuario SMTP, necesario si `MAIL_SMTP_AUTH=true` |
| `MAIL_PASSWORD` | Credencial SMTP, solo en backend |
| `MAIL_FROM` | Remitente autorizado por el proveedor |
| `FRONTEND_RESET_PASSWORD_URL` | `https://<host-de-Expo-Web>/reset-password` |
| `MAIL_SMTP_AUTH` | Predeterminado `true` |
| `MAIL_STARTTLS_ENABLE` / `MAIL_STARTTLS_REQUIRED` | Predeterminados `true` |
| `MAIL_SSL_ENABLE` | Predeterminado `false`; adaptar junto con puerto/TLS al proveedor |
| `PASSWORD_RESET_TTL_MINUTES` | Predeterminado 20; rango permitido 15–30 |

La URL del correo debe ser HTTPS; desarrollo admite `http://localhost:<puerto-Expo-Web>/reset-password` en el mismo equipo. No usar una IP LAN HTTP ni `repsolmobile://` en esa variable: Spring Boot los rechaza. Para abrir el enlace HTTPS se debe desplegar Expo Web y autorizar su origen exacto en `CORS_ALLOWED_ORIGINS`. La app nativa puede recibir `repsolmobile://reset-password?token=...` mediante su esquema existente, pero no se ha configurado asociación automática de enlaces HTTPS con iOS/Android. Pegar el enlace es la alternativa nativa sin infraestructura adicional.

No poner variables MAIL, credenciales de BD ni claves en `.env.local` del móvil. No se verificó SMTP real ni su configuración en el proceso de backend. Pendiente validar correo real, enlace vencido/usado, cambio de contraseña e invalidación del JWT anterior en dispositivo.

## Fase 3: productos e inventario

En Más, **Productos** abre el catálogo (`GET /api/products`); **Inventario** y la pestaña Stock abren las existencias (`GET /api/inventory`). Las tarjetas abren detalle de producto o inventario. Se eliminó ProductContext con sus productos demo, el alta local y el listado de productos erróneamente situado bajo Clientes. El acceso del inicio a inventario ya no muestra cifras de ese contexto; no se integró dashboard ni clientes.

ADMIN puede crear (`POST /api/products`), editar (`PUT /api/products/{id}`), activar/desactivar (`PATCH /api/products/{id}/status`), consultar kardex (`GET /api/inventory/kardex/{id}`) y registrar entradas o ajustes (`POST /api/inventory/entries`, `/adjustments/in`, `/adjustments/out`). VENDEDOR solo consulta catálogo/detalle y existencias dentro de este módulo. Spring Boot continúa autorizando cada solicitud.

Crear producto y registrar stock son operaciones independientes. El formulario conserva los campos reales: codigo, nombre, descripcion, presentacion, volumen, unidad y costoUnitario. No envía precio de venta ni stock dentro de ProductoRequest; no convierte costos con un cambio de moneda fijo. Los movimientos envían cantidad positiva (hasta 3 decimales), documento y observación; el servidor identifica al usuario. No existe salida manual genérica en el cliente. Las pantallas recargan al recuperar foco y tras guardar, cancelan consultas obsoletas y muestran errores sin sustituirlos por mocks.

Pruebas de fase 3: `npx tsc --noEmit`, `npx expo lint`, `node --test tests/api.test.cjs`. Las pruebas de contratos usan respuestas controladas. Las consultas reales sin JWT a `/api/products` y `/api/inventory` respondieron 401; falta validar con cuentas ADMIN/VENDEDOR y datos de prueba: alta, edición, cambio de estado, entradas, ajustes, kardex, saldo insuficiente, códigos duplicados, reconexión y persistencia al reiniciar. Ante una interrupción al guardar, consultar primero el servidor antes de repetir una mutación: los endpoints no declaran idempotencia.

## Get started

1. Install dependencies

   ```bash
   npm install
   ```

2. Start the app

   ```bash
   npx expo start
   ```

In the output, you'll find options to open the app in a

- [development build](https://docs.expo.dev/develop/development-builds/introduction/)
- [Android emulator](https://docs.expo.dev/workflow/android-studio-emulator/)
- [iOS simulator](https://docs.expo.dev/workflow/ios-simulator/)
- [Expo Go](https://expo.dev/go), a limited sandbox for trying out app development with Expo

You can start developing by editing the files inside the **app** directory. This project uses [file-based routing](https://docs.expo.dev/router/introduction).

## Get a fresh project

When you're ready, run:

```bash
npm run reset-project
```

This command will move the starter code to the **app-example** directory and create a blank **app** directory where you can start developing.

### Other setup steps

- To set up ESLint for linting, run `npx expo lint`, or follow our guide on ["Using ESLint and Prettier"](https://docs.expo.dev/guides/using-eslint/)
- If you'd like to set up unit testing, follow our guide on ["Unit Testing with Jest"](https://docs.expo.dev/develop/unit-testing/)
- Learn more about the TypeScript setup in this template in our guide on ["Using TypeScript"](https://docs.expo.dev/guides/typescript/)

## Learn more

To learn more about developing your project with Expo, look at the following resources:

- [Expo documentation](https://docs.expo.dev/): Learn fundamentals, or go into advanced topics with our [guides](https://docs.expo.dev/guides).
- [Learn Expo tutorial](https://docs.expo.dev/tutorial/introduction/): Follow a step-by-step tutorial where you'll create a project that runs on Android, iOS, and the web.

## Join the community

Join our community of developers creating universal apps.

- [Expo on GitHub](https://github.com/expo/expo): View our open source platform and contribute.
- [Discord community](https://chat.expo.dev): Chat with Expo users and ask questions.
