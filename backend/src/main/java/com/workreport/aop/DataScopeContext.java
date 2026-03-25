package com.workreport.aop;

import com.workreport.enums.ScopeType;

/**
 * Immutable snapshot of the current user's data-access scope.
 * Populated by {@link DataScopeAspect} and available throughout the
 * service-layer call stack via {@link DataScopeAspect#getCurrentScope()}.
 *
 * @param userId    the authenticated user's id
 * @param scopeType the resolved scope granted to this user
 */
public record DataScopeContext(Long userId, ScopeType scopeType) {
}
