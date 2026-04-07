# sdd-demo-v3 - 報工系統示範專案

> Vue 3 + TypeScript 前端 | Spring Boot 4 後端 | PostgreSQL 18

五角色報工系統，涵蓋專案建立、任務拆分、工時填報、時數增補申請與進度監控。

## 目錄

- [系統角色](#系統角色)
- [技術堆疊](#技術堆疊)
- [快速啟動](#快速啟動)
- [本地開發](#本地開發)
- [測試帳號](#測試帳號)
- [建構與測試](#建構與測試)
- [安全架構](#安全架構)
- [API 端點總覽](#api-端點總覽)
- [部署到遠端主機](#部署到遠端主機)
- [Auto-Fix 工作流程](#auto-fix-工作流程)
- [CI/CD 工作流程](#cicd-工作流程)
- [目錄結構](#目錄結構)

---

## 系統角色

| 角色 | 主要職責 |
| --- | --- |
| Admin（管理層） | 建立／修改／關閉專案；設定時數預算；審核時數增補申請 |
| PM | 拆分並指派任務；監控進度；向管理層提交時數增補申請 |
| Dept. Manager（部門主管） | 查看所屬部門的成員、任務與概覽 |
| Executor（執行人員） | 填報工時（最小單位 0.5h）；完成個人任務 |
| HR | 新增使用者、指派角色、停用／啟用帳號 |

---

## 技術堆疊

### 後端

| 技術 | 版本 |
| --- | --- |
| Java | 24 |
| Spring Boot | 4.0.2 |
| Spring Security + JWT（jjwt） | 0.12.6 |
| Spring Data JPA + Hibernate | — |
| Flyway | — |
| PostgreSQL | 18 |
| Gradle Wrapper | 8.x |
| JaCoCo | 0.8.13（Service 層最低覆蓋率 90%） |
| Checkstyle | 10.21.4 |
| Testcontainers | 1.21.1 |

### 前端

| 技術 | 版本 |
| --- | --- |
| Vue | 3.5.x |
| TypeScript | 5.7.x |
| Vite | 6.1.x |
| Pinia | 3.0.x |
| Vue Router | 4.5.x |
| Element Plus | 2.9.x |
| Axios | 1.7.x |
| Vitest | 3.0.x |
| Playwright | 1.50.x |

---

## 快速啟動

```bash
docker compose up -d --build
```

| 服務 | 對外位址 |
| --- | --- |
| 前端（Nginx） | http://localhost |
| 後端 API | http://localhost:8089 |
| PostgreSQL | localhost:5454 |

---

## 本地開發

### 前置需求

| 工具 | 版本 |
| --- | --- |
| JDK | 24（或 21 LTS） |
| Node.js | 22 |
| npm | 隨 Node.js 附帶 |
| Docker / Docker Compose | 最新穩定版 |

> **注意**：若出現 `Unsupported class file major version 69` 代表使用 Java 25，請切換至 24 或 21：
> ```bash
> export JAVA_HOME=/path/to/jdk24
> ```

### 啟動步驟

**1. 啟動資料庫**

```bash
docker compose up -d db
```

**2. 啟動後端**（http://localhost:8080）

```bash
cd backend
./gradlew bootRun
```

**3. 啟動前端**（http://localhost:5173）

```bash
cd frontend
npm install
npm run dev
```

---

## 測試帳號

資料庫啟動後由 Flyway（V2 seed）自動建立，**首次登入必須修改密碼**。

| 角色 | Email | 初始密碼 | 所屬部門 |
| --- | --- | --- | --- |
| Admin | admin@company.com | Welcome123 | 研發部 |
| PM | pm@company.com | Welcome123 | 研發部 |
| Dept. Manager | manager@company.com | Welcome123 | 研發部 |
| Executor | executor@company.com | Welcome123 | 研發部 |
| HR | hr@company.com | Welcome123 | 人力資源部 |

密碼規則：8 字元以上，含大寫字母、小寫字母、數字。

---

## 建構與測試

### 後端（Gradle）

```bash
cd backend

# 執行單元測試
./gradlew test

# 產生 JaCoCo 覆蓋率報告
./gradlew jacocoTestReport

# 覆蓋率門檻驗證（Service 層最低 90%）
./gradlew jacocoTestCoverageVerification

# 靜態程式碼分析
./gradlew checkstyleMain checkstyleTest

# CI 完整驗證（全部合一）
./gradlew clean test jacocoTestCoverageVerification checkstyleMain checkstyleTest

# 建構 JAR
./gradlew bootJar
```

JaCoCo HTML 報告：`backend/build/reports/jacoco/test/html/index.html`

### 前端

```bash
cd frontend

npm install           # 安裝相依套件
npm run lint          # ESLint（含自動修正）
npm run test:unit     # Vitest 單元測試
npm run build         # 型別檢查（vue-tsc）+ Vite build
```

### E2E 測試（Playwright）

```bash
cd frontend
npx playwright install   # 首次執行需安裝瀏覽器
npm run test:e2e
```

### 效能測試（k6）

腳本：`performance/k6/us1-smoke.js`

| 設定 | 值 |
| --- | --- |
| 虛擬使用者（VU） | 100 |
| 持續時間 | 1 分鐘 |
| 測試端點 | `GET /api/work-entries` |
| 門檻：p95 回應時間 | < 500 ms |
| 門檻：check 成功率 | > 99% |

```bash
# 安裝 k6（macOS）
brew install k6

# 對本地後端執行（預設 http://localhost:8080）
k6 run performance/k6/us1-smoke.js

# 指定目標與帶入 JWT Token
BASE_URL="http://your-api" TOKEN="your-jwt" k6 run performance/k6/us1-smoke.js
```

---

## 安全架構

每個進入後端的請求依序通過三層主動驗證，另有一層資料範圍基礎設施備用。

### Layer 1 — JwtAuthenticationFilter（OncePerRequestFilter）

- 從 `Authorization: Bearer <token>` 取出 JWT
- 驗證簽章與有效期（30 分鐘）
- 解析 `userId` + `roles` → 寫入 `SecurityContextHolder`
- 失敗時回傳 401

### Layer 2 — Spring Security URL 規則

```
POST /api/auth/login  → permitAll
GET  /actuator/health → permitAll
ALL  /api/**          → authenticated
其他                  → denyAll
```

未通過時回傳 403。

### Layer 3 — RoleCheckAspect（`@RequireRole` AOP）

- 功能層級授權：方法或類別上標註所需角色，使用者需持有至少一個
- 方法層級 annotation 優先於 class 層級
- Role Hierarchy：`ADMIN > HR`、`ADMIN > PM`、`ADMIN > DEPT_MANAGER`、`ADMIN > EXECUTOR`
- 未通過時拋出 `AccessDeniedException`（403）

### Layer 4 — DataScopeAspect（`@DataScope` AOP，基礎設施）

已實作完整的資料列層級授權基礎設施，角色對應如下：

| 角色 | ScopeType |
| --- | --- |
| ADMIN、HR | ALL |
| PM | PROJECT |
| Dept. Manager | DEPARTMENT |
| Executor | SELF |

透過 `ThreadLocal<DataScopeContext>` 儲存當前請求的範圍，服務層可呼叫 `DataScopeAspect.getCurrentScope()` 取得。目前尚無服務方法掛載 `@DataScope`，此層不主動攔截任何請求。

### 帳號安全

- 密碼以 BCrypt 雜湊儲存
- 連續登入失敗 15 次（5 分鐘內）→ 鎖定帳號 15 分鐘
- JWT 效期 30 分鐘（無狀態設計，無 Refresh Token）
- `Project`、`Task` 使用 `@Version` 樂觀鎖防止並發衝突

---

## API 端點總覽

| 模組 | 方法 | 路徑 | 可存取角色 |
| --- | --- | --- | --- |
| **認證** | POST | `/api/auth/login` | 所有人 |
| | POST | `/api/auth/change-password` | 登入使用者 |
| **使用者** | GET | `/api/users` | HR、ADMIN |
| | GET | `/api/users/{id}` | HR、ADMIN |
| | POST | `/api/users` | HR、ADMIN |
| | PUT | `/api/users/{id}` | HR、ADMIN |
| | POST | `/api/users/{id}/disable` | HR、ADMIN |
| | POST | `/api/users/{id}/enable` | HR、ADMIN |
| | POST | `/api/users/{id}/reset-password` | HR、ADMIN |
| **部門** | GET | `/api/hr/departments` | HR、ADMIN |
| | POST | `/api/hr/departments` | HR、ADMIN |
| | PUT | `/api/hr/departments/{id}` | HR、ADMIN |
| | DELETE | `/api/hr/departments/{id}` | HR、ADMIN |
| **專案** | GET | `/api/projects` | ADMIN |
| | GET | `/api/projects/{id}` | ADMIN |
| | POST | `/api/projects` | ADMIN |
| | PUT | `/api/projects/{id}` | ADMIN |
| | POST | `/api/projects/{id}/close` | ADMIN |
| | POST | `/api/projects/{id}/activate` | ADMIN |
| | DELETE | `/api/projects/{id}` | ADMIN |
| **任務** | GET | `/api/projects/{projectId}/tasks` | PM、ADMIN |
| | GET | `/api/projects/{projectId}/tasks/assignable-executors` | PM、ADMIN |
| | POST | `/api/projects/{projectId}/tasks` | PM、ADMIN |
| | PUT | `/api/projects/{projectId}/tasks/{id}` | PM、ADMIN |
| | POST | `/api/projects/{projectId}/tasks/{id}/close` | PM、ADMIN |
| | DELETE | `/api/projects/{projectId}/tasks/{id}` | PM、ADMIN |
| **工時紀錄** | GET | `/api/work-entries?startDate=&endDate=` | EXECUTOR、ADMIN |
| | POST | `/api/work-entries` | EXECUTOR、ADMIN |
| | PUT | `/api/work-entries/{id}` | EXECUTOR、ADMIN |
| **時數增補（PM）** | GET | `/api/hours-requests` | PM、ADMIN |
| | POST | `/api/hours-requests` | PM、ADMIN |
| **時數增補審核** | GET | `/api/admin/hours-requests` | ADMIN |
| | POST | `/api/admin/hours-requests/{id}/review` | ADMIN |
| **我的任務** | GET | `/api/my-tasks?status=` | EXECUTOR |
| | POST | `/api/my-tasks/{id}/complete` | EXECUTOR |
| **PM 專案** | GET | `/api/pm/projects` | PM、ADMIN |
| **部門總覽** | GET | `/api/dept/overview` | DEPT_MANAGER、ADMIN |
| | GET | `/api/dept/members` | DEPT_MANAGER、ADMIN |
| | GET | `/api/dept/members/{userId}/tasks` | DEPT_MANAGER、ADMIN |
| | GET | `/api/department/departments` | DEPT_MANAGER、ADMIN |
| **通知** | GET | `/api/notifications?unreadOnly=` | 登入使用者 |
| | PATCH | `/api/notifications/{id}/read` | 登入使用者 |
| | GET | `/api/notifications/unread-count` | 登入使用者 |

---

## 部署到遠端主機

### 一鍵部署

```bash
./deploy.sh --host <IP> --user <USER> --password '<PASSWORD>'
```

| 參數 | 說明 | 預設值 |
| --- | --- | --- |
| `--host` | 遠端主機 IP | 192.168.10.248 |
| `--user` | SSH 使用者名稱 | infoadmin |
| `--password` | SSH 密碼（需安裝 sshpass） | — |
| `--remote-path` | 遠端部署路徑 | /home/infoadmin/sdd-demo-v3 |
| `--sudo` | 以 sudo 執行 docker compose | — |
| `--sudo-password` | sudo 密碼 | — |
| `--no-build` | 跳過 `--build`（沿用現有映像） | — |

### 手動部署

```bash
# 1. 同步檔案
rsync -avz --delete \
  --exclude '.git' \
  --exclude 'backend/build' \
  --exclude 'backend/.gradle' \
  --exclude 'frontend/node_modules' \
  --exclude 'frontend/dist' \
  ./ <USER>@<IP>:/home/infoadmin/sdd-demo-v3/

# 2. 啟動容器
ssh <USER>@<IP>
cd /home/infoadmin/sdd-demo-v3
docker compose up -d --build

# 3. 確認狀態
docker compose ps
docker compose logs -f --tail=200
```

---

## Auto-Fix 工作流程

「Issue 指派給 copilot」觸發，不使用留言指令。

1. QA 或成員建立 Issue。
2. `auto-fix-on-issue.yml` 自動回覆 Issue Intake Summary。
3. 維護者將 Assignee 設為 `copilot`。
4. `auto-fix.yml` 觸發，在 Issue 留言詳細任務指示。
5. Copilot 修復程式碼、執行驗證並開立 PR。

**注意事項：**
- 只有 assignee 為 `copilot` 時才會觸發。
- 重新觸發：先移除 Assignee 再重新指派。
- 需在 Settings → Actions → General 啟用 Read/Write permissions 與允許建立 PR。

---

## CI/CD 工作流程

### CI（`.github/workflows/ci.yml`）

觸發：PR 或 push 至 `main`，兩個 Job 並行執行。

| Job | 環境 | 指令 |
| --- | --- | --- |
| Backend Test and Quality | ubuntu-latest / JDK 24 (temurin) | `./gradlew clean test jacocoTestCoverageVerification checkstyleMain checkstyleTest` |
| Frontend Test and Build | ubuntu-latest / Node 22 | `npm ci` → `npm run lint` → `npm run test:unit -- --run` → `npm run build` |

### CD（`.github/workflows/cd-deploy.yml`）

觸發：push 至 `main` 或手動（`workflow_dispatch`）。

```
Verify（前後端完整驗證，與 CI 相同）
     │ 通過
     ▼
Deploy（SSH → ./deploy.sh → docker compose up -d --build）
```

**必要 Secrets：**

| Secret | 說明 |
| --- | --- |
| `DEPLOY_SSH_PRIVATE_KEY` | 部署用 SSH 私鑰 |
| `DEPLOY_REMOTE_HOST` | 遠端主機 IP |
| `DEPLOY_REMOTE_USER` | SSH 使用者名稱 |
| `DEPLOY_REMOTE_PATH` | 遠端部署路徑 |
| `DEPLOY_USE_SUDO` | （選填）是否使用 sudo |
| `DEPLOY_SUDO_PASSWORD` | （選填）sudo 密碼 |

---

## 目錄結構

```text
sdd-demo-v3/
├── backend/
│   ├── src/main/java/com/workreport/
│   │   ├── annotation/          # @RequireRole、@DataScope
│   │   ├── aop/                 # RoleCheckAspect、DataScopeAspect
│   │   ├── config/              # SecurityConfig
│   │   ├── controller/          # REST Controllers（13 個）
│   │   ├── entity/              # User、Project、Task、WorkEntry、HoursRequest 等
│   │   ├── enums/               # Role、TaskStatus、ProjectStatus、ScopeType 等
│   │   ├── exception/           # BusinessRuleException、GlobalExceptionHandler
│   │   ├── repository/          # Spring Data JPA Repositories
│   │   ├── security/            # JwtAuthenticationFilter、JwtTokenProvider
│   │   └── service/             # 業務邏輯層
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/        # V1 schema、V2 seed data、V3 add department to project
│   └── src/test/                # 單元測試（Mockito）、整合測試（Testcontainers）
├── frontend/
│   ├── src/
│   │   ├── api/                 # Axios API 客戶端
│   │   ├── components/          # 共用元件
│   │   ├── pages/               # 各角色頁面
│   │   ├── router/              # Vue Router
│   │   └── stores/              # Pinia stores
│   └── tests/
│       ├── unit/                # Vitest
│       └── e2e/                 # Playwright
├── performance/
│   └── k6/
│       └── us1-smoke.js         # Smoke test（100 VU × 1 min，p95 < 500 ms）
├── .github/
│   ├── workflows/               # ci.yml、cd-deploy.yml、auto-fix.yml、auto-fix-on-issue.yml
│   ├── prompts/                 # Speckit slash command prompts
│   └── agents/                  # 自訂 agent 定義
├── docker-compose.yml
├── deploy.sh
└── README.md
```
