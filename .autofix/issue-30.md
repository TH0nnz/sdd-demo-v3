# Issue #30 Autofix Report — 角色HR增加「部門管理」

## 問題描述

HR 角色登入後，側邊欄缺少「部門管理」選單項目。HR 無法對部門進行新增、修改、刪除操作。使用者須能依照「使用者管理」的操作風格管理部門。

## 修改檔案清單

| 檔案 | 更動摘要 |
|------|---------|
| `backend/src/main/java/com/workreport/dto/department/CreateDepartmentRequest.java` | 新增：建立部門的請求 DTO |
| `backend/src/main/java/com/workreport/dto/department/UpdateDepartmentRequest.java` | 新增：更新部門的請求 DTO |
| `backend/src/main/java/com/workreport/service/DepartmentService.java` | 新增：部門 CRUD 業務邏輯服務 |
| `backend/src/main/java/com/workreport/controller/DepartmentController.java` | 新增：`/api/hr/departments` REST 控制器（HR 權限） |
| `backend/src/main/java/com/workreport/repository/UserRepository.java` | 新增 `existsByDepartmentId` 方法，供刪除部門時判斷是否有成員 |
| `frontend/src/api/dept.ts` | 新增 `listDepartmentsForHr`、`createDepartment`、`updateDepartment`、`deleteDepartment` API 方法 |
| `frontend/src/components/department/DepartmentForm.vue` | 新增：部門表單元件 |
| `frontend/src/pages/hr/DepartmentManagementPage.vue` | 新增：部門管理頁面（HR 專用） |
| `frontend/src/router/index.ts` | 新增 `/hr/departments` 路由，綁定 HR 角色 |
| `frontend/src/layouts/MainLayout.vue` | 在 HR 側邊欄新增「部門管理」選單項目 |

## 修復說明

### 後端

1. **`CreateDepartmentRequest` / `UpdateDepartmentRequest`**：定義 `name` 欄位的請求 DTO，以 Jakarta Validation 標注必填欄位。

2. **`DepartmentService`**：封裝部門 CRUD 邏輯：
   - `createDepartment`：檢查名稱重複後新增部門。
   - `updateDepartment`：查找部門後，若名稱有變更且與既有部門重複則拒絕，否則更新。
   - `deleteDepartment`：若部門仍有成員（`existsByDepartmentId`）則拒絕刪除，避免孤立使用者。

3. **`DepartmentController`**：以 `@PreAuthorize("hasRole('HR')")` 保護 `/api/hr/departments`，提供 GET/POST/PUT/DELETE 端點。

4. **`UserRepository`**：新增 `existsByDepartmentId` 衍生查詢，供刪除前檢查。

### 前端

1. **`deptApi`**：新增四個 API 方法對應後端端點。

2. **`DepartmentForm.vue`**：簡單的部門名稱表單，支援 `validate()` 及 `getData()` 公開方法，與 `UserForm.vue` 風格一致。

3. **`DepartmentManagementPage.vue`**：參照 `UserManagementPage.vue` 風格，提供：
   - 部門列表（`el-table`）
   - 新增/編輯 Dialog（含 `DepartmentForm`）
   - 刪除確認 Dialog

4. **路由 & 側邊欄**：在 `router/index.ts` 新增 `/hr/departments`，並在 `MainLayout.vue` 的 HR 區塊加入「部門管理」選單。

## 測試步驟

1. 啟動後端與前端服務。
2. 以 HR 帳號登入（如 `hr@company.com`）。
3. 確認左側選單出現「部門管理」項目。
4. 點選「部門管理」，確認頁面載入並顯示現有部門列表。
5. 點選「新增部門」，輸入名稱後按確認，確認新部門出現在列表中。
6. 點選某部門的「編輯」，修改名稱後確認，確認列表更新。
7. 點選某無成員部門的「刪除」，確認刪除成功。
8. 嘗試刪除仍有成員的部門，確認系統顯示錯誤訊息「此部門仍有成員，無法刪除」。
9. 以非 HR 帳號登入，確認「部門管理」選單不顯示，且直接存取 `/api/hr/departments` 返回 403。
