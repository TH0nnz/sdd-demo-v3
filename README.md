# sdd-demo-v3 - 報工系統示範專案

> Work-Reporting System Demo: Vue 3 + TypeScript 前端 | Spring Boot 4 後端 | PostgreSQL 18

本專案示範一套五角色報工系統，涵蓋專案建立、任務拆分、工時填報到進度監控。

## 目錄

- [專案說明](#專案說明)
- [技術堆疊](#技術堆疊)
- [快速啟動](#快速啟動)
- [本地開發](#本地開發)
- [測試帳號](#測試帳號)
- [建構與測試](#建構與測試)
- [部署到遠端主機](#部署到遠端主機)
- [Auto-Fix 工作流程（Assign Copilot）](#auto-fix-工作流程assign-copilot)
- [CI/CD 工作流程](#cicd-工作流程)
- [Speckit 斜線指令](#speckit-斜線指令)
- [目錄結構](#目錄結構)

## 專案說明

系統角色與職責如下：

| 角色 | 主要職責 |
| --- | --- |
| 管理層 (Admin) | 建立、修改、關閉專案；設定時數預算；審核時數增補申請 |
| PM | 拆分並指派任務；監控進度；向管理層申請時數 |
| 部門主管 (Dept. Manager) | 唯讀查看部門成員工時與任務狀態 |
| 執行人員 (Executor) | 填報工時（最小單位 0.5h）；管理個人任務狀態 |
| HR | 新增使用者、指派角色、停用帳號 |

詳細功能規格請參閱 [specs/002-work-reporting-system/spec.md](specs/002-work-reporting-system/spec.md)。

## 技術堆疊

### 後端

| 技術 | 版本 | 說明 |
| --- | --- | --- |
| Java | 24 | 執行環境（Gradle Toolchain） |
| Spring Boot | 4.0.2 | 應用程式框架 |
| Spring Security + JWT | jjwt 0.12.6 | 身分驗證與授權 |
| Spring Data JPA + Hibernate | - | ORM 資料存取層 |
| Flyway | - | 資料庫遷移與版本管理 |
| PostgreSQL | 18 | 關聯式資料庫 |
| Gradle | Wrapper（8.x） | 建構工具 |
| JaCoCo | 0.8.13 | 測試覆蓋率報告與門檻驗證（最低 90%） |
| Checkstyle | 10.21.4 | 靜態程式碼分析 |
| Testcontainers | 1.21.1 | 整合測試容器 |

### 前端

| 技術 | 版本 | 說明 |
| --- | --- | --- |
| Vue | 3.5.x | UI 框架 |
| TypeScript | 5.7.x | 型別安全 |
| Vite | 6.1.x | 前端建構工具 |
| Pinia | 3.0.x | 狀態管理 |
| Vue Router | 4.5.x | 路由管理 |
| Element Plus | 2.9.x | UI 元件函式庫 |
| Axios | 1.7.x | HTTP 客戶端 |
| Vitest | 3.0.x | 單元測試 |
| Playwright | 1.50.x | E2E 測試 |

## 快速啟動

一行啟動（Docker Compose）：

```bash
docker compose up -d --build
```

服務對應：

| 服務 | URL / Port |
| --- | --- |
| 前端（Nginx） | http://localhost |
| 後端 API | http://localhost:8089 |
| PostgreSQL | localhost:5454 |

## 本地開發

### 前置需求

| 工具 | 建議版本 |
| --- | --- |
| JDK | 24（或 21 LTS） |
| Node.js | 20 LTS 以上 |
| pnpm | 9.x |
| Docker / Docker Compose | 最新穩定版 |

注意：若出現 `Unsupported class file major version 69`，代表正在使用 Java 25。請切換至 Java 24 或 Java 21。

```bash
export JAVA_HOME=/path/to/jdk24
```

### 啟動步驟

1. 啟動資料庫

```bash
docker compose up -d db
```

2. 啟動後端

```bash
cd backend
./gradlew bootRun
```

後端本地預設為 `http://localhost:8080`。

3. 啟動前端

```bash
cd frontend
pnpm install
pnpm dev
```

前端本地預設為 `http://localhost:5173`。

## 測試帳號

系統啟動後會由資料初始化腳本建立以下帳號（首次登入需改密碼）：

| 角色 | Email | 初始密碼 |
| --- | --- | --- |
| 管理層 | admin@company.com | Welcome123 |
| PM | pm@company.com | Welcome123 |
| 部門主管 | manager@company.com | Welcome123 |
| 執行人員 | executor@company.com | Welcome123 |
| HR | hr@company.com | Welcome123 |

## 建構與測試

### 後端（Gradle）

```bash
cd backend

# 執行測試
./gradlew test

# 產生覆蓋率報告
./gradlew jacocoTestReport

# 覆蓋率門檻驗證（最低 90%）
./gradlew jacocoTestCoverageVerification

# 靜態分析
./gradlew checkstyleMain

# 建構 JAR
./gradlew bootJar
```

JaCoCo HTML 報告位置：`backend/build/reports/jacoco/test/html/index.html`

### 前端（Vite）

```bash
cd frontend

# 安裝相依套件
pnpm install

# 單元測試
pnpm test:unit

# Lint（含自動修正）
pnpm lint

# 建構正式版
pnpm build
```

### E2E 測試（Playwright）

```bash
cd frontend

# 首次執行需安裝瀏覽器
pnpm exec playwright install

# 執行 E2E
pnpm test:e2e
```

## 部署到遠端主機

### 一鍵部署

```bash
./deploy.sh --host <IP> --user <USER> --password '<PASSWORD>'
```

常用參數：

```bash
# 使用 sudo 執行 docker compose
./deploy.sh --host 192.168.10.248 --user infoadmin --password 'your_password' --sudo

# 指定遠端路徑
./deploy.sh --host 192.168.10.248 --user infoadmin --password 'your_password' \
  --remote-path /home/infoadmin/sdd-demo-v3

# 跳過重建映像
./deploy.sh --host 192.168.10.248 --user infoadmin --password 'your_password' --no-build
```

### 手動部署

1. 同步檔案

```bash
rsync -avz --delete \
  --exclude '.git' \
  --exclude 'backend/build' \
  --exclude 'backend/.gradle' \
  --exclude 'frontend/node_modules' \
  --exclude 'frontend/dist' \
  ./ user@<IP>:/home/infoadmin/sdd-demo-v3/
```

2. 啟動容器

```bash
ssh user@<IP>
cd /home/infoadmin/sdd-demo-v3
docker compose up -d --build
```

3. 檢查狀態

```bash
docker compose ps
docker compose logs -f --tail=200
```

## Auto-Fix 工作流程（Assign Copilot）

目前 Auto-Fix 採用「Issue 指派給 copilot」觸發，不使用 `/fix` 留言指令。

標準操作流程：

1. QA 或任一成員建立 Issue。
2. `auto-fix-on-issue.yml` 會自動回覆 Issue Intake Summary，整理欄位內容。
3. 維護者將該 Issue 的 Assignee 指派為 `copilot`。
4. `auto-fix.yml` 觸發，並在 Issue 留言 `@copilot` 任務指示。
5. Copilot 依指示修復問題、執行驗證並開立 PR。

流程重點：

1. 開 Issue 後會先執行 intake 流程，協助整理需求品質。
2. 只有當 assignee 為 `copilot` 時才會啟動修復流程。
3. 修復流程會由 workflow 發佈 `@copilot` 指示留言。
4. 建議修復內容與 `.autofix/issue-<編號>.md` 報告同一個 commit 提交。

限制條件：

- 僅支援 GitHub Issue（PR 不在此自動流程內）。
- 若要重新觸發，建議先移除 Assignee 再重新指派 `copilot`。
- 設定檔位置：`.github/workflows/auto-fix.yml`、`.github/workflows/auto-fix-on-issue.yml`。

完整操作方式請參考：`GITHUB_COPILOT_CICD_MANUAL.md`

GitHub Repository 設定勾選清單請參考：`GITHUB_REPOSITORY_SETTINGS_CHECKLIST.md`

## CI/CD 工作流程

本專案包含兩個主要 GitHub Actions workflow：

1. CI：PR / push 至 `main` 時驗證前後端品質。
2. CD：`main` push（或手動觸發）時先驗證再部署。

主要設定檔：

- `.github/workflows/ci.yml`
- `.github/workflows/cd-deploy.yml`

建議先設定下列 Secrets：

- `DEPLOY_SSH_PRIVATE_KEY`
- `DEPLOY_REMOTE_HOST`
- `DEPLOY_REMOTE_USER`
- `DEPLOY_REMOTE_PATH`
- `DEPLOY_USE_SUDO`（可選）
- `DEPLOY_SUDO_PASSWORD`（可選）

## Speckit 斜線指令

本專案採用 Speckit 流程，可在支援 Copilot Chat 的環境使用：

| 指令 | 說明 |
| --- | --- |
| `/speckit.specify` | 依需求產生 `spec.md` |
| `/speckit.plan` | 依 `spec.md` 產生 `plan.md` |
| `/speckit.tasks` | 依 `plan.md` 產生 `tasks.md` |
| `/speckit.implement` | 依序執行任務 |
| `/speckit.analyze` | 檢查 spec / plan / tasks 一致性 |
| `/speckit.clarify` | 針對規格模糊處提出澄清問題 |
| `/speckit.checklist` | 產生驗收清單 |
| `/speckit.taskstoissues` | 將任務轉為 GitHub Issues |
| `/speckit.constitution` | 建立或更新專案憲章 |

斜線指令來源與 Prompt 定義位於：`.github/prompts/`

## 目錄結構

```text
sdd-demo-v3/
├── backend/                        # Spring Boot 後端（Java 24 / Gradle）
│   ├── src/main/java/              # 業務邏輯、REST API、安全設定
│   ├── src/main/resources/         # application.yml、Flyway 腳本
│   └── src/test/                   # 單元測試 / 整合測試
├── frontend/                       # Vue 3 前端（TypeScript / Vite）
│   ├── src/
│   │   ├── api/                    # Axios API 客戶端
│   │   ├── components/             # 共用元件
│   │   ├── pages/                  # 各角色頁面
│   │   ├── stores/                 # Pinia 狀態管理
│   │   └── router/                 # 路由設定
│   └── tests/
│       ├── unit/                   # Vitest
│       └── e2e/                    # Playwright
├── specs/
│   └── 002-work-reporting-system/  # 規格、計劃、任務、研究文件
├── performance/
│   └── k6/                         # 效能測試腳本
├── .github/
│   ├── workflows/                  # CI / CD / Auto-Fix
│   ├── prompts/                    # Speckit slash commands 提示定義
│   └── agents/                     # 自訂 agent 定義
├── docker-compose.yml              # 一鍵啟動所有服務
├── deploy.sh                       # 遠端部署腳本
└── README.md
```
