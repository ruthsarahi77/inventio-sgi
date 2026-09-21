# PDF de proformas

`GET /api/proformas/{id}/pdf`, con `Authorization: Bearer <token>`.
Devuelve `application/pdf` y `Content-Disposition: inline; filename="proforma-{numero}.pdf"`.
ADMIN y SUPERVISOR pueden consultar las proformas; VENDEDOR solo las propias.
Se conserva el error global 404 para IDs inexistentes y 403 para acceso a otro vendedor.
Los caracteres del número no seguros para un nombre de archivo se sustituyen por `_`.
Las respuestas no se almacenan en caché. CORS expone Content-Disposition al cliente.

El controlador solo recibe el ID. QuotePdfService mantiene la transacción de lectura
durante la carga y construcción del PDF, reutiliza QuoteService y su validación de
propiedad. ProformaPdfRenderer solo dibuja; DocumentoCalculator realiza el desglose.
No hay llamadas externas ni acceso al frontend al generar el PDF.

## Convención monetaria del modelo actual

El modelo existente NO persiste moneda, tipo de cambio ni IGV por proforma.
Para esta implementación se asumen precios e importes con IGV incluido del 18%
y una única moneda para todas las proformas de la instalación, PEN por defecto.
Esta convención debe confirmarse antes de usar los documentos comercialmente.
El total y los importes de línea son exactamente los persistidos. La base se obtiene
en DocumentoCalculator como total / 1.18, HALF_UP a dos decimales; el IGV es la
diferencia respecto del total. No se agrega impuesto adicional ni se actualiza la BD.
No se imprime un tipo de cambio inventado. Para manejar monedas distintas por
proforma o convertir importes hace falta ampliar primero el modelo y sus reglas.
Cambiar la moneda de instalación solo cambia la denominación, NO convierte dinero.

## Datos institucionales

Configuración centralizada en ProformaPdfProperties, mediante propiedades Spring
`inventio.pdf.*` o variables de entorno:

| Variable | Predeterminado |
| --- | --- |
| INVENTIO_PDF_EMPRESA | BUTRÓN COMERCIO INTERNACIONAL |
| INVENTIO_PDF_DISTRIBUIDOR | DISTRIBUIDOR AUTORIZADO REPSOL |
| INVENTIO_PDF_RUC | 2061128732001 |
| INVENTIO_PDF_DIRECCION | MZC LOTE 19 TACNA |
| INVENTIO_PDF_TELEFONO | Vacío; configurar el teléfono real |
| INVENTIO_PDF_CORREO | Vacío; configurar el correo real |
| INVENTIO_PDF_MONEDA | PEN; admite PEN o USD |
| INVENTIO_PDF_ZONAHORARIA | America/Lima |
| INVENTIO_PDF_LOGO | classpath:pdf/repsol.png |

Los campos de contacto vacíos se muestran con un guion. El logo se empaqueta en
el backend, sin deformarlo. Fuente del recurso original:
https://www.repsol.com/content/dam/repsol-corporate/es/sala-de-prensa/repsol.png
Galería: https://www.repsol.com/en/press-room/multimedia-gallery/brand-logos-gallery/index.cshtml

PDFBox 3.0.8 es la única dependencia directa nueva. Fuentes PDF estándar Helvetica
con caracteres españoles; caracteres ajenos a su codificación se sustituyen por `?`.
La tabla repite encabezados, divide textos y filas extensas y reserva espacio para
el pie en todas las páginas. Los PDF de varias páginas incluyen numeración.

## Verificación

`./mvnw.cmd test` ejecuta pruebas de renderer (A4, logo, acentos, PEN/USD, textos
largos, 100 productos, paginación y márgenes), HTTP, seguridad y servicio.
Las pruebas generan ejemplos ficticios PDF y PNG en `target/pdf-preview/`.
No requieren conexión a Supabase. No se modifican el esquema ni los datos para
implementar esta funcionalidad. Angular podrá visualizar el blob e imprimirlo;
el backend no envía trabajos a ninguna impresora.
