package com.workreport.dto.department;

import com.workreport.entity.Department;

public record DepartmentResponse(Long id, String name) {

    public static DepartmentResponse from(Department dept) {
        return new DepartmentResponse(dept.getId(), dept.getName());
    }
}
