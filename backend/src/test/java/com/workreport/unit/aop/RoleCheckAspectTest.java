package com.workreport.unit.aop;

import com.workreport.annotation.RequireRole;
import com.workreport.aop.RoleCheckAspect;
import com.workreport.enums.Role;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
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

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoleCheckAspect")
class RoleCheckAspectTest {

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private RoleCheckAspect aspect;

    // ---- helper service classes used to carry annotations ----

    @RequireRole(Role.HR)
    static class HrService {
        public void doSomething() { }

        @RequireRole(Role.ADMIN)
        public void adminOnly() { }
    }

    static class NoAnnotationService {
        public void doSomething() { }
    }

    // ---- setup / teardown ----

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---- helpers ----

    private void authenticateAs(String... roleNames) {
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roleNames)
                .map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setUpJoinPoint(Class<?> targetClass, String methodName) throws NoSuchMethodException {
        Method method = targetClass.getMethod(methodName);
        when(pjp.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(pjp.getTarget()).thenReturn(newInstance(targetClass));
    }

    @SuppressWarnings("unchecked")
    private <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---- tests ----

    @Nested
    @DisplayName("class-level @RequireRole")
    class ClassLevel {

        @Test
        @DisplayName("正確角色通過，並呼叫 pjp.proceed()")
        void correctRole_proceeds() throws Throwable {
            authenticateAs("ROLE_HR");
            setUpJoinPoint(HrService.class, "doSomething");
            Object expected = new Object();
            when(pjp.proceed()).thenReturn(expected);

            Object result = aspect.checkRole(pjp);

            assertSame(expected, result);
            verify(pjp).proceed();
        }

        @Test
        @DisplayName("角色不符合時拋出 AccessDeniedException，不呼叫 proceed()")
        void wrongRole_throwsAccessDenied() throws Throwable {
            authenticateAs("ROLE_EXECUTOR");
            setUpJoinPoint(HrService.class, "doSomething");

            assertThrows(AccessDeniedException.class, () -> aspect.checkRole(pjp));
            verify(pjp, never()).proceed();
        }
    }

    @Nested
    @DisplayName("method-level @RequireRole overrides class-level")
    class MethodLevelOverride {

        @Test
        @DisplayName("方法層級 ADMIN 優先；ADMIN 用戶通過")
        void adminUser_methodLevelAnnotation_proceeds() throws Throwable {
            authenticateAs("ROLE_ADMIN");
            setUpJoinPoint(HrService.class, "adminOnly");
            when(pjp.proceed()).thenReturn(null);

            aspect.checkRole(pjp);

            verify(pjp).proceed();
        }

        @Test
        @DisplayName("方法層級 ADMIN 優先；HR 用戶被拒絕")
        void hrUser_adminOnlyMethod_throwsAccessDenied() throws Throwable {
            authenticateAs("ROLE_HR");
            setUpJoinPoint(HrService.class, "adminOnly");

            assertThrows(AccessDeniedException.class, () -> aspect.checkRole(pjp));
            verify(pjp, never()).proceed();
        }
    }

    @Nested
    @DisplayName("authentication checks")
    class AuthChecks {

        @Test
        @DisplayName("無認證時拋出 AccessDeniedException")
        void noAuthentication_throwsAccessDenied() throws Throwable {
            // SecurityContextHolder is empty (cleared in @BeforeEach)
            setUpJoinPoint(HrService.class, "doSomething");

            assertThrows(AccessDeniedException.class, () -> aspect.checkRole(pjp));
            verify(pjp, never()).proceed();
        }

        @Test
        @DisplayName("匿名用戶（anonymousUser）時拋出 AccessDeniedException")
        void anonymousUser_throwsAccessDenied() throws Throwable {
            UsernamePasswordAuthenticationToken anon =
                    new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
            SecurityContextHolder.getContext().setAuthentication(anon);
            setUpJoinPoint(HrService.class, "doSomething");

            assertThrows(AccessDeniedException.class, () -> aspect.checkRole(pjp));
            verify(pjp, never()).proceed();
        }
    }

    @Nested
    @DisplayName("error message content")
    class ErrorMessage {

        @Test
        @DisplayName("例外訊息包含所需角色名稱")
        void accessDenied_messageContainsRequiredRoles() throws Throwable {
            authenticateAs("ROLE_EXECUTOR");
            setUpJoinPoint(HrService.class, "doSomething");

            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> aspect.checkRole(pjp));

            assertTrue(ex.getMessage().contains("HR"));
        }
    }
}
