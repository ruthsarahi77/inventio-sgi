package com.ruth.inventio.controller;

import com.ruth.inventio.dto.LoginRequest;
import com.ruth.inventio.dto.LoginResponse;
import com.ruth.inventio.dto.AuthUserResponse;
import com.ruth.inventio.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service=service; }
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
    @GetMapping("/me")
    public AuthUserResponse me() { return service.me(); }
}
