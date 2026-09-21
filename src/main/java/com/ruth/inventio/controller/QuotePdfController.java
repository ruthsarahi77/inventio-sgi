package com.ruth.inventio.controller;

import com.ruth.inventio.service.QuotePdfService;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proformas")
public class QuotePdfController {
    private final QuotePdfService service;
    public QuotePdfController(QuotePdfService service) { this.service = service; }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> obtener(@PathVariable Long id) throws IOException {
        var document = service.obtener(id);
        String safeNumber = document.numero().replaceAll("[^A-Za-z0-9._-]", "_");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("proforma-" + safeNumber + ".pdf").build().toString())
                .cacheControl(CacheControl.noStore())
                .body(document.contenido());
    }
}
