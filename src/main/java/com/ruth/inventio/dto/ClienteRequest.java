package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.ruth.inventio.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ClienteRequest(@NotBlank @Size(max=50) String identificacion, @NotBlank @Size(max=200) String nombre,
        @Size(max=30) String telefono, @Email @Size(max=254) String email, @Size(max=500) String direccion) {
}
