package com.ruth.inventio.pdf;

import com.ruth.inventio.config.ProformaPdfProperties;
import com.ruth.inventio.entity.Proforma;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/** Solo dibuja datos; no consulta repositorios ni calcula impuestos o totales. */
@Component
public class ProformaPdfRenderer {
    private final ProformaPdfProperties config;
    private final ResourceLoader resources;

    public ProformaPdfRenderer(ProformaPdfProperties config, ResourceLoader resources) {
        this.config = config;
        this.resources = resources;
    }

    public byte[] render(Proforma quote, String currency, BigDecimal subtotal, BigDecimal igv) throws IOException {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            byte[] logoBytes;
            try (var input = resources.getResource(config.getLogo()).getInputStream()) {
                logoBytes = input.readAllBytes();
            }
            var logo = PDImageXObject.createFromByteArray(document, logoBytes, "Repsol");
            try (var layout = new Layout(document, quote, logo)) {
                layout.newPage();
                var client = quote.getCliente();
                layout.columns(List.of("Cliente / Razón Social: " + value(client.getNombre()),
                        "RUC / Documento: " + value(client.getIdentificacion()),
                        "Dirección: " + value(client.getDireccion()), "Teléfono: " + value(client.getTelefono())),
                        List.of("Vendedor: " + value(quote.getUsuario().getNombre()), "Moneda: " + currency));
                layout.tableHeader();
                for (var detail : quote.getDetalles()) {
                    var product = detail.getProducto();
                    String description = value(product.getNombre());
                    if (product.getDescripcion() != null && !product.getDescripcion().isBlank()) {
                        description += " - " + product.getDescripcion();
                    }
                    layout.row(List.of(value(product.getCodigo()), value(product.getPresentacion()), description,
                            detail.getCantidad().stripTrailingZeros().toPlainString(),
                            amount(detail.getPrecioUnitario()), amount(detail.getSubtotal())));
                }
                layout.totals(currency, subtotal, igv, quote.getTotal());
            }
            document.getDocumentInformation().setTitle("Proforma " + quote.getNumero());
            document.getDocumentInformation().setAuthor(config.getEmpresa());
            document.save(output);
            return output.toByteArray();
        }
    }

    private static String value(String value) { return value == null || value.isBlank() ? "—" : value; }
    private static String amount(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }

    private final class Layout implements AutoCloseable {
        private static final float MARGIN = 40;
        private static final float LEADING = 12;
        private final float width = PDRectangle.A4.getWidth() - 2 * MARGIN;
        private final float[] widths = {65, 78, 157, 55, 80, 80.2756f};
        private final PDFont normal = new PDType1Font(FontName.HELVETICA);
        private final PDFont bold = new PDType1Font(FontName.HELVETICA_BOLD);
        private final PDDocument document;
        private final Proforma quote;
        private final PDImageXObject logo;
        private final List<String> footer;
        private final float bottom;
        private PDPageContentStream stream;
        private float y;

        Layout(PDDocument document, Proforma quote, PDImageXObject logo) throws IOException {
            this.document = document; this.quote = quote; this.logo = logo;
            footer = wrap(config.getDireccion() + "\nTeléfono: " + value(config.getTelefono())
                    + "  |  Correo: " + value(config.getCorreo()), width, normal, 8);
            bottom = 40 + footer.size() * 10 + 20;
            if (bottom > 180) throw new IOException("Los datos del pie institucional son demasiado extensos.");
        }

        void newPage() throws IOException {
            if (stream != null) stream.close();
            var page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            float top = PDRectangle.A4.getHeight() - MARGIN;
            float scale = Math.min(145f / logo.getWidth(), 70f / logo.getHeight());
            stream.drawImage(logo, MARGIN, top - logo.getHeight() * scale, logo.getWidth() * scale, logo.getHeight() * scale);
            float leftY = top - 83;
            for (String line : wrap(config.getEmpresa(), 290, bold, 10)) {
                text(line, MARGIN, leftY, bold, 10); leftY -= LEADING;
            }
            for (String line : wrap(config.getDistribuidor() + "\nRUC " + config.getRuc() + "\n" + config.getDireccion(), 290, normal, 9)) {
                text(line, MARGIN, leftY, normal, 9); leftY -= LEADING;
            }
            text("PROFORMA", MARGIN + 315, top - 22, bold, 18);
            float rightY = top - 45;
            for (String line : wrap("N° " + quote.getNumero(), width - 315, bold, 10)) {
                text(line, MARGIN + 315, rightY, bold, 10); rightY -= LEADING;
            }
            text("Fecha: " + DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(config.getZonaHoraria()).format(quote.getFecha()),
                    MARGIN + 315, rightY - 8, normal, 10);
            y = Math.min(leftY, rightY - 28) - 10;
            if (y < bottom + 120) throw new IOException("El encabezado institucional es demasiado extenso.");
            rule(y); y -= 20;
        }

        void columns(List<String> left, List<String> right) throws IOException {
            var a = wrap(String.join("\n", left), 290, normal, 9);
            var b = wrap(String.join("\n", right), width - 315, normal, 9);
            for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
                if (y - LEADING < bottom) newPage();
                if (i < a.size()) text(a.get(i), MARGIN, y, normal, 9);
                if (i < b.size()) text(b.get(i), MARGIN + 315, y, normal, 9);
                y -= LEADING;
            }
            y -= 18;
        }

        void tableHeader() throws IOException {
            if (y - 45 < bottom) newPage();
            rule(y + 8);
            String[] labels = {"Código", "Presentación", "Descripción", "Cant.", "P. Unit", "Importe"};
            float x = MARGIN;
            for (int i = 0; i < labels.length; i++) {
                float tx = i < 3 ? x + 4 : x + widths[i] - 4 - measure(labels[i], bold, 9);
                text(labels[i], tx, y - 5, bold, 9); x += widths[i];
            }
            y -= 15; rule(y); y -= 15;
        }

        void row(List<String> values) throws IOException {
            List<List<String>> cells = new ArrayList<>();
            int lines = 1;
            for (int i = 0; i < values.size(); i++) {
                var cell = wrap(values.get(i), widths[i] - 8, normal, 9);
                cells.add(cell); lines = Math.max(lines, cell.size());
            }
            // Filas normales se mantienen juntas; filas mayores a una página se dividen por líneas.
            float fullPageCapacity = PDRectangle.A4.getHeight() - MARGIN - 210 - bottom;
            if (lines * LEADING + 10 <= fullPageCapacity && y - lines * LEADING - 10 < bottom) {
                newPage(); tableHeader();
            }
            for (int line = 0; line < lines; line++) {
                if (y - LEADING - 10 < bottom) { newPage(); tableHeader(); }
                float x = MARGIN;
                for (int col = 0; col < cells.size(); col++) {
                    if (line < cells.get(col).size()) {
                        String value = cells.get(col).get(line);
                        float tx = col < 3 ? x + 4 : x + widths[col] - 4 - measure(value, normal, 9);
                        text(value, tx, y, normal, 9);
                    }
                    x += widths[col];
                }
                y -= LEADING;
            }
            rule(y + 3); y -= 9;
        }

        void totals(String currency, BigDecimal subtotal, BigDecimal igv, BigDecimal total) throws IOException {
            if (y - 100 < bottom) newPage();
            y -= 14;
            String symbol = "PEN".equals(currency) ? "S/ " : "US$ ";
            String[] labels = {"Subtotal", "IGV (18%)", "Total"};
            BigDecimal[] amounts = {subtotal, igv, total};
            for (int i = 0; i < labels.length; i++) {
                var font = i == 2 ? bold : normal;
                String value = symbol + amount(amounts[i]);
                text(labels[i], MARGIN + width - 250, y, font, 10);
                text(value, MARGIN + width - measure(value, font, 10), y, font, 10);
                y -= 22;
            }
        }

        private String printable(String input) throws IOException {
            var result = new StringBuilder();
            for (int cp : input.codePoints().toArray()) {
                if (cp == '\n') { result.append('\n'); continue; }
                if (Character.isWhitespace(cp) || Character.isISOControl(cp)) { result.append(' '); continue; }
                String character = new String(Character.toChars(cp));
                try { normal.encode(character); result.append(character); }
                catch (IllegalArgumentException unsupported) { result.append('?'); }
            }
            return result.toString();
        }

        private List<String> wrap(String input, float maxWidth, PDFont font, float size) throws IOException {
            List<String> lines = new ArrayList<>();
            for (String paragraph : printable(value(input)).split("\n", -1)) {
                String remaining = paragraph.strip();
                if (remaining.isEmpty()) { lines.add(""); continue; }
                while (!remaining.isEmpty()) {
                    int end = 0;
                    while (end < remaining.length() && measure(remaining.substring(0, end + 1), font, size) <= maxWidth) end++;
                    end = Math.max(1, end);
                    if (end < remaining.length()) {
                        int space = remaining.lastIndexOf(' ', end);
                        if (space > 0) end = space;
                    }
                    lines.add(remaining.substring(0, end).stripTrailing());
                    remaining = remaining.substring(end).stripLeading();
                }
            }
            return lines;
        }

        private float measure(String value, PDFont font, float size) throws IOException {
            return font.getStringWidth(value) * size / 1000;
        }

        private void text(String value, float x, float baseline, PDFont font, float size) throws IOException {
            stream.beginText(); stream.setFont(font, size); stream.newLineAtOffset(x, baseline);
            stream.showText(printable(value).replace('\n', ' ')); stream.endText();
        }

        private void rule(float baseline) throws IOException {
            stream.setStrokingColor(0.75f); stream.setLineWidth(0.4f);
            stream.moveTo(MARGIN, baseline); stream.lineTo(MARGIN + width, baseline); stream.stroke();
        }

        @Override public void close() throws IOException {
            if (stream != null) { stream.close(); stream = null; }
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                try (var footerStream = new PDPageContentStream(document, document.getPage(page),
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    stream = footerStream;
                    rule(bottom - 12);
                    float baseline = bottom - 27;
                    for (String line : footer) {
                        text(line, MARGIN, baseline, normal, 8); baseline -= 10;
                    }
                    if (document.getNumberOfPages() > 1) {
                        String number = "Página " + (page + 1) + " de " + document.getNumberOfPages();
                        text(number, MARGIN + width - measure(number, normal, 8), 25, normal, 8);
                    }
                } finally { stream = null; }
            }
        }
    }
}
