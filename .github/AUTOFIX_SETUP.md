# Auto-Fix Workflow 設定指南

本文件說明如何正確設定 `/fix` 斜線指令所需的權限。

## ⚠️ 前置需求（必須先完成）

**在使用 `/fix` 指令之前，必須先啟用 GitHub Actions 的基本權限**，否則 workflow 連 checkout 程式碼都無法執行。

### 🔧 立即設定（必要步驟）

1. 前往儲存庫設定頁面：
   - **https://github.com/TH0nnz/sdd-demo-v3/settings/actions**

2. 找到 **Workflow permissions** 區段

3. **必須**選擇以下其中一項：
   - ✅ **Read and write permissions**（推薦，完整功能）
   - ⚠️ **Read repository contents and packages permissions**（僅能讀取，無法推送）

4. **強烈建議**同時勾選：
   - ✅ **Allow GitHub Actions to create and approve pull requests**

5. 點擊 **Save** 儲存設定

### ❌ 如果未設定會發生什麼？

- `fatal: could not read Username for 'https://github.com'` - 連 checkout 都失敗
- `Permission denied` - 無法推送分支
- `GitHub Actions is not permitted to create pull requests` - 無法建立 PR

---

## 常見問題症狀

當在 Issue 中執行 `/fix` 指令時，可能出現以下錯誤：

- `Permission to TH0nnz/sdd-demo-v3.git denied`
- `GitHub Actions is not permitted to create or approve pull requests`
- `fatal: unable to access 'https://github.com/...'`
- `fatal: could not read Username for 'https://github.com'`

## 進階解決方案

如果上述前置需求已完成，但仍然遇到問題，可以嘗試以下進階方案：

### 方案 A：確認權限設定（重新檢查）

有時候設定沒有正確儲存，請再次確認：

1. 前往儲存庫首頁
2. 點擊 **Settings**（設定）
3. 左側選單選擇 **Actions** → **General**
4. 滾動到 **Workflow permissions** 區段
5. 選擇 **✅ Read and write permissions**
6. 勾選 **✅ Allow GitHub Actions to create and approve pull requests**
7. 點擊 **Save** 儲存設定

---

### 方案 B：使用 Personal Access Token（進階使用者）

**⚠️ 注意**：即使使用 PAT，仍然需要先完成上述前置需求中的步驟 3（至少選擇 Read permissions）。

#### 步驟 1：建立 Personal Access Token

1. 前往 GitHub 右上角個人頭像 → **Settings**
2. 左側選單滾動到最下方 → **Developer settings**
3. 選擇 **Personal access tokens** → **Tokens (classic)**
4. 點擊 **Generate new token (classic)**
5. 填寫以下資訊：
   - **Note**: `AutoFix Workflow for sdd-demo-v3`（或任何你喜歡的名稱）
   - **Expiration**: 選擇有效期限（建議 90 days 或 No expiration）
6. 勾選以下權限：
   - ✅ **repo** (Full control of private repositories)
     - 這會自動勾選所有子項目
7. 滾動到最下方點擊 **Generate token**
8. **⚠️ 重要**：立即複製 token，離開頁面後將無法再次查看

#### 步驟 2：新增 Secret 到儲存庫

1. 回到專案儲存庫首頁
2. 點擊 **Settings**
3. 左側選單選擇 **Secrets and variables** → **Actions**
4. 點擊 **New repository secret**
5. 填寫以下資訊：
   - **Name**: `PAT_TOKEN`（必須是這個名稱）
   - **Secret**: 貼上剛才複製的 token
6. 點擊 **Add secret**

#### 優點：
- ✅ 不需要修改儲存庫設定
- ✅ 可以設定有效期限
- ✅ 隨時可以撤銷 token

#### 適用情況：
- 無法存取儲存庫的 Actions 設定
- 需要更高的安全性控制
- 在組織管理的儲存庫中工作

---

## 驗證設定

設定完成後，在任何 Issue 下留言 `/fix` 測試：

```
/fix
```

或指定修復類別：

```
/fix ui
/fix flow
/fix logic
```

### 預期結果：

1. ✅ GitHub Actions workflow 開始執行
2. ✅ 建立新分支 `autofix/issue-<編號>`
3. ✅ 產生 `.autofix/issue-<編號>.md` 檔案
4. ✅ 自動開啟 Pull Request

### 如果仍然失敗：

1. 檢查 Actions 執行日誌中的錯誤訊息
2. 確認你的 GitHub 帳號是 `TH0nnz`（或修改 `.github/workflows/auto-fix.yml` 中的帳號限制）
3. 確認留言是在 Issue 上（不是 PR）
4. 檢查 PAT_TOKEN secret 的名稱是否正確（區分大小寫）

---

## 進階設定

### 允許其他使用者執行 /fix

編輯 `.github/workflows/auto-fix.yml`，修改第 17 行：

```yaml
# 原本：
github.event.comment.user.login == 'TH0nnz' &&

# 改為允許多個使用者：
contains(fromJSON('["TH0nnz", "user2", "user3"]'), github.event.comment.user.login) &&

# 或移除限制（允許所有人，不建議）：
# 直接刪除這一行
```

### 自訂修復類別

在 `.github/workflows/auto-fix.yml` 第 133 行修改正則表達式：

```javascript
// 原本支援：ui, flow, logic
const match = commentBody.match(/\/fix\s+(ui|flow|logic)/i);

// 新增更多類別：
const match = commentBody.match(/\/fix\s+(ui|flow|logic|api|db|test)/i);
```

---

## 故障排除

### 錯誤：403 Forbidden

**原因**：權限不足

**解決**：依照上述方案 1 或方案 2 設定權限

### 錯誤：non-fast-forward

**原因**：分支已存在且有不同的提交

**解決**：已修復，workflow 使用 `--force` 推送

### 錯誤：PR 已存在

**原因**：相同分支的 PR 已開啟

**解決**：已修復，workflow 會自動更新現有 PR

---

## 安全性建議

1. **PAT Token 管理**：
   - 定期更換 tokens
   - 設定有效期限
   - 不要分享或提交到版本控制
   
2. **權限最小化**：
   - 只給予必要的權限
   - 考慮使用 fine-grained tokens（未來支援）

3. **審查機制**：
   - 限制可以執行 `/fix` 的使用者
   - 所有 PR 都應該經過 code review
   - Auto-fix 只建立 PR，不會自動合併

---

**最後更新**：2026-02-28
