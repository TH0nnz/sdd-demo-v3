package com.workreport.unit.aop;

import com.workreport.annotation.DataScope;
import com.workreport.aop.DataScopeAspect;
import com.workreport.aop.DataScopeContext;
import com.workreport.enums.ScopeType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataScopeAspect")
class DataScopeAspectTest {

    @Mock
    private ProceedingJoinPoint pjp;

    @InjectMocks
    private DataScopeAspect aspect;

    private final DataScope dataScopeAnnotation = mock(DataScope.class);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- helpers ----

    private void authenticateAs(Long userId, String... roleNames) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roleNames)
                .map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ---- getCurrentScope ----

    @Nested
    @DisplayName("getCurrentScope")
    class GetCurrentScope {

        @Test
        @DisplayName("Aspect 未執行時回傳 null")
        void noAspect_returnsNull() {
            assertNull(DataScopeAspect.getCurrentScope());
        }
    }

    // ---- resolveScopeType ----

    @Nested
    @DisplayName("resolveScopeType")
    class ResolveScopeType {

        @Test
        @DisplayName("ROLE_ADMIN → ALL")
        void adminRole_resolvesToAll() {
            authenticateAs(1L, "ROLE_ADMIN");
            ScopeType result = DataScopeAspect.resolveScopeType(
                    SecurityContextHolder.getContext().getAuthentication());
            assertEquals(ScopeType.ALL, result);
        }

        @Test
        @DisplayName("ROLE_HR → ALL")
        void hrRole_resolvesToAll() {
            authenticateAs(1L, "ROLE_HR");
            ScopeType result = DataScopeAspect.resolveScopeType(
                    SecurityContextHolder.getContext().getAuthentication());
            assertEquals(ScopeType.ALL, result);
        }

        @Test
        @DisplayName("ROLE_PM → PROJECT")
        void pmRole_resolvesToProject() {
            authenticateAs(1L, "ROLE_PM");
            ScopeType result = DataScopeAspect.resolveScopeType(
                    SecurityContextHolder.getContext().getAuthentication());
            assertEquals(ScopeType.PROJECT, result);
        }

        @Test
        @DisplayName("ROLE_DEPT_MANAGER → DEPARTMENT")
        void deptManagerRole_resolvesToDepartment() {
            authenticateAs(1L, "ROLE_DEPT_MANAGER");
            ScopeType result = DataScopeAspect.resolveScopeType(
                    SecurityContextHolder.getContext().getAuthentication());
            assertEquals(ScopeType.DEPARTMENT, result);
        }

        @Test
        @DisplayName("ROLE_EXECUTOR → SELF")
        void executorRole_resolvesToSelf() {
            authenticateAs(1L, "ROLE_EXECUTOR");
            ScopeType result = DataScopeAspect.resolveScopeType(
                    SecurityContextHolder.getContext().getAuthentication());
            assertEquals(ScopeType.SELF, result);
        }

        @Test
        @DisplayName("無已知角色時預設 SELF")
        void unknownRole_defaultsToSelf() {
            authenticateAs(1L, "ROLE_UNKNOWN");
            ScopeType result = DataScopeAspect.resolveScopeType(
                    SecurityContextHolder.getContext().getAuthentication());
            assertEquals(ScopeType.SELF, result);
        }
    }

    // ---- applyDataScope ----

    @Nested
    @DisplayName("applyDataScope")
    class ApplyDataScope {

        @Test
        @DisplayName("無認證時拋出 AccessDeniedException，不呼叫 proceed()")
        void noAuthentication_throwsAccessDenied() {
            assertThrows(AccessDeniedException.class,
                    () -> aspect.applyDataScope(pjp, dataScopeAnnotation));
            assertNull(DataScopeAspect.getCurrentScope());
        }

        @Test
        @DisplayName("匿名用戶時拋出 AccessDeniedException，不呼叫 proceed()")
        void anonymousUser_throwsAccessDenied() {
            UsernamePasswordAuthenticationToken anon =
                    new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
            SecurityContextHolder.getContext().setAuthentication(anon);

            assertThrows(AccessDeniedException.class,
                    () -> aspect.applyDataScope(pjp, dataScopeAnnotation));
        }

        @Test
        @DisplayName("EXECUTOR 用戶：context 正確設定為 SELF，proceed() 被呼叫，context 事後清除")
        void executorUser_scopeSetToSelf_contextClearedAfter() throws Throwable {
            authenticateAs(42L, "ROLE_EXECUTOR");
            Object returnValue = new Object();
            when(pjp.proceed()).thenReturn(returnValue);

            Object result = aspect.applyDataScope(pjp, dataScopeAnnotation);

            assertSame(returnValue, result);
            verify(pjp).proceed();
            // After the advice completes, the ThreadLocal must be cleared
            assertNull(DataScopeAspect.getCurrentScope());
        }

        @Test
        @DisplayName("ADMIN 用戶：context 包含 userId 與 ScopeType.ALL")
        void adminUser_scopeSetToAll() throws Throwable {
            authenticateAs(10L, "ROLE_ADMIN");
            final DataScopeContext[] capturedContext = new DataScopeContext[1];
            when(pjp.proceed()).thenAnswer(inv -> {
                capturedContext[0] = DataScopeAspect.getCurrentScope();
                return null;
            });

            aspect.applyDataScope(pjp, dataScopeAnnotation);

            assertEquals(10L, capturedContext[0].userId());
            assertEquals(ScopeType.ALL, capturedContext[0].scopeType());
            // Cleared after
            assertNull(DataScopeAspect.getCurrentScope());
        }

        @Test
        @DisplayName("DEPT_MANAGER 用戶：context 包含 ScopeType.DEPARTMENT")
        void deptManagerUser_scopeSetToDepartment() throws Throwable {
            authenticateAs(5L, "ROLE_DEPT_MANAGER");
            final DataScopeContext[] capturedContext = new DataScopeContext[1];
            when(pjp.proceed()).thenAnswer(inv -> {
                capturedContext[0] = DataScopeAspect.getCurrentScope();
                return null;
            });

            aspect.applyDataScope(pjp, dataScopeAnnotation);

            assertEquals(5L, capturedContext[0].userId());
            assertEquals(ScopeType.DEPARTMENT, capturedContext[0].scopeType());
        }

        @Test
        @DisplayName("PM 用戶：context 包含 ScopeType.PROJECT")
        void pmUser_scopeSetToProject() throws Throwable {
            authenticateAs(7L, "ROLE_PM");
            final DataScopeContext[] capturedContext = new DataScopeContext[1];
            when(pjp.proceed()).thenAnswer(inv -> {
                capturedContext[0] = DataScopeAspect.getCurrentScope();
                return null;
            });

            aspect.applyDataScope(pjp, dataScopeAnnotation);

            assertEquals(7L, capturedContext[0].userId());
            assertEquals(ScopeType.PROJECT, capturedContext[0].scopeType());
        }

        @Test
        @DisplayName("proceed() 拋出例外時，context 仍應被清除")
        void proceedThrows_contextStillCleared() {
            authenticateAs(1L, "ROLE_EXECUTOR");
            RuntimeException expected = new RuntimeException("service error");

            try {
                when(pjp.proceed()).thenThrow(expected);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }

            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> aspect.applyDataScope(pjp, dataScopeAnnotation));

            assertSame(expected, thrown);
            assertNull(DataScopeAspect.getCurrentScope());
        }
    }
}
