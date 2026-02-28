# sdd-demo-v3 Development Guidelines

工時回報系統 (Work Reporting System) — 全端應用程式。

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 24 + Spring Boot 4.0.2, Spring Security 7.0.2, Spring Data JPA 4.0 |
| Frontend | Vue 3 + TypeScript + Element Plus + Pinia + Vue Router |
| Database | PostgreSQL (via Docker) |
| Build | Gradle (backend), Vite (frontend) |

---

## Project Layout

```
backend/src/main/java/com/workreport/
  controller/        ← REST controllers (HTTP 入口點)
  service/           ← 商業邏輯
  repository/        ← Spring Data JPA repositories
  entity/            ← JPA 實體 (DB table 對應)
  dto/               ← Request / Response DTO
  security/          ← JWT filter + token provider
  config/            ← Security, JPA auditing 設定
  exception/         ← Global exception handler + 自訂 exception
  enums/             ← Enum 定義
  util/              ← 工具類別

frontend/src/
  pages/             ← 頁面元件 (路由對應)
  components/        ← 可複用 UI 元件
  api/               ← Axios API 呼叫函式 (對應後端 controller)
  stores/            ← Pinia 狀態管理
  router/            ← Vue Router 路由定義
  types/             ← TypeScript 介面/類別定義
```

### Backend — 關鍵檔案對應

| 功能 | Controller | Service | Repository |
|---|---|---|---|
| 認證 | `AuthController` | `AuthService` | `UserRepository` |
| 工時紀錄 | `WorkEntryController` | `WorkEntryService` | `WorkEntryRepository` |
| 任務 | `TaskController`, `ExecutorTaskController` | `TaskService`, `ExecutorTaskService` | `TaskRepository` |
| 專案 | `ProjectController`, `PmProjectController` | `ProjectService`, `PmProjectService` | `ProjectRepository` |
| 工時申請 | `HoursRequestController` | `HoursRequestService`, `HoursRequestReviewService` | `HoursRequestRepository` |
| 通知 | `NotificationController` | `NotificationService` | `NotificationRepository` |
| 部門 | `DeptOverviewController` | `DeptOverviewService` | `DepartmentRepository` |
| 使用者管理 | `UserController` | `UserService` | `UserRepository` |

### Frontend — 頁面對應角色

| 路徑 | 頁面元件 | 使用者角色 |
|---|---|---|
| `/work-entry` | `pages/executor/WorkEntryPage.vue` | Executor (工程師) |
| `/my-tasks` | `pages/executor/MyTasksPage.vue` | Executor |
| `/pm/projects` | `pages/pm/ProjectDashboardPage.vue` | PM |
| `/pm/tasks` | `pages/pm/TaskManagementPage.vue` | PM |
| `/pm/hours-requests` | `pages/pm/HoursRequestPage.vue` | PM |
| `/admin/hours-review` | `pages/admin/HoursReviewPage.vue` | Admin/HR |
| `/admin/projects` | `pages/admin/ProjectManagementPage.vue` | Admin |
| `/hr/users` | `pages/hr/UserManagementPage.vue` | HR |
| `/dept` | `pages/dept/DepartmentOverviewPage.vue` | Dept Manager |

---

## Build & Test Commands

```bash
# Backend
cd backend
./gradlew bootRun          # 啟動後端 (port 8080)
./gradlew test             # 執行所有測試
./gradlew build            # 建構 JAR

# Frontend
cd frontend
npm install
npm run dev                # 啟動前端 (port 5173)
npm run test:unit          # 執行單元測試
npm run build              # 建構產出
```

---

## Code Conventions

### Backend (Java)
- Controller 只做 request/response 轉換，邏輯全在 Service
- Service 用 `@Transactional` 標記寫入方法
- Repository 繼承 `JpaRepository<Entity, Long>`
- DTO 用 record 或 class，放在 `dto/` 子目錄按功能分組
- Exception 拋出自訂 exception (`ResourceNotFoundException`, `BusinessRuleException` 等)，由 `GlobalExceptionHandler` 統一處理
- 回應格式：成功用 `ResponseEntity<T>`，錯誤用 `ErrorResponse`
- 所有 API 前綴 `/api/v1/`
- JWT 由 `JwtAuthenticationFilter` 驗證，token 由 `JwtTokenProvider` 產生

### Frontend (Vue 3 + TypeScript)
- Composition API (`<script setup>`)，不用 Options API
- 狀態管理用 Pinia store (`stores/`)
- API 呼叫統一放 `api/` 目錄，用 Axios (`api/http.ts` 為 base client)
- 路由定義在 `router/index.ts`
- TypeScript 介面定義在 `types/index.ts`
- UI 元件用 Element Plus

---

## How to Fix a Bug (給 Copilot 的指示)

當你被指派處理一個 issue，請按照以下步驟：

1. **閱讀 issue 內容** — 找出「類型」(ui/flow/logic/add)、重現步驟、實際結果、預期結果
2. **找到相關程式碼** — 根據 issue 提供的「相關元件/檔案」欄位，或根據功能描述對照上方的對應表找到正確的 controller/service/component
3. **找出 root cause** — 閱讀相關程式碼，找到導致問題的確切行數
4. **進行最小範圍修改** — 只改有問題的程式碼，不要重構無關的東西
5. **確認修改** — 後端改動要確認 service/repository 邏輯一致，前端改動要確認 API 呼叫與型別正確
6. **建立 PR** — PR title 格式：`fix: [簡短描述] (closes #issue_number)`，PR body 說明修改了什麼以及為什麼

### 常見 Bug 類型對應位置

| issue 類型 | 主要查找位置 |
|---|---|
| `ui` | `frontend/src/pages/`, `frontend/src/components/` |
| `flow` | `frontend/src/router/index.ts`, `frontend/src/stores/`, backend Controller |
| `logic` | `backend/.../service/`, `backend/.../util/` |
| `add` | 根據要新增的功能決定，通常需要新增 controller + service + repository + frontend page |
