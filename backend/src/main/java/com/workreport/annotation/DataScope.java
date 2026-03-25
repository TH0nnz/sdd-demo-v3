package com.workreport.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AOP annotation for data-scope enforcement.
 * Mark service methods that must run inside a resolved {@link com.workreport.aop.DataScopeContext}.
 * The surrounding {@link com.workreport.aop.DataScopeAspect} will populate a thread-local
 * {@code DataScopeContext} (containing the current user id and their {@link com.workreport.enums.ScopeType})
 * before the method executes and remove it afterwards.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
}
