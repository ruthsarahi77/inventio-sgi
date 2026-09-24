package com.ruth.inventio.dto;

import com.ruth.inventio.validation.ValidPassword;
import jakarta.validation.constraints.*;

public record ResetPasswordRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String token,
                                   @ValidPassword String newPassword) {
    @Override public String toString() { return "ResetPasswordRequest[redacted]"; }
}
