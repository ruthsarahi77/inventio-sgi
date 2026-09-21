package com.ruth.inventio.controller;

import com.ruth.inventio.dto.DashboardStatsResponse;
import com.ruth.inventio.service.DashboardService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> stats(@RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.obtener(year, month));
    }
}
