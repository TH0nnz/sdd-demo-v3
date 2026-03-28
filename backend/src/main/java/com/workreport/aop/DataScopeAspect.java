package com.workreport.aop;

import com.workreport.annotation.DataScope;
import com.workreport.enums.ScopeType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * AOP aspect that enforces {@link DataScope} annotations.
 *
 * <p>Before the annotated method executes the aspect resolves the current
 * user's {@link ScopeType} from their granted authorities, stores a
 * {@link DataScopeContext} in a thread-local, and removes it when the method
 * returns (or throws). Service-layer code can call
 * {@link #getCurrentScope()} to retrieve the context and apply
 * role-appropriate data filtering.
 *
 * <p>Role-to-scope mapping:
 * <ul>
 *   <li>ROLE_ADMIN, ROLE_HR  → {@link ScopeType#ALL}</li>
 *   <li>ROLE_PM              → {@link ScopeType#PROJECT}</li>
 *   <li>ROLE_DEPT_MANAGER    → {@link ScopeType#DEPARTMENT}</li>
 *   <li>ROLE_EXECUTOR (default) → {@link ScopeType#SELF}</li>
 * </ul>
 */
@Aspect
@Component
public class DataScopeAspect {

    private static final ThreadLocal<DataScopeContext> SCOPE_HOLDER = new ThreadLocal<>();

    /**
     * Returns the {@link DataScopeContext} set by this aspect for the current thread,
     * or {@code null} if the calling method is not annotated with {@link DataScope}.
     */
    public static DataScopeContext getCurrentScope() {
        return SCOPE_HOLDER.get();
    }

    @Around("@annotation(dataScope)")
    public Object applyDataScope(ProceedingJoinPoint pjp, DataScope dataScope) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Authentication required");
        }

        Long userId = (Long) auth.getPrincipal();
        ScopeType scopeType = resolveScopeType(auth);

        SCOPE_HOLDER.set(new DataScopeContext(userId, scopeType));
        try {
            return pjp.proceed();
        } finally {
            SCOPE_HOLDER.remove();
        }
    }

    public static ScopeType resolveScopeType(Authentication auth) {
        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        if (authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_HR")) {
            return ScopeType.ALL;
        }
        if (authorities.contains("ROLE_PM")) {
            return ScopeType.PROJECT;
        }
        if (authorities.contains("ROLE_DEPT_MANAGER")) {
            return ScopeType.DEPARTMENT;
        }
        return ScopeType.SELF;
    }
}
