package com.healthsuite.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class BangladeshPhoneValidator implements ConstraintValidator<BangladeshPhone, String> {

    private static final Pattern BD_PHONE = Pattern.compile("^(\\+8801|01)[3-9]\\d{8}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null separately
        }
        return BD_PHONE.matcher(value.trim()).matches();
    }
}
