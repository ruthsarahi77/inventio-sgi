package com.ruth.inventio.service;

import com.ruth.inventio.config.ProformaPdfProperties;
import com.ruth.inventio.pdf.ProformaPdfRenderer;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuotePdfService {
    public record Documento(String numero, byte[] contenido) {}
    private final QuoteService quotes;
    private final DocumentoCalculator calculator;
    private final ProformaPdfRenderer renderer;
    private final ProformaPdfProperties config;

    public QuotePdfService(QuoteService quotes, DocumentoCalculator calculator,
            ProformaPdfRenderer renderer, ProformaPdfProperties config) {
        this.quotes = quotes; this.calculator = calculator; this.renderer = renderer; this.config = config;
    }

    @Transactional(readOnly = true)
    public Documento obtener(Long id) throws IOException {
        var quote = quotes.obtenerParaDocumento(id);
        var amounts = calculator.desglosarIgvIncluido(quote.getTotal());
        return new Documento(quote.getNumero(), renderer.render(quote, config.getMoneda().name(),
                amounts.subtotal(), amounts.igv()));
    }
}
