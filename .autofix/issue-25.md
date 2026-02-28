# Issue #25 修復報告

## 問題描述

PM 在任務管理功能中，點選「新增任務」時，指派人員（assigneeId）下拉選單為空白。預期行為是下拉選單應帶出**與此專案相同部門的執行人員**（role = EXECUTOR）名單。

根本原因有兩點：

1. `TaskForm.vue` 的 `loadExecutors()` 函式呼叫 `/users?role=EXECUTOR&size=100`，但 `UserController` 設有 `@PreAuthorize("hasRole('HR')")`，PM 使用者無權存取此端點，導致請求被拒絕。
2. 即使能存取，回傳的是所有部門的執行人員，未依專案所屬部門篩選；且當 `role` 參數存在時 API 回傳純陣列而非分頁物件，`data.content` 為 `undefined`，所以 `executors` 始終為空。

## 修改檔案清單

| 檔案 | 更動摘要 |
|------|---------|
| `backend/src/main/java/com/workreport/repository/UserRepository.java` | 新增 `findByRoleAndDepartmentId` JPQL 查詢方法，依角色與部門 ID 過濾有效使用者 |
| `backend/src/main/java/com/workreport/service/TaskService.java` | 新增 `getAssignableExecutors(Long projectId)` 方法：取得專案所屬部門中 role=EXECUTOR 的使用者清單 |
| `backend/src/main/java/com/workreport/controller/TaskController.java` | 新增 `GET /api/projects/{projectId}/tasks/assignable-executors` 端點，PM 可存取 |
| `frontend/src/components/task/TaskForm.vue` | 更新 `loadExecutors()` 改呼叫新端點，取得正確的執行人員清單 |

## 修復說明

### 後端

1. **`UserRepository`**：新增依角色與部門 ID 查詢的 JPQL 方法：
   ```java
   @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role AND u.department.id = :departmentId AND u.active = true")
   List<User> findByRoleAndDepartmentId(@Param("role") Role role, @Param("departmentId") Long departmentId);
   ```

2. **`TaskService`**：新增 `getAssignableExecutors` 方法，先取得專案，再利用專案的 `departmentId` 查詢同部門的 EXECUTOR：
   ```java
   public List<UserResponse> getAssignableExecutors(Long projectId) {
       Project project = projectRepository.findById(projectId)
               .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));
       if (project.getDepartment() == null) {
           return List.of();
       }
       return userRepository.findByRoleAndDepartmentId(Role.EXECUTOR, project.getDepartment().getId())
               .stream().map(UserResponse::from).toList();
   }
   ```

3. **`TaskController`**：在既有的 `@PreAuthorize("hasAnyRole('PM', 'ADMIN')")` 控制器下新增端點：
   ```java
   @GetMapping("/assignable-executors")
   public ResponseEntity<List<UserResponse>> getAssignableExecutors(@PathVariable Long projectId) {
       return ResponseEntity.ok(taskService.getAssignableExecutors(projectId));
   }
   ```

### 前端

**`TaskForm.vue`** 的 `loadExecutors()` 改為呼叫新端點，回應為純陣列直接賦值：
```typescript
async function loadExecutors() {
  try {
    const { data } = await http.get<User[]>(`/projects/${props.projectId}/tasks/assignable-executors`)
    executors.value = data
  } catch {
    executors.value = []
  }
}
```

## 測試步驟

1. 以 PM 角色登入系統。
2. 進入任務管理頁面（選擇一個有指定部門的專案）。
3. 點選「新增任務」按鈕。
4. 確認「指派人員」下拉選單中顯示與該專案**相同部門**且角色為 EXECUTOR 的使用者清單。
5. 確認**不同部門**的 EXECUTOR 不會出現在清單中。
6. 選擇一位執行人員，填寫任務名稱與配額時數，點選「建立」，確認任務正常建立且指派人員欄位有值。
