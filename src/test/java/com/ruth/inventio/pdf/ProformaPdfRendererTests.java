package com.ruth.inventio.pdf;

import com.ruth.inventio.config.ProformaPdfProperties;
import com.ruth.inventio.entity.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import static org.junit.jupiter.api.Assertions.*;

class ProformaPdfRendererTests {
    private final ProformaPdfProperties config = new ProformaPdfProperties();

    private Proforma quote(int count, boolean longText) {
        var q = new Proforma(); q.setNumero("PRO-2026-00001"); q.setFecha(Instant.parse("2026-09-20T15:00:00Z"));
        var client = new Cliente(); client.setNombre("Comercial Muñoz e Hijos " + (longText ? "Razón social extensa ".repeat(15) : "S.A.C."));
        client.setIdentificacion("20123456789"); client.setDireccion("Avenida Industrial 123 " + (longText ? "Sector de distribución ".repeat(15) : "Tacna"));
        client.setTelefono("+51 999 000 000"); q.setCliente(client);
        var user = new Usuario(); user.setNombre("María López"); q.setUsuario(user);
        q.setTotal(new BigDecimal("118.00"));
        for (int i = 0; i < count; i++) {
            var p = new Producto(); p.setCodigo("COD-" + i); p.setNombre("Lubricante REPSOL " + i);
            p.setPresentacion(longText ? "Presentación industrial de gran capacidad ".repeat(3) : "Bidón 20 L");
            p.setDescripcion(longText && i == 0 ? "Descripción técnica " .repeat(600) + "FIN-DESCRIPCION" : "Aceite para motores diésel");
            var d = new ProformaDetalle(); d.setProducto(p); d.setCantidad(new BigDecimal("2.500"));
            d.setPrecioUnitario(new BigDecimal("47.20")); d.setSubtotal(new BigDecimal("118.00")); q.getDetalles().add(d);
        }
        return q;
    }

    @Test void producesA4PdfWithPersistedAmountsLogoAndAccents() throws Exception {
        var renderer = new ProformaPdfRenderer(config, new DefaultResourceLoader());
        byte[] bytes = renderer.render(quote(1, false), "PEN", new BigDecimal("100"), new BigDecimal("18"));
        try (var doc = Loader.loadPDF(bytes)) {
            assertEquals(1, doc.getNumberOfPages());
            assertEquals(PDRectangle.A4.getWidth(), doc.getPage(0).getMediaBox().getWidth());
            assertTrue(doc.getPage(0).getResources().getXObjectNames().iterator().hasNext());
            String text = new PDFTextStripper().getText(doc);
            for (String expected : new String[]{"BUTRÓN", "Muñoz", "María López", "47.20", "118.00", "S/ 100.00", "S/ 18.00", "S/ 118.00"}) assertTrue(text.contains(expected), expected);
            assertFalse(text.contains("Tipo de cambio"));
            Path dir = Path.of("target/pdf-preview"); Files.createDirectories(dir);
            Files.write(dir.resolve("proforma.pdf"), bytes);
            ImageIO.write(new PDFRenderer(doc).renderImageWithDPI(0, 110), "png", dir.resolve("proforma.png").toFile());
        }
    }

    @Test void splitsOversizeRowsRepeatsHeadersAndKeepsFooterOutsideContent() throws Exception {
        var renderer = new ProformaPdfRenderer(config, new DefaultResourceLoader());
        byte[] bytes = renderer.render(quote(100, true), "USD", new BigDecimal("100"), new BigDecimal("18"));
        try (var doc = Loader.loadPDF(bytes)) {
            assertTrue(doc.getNumberOfPages() > 2);
            String all = new PDFTextStripper().getText(doc);
            assertTrue(all.contains("FIN-DESCRIPCION")); assertTrue(all.contains("COD-99"));
            assertTrue(all.contains("US$ 118.00"));
            for (int page = 1; page <= doc.getNumberOfPages(); page++) {
                var stripper = new PDFTextStripper(); stripper.setStartPage(page); stripper.setEndPage(page);
                String text = stripper.getText(doc);
                assertTrue(text.contains("Página " + page + " de " + doc.getNumberOfPages()));
                assertTrue(text.contains("Correo:"));
                if (text.contains("COD-") || text.contains("Descripción técnica")) assertTrue(text.contains("P. Unit"));
            }
            var bounds = new PDFTextStripper() {
                @Override protected void processTextPosition(TextPosition position) {
                    assertTrue(position.getXDirAdj() >= 39);
                    assertTrue(position.getXDirAdj() + position.getWidthDirAdj() <= PDRectangle.A4.getWidth() - 38);
                    assertTrue(position.getYDirAdj() < PDRectangle.A4.getHeight() - 20);
                    super.processTextPosition(position);
                }
            };
            bounds.getText(doc);
            Path dir = Path.of("target/pdf-preview"); Files.createDirectories(dir);
            Files.write(dir.resolve("proforma-multipagina.pdf"), bytes);
            var images = new PDFRenderer(doc);
            ImageIO.write(images.renderImageWithDPI(1, 90), "png", dir.resolve("continuacion.png").toFile());
            ImageIO.write(images.renderImageWithDPI(doc.getNumberOfPages() - 1, 90), "png", dir.resolve("ultima.png").toFile());
        }
    }
}
