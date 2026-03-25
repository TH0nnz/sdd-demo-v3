package com.workreport.annotation;

import com.workreport.enums.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AOP annotation for declarative role enforcement.
 * Can be placed on a method or a class. Method-level takes precedence.
 * At least one of the specified roles must match the authenticated user.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /** One or more roles; the caller must hold at least one. */
    Role[] value();
}
