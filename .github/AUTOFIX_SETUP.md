# Auto-Fix Workflow 設定指南

本文件說明 Auto-Fix 工作流的運作方式與所需的設定。

## 參數設定總覽（請先看這段）

以下是目前兩個 workflow 內「需要你依環境調整」的參數。請先逐一確認，再往下看詳細設定。

1. `github.event.issue.user.login == 'TH0nnz'`
    - 檔案：`.github/workflows/auto-fix-on-issue.yml`
    - 作用：限制只有指定帳號開的 Issue 才會自動觸發
    - 如何設定：將 `'TH0nnz'` 改成你的 GitHub 帳號，或改成白名單條件

2. `github.event.comment.user.login == 'TH0nnz'`
    - 檔案：`.github/workflows/auto-fix.yml`
    - 作用：限制只有指定帳號可用 `/fix` 觸發
    - 如何設定：將 `'TH0nnz'` 改成你的 GitHub 帳號，或改成白名單條件

3. `contains(github.event.comment.body, '/fix')`
    - 檔案：`.github/workflows/auto-fix.yml`
    - 作用：指定手動觸發指令關鍵字
    - 如何設定：若要改指令（例如 `/autofix`），請同步修改這段與使用說明

4. `CATEGORY` 抓取規則（`ui|flow|logic|add`）
    - 檔案：`.github/workflows/auto-fix.yml`、`.github/workflows/auto-fix-on-issue.yml`
    - 作用：限制可接受的修復類型
    - 如何設定：新增類別時，兩個 workflow 與 Issue 範本要一起改

5. `permissions`（`contents/pull-requests/issues: write`）
    - 檔案：兩個 workflow 都有
    - 作用：授權 GitHub Actions 建立 comment、推送變更、開 PR
    - 如何設定：通常維持 `write`；若降權，可能導致流程失敗

6. `@copilot`（留言內容）
    - 檔案：兩個 workflow 的 `github-script`
    - 作用：透過 mention 通知 Copilot agent
    - 如何設定：建議保持 `@copilot`，不要改成其他字串

7. `baseBranch`（`context.payload.repository.default_branch || 'main'`）
    - 檔案：兩個 workflow 的 `github-script`
    - 作用：決定 PR 目標分支顯示值
    - 如何設定：建議維持自動抓取；若你要固定分支可改成常數字串

## 工作流程說明

```
Issue 開啟（使用 fix.yml 範本）
    ↓
auto-fix-on-issue.yml 觸發
    ↓
發佈任務指示留言（@copilot ...）
透過 mention 機制通知 Copilot agent
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
4. 測試：在任一 Issue 留言 `@copilot 測試`，確認可成功發佈且不出現權限錯誤

---

## 觸發方式

### 方式一：開啟 Issue（自動觸發）

使用 `fix.yml` Issue 範本提交 Issue，系統會自動：
1. 觸發 `auto-fix-on-issue.yml`
2. 透過 `@copilot` mention 發送任務指示
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

此方式觸發 `auto-fix.yml`，會重新發佈含 `@copilot` 的指示留言。

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
3. 確認 Issue 下已出現 `@copilot` 指示留言
4. 確認 Issue 下出現 Copilot 指示留言（含 `@copilot`）
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

同理，`auto-fix.yml` 也要把留言觸發條件改成白名單：

```yaml
contains(fromJSON('["TH0nnz", "other-user"]'), github.event.comment.user.login)
```

註解：
- `fromJSON('["..."]')`：建立可維護的帳號白名單
- `contains(..., github.event.xxx.user.login)`：判斷觸發者是否在白名單內

### 自訂修復類別

Issue 範本目前支援以下類別：`ui`、`flow`、`logic`、`add`

如需新增類別，修改 `.github/ISSUE_TEMPLATE/fix.yml` 的 `options` 清單。

你也必須同步修改兩個 workflow 的抓取規則，否則新類別不會被辨識：

```bash
# auto-fix.yml（/fix 指令）
grep -Eo '/fix\s+(ui|flow|logic|add|<你的新類別>)'

# auto-fix-on-issue.yml（Issue 內文）
grep -Eo '(ui|flow|logic|add|<你的新類別>)'
```

註解：
- `ui|flow|logic|add` 是白名單，未列入的類別會被忽略
- 請保持 `fix.yml` 範本的選項與 workflow 正則一致

---

## 參數註解範例（可直接對照 workflow 修改）

以下範例示範「每個可調參數該改哪裡」。

```yaml
# .github/workflows/auto-fix-on-issue.yml
if: github.event.issue.user.login == 'TH0nnz' # 設定允許自動觸發的人；改成你的帳號或白名單

permissions:
    contents: write      # 需要 write 才能推送分支/提交變更
    pull-requests: write # 需要 write 才能建立或更新 PR
    issues: write        # 需要 write 才能在 Issue 發留言
```

```yaml
# .github/workflows/auto-fix.yml
if: |
    github.event.issue.pull_request == null &&
    github.event.comment.user.login == 'TH0nnz' && # 設定誰可以用 /fix 觸發
    contains(github.event.comment.body, '/fix')     # 設定觸發指令關鍵字
```

```javascript
// 兩個 workflow 的 github-script 都有類似片段
const category = process.env.CATEGORY ?? ''; // 若抓不到類別，預設為空字串
const categoryTag = category ? ` [${category}]` : ''; // 有類別才附加標籤
const baseBranch = context.payload.repository.default_branch || 'main'; // 建議保持自動抓預設分支

const comment = [
    `@copilot 請根據此 Issue 的描述進行修復${categoryTag}。`, // 建議保留 @copilot，不要更名
].join('\n');
```

設定建議：
1. 個人倉庫：`TH0nnz` 改成你的帳號即可
2. 團隊倉庫：用白名單 `fromJSON('["user1","user2"]')`
3. 自訂指令：改 `'/fix'` 後，文件與團隊習慣要一起更新
4. 新增類別：Issue 範本、兩個 workflow 的正則三處都要同步

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
