package com.healthsuite.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = BangladeshPhoneValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface BangladeshPhone {

    String message() default "Phone number must be a valid Bangladeshi mobile number (e.g. 01712345678 or +8801712345678)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
