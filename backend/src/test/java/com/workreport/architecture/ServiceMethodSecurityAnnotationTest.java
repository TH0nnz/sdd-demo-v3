package com.workreport.architecture;

import com.workreport.annotation.PublicApi;
import com.workreport.annotation.RequireRole;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceMethodSecurityAnnotationTest {

    @Test
    void everyPublicServiceMethodMustDeclareRequireRoleOrPublicApi() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));

        List<String> violations = new ArrayList<>();
        for (var candidate : scanner.findCandidateComponents("com.workreport.service")) {
            Class<?> serviceClass = Class.forName(candidate.getBeanClassName());
            for (Method method : serviceClass.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers())
                        || Modifier.isStatic(method.getModifiers())
                        || method.isSynthetic()
                        || method.isBridge()) {
                    continue;
                }

                boolean annotated = method.isAnnotationPresent(RequireRole.class)
                        || method.isAnnotationPresent(PublicApi.class);
                if (!annotated) {
                    violations.add(serviceClass.getSimpleName() + "#" + method.getName());
                }
            }
        }

        violations.sort(Comparator.naturalOrder());
        assertTrue(
                violations.isEmpty(),
                () -> "Missing @RequireRole or @PublicApi on service methods: " + String.join(", ", violations)
        );
    }
}