package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.ruth.inventio.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record LoginRequest(@NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=72) String password) {
}
