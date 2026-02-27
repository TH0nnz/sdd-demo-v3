# sdd-demo-v3 — 報工系統示範專案

> **Work-Reporting System Demo** — Vue 3 前端 + Spring Boot 後端 + PostgreSQL

---

## 目錄

- [專案說明](#專案說明)
- [快速啟動](#快速啟動)
- [Auto-Fix 工作流程（`/fix` 指令）](#auto-fix-工作流程fix-指令)
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

## Auto-Fix 工作流程（`/fix` 指令）

當你在任意 **Issue** 留言 `/fix`，GitHub Actions 會自動：

1. 建立分支 `autofix/issue-<編號>`
2. 在 `.autofix/` 目錄產生包含 Issue 完整上下文的 Markdown 草稿
3. 開啟一個 Pull Request 供你審查並補充實際修復內容

### 使用方式

在 Issue 的留言欄輸入下列任一指令，然後送出：

```
/fix
```

附帶分類標籤（可選）：

```
/fix ui
/fix flow
/fix logic
```

> **注意**：只有帳號 **TH0nnz** 的留言才會觸發此工作流程；留言必須在 Issue 上（PR 留言不會觸發）。
> 允許的帳號設定於 [`.github/workflows/auto-fix.yml`](.github/workflows/auto-fix.yml)（`github.event.comment.user.login == 'TH0nnz'`）。

### 流程範例

```
Issue #42: 按鈕樣式跑版

留言：/fix ui
  └─▶ Actions 執行
       ├─ 建立分支 autofix/issue-42
       ├─ 寫入 .autofix/issue-42.md（含 Issue 標題、內文、留言）
       └─ 開啟 PR: "autofix [ui]: issue #42 – 按鈕樣式跑版"
```

產生的 PR 會列出 Issue 的完整資訊，方便你（或 Copilot）直接在分支上進行修復。

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
│   │   └── auto-fix.yml            # /fix 斜線指令自動開 PR
│   ├── prompts/                    # Speckit prompt 定義
│   └── agents/                     # Speckit agent 定義
├── docker-compose.yml              # 一鍵啟動所有服務
└── README.md                       # 本文件
```
