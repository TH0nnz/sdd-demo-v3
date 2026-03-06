package com.workreport.unit.service;

import com.workreport.dto.department.CreateDepartmentRequest;
import com.workreport.dto.department.DepartmentResponse;
import com.workreport.dto.department.UpdateDepartmentRequest;
import com.workreport.entity.Department;
import com.workreport.exception.BusinessRuleException;
import com.workreport.repository.DepartmentRepository;
import com.workreport.repository.UserRepository;
import com.workreport.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentService")
class DepartmentServiceTest {

    private static final Long DEPT_ID = 1L;
    private static final String DEPT_NAME = "Engineering";

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DepartmentService departmentService;

    private Department department;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(DEPT_ID);
        department.setName(DEPT_NAME);
    }

    @Nested
    @DisplayName("listDepartments")
    class ListDepartments {

        @Test
        @DisplayName("無部門時回傳空列表")
        void whenEmpty_returnsEmptyList() {
            when(departmentRepository.findAll()).thenReturn(List.of());

            List<DepartmentResponse> result = departmentService.listDepartments();

            assertEquals(List.of(), result);
            verify(departmentRepository).findAll();
        }

        @Test
        @DisplayName("有部門時回傳對應的 DepartmentResponse 列表")
        void whenHasData_returnsMappedResponses() {
            when(departmentRepository.findAll()).thenReturn(List.of(department));

            List<DepartmentResponse> result = departmentService.listDepartments();

            assertEquals(1, result.size());
            assertEquals(DEPT_ID, result.get(0).id());
            assertEquals(DEPT_NAME, result.get(0).name());
            verify(departmentRepository).findAll();
        }
    }

    @Nested
    @DisplayName("createDepartment")
    class CreateDepartment {

        @Test
        @DisplayName("名稱已存在時拋出 BusinessRuleException")
        void whenNameExists_throwsBusinessRuleException() {
            CreateDepartmentRequest request = new CreateDepartmentRequest("Engineering");
            when(departmentRepository.existsByName("Engineering")).thenReturn(true);

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> departmentService.createDepartment(request));

            assertEquals("部門名稱已存在: Engineering", ex.getMessage());
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            verify(departmentRepository).existsByName("Engineering");
            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("成功建立並回傳 DepartmentResponse")
        void success_returnsDepartmentResponse() {
            CreateDepartmentRequest request = new CreateDepartmentRequest("NewDept");
            when(departmentRepository.existsByName("NewDept")).thenReturn(false);
            Department saved = new Department();
            saved.setId(2L);
            saved.setName("NewDept");
            when(departmentRepository.save(any(Department.class))).thenReturn(saved);

            DepartmentResponse result = departmentService.createDepartment(request);

            assertEquals(2L, result.id());
            assertEquals("NewDept", result.name());
            verify(departmentRepository).existsByName("NewDept");
            verify(departmentRepository).save(any(Department.class));
        }
    }

    @Nested
    @DisplayName("updateDepartment")
    class UpdateDepartment {

        @Test
        @DisplayName("部門不存在時拋出 BusinessRuleException NOT_FOUND")
        void whenNotFound_throwsBusinessRuleExceptionNotFound() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.empty());
            UpdateDepartmentRequest request = new UpdateDepartmentRequest("NewName");

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> departmentService.updateDepartment(DEPT_ID, request));

            assertEquals("部門不存在", ex.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            verify(departmentRepository).findById(DEPT_ID);
            verify(departmentRepository, never()).existsByName(any());
            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("改名且新名稱已存在時拋出 BusinessRuleException")
        void whenRenameAndNameExists_throwsBusinessRuleException() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(departmentRepository.existsByName("OtherDept")).thenReturn(true);
            UpdateDepartmentRequest request = new UpdateDepartmentRequest("OtherDept");

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> departmentService.updateDepartment(DEPT_ID, request));

            assertEquals("部門名稱已存在: OtherDept", ex.getMessage());
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            verify(departmentRepository).findById(DEPT_ID);
            verify(departmentRepository).existsByName("OtherDept");
            verify(departmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("不改名時不檢查 existsByName，直接更新並回傳")
        void whenNameUnchanged_updatesAndReturns() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

            DepartmentResponse result = departmentService.updateDepartment(DEPT_ID,
                    new UpdateDepartmentRequest(DEPT_NAME));

            assertEquals(DEPT_ID, result.id());
            assertEquals(DEPT_NAME, result.name());
            verify(departmentRepository).findById(DEPT_ID);
            verify(departmentRepository).save(department);
            // 名稱相同時不會呼叫 existsByName
            verify(departmentRepository, never()).existsByName(any());
        }

        @Test
        @DisplayName("成功改名並回傳 DepartmentResponse")
        void success_returnsDepartmentResponse() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(departmentRepository.existsByName("UpdatedName")).thenReturn(false);
            when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

            DepartmentResponse result = departmentService.updateDepartment(DEPT_ID,
                    new UpdateDepartmentRequest("UpdatedName"));

            assertEquals(DEPT_ID, result.id());
            assertEquals("UpdatedName", result.name());
            verify(departmentRepository).findById(DEPT_ID);
            verify(departmentRepository).existsByName("UpdatedName");
            verify(departmentRepository).save(department);
        }
    }

    @Nested
    @DisplayName("deleteDepartment")
    class DeleteDepartment {

        @Test
        @DisplayName("部門不存在時拋出 BusinessRuleException NOT_FOUND")
        void whenNotFound_throwsBusinessRuleExceptionNotFound() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.empty());

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> departmentService.deleteDepartment(DEPT_ID));

            assertEquals("部門不存在", ex.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            verify(departmentRepository).findById(DEPT_ID);
            verify(userRepository, never()).existsByDepartmentId(any());
            verify(departmentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("部門仍有成員時拋出 BusinessRuleException")
        void whenHasMembers_throwsBusinessRuleException() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(userRepository.existsByDepartmentId(DEPT_ID)).thenReturn(true);

            BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                    () -> departmentService.deleteDepartment(DEPT_ID));

            assertEquals("此部門仍有成員，無法刪除", ex.getMessage());
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            verify(departmentRepository).findById(DEPT_ID);
            verify(userRepository).existsByDepartmentId(DEPT_ID);
            verify(departmentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("成功刪除部門")
        void success_deletesDepartment() {
            when(departmentRepository.findById(DEPT_ID)).thenReturn(Optional.of(department));
            when(userRepository.existsByDepartmentId(DEPT_ID)).thenReturn(false);

            departmentService.deleteDepartment(DEPT_ID);

            verify(departmentRepository).findById(DEPT_ID);
            verify(userRepository).existsByDepartmentId(DEPT_ID);
            verify(departmentRepository).delete(department);
        }
    }
}
