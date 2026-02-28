package com.workreport.service;

import com.workreport.dto.department.CreateDepartmentRequest;
import com.workreport.dto.department.DepartmentResponse;
import com.workreport.dto.department.UpdateDepartmentRequest;
import com.workreport.entity.Department;
import com.workreport.exception.BusinessRuleException;
import com.workreport.repository.DepartmentRepository;
import com.workreport.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentService(DepartmentRepository departmentRepository,
                             UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> listDepartments() {
        return departmentRepository.findAll().stream()
                .map(DepartmentResponse::from)
                .toList();
    }

    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new BusinessRuleException("部門名稱已存在: " + request.name());
        }
        Department dept = new Department();
        dept.setName(request.name());
        LocalDateTime now = LocalDateTime.now();
        dept.setCreatedAt(now);
        dept.setUpdatedAt(now);
        return DepartmentResponse.from(departmentRepository.save(dept));
    }

    public DepartmentResponse updateDepartment(Long id, UpdateDepartmentRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("部門不存在", HttpStatus.NOT_FOUND));
        if (!dept.getName().equals(request.name()) && departmentRepository.existsByName(request.name())) {
            throw new BusinessRuleException("部門名稱已存在: " + request.name());
        }
        dept.setName(request.name());
        dept.setUpdatedAt(LocalDateTime.now());
        return DepartmentResponse.from(departmentRepository.save(dept));
    }

    public void deleteDepartment(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("部門不存在", HttpStatus.NOT_FOUND));
        if (userRepository.existsByDepartmentId(id)) {
            throw new BusinessRuleException("此部門仍有成員，無法刪除");
        }
        departmentRepository.delete(dept);
    }
}
