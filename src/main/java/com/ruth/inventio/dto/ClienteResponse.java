package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.ruth.inventio.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ClienteResponse(Long id, String identificacion, String nombre, String telefono, String email,
        String direccion, EstadoRegistro estado) {
}
