# sdd-demo-v3 — 報工系統示範專案

> **Work-Reporting System Demo** — Vue 3 前端 + Spring Boot 後端 + PostgreSQL

---

## 目錄

- [專案說明](#專案說明)
- [快速啟動](#快速啟動)
- [Auto-Fix 工作流程（自動呼叫 Copilot）](#auto-fix-工作流程自動呼叫-copilot)
- [Speckit 斜線指令](#speckit-斜線指令)
- [目錄結構](#目錄結構)

---

## 專案說明

本專案示範一套五角色**報工系統**：

| 角色 | 主要職責 |
|------|---------|
| 管理層 | 建立／修改／關閉專案、審核時數增補申請 |
| PM | 拆分並指派 Task、監控進度、向管理層申請時數 |
| 部門主管 | 查看部門工時與 Task 狀態 |
| 執行人員 | 填報工時、管理個人 Task 狀態 |
| HR | 新增人員、指派角色 |

詳細功能規格請參閱 [`specs/002-work-reporting-system/spec.md`](specs/002-work-reporting-system/spec.md)。

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

### 本地開發模式

完整的本地開發步驟（含測試帳號、環境變數說明）請參閱：

👉 [`specs/002-work-reporting-system/quickstart.md`](specs/002-work-reporting-system/quickstart.md)

---

## Auto-Fix 工作流程（自動呼叫 Copilot）

本專案整合兩種方式自動觸發 GitHub Copilot Coding Agent 進行修復。
完整設定說明請參閱 [`.github/AUTOFIX_SETUP.md`](.github/AUTOFIX_SETUP.md)。

### 方式一：開啟 Issue（自動觸發）

使用 **`fix.yml`** Issue 範本提交 Issue 後，GitHub Actions 會自動：

1. 將 Issue 指派給 GitHub Copilot（`@copilot`）
2. 發佈詳細的任務指示留言，告知 Copilot 需修復的內容
3. Copilot 自動分析、修復程式碼，並建立 `.autofix/issue-<N>.md` 報告及 PR

觸發工作流程：[`.github/workflows/auto-fix-on-issue.yml`](.github/workflows/auto-fix-on-issue.yml)

### 方式二：在 Issue 留言 `/fix`（手動重觸發）

若需重新觸發，在任意 Issue 的留言欄輸入：

```
/fix
```

附帶分類標籤（可選）：

```
/fix ui
/fix flow
/fix logic
/fix add
```

觸發工作流程：[`.github/workflows/auto-fix.yml`](.github/workflows/auto-fix.yml)

> **注意**：只有帳號 **TH0nnz** 的 Issue / 留言才會觸發以上工作流程；留言必須在 Issue 上（PR 留言不會觸發）。

### 流程說明

```
Issue 開啟（使用 fix.yml 範本）
    ↓
auto-fix-on-issue.yml 觸發
    ↓
將 Issue 指派給 Copilot + 發佈任務指示留言
    ↓
Copilot Coding Agent 自動執行
    ├─ 分析 Issue 描述
    ├─ 修改相關程式碼
    ├─ 建立 .autofix/issue-<N>.md（修復報告）
    └─ 開立 PR 供審核
```

---

## Speckit 斜線指令

本專案整合了 **speckit** 設計工件工作流程。在支援 GitHub Copilot Chat 的編輯器中，可使用下列指令：

| 指令 | 說明 |
|------|------|
| `/speckit.specify` | 根據需求描述產生 `spec.md` |
| `/speckit.plan` | 根據 spec.md 產生實作計劃 `plan.md` |
| `/speckit.tasks` | 根據 plan.md 產生可執行任務清單 `tasks.md` |
| `/speckit.implement` | 依序執行 tasks.md 中的任務 |
| `/speckit.analyze` | 跨工件一致性分析（需 spec/plan/tasks 都存在） |
| `/speckit.clarify` | 對規格中模糊之處提出澄清問題 |
| `/speckit.checklist` | 產生功能驗收清單 |
| `/speckit.taskstoissues` | 將 tasks.md 轉換為 GitHub Issues |
| `/speckit.constitution` | 建立或更新專案治理憲章 |

Prompt 定義位於 [`.github/prompts/`](.github/prompts/)。

---

## 目錄結構

```
sdd-demo-v3/
├── backend/                        # Spring Boot 後端（Java / Gradle）
│   ├── src/main/java/              # 業務邏輯、API、安全性
│   ├── src/main/resources/         # application.yml、Flyway 遷移腳本
│   └── src/test/                   # 單元測試 + 整合測試
├── frontend/                       # Vue 3 前端（TypeScript / Vite）
│   ├── src/                        # 元件、頁面、Pinia Store、Router
│   └── tests/                      # Vitest 單元測試 + Playwright E2E
├── specs/
│   └── 002-work-reporting-system/  # 規格、計劃、任務、合約文件
├── .github/
│   ├── workflows/
│   │   ├── auto-fix-on-issue.yml   # Issue 開啟時自動呼叫 Copilot
│   │   └── auto-fix.yml            # /fix 留言指令重新觸發 Copilot
│   ├── ISSUE_TEMPLATE/
│   │   └── fix.yml                 # Bug 回報範本（含類型選項）
│   ├── AUTOFIX_SETUP.md            # Auto-Fix 完整設定指南
│   ├── prompts/                    # Speckit prompt 定義
│   └── agents/                     # Speckit agent 定義
├── docker-compose.yml              # 一鍵啟動所有服務
└── README.md                       # 本文件
```
# 報工系統 (Work Report System)

報工時間記錄與管理系統，用於工作項目的時間追蹤與報表統計。

**Feature**: 002-work-reporting-system  
**Date**: 2026-02-23

---

## 前置需求

| 工具 | 版本 | 說明 |
|------|------|------|
| JDK | 24 | 後端執行環境 |
| Gradle | 8.x | 後端建構工具（使用 Wrapper） |
| Node.js | 20 LTS+ | 前端建構環境 |
| pnpm | 9.x | 前端套件管理 |
| Docker & Docker Compose | 最新 | 本地開發資料庫 + 部署 |
| PostgreSQL | 18.1 | 透過 Docker 執行即可 |

---

## 快速啟動（本地開發）

### 1. 複製並切換分支

```bash
git checkout 002-work-reporting-system
```

### 2. 啟動 PostgreSQL（透過 Docker）

```bash
docker compose up -d db
```

預設連線資訊（定義於 `docker-compose.yml`）：
- Host: `localhost:5432`
- Database: `workreport`
- User: `workreport`
- Password: `workreport`

### 3. 啟動後端

```bash
cd backend
./gradlew bootRun
```

後端啟動於 `http://localhost:8080`。
Flyway 將自動執行資料庫遷移。

### 4. 啟動前端

```bash
cd frontend
pnpm install
pnpm dev
```

前端啟動於 `http://localhost:5173`。
Vite dev server 自動代理 `/api` 請求至後端。

### 5. 初始測試帳號

系統啟動後，透過 Flyway seed 資料自動建立以下測試帳號：

| 角色 | Email | 初始密碼 | 備註 |
|------|-------|---------|------|
| 管理層 | admin@company.com | Welcome123 | 首次登入須改密碼 |
| PM | pm@company.com | Welcome123 | 首次登入須改密碼 |
| 部門主管 | manager@company.com | Welcome123 | 首次登入須改密碼 |
| 執行人員 | executor@company.com | Welcome123 | 首次登入須改密碼 |
| HR | hr@company.com | Welcome123 | 首次登入須改密碼 |

---

## Docker Compose 部署（Linux 伺服器）

### 1. 準備環境

```bash
# 安裝 Docker
curl -fsSL https://get.docker.com | sh

# 安裝 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. 部署應用

```bash
# 複製專案
git clone <repo-url> sdd-demo-v3
cd sdd-demo-v3

# 切換分支
git checkout 002-work-reporting-system

# 啟動所有服務
sudo docker-compose up -d

# 查看狀態
sudo docker-compose ps
```

### 3. 服務訪問

- 前端（Nginx）：`http://<伺服器IP>`
- 後端 API：`http://<伺服器IP>:8089`
- 資料庫：`<伺服器IP>:5454`

---

## 建構指令

### 後端

```bash
cd backend

# 執行所有測試
./gradlew test

# 測試覆蓋率報告（JaCoCo）
./gradlew jacocoTestReport
# 報告位於 build/reports/jacoco/test/html/index.html

# 覆蓋率門檻檢查（≥ 80%）
./gradlew jacocoTestCoverageVerification

# 建構 JAR
./gradlew bootJar

# Lint / 靜態分析
./gradlew checkstyleMain spotbugsMain
```

### 前端

```bash
cd frontend

# 安裝相依套件
pnpm install

# 開發模式
pnpm dev

# 執行單元測試
pnpm test

# 測試覆蓋率
pnpm test:coverage

# Lint
pnpm lint

# 建構生產版本
pnpm build
```

### E2E 測試

```bash
cd frontend

# 安裝 Playwright 瀏覽器
pnpm exec playwright install

# 執行 E2E 測試（需先啟動前後端）
pnpm test:e2e
```

---

## 環境變數

### 後端（application.yml / 環境變數）

| 變數 | 預設值 | 說明 |
|------|--------|------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/workreport` | 資料庫連線 URL |
| `DB_USERNAME` | `workreport` | 資料庫使用者 |
| `DB_PASSWORD` | `workreport` | 資料庫密碼 |
| `JWT_SECRET` | （開發環境預設值） | JWT 簽章密鑰 |
| `JWT_EXPIRATION_MS` | `1800000` | JWT 有效期（毫秒，預設 30 分鐘） |

### 前端（.env）

| 變數 | 預設值 | 說明 |
|------|--------|------|
| `VITE_API_BASE_URL` | `/api` | API 基礎路徑 |

---

## 目錄結構

```
project-root/
├── backend/                 # Spring Boot 後端
│   ├── src/main/java/       # Java 原始碼
│   ├── src/main/resources/  # 設定檔 + Flyway 遷移
│   ├── src/test/java/       # 測試
│   ├── build.gradle         # Gradle 建構檔
│   └── Dockerfile
├── frontend/                # Vue 3 前端
│   ├── src/                 # TypeScript/Vue 原始碼
│   ├── tests/               # 測試
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile
├── docker-compose.yml       # 本地開發 + 部署設定
└── specs/                   # 規格文件
    └── 002-work-reporting-system/
        ├── spec.md
        ├── plan.md
        ├── research.md
        ├── data-model.md
        ├── quickstart.md
        └── contracts/
```

---

## 常用 Docker 命令

```bash
# 查看執行中的服務
docker-compose ps

# 檢視日誌
docker-compose logs -f

# 停止所有服務
docker-compose down

# 完全清除（包括 volume）
docker-compose down -v

# 重啟服務
docker-compose restart

# 進入資料庫
docker-compose exec db psql -U workreport -d workreport
```

---

## 數據庫遷移說明

使用 Flyway 進行資料庫版本管理。遷移文件位於 `backend/src/main/resources/db/migration/`。

首次啟動時，系統會自動執行所有遷移文件：
- `V1__...` - 建立資料庫結構
- `V2__...` - 插入測試資料

**重要**: 如需重置資料庫，請：
1. 停止容器：`docker-compose down -v`
2. 刪除 volume：`docker volume prune`
3. 重新啟動：`docker-compose up -d`

---

## 詳細文件

更多詳細說明請參考 [specs/002-work-reporting-system/quickstart.md](specs/002-work-reporting-system/quickstart.md)

---

### 結語：
切記如果用docker compose在linux上啟動，會自動幫你init db，要砍掉所有table 在使用/Volumes/DOCKER_SSD/workspace/sdd-demo-v3/backend/src/main/resources/db/migration 中的v1 與 v2 重建db


**最後更新**: 2026-02-26
