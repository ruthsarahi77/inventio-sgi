package com.ruth.inventio.service;

import com.ruth.inventio.config.ProformaPdfProperties;
import com.ruth.inventio.entity.Proforma;
import com.ruth.inventio.entity.Usuario;
import com.ruth.inventio.exception.RecursoNoEncontradoException;
import com.ruth.inventio.pdf.ProformaPdfRenderer;
import com.ruth.inventio.repository.*;
import com.ruth.inventio.security.CurrentUser;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuotePdfServiceTests {
    @Test void reusesOwnershipValidationAndPersistedTotal() throws Exception {
        var repository = mock(ProformaRepository.class);
        var current = mock(CurrentUser.class);
        var calculator = new DocumentoCalculator(null, null);
        var quotes = new QuoteService(repository, mock(ProformaDetalleRepository.class), mock(ClienteRepository.class),
                mock(VentaRepository.class), calculator, current);
        var renderer = mock(ProformaPdfRenderer.class);
        var service = new QuotePdfService(quotes, calculator, renderer, new ProformaPdfProperties());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(99L));
        verifyNoInteractions(renderer);
        var quote = new Proforma(); quote.setNumero("PRO-7"); quote.setTotal(new BigDecimal("118.01"));
        var user = new Usuario(); ReflectionTestUtils.setField(user, "id", 2L); quote.setUsuario(user);
        when(repository.findById(7L)).thenReturn(Optional.of(quote));
        doThrow(new AccessDeniedException("Otro vendedor")).when(current).verificarPropietario(2L);
        assertThrows(AccessDeniedException.class, () -> service.obtener(7L));
        verifyNoInteractions(renderer);
        doNothing().when(current).verificarPropietario(2L);
        when(renderer.render(quote, "PEN", new BigDecimal("100.01"), new BigDecimal("18.00"))).thenReturn(new byte[]{1, 2});
        var result = service.obtener(7L);
        assertEquals("PRO-7", result.numero()); assertArrayEquals(new byte[]{1, 2}, result.contenido());
        assertEquals(new BigDecimal("118.01"), quote.getTotal());
        verify(repository, never()).save(any());
    }

    @Test void taxBreakdownPreservesRoundedTotal() {
        var calculator = new DocumentoCalculator(null, null);
        for (String amount : new String[]{"0.00", "0.01", "118.00", "118.01", "99999999999999999.99"}) {
            var total = new BigDecimal(amount);
            var parts = calculator.desglosarIgvIncluido(total);
            assertEquals(total, parts.subtotal().add(parts.igv()));
            assertEquals(2, parts.subtotal().scale()); assertEquals(2, parts.igv().scale());
        }
    }
}
