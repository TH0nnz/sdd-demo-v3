# sdd-demo-v3 — 報工系統示範專案

> **Work-Reporting System Demo** — Vue 3 + TypeScript 前端 ｜ Spring Boot 4 後端 ｜ PostgreSQL 18

---

## 目錄

- [專案說明](#專案說明)
- [技術堆疊](#技術堆疊)
- [快速啟動](#快速啟動)
- [本地開發](#本地開發)
- [測試帳號](#測試帳號)
- [建構與測試指令](#建構與測試指令)
- [部署到遠端主機](#部署到遠端主機)
- [Auto-Fix 工作流程](#auto-fix-工作流程fix-指令)
- [Speckit 斜線指令](#speckit-斜線指令)
- [目錄結構](#目錄結構)

---

## 專案說明

本專案示範一套五角色**報工系統**，涵蓋從專案建立、任務拆分、工時填報到進度監控的完整生命週期：

| 角色 | 主要職責 |
|------|-------|
| 管理層 (Admin) | 建立／修改／關閉專案、設定時數預算、審核時數增補申請 |
| PM | 拆分並指派 Task、監控進度、向管理層申請時數 |
| 部門主管 (Dept. Manager) | 唯讀查看部門成員工時與 Task 狀態 |
| 執行人員 (Executor) | 填報工時（最小單位 0.5h）、管理個人 Task 狀態 |
| HR | 新增使用者、指派角色、停用帳號 |

詳細功能規格請參閱 [specs/002-work-reporting-system/spec.md](specs/002-work-reporting-system/spec.md)。

---

## 技術堆疊

### 後端

| 技術 | 版本 | 說明 |
|------|------|------|
| Java | 24 | 執行環境 |
| Spring Boot | 4.0.2 | 應用程式框架 |
| Spring Security + JWT | — | 身份驗證與授權（jjwt 0.12.6） |
| Spring Data JPA + Hibernate | — | ORM 資料存取層 |
| Flyway | — | 資料庫版本控制與遷移 |
| PostgreSQL | 18 | 關聯式資料庫 |
| Gradle | 8.x | 建構工具（含 Wrapper） |
| JaCoCo | — | 測試覆蓋率（門櫛 ≥ 80%） |
| Checkstyle | — | 靜態程式碼分析 |
| Testcontainers | 1.21.1 | 整合測試用容器 |

### 前端

| 技術 | 版本 | 說明 |
|------|------|------|
| Vue | 3.5 | UI 框架 |
| TypeScript | 5.7 | 型別安全 |
| Vite | 6.1 | 建構工具 |
| Pinia | 3.0 | 狀態管理 |
| Vue Router | 4.5 | 路由管理 |
| Element Plus | 2.9 | UI 元件函式庫 |
| Axios | 1.7 | HTTP 客戶端 |
| Vitest | 3.0 | 單元測試 |
| Playwright | 1.50 | E2E 測試 |

---

## 快速啟動

### 一行啟動（Docker Compose）

```bash
docker compose up -d --build
```

| 服務 | URL |
|------|-----|
| 前端（Nginx） | <http://localhost> |
| 後端 API | <http://localhost:8089> |
| PostgreSQL | `localhost:5454` |

Flyway 會自動建立資料庫結構並植入測試帳號。

---

## 本地開發

### 前置需求

| 工具 | 版本 |
|------|------|
| JDK | 24（成24 LTS） |
| Node.js | 20 LTS+ |
| pnpm | 9.x |
| Docker & Docker Compose | 最新 |

> **注意**：若出現 `Unsupported class file major version 69`，表示使用了 Java 25。
> Gradle 8.x 尚不支援 Java 25，請切換至 Java 24 或 21 LTS：
> ```bash
> export JAVA_HOME=/path/to/jdk24
> ```

### 步驟

**1. 僅啟動資料庫**

```bash
docker compose up -d db
```

**2. 啟動後端**

```bash
cd backend
./gradlew bootRun
```

後端啟動於 `http://localhost:8080`，Flyway 自動執行資料庫遷移。

**3. 啟動前端**

```bash
cd frontend
pnpm install
pnpm dev
```

前端啟動於 `http://localhost:5173`，Vite dev server 自動代理 `/api` 請求至後端。

---

## 測試帳號

系統啟動後，Flyway seed 資料自動建立以下帳號（首次登入須強制改密碼）：

| 角色 | Email | 初始密碼 |
|------|-------|-------|
| 管理層 | admin@company.com | Welcome123 |
| PM | pm@company.com | Welcome123 |
| 部門主管 | manager@company.com | Welcome123 |
| 執行人員 | executor@company.com | Welcome123 |
| HR | hr@company.com | Welcome123 |

---

## 建構與測試指令

### 後端

```bash
cd backend

# 執行所有測試
./gradlew test

# 測試覆蓋率報告（JaCoCo）
./gradlew jacocoTestReport
# 報告位於 build/reports/jacoco/test/html/index.html

# 覆蓋率門櫛檢查（≥ 80%）
./gradlew jacocoTestCoverageVerification

# Lint / 靜態分析
./gradlew checkstyleMain

# 建構 JAR
./gradlew bootJar
```

### 前端

```bash
cd frontend

# 安裝相依套件
pnpm install

# 執行單元測試（Vitest）
pnpm test:unit

# 測試覆蓋率
pnpm test:coverage

# Lint
pnpm lint

# 建構生產版本
pnpm build
```

### E2E 測試（Playwright）

```bash
cd frontend

# 安裝 Playwright 瀏覽器（首次執行）
pnpm exec playwright install

# 執行 E2E 測試（需先啟動前後端）
pnpm test:e2e
```

---

## 部署到遠端主機

### 一鍵部署腳本

```bash
./deploy.sh --host <IP> --user <USER> --password '<PASSWORD>'
```

常用參數：

```bash
# 需要 sudo
./deploy.sh --host 192.168.10.248 --user infoadmin --password 'your_password' --sudo

# 指定遠端路徑
./deploy.sh --host 192.168.10.248 --user infoadmin --password 'your_password' \
  --remote-path /home/infoadmin/sdd-demo-v3

# 跳過重新建構映像
./deploy.sh --host 192.168.10.248 --user infoadmin --password 'your_password' --no-build
```

### 手動部署步驟

**1. 同步專案到遠端**

```bash
rsync -avz --delete \
  --exclude '.git' \
  --exclude 'backend/build' \
  --exclude 'backend/.gradle' \
  --exclude 'frontend/node_modules' \
  --exclude 'frontend/dist' \
  ./ user@<IP>:/home/infoadmin/sdd-demo-v3/
```

**2. 連線並啟動容器**

```bash
ssh user@<IP>
cd /home/infoadmin/sdd-demo-v3
docker compose up -d --build
```

**3. 驗證服務狀態**

```bash
docker compose ps
docker compose logs -f --tail=200
```

### 常用維運指令

```bash
# 停止所有服務
docker compose down

# 重啟單一服務（範例：backend）
docker compose up -d --build backend

# 清理未使用映像
docker image prune -f
```

### 連接埠對應

| 服務 | 主機連接埠 |
|------|----------|
| 前端（Nginx） | 80 |
| 後端 API | 8089 |
| PostgreSQL | 5454 |

---

## Auto-Fix 工作流程（`/fix` 指令）

當你在任意 **Issue** 留言 `/fix`，GitHub Actions 會自動：

1. 建立分支 `autofix/issue-<編號>`
2. 在 `.autofix/` 目錄產生包含 Issue 完整上下文的 Markdown 草稿
3. 開啟一個 Pull Request 供你審查並補充實際修復內容

### 使用方式

在 Issue 的留言欄輸入指令後送出：

```
/fix
/fix ui
/fix flow
/fix logic
```

> **注意**：只有帳號 **TH0nnz** 的留言才會觸發此工作流程，且必須是 Issue 留言（PR 留言不觸發）。
> 允許帳號設定於 [`.github/workflows/auto-fix.yml`](.github/workflows/auto-fix.yml)。

### 流程範例

```
Issue #42: 按鈕樣式跑版

留言：/fix ui
  └─▶ Actions 執行
       ├─ 建立分支 autofix/issue-42
       ├─ 寫入 .autofix/issue-42.md（含 Issue 標題、內文、留言）
       └─ 開啟 PR: "autofix [ui]: issue #42 – 按鈕樣式跑版"
```

---

## Speckit 斜線指令

本專案整合了 **speckit** 設計工件工作流程。在支援 GitHub Copilot Chat 的編輯器中，可使用下列指令：

| 指令 | 說明 |
|------|------|
| `/speckit.specify` | 根據需求描述產生 `spec.md` |
| `/speckit.plan` | 根據 `spec.md` 產生實作計劃 `plan.md` |
| `/speckit.tasks` | 根據 `plan.md` 產生可執行任務清單 `tasks.md` |
| `/speckit.implement` | 依序執行 `tasks.md` 中的任務 |
| `/speckit.analyze` | 跨工件一致性分析（需 spec / plan / tasks 都存在） |
| `/speckit.clarify` | 對規格中模糊之處提出澄清問題 |
| `/speckit.checklist` | 產生功能驗收清單 |
| `/speckit.taskstoissues` | 將 `tasks.md` 轉換為 GitHub Issues |
| `/speckit.constitution` | 建立或更新專案治理憲章 |

Prompt 定義位於 [`.github/prompts/`](.github/prompts/)。

---

## 目錄結構

```
sdd-demo-v3/
├── backend/                        # Spring Boot 後端（Java 24 / Gradle）
│   ├── src/main/java/              # 業務邏輯、REST API、Spring Security
│   ├── src/main/resources/         # application.yml、Flyway 遷移腳本
│   └── src/test/                   # 單元測試 + Testcontainers 整合測試
├── frontend/                       # Vue 3 前端（TypeScript / Vite）
│   ├── src/
│   │   ├── api/                    # Axios API 客戶端
│   │   ├── components/             # 可重用 UI 元件
│   │   ├── pages/                  # 各角色頁面（admin/pm/dept/executor/hr）
│   │   ├── stores/                 # Pinia 狀態管理
│   │   └── router/                 # Vue Router 路由設定
│   └── tests/
│       ├── unit/                   # Vitest 單元測試
│       └── e2e/                    # Playwright E2E 測試
├── specs/
│   └── 002-work-reporting-system/  # 規格、計劃、任務、API 合約文件
├── performance/
│   └── k6/                         # k6 效能測試腳本
├── .github/
│   ├── workflows/
│   │   └── auto-fix.yml            # /fix 斜線指令自動開 PR
│   ├── prompts/                    # Speckit prompt 定義
│   └── agents/                     # Speckit agent 定義
├── docker-compose.yml              # 一鍵啟動所有服務
├── deploy.sh                       # 一鍵部署腳本
└── README.md                       # 本文件
```
