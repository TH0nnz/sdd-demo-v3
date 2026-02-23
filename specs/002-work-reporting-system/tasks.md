# Tasks: 報工系統

**Input**: Design documents from `/specs/002-work-reporting-system/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Constitution 要求 TDD（Red → Green → Refactor），因此每個 User Story 包含測試任務。

**Organization**: 任務依 User Story 分組，每個 Story 可獨立實作與測試。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可平行執行（不同檔案、無相依性）
- **[Story]**: 所屬 User Story（例如 US1, US2, US3）
- 包含確切的檔案路徑

## Path Conventions

- **後端**: `backend/src/main/java/com/workreport/`, `backend/src/test/java/com/workreport/`
- **前端**: `frontend/src/`, `frontend/tests/`
- **資料庫遷移**: `backend/src/main/resources/db/migration/`

---

## Phase 1: Setup（專案初始化）

**Purpose**: 建立後端與前端專案骨架，設定建構工具、相依套件與開發環境

- [x] T001 建立後端 Spring Boot 專案結構與 build.gradle（JDK 24, Spring Boot 4.0.2, Spring Security 7.0.2, Spring Data JPA 4.0, Flyway, PostgreSQL driver）in `backend/build.gradle`
- [x] T002 建立後端 application.yml 設定（資料庫連線、JPA、Flyway、JWT、Virtual Threads）in `backend/src/main/resources/application.yml`
- [x] T003 [P] 建立前端 Vue 3 + TypeScript 專案（Vite, Pinia, Vue Router, Element Plus, Axios）in `frontend/package.json`, `frontend/vite.config.ts`, `frontend/tsconfig.json`
- [x] T004 [P] 建立 Docker Compose 開發環境（PostgreSQL 18.1）in `docker-compose.yml`
- [x] T005 [P] 設定後端 Checkstyle + SpotBugs + JaCoCo in `backend/build.gradle`, `backend/config/checkstyle/checkstyle.xml`
- [x] T006 [P] 設定前端 ESLint + Prettier in `frontend/eslint.config.js`, `frontend/.prettierrc`
- [x] T007 建立後端主應用程式類別 in `backend/src/main/java/com/workreport/WorkReportApplication.java`
- [x] T008 建立 Dockerfile（後端與前端）in `backend/Dockerfile`, `frontend/Dockerfile`

---

## Phase 2: Foundational（阻塞性基礎建設）

**Purpose**: 核心基礎設施——所有 User Story 開始前必須完成

**⚠️ CRITICAL**: 在此階段完成前，不得啟動任何 User Story 的實作

### 資料庫 Schema 與 Entity

- [x] T009 建立 Flyway 初始遷移腳本（Department, User, UserRole, Project, Task, WorkEntry, HoursRequest, AuditLog, Notification 共 9 張表 + 索引）in `backend/src/main/resources/db/migration/V1__init_schema.sql`
- [x] T010 建立 Flyway seed 資料遷移腳本（初始部門、5 種角色測試帳號）in `backend/src/main/resources/db/migration/V2__seed_data.sql`
- [x] T011 [P] 建立列舉型別（Role, ProjectStatus, TaskStatus, HoursRequestStatus, HoursRequestTargetType, NotificationType, AuditActionType）in `backend/src/main/java/com/workreport/enums/`
- [x] T012 [P] 建立 BaseEntity 抽象類別（id, createdAt, updatedAt, createdBy, updatedBy + JPA Auditing）in `backend/src/main/java/com/workreport/entity/BaseEntity.java`
- [x] T013 建立 Department Entity in `backend/src/main/java/com/workreport/entity/Department.java`
- [x] T014 建立 User Entity（含 UserRole 集合、帳號鎖定欄位、password_changed）in `backend/src/main/java/com/workreport/entity/User.java`
- [x] T015 [P] 建立 Project Entity in `backend/src/main/java/com/workreport/entity/Project.java`
- [x] T016 [P] 建立 Task Entity in `backend/src/main/java/com/workreport/entity/Task.java`
- [x] T017 [P] 建立 WorkEntry Entity in `backend/src/main/java/com/workreport/entity/WorkEntry.java`
- [x] T018 [P] 建立 HoursRequest Entity in `backend/src/main/java/com/workreport/entity/HoursRequest.java`
- [x] T019 [P] 建立 AuditLog Entity（append-only，無 UPDATE/DELETE）in `backend/src/main/java/com/workreport/entity/AuditLog.java`
- [x] T020 [P] 建立 Notification Entity in `backend/src/main/java/com/workreport/entity/Notification.java`

### Repository

- [x] T021 [P] 建立所有 Repository 介面（DepartmentRepository, UserRepository, ProjectRepository, TaskRepository, WorkEntryRepository, HoursRequestRepository, AuditLogRepository, NotificationRepository）in `backend/src/main/java/com/workreport/repository/`

### 認證與授權基礎設施

- [x] T022 實作 JWT 工具類別（產生 / 解析 / 驗證 Token）in `backend/src/main/java/com/workreport/security/JwtTokenProvider.java`
- [x] T023 實作 JwtAuthenticationFilter（從請求標頭擷取 Token 並建立 SecurityContext）in `backend/src/main/java/com/workreport/security/JwtAuthenticationFilter.java`
- [x] T024 實作 Spring Security 設定（SecurityFilterChain、CORS、RBAC 端點權限、BCrypt PasswordEncoder）in `backend/src/main/java/com/workreport/config/SecurityConfig.java`
- [x] T025 實作 JPA Auditing 設定（AuditorAware 從 SecurityContext 取得當前使用者）in `backend/src/main/java/com/workreport/config/JpaAuditingConfig.java`

### 共用基礎設施

- [x] T026 實作全域例外處理器（@RestControllerAdvice，統一錯誤回應格式：context + what + action）in `backend/src/main/java/com/workreport/exception/GlobalExceptionHandler.java`
- [x] T027 [P] 建立自訂例外類別（ResourceNotFoundException, BusinessRuleException, PasswordPolicyException 等）in `backend/src/main/java/com/workreport/exception/`
- [x] T028 [P] 建立共用 DTO（PageResponse, ErrorResponse）in `backend/src/main/java/com/workreport/dto/common/`
- [x] T029 [P] 實作 AuditLogService（append-only 寫入稽核日誌）in `backend/src/main/java/com/workreport/service/AuditLogService.java`
- [x] T030 [P] 實作 NotificationService（建立站內通知）in `backend/src/main/java/com/workreport/service/NotificationService.java`
- [x] T031 [P] 實作 WorkDayUtils 工具類別（計算過去三工作天範圍、判斷日期是否可編輯）in `backend/src/main/java/com/workreport/util/WorkDayUtils.java`
- [x] T032 實作 WorkDayUtils 單元測試（涵蓋跨週、週一、週五等邊界情境）in `backend/src/test/java/com/workreport/unit/util/WorkDayUtilsTest.java`

### 認證 API

- [x] T033 建立 AuthController — POST /api/auth/login（帳號鎖定、停用帳號檢查、首次改密碼標記）in `backend/src/main/java/com/workreport/controller/AuthController.java`
- [x] T034 建立 AuthController — POST /api/auth/change-password（密碼複雜度驗證、首次改密碼標記更新）in `backend/src/main/java/com/workreport/controller/AuthController.java`
- [x] T035 實作 AuthService（登入驗證、密碼雜湊比對、鎖定邏輯、密碼變更）in `backend/src/main/java/com/workreport/service/AuthService.java`
- [x] T036 建立認證 DTO（LoginRequest, LoginResponse, ChangePasswordRequest）in `backend/src/main/java/com/workreport/dto/auth/`
- [x] T037 建立認證整合測試（登入成功/失敗、帳號鎖定、首次改密碼強制、密碼複雜度）in `backend/src/test/java/com/workreport/integration/AuthIntegrationTest.java`

### 前端基礎設施

- [x] T038 [P] 建立前端 TypeScript 型別定義（User, Project, Task, WorkEntry, HoursRequest, Notification 等）in `frontend/src/types/`
- [x] T039 [P] 建立 Axios 實例與攔截器（JWT Token 附加、401 導向登入、錯誤統一處理）in `frontend/src/api/http.ts`
- [x] T040 [P] 建立 Auth API 封裝（login, changePassword）in `frontend/src/api/auth.ts`
- [x] T041 [P] 建立 Auth Store（Pinia — token 管理、使用者資訊、角色判斷、forcePasswordChange）in `frontend/src/stores/auth.ts`
- [x] T042 建立 Vue Router 設定與角色路由守衛（依角色分流至對應頁面、未登入導向登入頁、強制改密碼攔截）in `frontend/src/router/index.ts`
- [x] T043 建立主佈局元件（側邊欄導覽依角色動態顯示、頂部通知圖示、登出）in `frontend/src/layouts/MainLayout.vue`
- [x] T044 [P] 建立登入頁面 in `frontend/src/pages/auth/LoginPage.vue`
- [x] T045 [P] 建立強制改密碼頁面 in `frontend/src/pages/auth/ChangePasswordPage.vue`
- [x] T046 [P] 建立 Element Plus 與 i18n 設定（繁體中文 locale）in `frontend/src/plugins/element-plus.ts`

**Checkpoint**: 基礎建設就緒——使用者可登入、強制改密碼、依角色分流至空白頁面。User Story 實作可以開始。

---

## Phase 3: User Story 1 — 執行人員填報每日工時 (Priority: P1) 🎯 MVP

**Goal**: 執行人員可以查看指派的 Task、填報工時（即時扣減）、修改近三工作天紀錄、手動完成 Task

**Independent Test**: 以執行人員帳號登入，對已指派 Task 填報工時，確認剩餘時數正確扣減，超過三工作天的紀錄無法修改

### Tests for User Story 1 ⚠️

> **NOTE: 先寫測試，確認測試 FAIL，再實作**

- [x] T047 [P] [US1] 建立 WorkEntryService 單元測試（填報工時扣減、三工作天限制、終態 Task 拒絕、時數歸零通知、當日累計上限 24h、0.5 倍數驗證）in `backend/src/test/java/com/workreport/unit/service/WorkEntryServiceTest.java`
- [x] T048 [P] [US1] 建立 WorkEntry API 整合測試（POST/PUT /api/work-entries、GET /api/work-entries、GET /api/my-tasks、POST /api/my-tasks/{id}/complete）in `backend/src/test/java/com/workreport/integration/WorkEntryIntegrationTest.java`
- [x] T049 [P] [US1] 建立 WorkEntry API 合約測試（請求/回應格式驗證、錯誤回應格式）in `backend/src/test/java/com/workreport/contract/WorkEntryContractTest.java`

### Implementation for User Story 1

- [x] T050 [P] [US1] 建立 WorkEntry DTO（CreateWorkEntryRequest, UpdateWorkEntryRequest, WorkEntryResponse）in `backend/src/main/java/com/workreport/dto/workentry/`
- [x] T051 [P] [US1] 建立 TaskResponse DTO（執行人員 Task 清單用）in `backend/src/main/java/com/workreport/dto/task/TaskResponse.java`
- [x] T052 [US1] 實作 WorkEntryService（填報工時、修改工時、三工作天驗證、Task 狀態與時數檢查、Task 自動轉 IN_PROGRESS、時數歸零觸發通知）in `backend/src/main/java/com/workreport/service/WorkEntryService.java`
- [x] T053 [US1] 實作 ExecutorTaskService（查詢指派 Task 清單、標記 Task 完成、凍結工時填報）in `backend/src/main/java/com/workreport/service/ExecutorTaskService.java`
- [x] T054 [US1] 建立 WorkEntryController — GET/POST/PUT /api/work-entries in `backend/src/main/java/com/workreport/controller/WorkEntryController.java`
- [x] T055 [US1] 建立 ExecutorTaskController — GET /api/my-tasks、POST /api/my-tasks/{id}/complete in `backend/src/main/java/com/workreport/controller/ExecutorTaskController.java`

### 前端 — User Story 1

- [x] T056 [P] [US1] 建立 WorkEntry API 封裝（getWorkEntries, createWorkEntry, updateWorkEntry）in `frontend/src/api/work-entries.ts`
- [x] T057 [P] [US1] 建立 MyTasks API 封裝（getMyTasks, completeTask）in `frontend/src/api/my-tasks.ts`
- [x] T058 [P] [US1] 建立 WorkEntry Store（Pinia — 工時填報清單、CRUD 操作）in `frontend/src/stores/work-entries.ts`
- [x] T059 [US1] 建立「我的 Task 清單」頁面（顯示所有指派 Task、狀態、已消耗/剩餘時數、完成按鈕）in `frontend/src/pages/executor/MyTasksPage.vue`
- [x] T060 [US1] 建立「工時填報」頁面（選擇 Task、選擇日期、輸入工時 0.5 倍數、近三工作天紀錄列表含可編輯/唯讀標記）in `frontend/src/pages/executor/WorkEntryPage.vue`
- [x] T061 [US1] 建立工時填報表單元件（Task 下拉、日期選擇器限三工作天、工時輸入 step=0.5、時數歸零警告提示）in `frontend/src/components/work-entry/WorkEntryForm.vue`
- [x] T062 [US1] 建立工時記錄列表元件（每日紀錄、可編輯/唯讀狀態切換、編輯模態框）in `frontend/src/components/work-entry/WorkEntryList.vue`

**Checkpoint**: 執行人員可登入 → 查看 Task → 填報/修改工時 → 完成 Task。User Story 1 可獨立測試。

---

## Phase 4: User Story 2 — PM 管理 Task 並即時監控進度 (Priority: P2)

**Goal**: PM 可建立/修改/關閉/刪除 Task、指派執行人員、查看專案儀表板、提交時數增補申請

**Independent Test**: 以 PM 帳號建立 Task 並指派，確認執行人員填報後儀表板 5 分鐘內更新

### Tests for User Story 2 ⚠️

- [x] T063 [P] [US2] 建立 TaskService 單元測試（CRUD、指派、關閉、刪除、專案已關閉拒絕建立）in `backend/src/test/java/com/workreport/unit/service/TaskServiceTest.java`
- [x] T064 [P] [US2] 建立 HoursRequestService 單元測試（建立申請、target_type 驗證）in `backend/src/test/java/com/workreport/unit/service/HoursRequestServiceTest.java`
- [x] T065 [P] [US2] 建立 PM API 整合測試（Task CRUD、儀表板查詢、時數申請）in `backend/src/test/java/com/workreport/integration/PmIntegrationTest.java`

### Implementation for User Story 2

- [x] T066 [P] [US2] 建立 Task DTO（CreateTaskRequest, UpdateTaskRequest, TaskDetailResponse）in `backend/src/main/java/com/workreport/dto/task/`
- [x] T067 [P] [US2] 建立 HoursRequest DTO（CreateHoursRequestRequest, HoursRequestResponse）in `backend/src/main/java/com/workreport/dto/hoursrequest/`
- [x] T068 [P] [US2] 建立 ProjectDashboard DTO（專案摘要含 Task 統計、使用率）in `backend/src/main/java/com/workreport/dto/project/ProjectDashboardResponse.java`
- [x] T069 [US2] 實作 TaskService（建立、修改、關閉、刪除 Task，含專案狀態與工時紀錄檢查，PM 強制關閉觸發 AuditLog）in `backend/src/main/java/com/workreport/service/TaskService.java`
- [x] T070 [US2] 實作 HoursRequestService（建立增補申請、驗證 PM 所屬專案）in `backend/src/main/java/com/workreport/service/HoursRequestService.java`
- [x] T071 [US2] 實作 PmProjectService（PM 專案列表、專案儀表板含 Task 統計與使用率）in `backend/src/main/java/com/workreport/service/PmProjectService.java`
- [x] T072 [US2] 建立 TaskController — CRUD /api/projects/{projectId}/tasks in `backend/src/main/java/com/workreport/controller/TaskController.java`
- [x] T073 [US2] 建立 HoursRequestController — POST /api/hours-requests、GET /api/hours-requests in `backend/src/main/java/com/workreport/controller/HoursRequestController.java`
- [x] T074 [US2] 建立 PmProjectController — GET /api/pm/projects in `backend/src/main/java/com/workreport/controller/PmProjectController.java`

### 前端 — User Story 2

- [x] T075 [P] [US2] 建立 Task API 封裝（CRUD /api/projects/{projectId}/tasks）in `frontend/src/api/tasks.ts`
- [x] T076 [P] [US2] 建立 HoursRequest API 封裝（create, list）in `frontend/src/api/hours-requests.ts`
- [x] T077 [P] [US2] 建立 PM Projects API 封裝（dashboard）in `frontend/src/api/pm-projects.ts`
- [x] T078 [US2] 建立 PM 專案儀表板頁面（專案清單、時數使用率進度條、Task 狀態統計圓餅圖）in `frontend/src/pages/pm/ProjectDashboardPage.vue`
- [x] T079 [US2] 建立 Task 管理頁面（Task 列表、建立/編輯表單、指派執行人員、關閉/刪除操作）in `frontend/src/pages/pm/TaskManagementPage.vue`
- [x] T080 [US2] 建立時數增補申請頁面（申請表單、target_type 選擇、申請歷史列表含狀態追蹤）in `frontend/src/pages/pm/HoursRequestPage.vue`
- [x] T081 [P] [US2] 建立 Task 表單元件（名稱、時數、指派人員下拉）in `frontend/src/components/task/TaskForm.vue`

**Checkpoint**: PM 可建立/管理 Task → 查看儀表板 → 提交增補申請。User Story 2 可獨立測試。

---

## Phase 5: User Story 3 — 管理層管理專案與審核時數申請 (Priority: P3)

**Goal**: 管理層可建立/修改/關閉/刪除專案、指派 PM、審核時數增補申請

**Independent Test**: 以管理層帳號建立專案並指派 PM，確認 PM 帳號可管理該專案

### Tests for User Story 3 ⚠️

- [x] T082 [P] [US3] 建立 ProjectService 單元測試（CRUD、關閉前檢查非終態 Task、刪除前檢查工時紀錄）in `backend/src/test/java/com/workreport/unit/service/ProjectServiceTest.java`
- [x] T083 [P] [US3] 建立 HoursRequestReviewService 單元測試（核准補至 Task、核准補至專案、拒絕含原因）in `backend/src/test/java/com/workreport/unit/service/HoursRequestReviewServiceTest.java`
- [x] T084 [P] [US3] 建立管理層 API 整合測試（專案 CRUD、時數審核、通知觸發）in `backend/src/test/java/com/workreport/integration/AdminIntegrationTest.java`

### Implementation for User Story 3

- [x] T085 [P] [US3] 建立 Project DTO（CreateProjectRequest, UpdateProjectRequest, ProjectResponse）in `backend/src/main/java/com/workreport/dto/project/`
- [x] T086 [P] [US3] 建立 HoursRequestReview DTO（ApproveRequest, RejectRequest）in `backend/src/main/java/com/workreport/dto/hoursrequest/`
- [x] T087 [US3] 實作 ProjectService（建立、修改、關閉、刪除專案，含非終態 Task 檢查與工時紀錄檢查）in `backend/src/main/java/com/workreport/service/ProjectService.java`
- [x] T088 [US3] 實作 HoursRequestReviewService（核准——依 target_type 增加 Task 或 Project 時數、拒絕——必填原因、觸發通知與 AuditLog）in `backend/src/main/java/com/workreport/service/HoursRequestReviewService.java`
- [x] T089 [US3] 建立 ProjectController — CRUD /api/projects、POST /api/projects/{id}/close、DELETE /api/projects/{id} in `backend/src/main/java/com/workreport/controller/ProjectController.java`
- [x] T090 [US3] 建立 HoursRequestReviewController — POST /api/admin/hours-requests/{id}/review in `backend/src/main/java/com/workreport/controller/HoursRequestReviewController.java`

### 前端 — User Story 3

- [x] T091 [P] [US3] 建立 Projects API 封裝（CRUD, close, delete）in `frontend/src/api/projects.ts`
- [x] T092 [P] [US3] 建立 HoursRequest 審核 API 封裝（approve, reject）in `frontend/src/api/admin-hours-requests.ts`
- [x] T093 [US3] 建立專案管理頁面（專案列表含狀態篩選、建立/編輯表單、指派 PM 下拉、關閉/刪除操作）in `frontend/src/pages/admin/ProjectManagementPage.vue`
- [x] T094 [US3] 建立時數審核頁面（待審核申請列表、核准/拒絕操作、拒絕原因必填、歷史審核紀錄）in `frontend/src/pages/admin/HoursReviewPage.vue`
- [x] T095 [P] [US3] 建立專案表單元件（名稱、時數預算、PM 選擇）in `frontend/src/components/project/ProjectForm.vue`

**Checkpoint**: 管理層可建立/管理專案 → 審核時數申請 → 核准/拒絕立即生效。User Story 3 可獨立測試。

---

## Phase 6: User Story 4 — HR 管理使用者與角色 (Priority: P4)

**Goal**: HR 可新增使用者、指派角色、變更角色、停用/啟用帳號

**Independent Test**: 以 HR 帳號新增執行人員，新帳號可登入並看到執行人員操作介面

### Tests for User Story 4 ⚠️

- [x] T096 [P] [US4] 建立 UserService 單元測試（新增使用者、角色變更觸發 AuditLog、停用帳號觸發 Task 轉未指派）in `backend/src/test/java/com/workreport/unit/service/UserServiceTest.java`
- [x] T097 [P] [US4] 建立 HR API 整合測試（使用者 CRUD、角色變更、停用/啟用、AuditLog 驗證）in `backend/src/test/java/com/workreport/integration/HrIntegrationTest.java`

### Implementation for User Story 4

- [x] T098 [P] [US4] 建立 User DTO（CreateUserRequest, UpdateUserRequest, UserResponse）in `backend/src/main/java/com/workreport/dto/user/`
- [x] T099 [US4] 實作 UserService（新增使用者含初始密碼 BCrypt、角色變更含 AuditLog、停用帳號觸發非終態 Task 轉未指派與通知 PM、啟用帳號含 AuditLog）in `backend/src/main/java/com/workreport/service/UserService.java`
- [x] T100 [US4] 建立 UserController — GET/POST/PUT /api/users、POST /api/users/{id}/deactivate、POST /api/users/{id}/activate in `backend/src/main/java/com/workreport/controller/UserController.java`

### 前端 — User Story 4

- [x] T101 [P] [US4] 建立 Users API 封裝（CRUD, deactivate, activate）in `frontend/src/api/users.ts`
- [x] T102 [US4] 建立使用者管理頁面（使用者列表含部門/狀態篩選、新增/編輯表單、角色多選、停用/啟用操作）in `frontend/src/pages/hr/UserManagementPage.vue`
- [x] T103 [P] [US4] 建立使用者表單元件（姓名、email、部門選擇、角色多選 checkbox）in `frontend/src/components/user/UserForm.vue`

**Checkpoint**: HR 可新增/管理使用者 → 指派角色 → 停用/啟用帳號。User Story 4 可獨立測試。

---

## Phase 7: User Story 5 — 部門主管查看部門工時與 Task 狀態 (Priority: P5)

**Goal**: 部門主管可唯讀查看部門成員的工時統計與 Task 清單

**Independent Test**: 以部門主管帳號登入，確認可見部門成員工時與 Task，介面無任何修改入口

### Tests for User Story 5 ⚠️

- [x] T104 [P] [US5] 建立 DepartmentService 單元測試（部門成員查詢限本部門、工時統計計算）in `backend/src/test/java/com/workreport/unit/service/DeptOverviewServiceTest.java`
- [x] T105 [P] [US5] 建立部門主管 API 整合測試（成員列表、Task 詳情、跨部門拒絕存取）in `backend/src/test/java/com/workreport/integration/DeptManagerIntegrationTest.java`

### Implementation for User Story 5

- [x] T106 [P] [US5] 建立 Department DTO（DepartmentMemberResponse, MemberTaskResponse）in `backend/src/main/java/com/workreport/dto/department/`
- [x] T107 [US5] 實作 DepartmentService（部門成員工時統計——本週/本月、成員 Task 清單、限制本部門存取）in `backend/src/main/java/com/workreport/service/DeptOverviewService.java`
- [x] T108 [US5] 建立 DepartmentController — GET /api/department/members、GET /api/department/members/{userId}/tasks in `backend/src/main/java/com/workreport/controller/DeptOverviewController.java`

### 前端 — User Story 5

- [x] T109 [P] [US5] 建立 Department API 封裝（getMembers, getMemberTasks）in `frontend/src/api/dept.ts`
- [x] T110 [US5] 建立部門工時總覽頁面（成員列表含本週/本月工時、點擊展開 Task 詳情、純唯讀無任何編輯入口）in `frontend/src/pages/dept/DepartmentOverviewPage.vue`
- [x] T111 [US5] 建立成員 Task 詳情元件（Task 清單含名稱、專案、狀態、已消耗時數、唯讀）in `frontend/src/components/department/MemberTaskDetail.vue`

**Checkpoint**: 部門主管可查看部門工時統計與 Task 狀態，介面純唯讀。所有 5 個 User Story 完成。

---

## Phase 8: 通知系統與 Cross-Cutting

**Purpose**: 完成站內通知前端、跨功能整合與最終品質保障

- [x] T112 [P] 建立 Notification API 封裝（getNotifications, markAsRead）in `frontend/src/api/notifications.ts`
- [x] T113 [P] 建立 Notification Store（Pinia — 輪詢 30 秒、未讀數量）in `frontend/src/stores/notifications.ts`
- [x] T114 建立通知下拉元件（頂部鈴鐺圖示、未讀數量 badge、下拉通知列表、標記已讀）in `frontend/src/components/notification/NotificationDropdown.vue`
- [x] T115 建立 NotificationController — GET /api/notifications、PATCH /api/notifications/{id}/read in `backend/src/main/java/com/workreport/controller/NotificationController.java`

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 品質保障、效能最佳化與最終驗證

- [x] T116 [P] RBAC 端到端安全測試（每個角色嘗試存取其他角色 API，驗證 403）in `backend/src/test/java/com/workreport/integration/RbacSecurityTest.java`
- [x] T117 [P] 資料庫查詢效能驗證（關鍵查詢 EXPLAIN ANALYZE、確認索引使用、禁止全表掃描）in `backend/src/test/java/com/workreport/integration/QueryPerformanceTest.java`
- [x] T118 [P] 樂觀鎖定並行測試（多名執行人員同時填報同一 Task 工時）in `backend/src/test/java/com/workreport/integration/ConcurrencyTest.java`
- [x] T119 [P] 前端 Loading / Empty / Error 三態檢查（所有頁面須具備三態 UI）across `frontend/src/pages/`
- [ ] T120 [P] 前端無障礙掃描（WCAG 2.1 AA 檢查）across `frontend/src/`（skipped，待後續補齊）
- [x] T121 程式碼清理與重構（移除 TODO、dead code、確保 Checkstyle/ESLint zero warnings）
- [x] T122 執行 quickstart.md 驗證（從零啟動至登入成功的完整流程）per `specs/002-work-reporting-system/quickstart.md`
- [x] T123 [P] 建立前端 Vitest 單元測試（stores/router）in `frontend/tests/unit/`
- [x] T124 [P] 建立 Playwright E2E 測試（US1 驗收情境）in `frontend/tests/e2e/us1-work-entry.spec.ts`
- [x] T125 [P] 建立負載測試腳本（100 並發，p95 ≤ 500ms）in `performance/k6/us1-smoke.js`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 無相依性 — 立即開始
- **Foundational (Phase 2)**: 依賴 Phase 1 完成 — **阻塞所有 User Story**
- **User Story 1 (Phase 3)**: 依賴 Phase 2 完成
- **User Story 2 (Phase 4)**: 依賴 Phase 2 完成（可與 US1 平行，但 US2 的儀表板驗證需 US1 的工時填報功能）
- **User Story 3 (Phase 5)**: 依賴 Phase 2 完成（與 US1/US2 弱相依：審核申請需 US2 的申請功能）
- **User Story 4 (Phase 6)**: 依賴 Phase 2 完成（與其他 Story 獨立）
- **User Story 5 (Phase 7)**: 依賴 Phase 2 完成（唯讀查詢，與其他 Story 獨立）
- **通知系統 (Phase 8)**: 依賴 Phase 2 NotificationService
- **Polish (Phase 9)**: 依賴所有 User Story 完成

### User Story Dependencies

- **US1（P1 — 執行人員填報）**: Phase 2 完成後立即開始，無其他 Story 相依
- **US2（P2 — PM 管理 Task）**: Phase 2 完成後開始，US2 的 HoursRequestController 部分在 US3 擴充
- **US3（P3 — 管理層專案管理）**: Phase 2 完成後開始，審核功能擴充 US2 的 HoursRequestController
- **US4（P4 — HR 使用者管理）**: Phase 2 完成後開始，完全獨立
- **US5（P5 — 部門主管檢視）**: Phase 2 完成後開始，完全獨立

### Within Each User Story

1. 測試先寫（Red）→ 確認 FAIL
2. DTO 先建立（可平行）
3. Service 實作（依賴 DTO + Repository）
4. Controller 實作（依賴 Service + DTO）
5. 前端 API 封裝（可與後端平行）
6. 前端頁面與元件（依賴 API 封裝）
7. 確認測試 PASS（Green）
8. Refactor

### Parallel Opportunities

- Phase 1: T003, T004, T005, T006 可平行
- Phase 2: T011~T020 的 Entity 建立大部分可平行；T027~T031 基礎設施可平行；T038~T046 前端基礎可平行
- Phase 3~7: 各 Story 的測試可平行；DTO 可平行；前端 API 封裝可平行
- 跨 Story: US1 與 US4/US5 完全獨立，可由不同開發者同時進行

---

## Parallel Example: User Story 1

```bash
# 同時啟動所有 US1 測試（Red phase）:
Task T047: "WorkEntryService 單元測試"
Task T048: "WorkEntry API 整合測試"
Task T049: "WorkEntry API 合約測試"

# 同時建立所有 US1 DTO:
Task T050: "WorkEntry DTO"
Task T051: "TaskResponse DTO"

# 同時建立所有 US1 前端 API 封裝:
Task T056: "WorkEntry API 封裝"
Task T057: "MyTasks API 封裝"
Task T058: "WorkEntry Store"
```

---

## Implementation Strategy

### MVP First（User Story 1 Only）

1. 完成 Phase 1: Setup
2. 完成 Phase 2: Foundational（CRITICAL — 阻塞所有 Story）
3. 完成 Phase 3: User Story 1
4. **STOP and VALIDATE**: 以執行人員帳號獨立測試工時填報流程
5. 可部署/展示 MVP

### Incremental Delivery

1. Setup + Foundational → 基礎就緒（可登入、改密碼、角色分流）
2. + User Story 1 → 執行人員可填報工時（MVP!）
3. + User Story 2 → PM 可管理 Task 與監控進度
4. + User Story 3 → 管理層可管理專案與審核申請
5. + User Story 4 → HR 可管理使用者
6. + User Story 5 → 部門主管可檢視工時
7. + Notification + Polish → 完整功能

### Parallel Team Strategy

Phase 2 完成後：
- **Developer A**: User Story 1（P1 — 核心填報流程）
- **Developer B**: User Story 4（P4 — HR 使用者管理，完全獨立）
- **Developer C**: User Story 5（P5 — 部門主管檢視，完全獨立）
- A 完成後 → User Story 2（P2 — PM 管理）
- B/C 完成後 → User Story 3（P3 — 管理層）

---

## Notes

- [P] 標記 = 不同檔案、無相依性，可平行執行
- [Story] 標籤將任務對應至特定 User Story，確保可追溯性
- 每個 User Story 可獨立完成並測試
- 測試先寫（Red）→ 確認失敗 → 實作（Green）→ 重構（Refactor）
- 每完成一個 Task 或邏輯群組即 commit
- 在任何 Checkpoint 處可停下來獨立驗證該 Story
- 避免：模糊的任務描述、同一檔案衝突、破壞 Story 獨立性的跨 Story 相依
