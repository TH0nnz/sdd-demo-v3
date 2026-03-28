package com.workreport.aop;

import com.workreport.annotation.RequireRole;
import com.workreport.enums.Role;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AOP aspect that enforces {@link RequireRole} annotations.
 *
 * <p>Method-level annotation takes precedence over class-level. At least one
 * of the declared roles must appear in the current user's granted authorities.
 * If no valid authentication is found, or the user holds none of the required
 * roles, an {@link AccessDeniedException} is thrown.
 */
@Aspect
@Component
public class RoleCheckAspect {

    @Around("@annotation(com.workreport.annotation.RequireRole) || @within(com.workreport.annotation.RequireRole)")
    public Object checkRole(ProceedingJoinPoint pjp) throws Throwable {
        RequireRole annotation = resolveAnnotation(pjp);
        validateRole(annotation);
        return pjp.proceed();
    }

    private RequireRole resolveAnnotation(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();

        // Method-level annotation takes precedence
        RequireRole methodAnnotation = method.getAnnotation(RequireRole.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return pjp.getTarget().getClass().getAnnotation(RequireRole.class);
    }

    private void validateRole(RequireRole annotation) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Authentication required");
        }

        Set<String> requiredAuthorities = Arrays.stream(annotation.value())
                .map(Role::name)
                .map(name -> "ROLE_" + name)
                .collect(Collectors.toSet());

        boolean hasRole = auth.getAuthorities().stream()
                .anyMatch(a -> requiredAuthorities.contains(a.getAuthority()));

        if (!hasRole) {
            throw new AccessDeniedException(
                    "Access denied. Required role(s): " + Arrays.toString(annotation.value()));
        }
    }
}
