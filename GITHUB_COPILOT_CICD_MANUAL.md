# GitHub Copilot CI/CD 操作手冊

本手冊說明本專案在 GitHub 上的標準流程：

1. QA 提出 Issue
2. GitHub Actions 自動整理 Issue 內容
3. 維護者手動指派 Issue 給 Copilot
4. Copilot 修復問題、測試並建立 PR
5. GitHub Actions 執行 CI
6. 維護者審查並合併 PR
7. GitHub Actions 執行 CD，自動部署到目標主機

---

## 一、角色分工

### QA

- 建立 Issue，描述錯誤現象與重現方式
- 補齊必要資訊，協助 Copilot 理解問題
- 驗證修復結果是否符合需求

### 維護者

- 檢查 QA Issue 是否完整
- 手動將 Issue 指派給 `copilot`
- 審查 Copilot 建立的 PR
- 決定是否合併與部署

### GitHub Copilot

- 依照 Issue 內容與 workflow 留言進行修復
- 修改程式碼與必要測試
- 建立 PR 供審查

### GitHub Actions

- 整理 Issue 內容
- 驗證 PR 品質
- 合併後自動部署

---

## 二、Workflow 對應

### 1. Issue Intake

- 檔案：`.github/workflows/auto-fix-on-issue.yml`
- 觸發時機：Issue 開立時
- 功能：自動整理 Issue 內容並留言摘要

### 2. Copilot Fix

- 檔案：`.github/workflows/auto-fix.yml`
- 觸發時機：Issue 被指派給 `copilot`
- 功能：發送修復指示給 Copilot

### 3. CI

- 檔案：`.github/workflows/ci.yml`
- 觸發時機：PR / push 到 `main`
- 功能：驗證前後端測試、lint、build、coverage、checkstyle

### 4. CD

- 檔案：`.github/workflows/cd-deploy.yml`
- 觸發時機：push 到 `main` 或手動執行
- 功能：驗證後部署到遠端主機

---

## 三、QA 操作步驟

### Step 1：建立 Issue

請 QA 在 GitHub 建立 Issue，並盡量填寫以下資訊：

- 類型：UI / Flow / Logic / Add
- 重現步驟
- 實際結果
- 預期結果
- 相關元件或檔案
- 補充資訊（截圖、錯誤訊息、Console log）

### Step 2：等待系統自動整理

Issue 建立後，GitHub Actions 會自動留言一則 `Issue Intake Summary`。

QA 需要確認這則摘要是否正確；若資訊不足，請直接補充在原始 Issue 內文或後續留言中。

---

## 四、維護者操作步驟

### Step 1：檢查 Issue 內容

維護者收到 QA Issue 後，先看兩個地方：

1. 原始 Issue 描述是否完整
2. `Issue Intake Summary` 是否已整理出足夠資訊

如果資訊不足，先請 QA 補件，不要急著指派給 Copilot。

### Step 2：指派給 Copilot

當 Issue 資訊足夠時，在 GitHub Issue 右側 `Assignees` 將 Issue 指派給 `copilot`。

指派後會觸發 `.github/workflows/auto-fix.yml`。

### Step 3：確認修復指示留言已出現

成功觸發後，Issue 會新增一則給 Copilot 的工作指示留言，內容包含：

- 修復要求
- 目標分支
- 檢查清單
- 測試與 PR 要求

如果沒有出現，請到 GitHub Actions 檢查 workflow 是否失敗。

---

## 五、Copilot 修復流程

當 Issue 已指派給 `copilot` 且工作流觸發成功後，Copilot 會開始：

1. 讀取 Issue 與相關程式碼
2. 修改程式碼
3. 視需要補上或調整測試
4. 建立修復 PR

維護者不需要手動觸發 PR；PR 由 Copilot 建立。

---

## 六、PR 與 CI 流程

當 Copilot 建立 PR 時，`.github/workflows/ci.yml` 會自動執行。

### Backend 驗證內容

- Gradle test
- JaCoCo coverage verification
- Checkstyle

### Frontend 驗證內容

- npm ci
- lint
- unit test
- build

### 維護者要做的事

1. 檢查 CI 是否全數通過
2. 審查 PR 程式碼內容
3. 視需要要求再修正
4. 確認沒問題後合併到 `main`

---

## 七、部署流程

當 PR 合併到 `main` 後，`.github/workflows/cd-deploy.yml` 會自動啟動。

部署流程如下：

1. 重新驗證前後端
2. 使用 GitHub Secrets 建立 SSH 連線
3. 執行 `deploy.sh`
4. 同步專案到遠端主機
5. 以 Docker Compose 重建並啟動服務

### 部署需要的 Secrets

必要：

- `DEPLOY_SSH_PRIVATE_KEY`
- `DEPLOY_REMOTE_HOST`
- `DEPLOY_REMOTE_USER`
- `DEPLOY_REMOTE_PATH`

可選：

- `DEPLOY_USE_SUDO`
- `DEPLOY_SUDO_PASSWORD`

---

## 八、GitHub 設定檢查清單

### Actions 權限

請在 GitHub Repository Settings 確認：

- Allow all actions and reusable workflows
- Workflow permissions: Read and write permissions
- Allow GitHub Actions to create and approve pull requests

### Branch Protection 建議

建議將以下 job 設為 required checks：

- Backend Test and Quality
- Frontend Test and Build

完整勾選項目請參考：`GITHUB_REPOSITORY_SETTINGS_CHECKLIST.md`

---

## 九、日常操作速查

### QA

1. 建立 Issue
2. 確認 Intake Summary 是否正確
3. 補充不足資訊
4. 等待維護者指派 Copilot

### 維護者

1. 檢查 Issue
2. 指派 `copilot`
3. 等待 Copilot 建立 PR
4. 檢查 CI
5. Review 並 merge
6. 確認 CD 部署完成

---

## 十、常見問題

### Q1：Issue 開了但沒有出現整理摘要

請先檢查：

1. `.github/workflows/auto-fix-on-issue.yml` 是否存在於預設分支
2. GitHub Actions 是否已啟用
3. Actions 執行紀錄是否有失敗

### Q2：指派給 Copilot 後沒有開始修復

請檢查：

1. assignee 是否真的是 `copilot`
2. `.github/workflows/auto-fix.yml` 是否執行成功
3. Repository 是否支援 GitHub Copilot Coding Agent

### Q3：PR 有開，但 CI 失敗

這代表修復尚未通過品質門檻。請依 CI 失敗項目決定：

1. 讓 Copilot 再修一次
2. 由人工補修後再推送

### Q4：CI 通過，但沒有自動部署

請檢查：

1. PR 是否已 merge 到 `main`
2. `.github/workflows/cd-deploy.yml` 是否成功執行
3. GitHub Secrets 是否完整
4. 遠端主機 SSH 與 Docker Compose 是否正常

---

## 十一、完整流程摘要

1. QA 提 Issue
2. GitHub Actions 整理 Issue
3. 維護者確認內容
4. 維護者指派 `copilot`
5. GitHub Actions 發修復指示
6. Copilot 修復並建立 PR
7. CI 自動驗證
8. 維護者合併 PR
9. CD 自動部署

這套流程即為本專案的標準操作模式。