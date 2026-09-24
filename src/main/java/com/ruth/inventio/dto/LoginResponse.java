package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.ruth.inventio.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UsuarioResponse usuario) {
    @com.fasterxml.jackson.annotation.JsonProperty("user")
    public AuthUserResponse user() { return AuthUserResponse.from(usuario); }
}
