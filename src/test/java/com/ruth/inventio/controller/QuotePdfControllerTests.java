package com.ruth.inventio.controller;

import com.ruth.inventio.exception.GlobalExceptionHandler;
import com.ruth.inventio.exception.RecursoNoEncontradoException;
import com.ruth.inventio.service.QuotePdfService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QuotePdfControllerTests {
    @Test void returnsInlinePdfAndUsesGlobalErrors() throws Exception {
        var service = mock(QuotePdfService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new QuotePdfController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        byte[] bytes = "%PDF-test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(service.obtener(1L)).thenReturn(new QuotePdfService.Documento("PRO-123", bytes));
        mvc.perform(get("/api/proformas/1/pdf")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"proforma-PRO-123.pdf\""))
                .andExpect(header().string("Cache-Control", "no-store")).andExpect(content().bytes(bytes));
        when(service.obtener(2L)).thenThrow(new RecursoNoEncontradoException("Proforma no encontrada: 2"));
        mvc.perform(get("/api/proformas/2/pdf")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        when(service.obtener(3L)).thenThrow(new AccessDeniedException("Otra proforma"));
        mvc.perform(get("/api/proformas/3/pdf")).andExpect(status().isForbidden());
    }
}
