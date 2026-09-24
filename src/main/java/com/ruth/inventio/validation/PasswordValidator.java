package com.ruth.inventio.validation;

import com.ruth.inventio.security.PasswordPolicy;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    private boolean optional;
    @Override public void initialize(ValidPassword annotation) { optional = annotation.optional(); }
    @Override public boolean isValid(String value, ConstraintValidatorContext context) {
        return (optional && value == null) || PasswordPolicy.isValid(value);
    }
}
