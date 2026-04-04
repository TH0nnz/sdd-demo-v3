# GitHub Repository Settings 勾選清單

本清單用於設定本專案的 GitHub Repository，讓以下流程可以正常運作：

1. QA 建立 Issue
2. GitHub Actions 自動整理 Issue
3. 維護者手動指派 `copilot`
4. Copilot 修復並建立 PR
5. PR 自動執行 CI
6. merge 到 `main` 後自動部署

---

## 一、進入設定頁

進入你的 GitHub repository 後，依序點選：

1. `Settings`
2. 依下面各章節完成設定

---

## 二、Actions 設定

路徑：`Settings` → `Actions` → `General`

請確認以下選項：

### Actions permissions

- 勾選 `Allow all actions and reusable workflows`

### Workflow permissions

- 勾選 `Read and write permissions`
- 勾選 `Allow GitHub Actions to create and approve pull requests`

### Fork pull request workflows

- 如果你的流程不依賴 fork PR，可維持預設
- 若未來 QA 會從 fork 提 PR，再另外檢查這區設定

---

## 三、Secrets 設定

路徑：`Settings` → `Secrets and variables` → `Actions`

在 `Repository secrets` 建立以下項目。

### 必要 Secrets

- `DEPLOY_SSH_PRIVATE_KEY`
- `DEPLOY_REMOTE_HOST`
- `DEPLOY_REMOTE_USER`
- `DEPLOY_REMOTE_PATH`

### 可選 Secrets

- `DEPLOY_USE_SUDO`
- `DEPLOY_SUDO_PASSWORD`

### 建議填值範例

- `DEPLOY_REMOTE_HOST`：`192.168.10.248`
- `DEPLOY_REMOTE_USER`：`infoadmin`
- `DEPLOY_REMOTE_PATH`：`/home/infoadmin/sdd-demo-v3`
- `DEPLOY_USE_SUDO`：`1`

### SSH Key 檢查

- `DEPLOY_SSH_PRIVATE_KEY` 必須是完整私鑰內容
- 對應公鑰必須已加入遠端主機的 `~/.ssh/authorized_keys`
- 建議使用部署專用 SSH key，不要共用個人主要金鑰

---

## 四、Branch Protection 設定

路徑：`Settings` → `Branches`

在 `Branch protection rules` 為 `main` 建立規則。

### Branch name pattern

- 輸入 `main`

### 建議勾選項目

- 勾選 `Require a pull request before merging`
- 勾選 `Require approvals`
- 建議至少 `1` 位 reviewer
- 勾選 `Dismiss stale pull request approvals when new commits are pushed`
- 勾選 `Require status checks to pass before merging`
- 勾選 `Require branches to be up to date before merging`

### Required status checks

請加入以下 checks：

- `Backend Test and Quality`
- `Frontend Test and Build`

### 可選但建議項目

- 勾選 `Require conversation resolution before merging`
- 勾選 `Do not allow bypassing the above settings`
- 若團隊流程嚴格，可勾選 `Restrict who can push to matching branches`

---

## 五、Issues 設定

路徑：`Settings` → `General` → `Features`

請確認：

- `Issues` 已啟用

沒有啟用 `Issues`，QA 就無法建立 Issue，也不會觸發 intake workflow。

---

## 六、Pull Requests 設定

路徑：`Settings` → `General` → `Pull Requests`

建議確認：

- `Allow merge commits`：依團隊習慣
- `Allow squash merging`：建議開啟
- `Allow rebase merging`：依團隊習慣
- `Automatically delete head branches`：建議開啟

若你希望 Copilot 開出的修復 PR 在 merge 後自動清理分支，建議開啟 `Automatically delete head branches`。

---

## 七、Copilot 與 Assignees 檢查

這是流程能否成立的關鍵檢查。

### 必須確認

- Repository 已支援 GitHub Copilot Coding Agent
- 你可以在 Issue 的 `Assignees` 中選到 `copilot`

### 驗證方式

1. 任選一張測試 Issue
2. 在 `Assignees` 嘗試指派 `copilot`
3. 指派成功後，檢查 `.github/workflows/auto-fix.yml` 是否被觸發

如果根本選不到 `copilot`，代表不是 workflow 問題，而是 GitHub/Copilot 功能或權限尚未就緒。

---

## 八、Environment 保護（可選，但建議）

如果你不希望 merge 後直接無條件部署，可加入 environment 保護。

路徑：`Settings` → `Environments`

可建立：

- `staging`
- `production`

### production 建議設定

- 勾選 `Required reviewers`
- 指定 1 位或以上 reviewer
- 視需要加入 deployment branch rule，只允許 `main`

這樣 CD workflow 可以改成部署前需要人工核准，風險更低。

---

## 九、第一次上線前驗證清單

請逐項確認：

- `Issues` 功能已開啟
- Actions 已允許執行
- Actions 具有 `Read and write permissions`
- Actions 可以建立與核准 PR
- Secrets 已全部建立
- `main` 已設定 branch protection
- required checks 已加入 CI job
- 可以在 Issue 指派 `copilot`
- 遠端主機 SSH 金鑰與 Docker Compose 正常

---

## 十、建議驗證流程

### 測試 1：Issue Intake

1. 建立一張測試 Issue
2. 確認 `Issue Intake Summary` comment 自動出現

### 測試 2：Copilot Fix

1. 將該 Issue 指派給 `copilot`
2. 確認 `Auto-Fix on Copilot Assigned` workflow 執行
3. 確認 Issue 出現修復指示留言

### 測試 3：CI

1. 等 Copilot 建立 PR
2. 確認 CI 兩個 job 都有跑：
   - `Backend Test and Quality`
   - `Frontend Test and Build`

### 測試 4：CD

1. merge 該 PR 到 `main`
2. 確認 `CD Deploy` workflow 啟動
3. 確認遠端部署成功

---

## 十一、最小必勾項目摘要

如果你只想先完成最小可用設定，至少要確認這些：

- `Issues` 已開啟
- `Allow all actions and reusable workflows`
- `Read and write permissions`
- `Allow GitHub Actions to create and approve pull requests`
- `DEPLOY_SSH_PRIVATE_KEY`
- `DEPLOY_REMOTE_HOST`
- `DEPLOY_REMOTE_USER`
- `DEPLOY_REMOTE_PATH`
- `main` branch protection 已建立
- `Backend Test and Quality` 已設為 required
- `Frontend Test and Build` 已設為 required
- 可以指派 `copilot`

完成以上項目後，這套流程就能進入可實際使用狀態。