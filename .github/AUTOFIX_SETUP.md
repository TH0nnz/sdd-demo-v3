# Auto-Fix Workflow 設定指南

本文件說明 Auto-Fix 工作流的運作方式與所需的設定。

## 工作流程說明

```
Issue 開啟（使用 fix.yml 範本）
    ↓
auto-fix-on-issue.yml 觸發
    ↓
將 Issue 指派給 GitHub Copilot
+ 發佈任務指示留言（@copilot ...）
    ↓
Copilot Coding Agent 自動執行
    ↓
• 分析 Issue 描述
• 修改相關程式碼
• 建立 .autofix/issue-<N>.md（修復報告）
• 建立 PR 供審核
    ↓
由你（TH0nnz）審核並合併 PR
```

## ⚠️ 前置需求（必須先完成）

### 🔧 必要的 GitHub Actions 權限設定

1. 前往儲存庫設定頁面：
   - **https://github.com/TH0nnz/sdd-demo-v3/settings/actions**

2. 找到 **Workflow permissions** 區段

3. **必須**選擇：
   - ✅ **Read and write permissions**

4. **必須**勾選：
   - ✅ **Allow GitHub Actions to create and approve pull requests**

5. 點擊 **Save** 儲存設定

### 🤖 Copilot 訂閱需求

- 需要有效的 **GitHub Copilot** 訂閱（個人或組織）
- 確認 Copilot 在此儲存庫已啟用

**驗證 Copilot 是否已啟用：**
1. 前往 https://github.com/settings/copilot
2. 確認 **Copilot** 訂閱狀態為 Active
3. 前往 https://github.com/TH0nnz/sdd-demo-v3/settings/copilot（若為組織）或確認個人帳號已啟用
4. 測試：在任何 Issue 的 Assignees 欄位中手動輸入 `copilot`，若可被選取表示已正確啟用

---

## 觸發方式

### 方式一：開啟 Issue（自動觸發）

使用 `fix.yml` Issue 範本提交 Issue，系統會自動：
1. 觸發 `auto-fix-on-issue.yml`
2. 將 Issue 指派給 Copilot
3. Copilot 開始分析並修復

### 方式二：在 Issue 留言 `/fix`（手動重觸發）

在任何 Issue 的留言區輸入：

```
/fix
```

或加上類別標籤：

```
/fix ui
/fix flow
/fix logic
/fix add
```

此方式觸發 `auto-fix.yml`，會重新指派給 Copilot 並發佈新的指示。

---

## Copilot 產出的 `.autofix/issue-<N>.md` 格式

Copilot 會在每次修復後建立以下格式的報告：

```markdown
# Auto-Fix: Issue #N

## 問題描述
...

## 修改檔案清單
- `path/to/file.ts`：說明更動內容
...

## 修復說明
說明修復邏輯與思路...

## 測試步驟
1. ...
```

---

## 驗證設定

1. 使用 fix.yml 範本開啟一個測試 Issue
2. 查看 **Actions** 頁籤，應看到 `Auto-Fix on Issue Opened` workflow 執行
3. 確認 Issue 已被指派給 Copilot
4. 確認 Issue 下出現 Copilot 指示留言
5. 等待 Copilot 建立 PR（通常數分鐘內完成）

---

## 進階設定

### 允許其他使用者觸發

編輯 `.github/workflows/auto-fix-on-issue.yml` 和 `.github/workflows/auto-fix.yml`，將：

```yaml
github.event.issue.user.login == 'TH0nnz'
```

改為：

```yaml
contains(fromJSON('["TH0nnz", "other-user"]'), github.event.issue.user.login)
```

### 自訂修復類別

Issue 範本目前支援以下類別：`ui`、`flow`、`logic`、`add`

如需新增類別，修改 `.github/ISSUE_TEMPLATE/fix.yml` 的 `options` 清單。

---

## 故障排除

### Copilot 未收到指派

**原因**：Copilot 訂閱未啟用，或 `issues: write` 權限不足

**解決**：確認 Copilot 訂閱有效；確認 Actions 權限設定（如上）

### Actions workflow 執行失敗

**原因**：`issues: write` 權限不足

**解決**：前往 Settings > Actions > General，啟用 Read and write permissions

---

## 安全性說明

- Auto-fix 只建立 PR，**不會自動合併**
- 所有變更均需經過人工 code review
- 只有 `TH0nnz` 的 Issue / 留言才能觸發 workflow
- 不再需要 PAT_TOKEN（已改用 GITHUB_TOKEN + Copilot agent）

---

**最後更新**：2026-02-28
