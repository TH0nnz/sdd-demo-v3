# 變更日誌 (CHANGELOG)

所有本專案的重要變更將記錄於此。格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-TW/)，版本號採 [語義化版本](https://semver.org/zh-TW/)。

---

## [Unreleased]

### 新增 (Added)
- 新增 `checkServiceAnnotations` Gradle 任務，強制檢驗所有 Service 層 public method 必須標註權限 annotation
- 新增詳細的中文註解至前端測試與效能測試腳本
- 新增部門管理功能（Dept. Manager 角色支援）
- 新增通知系統（Notification Entity 與相關 API）

### 修正 (Fixed)
- 修復 `RoleCheckAspectTest`：調整 Mockito mock 策略，移除不必要的 stub 以解決 `UnnecessaryStubbingException`
  - 方法層級 `@RequireRole` 優先於 class 層級的覆蓋測試
  - HR 用戶被拒絕的情境驗證
- 修復 `ProjectServiceTest.updateProject`：補齊 `department` mock，解決 `ResourceNotFoundException`
- 修復 `WorkEntryServiceTest`：
  - `createWorkEntry`：強化 task 為 COMPLETED 時的 `BusinessRuleException` 驗證
  - `updateWorkEntry`：強化 task 為 CLOSED 時的狀態檢驗邏輯
- 改進 `WorkEntryService` 工時異常訊息，細分中文提示以提升使用者體驗

### 改進 (Improved)
- `DataScopeAspect`：優化 ThreadLocal 上下文管理，減少記憶體洩漏風險
- `ProjectRepository`：增強 query 方法，避免 N+1 查詢問題
- `TaskService` 與 `WorkEntryService`：調整例外訊息，提升系統中文提示品質
- JaCoCo 測試覆蓋率驗證門檻維持 Service 層 90% 以上

---

## [0.1.0] - 2026-03-15

### 新增 (Added)
- ✨ 五角色報工系統核心功能
  - **Admin**：專案管理、時數預算設定、增補申請審核
  - **PM**：任務拆分指派、進度監控、增補申請提交
  - **Dept. Manager**：部門成員與任務狀態查看
  - **Executor**：工時填報（最小單位 0.5 小時）、任務完成紀錄
  - **HR**：使用者帳號管理、角色指派、帳號啟用/停用

- 🔐 多層安全架構
  - JWT 無狀態認證（效期 30 分鐘）
  - Spring Security 角色授權
  - `@RequireRole` AOP 功能層級檢查
  - `@DataScope` AOP 資料列層級限制

- 📊 工時管理模組
  - 工時紀錄建立、編輯、查詢
  - 任務週期內工時填報限制
  - 時數不足自動計算與增補申請流程
  - Admin 審核與核准機制

- 🛡️ 資料保護
  - 樂觀鎖機制（Project、Task 使用 `@Version`）
  - 避免並發更新衝突
  - 帳號登入失敗鎖定機制（15 次失敗鎖定 15 分鐘）

- 📝 完整 API
  - 認證：登入、修改密碼
  - 使用者管理：CRUD、角色管理
  - 專案管理：建立、編輯、關閉、啟用
  - 任務管理：拆分、指派、狀態更新
  - 工時記錄：填報、查詢範圍
  - 增補申請：提交、審核、核准

- 🧪 測試覆蓋
  - 單元測試（Mockito）：Service 層 90% 覆蓋率
  - 整合測試（Testcontainers + PostgreSQL）
  - E2E 測試（Playwright）
  - 效能測試（k6 Smoke Test）

- 📦 CI/CD 流程
  - GitHub Actions CI：後端 Gradle 驗證、前端 Node.js 驗證
  - CD 自動部署至遠端主機
  - Auto-Fix 工作流（Issue → Copilot → PR）

- 🐳 容器化部署
  - Docker Compose 本地開發環境
  - 一鍵遠端部署腳本 (`deploy.sh`)
  - PostgreSQL 18、Nginx 反向代理

### 技術堆疊

**後端**
- Java 24 + Spring Boot 4.0.2
- Spring Security + JWT (jjwt 0.12.6)
- Spring Data JPA + Hibernate
- PostgreSQL 18
- Gradle 8.x + Wrapper
- JaCoCo 0.8.13（覆蓋率驗證）
- Checkstyle 10.21.4（程式碼風格檢查）
- Testcontainers 1.21.1（整合測試）

**前端**
- Vue 3.5.x + TypeScript 5.7.x
- Vite 6.1.x（快速開發與建構）
- Pinia 3.0.x（狀態管理）
- Vue Router 4.5.x（路由管理）
- Element Plus 2.9.x（UI 元件庫）
- Axios 1.7.x（HTTP 客戶端）
- Vitest 3.0.x（單元測試）
- Playwright 1.50.x（E2E 測試）

**效能測試**
- k6（效能與負載測試）
  - 100 VU（虛擬使用者）× 1 分鐘
  - p95 回應時間門檻 < 500ms
  - 請求成功率 > 99%

### 文件
- ✅ 詳細的中文 README.md
- ✅ API 端點完整列表
- ✅ 部署指南（本地開發、Docker、遠端主機）
- ✅ 安全架構文件
- ✅ 測試與 CI/CD 工作流程說明

---

## 版本控制規則

### 版本號格式：`X.Y.Z`
- **X（主版本）**：不相容的 API 變更、重大功能重構
- **Y（次版本）**：新增功能，向下相容
- **Z（修訂版本）**：Bug 修正、小幅改進

### Commit 訊息格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

**type 類型：**
- `feat`：新功能
- `fix`：Bug 修正
- `docs`：文件更新
- `style`：程式碼風格（無功能變化）
- `refactor`：重構程式碼
- `perf`：效能優化
- `test`：測試新增或修改
- `chore`：建構、工具、依賴更新

**scope 範圍（示例）：**
- `backend`、`frontend`、`auth`、`test`、`ci`、`deploy`

**subject 標題：**
- 使用祈使句（命令式）
- 不以句號結尾
- 中文或英文清晰表達核心改動

**body 本文：**
- 詳細說明改動的原因與方式
- 支援中文描述

**footer 頁腳：**
- 參考相關 Issue：`Closes #123`、`Fixes #456`
- Breaking Changes 標記：`BREAKING CHANGE: ...`

### 示例

```
feat(auth): 新增帳號登入失敗鎖定機制

連續登入失敗 15 次（5 分鐘內）自動鎖定帳號 15 分鐘，
強化帳號安全性防止暴力破解。

Closes #42
```

---

## 測試政策

### 覆蓋率門檻
- **Service 層**：最低 90% 行覆蓋率（強制驗證）
- **Controller 層**：最低 80% 行覆蓋率
- **Entity 層**：最低 60% 行覆蓋率

### 測試類型

| 類型 | 工具 | 檢查 | 頻率 |
|------|------|------|------|
| 單元測試 | Mockito | Service 業務邏輯 | 每次 commit |
| 整合測試 | Testcontainers | 與 PostgreSQL 互動 | 每次 commit |
| E2E 測試 | Playwright | 完整使用者流程 | PR 前 |
| 效能測試 | k6 | API 回應時間 & 吞吐量 | 每週 / 部署前 |
| 程式碼檢查 | Checkstyle | 編碼風格 | 每次 commit |

---

## 部署檢查清單

### 上線前驗證
- [ ] 所有測試通過（`./gradlew clean test jacocoTestCoverageVerification`）
- [ ] Checkstyle 檢查無誤（`./gradlew checkstyleMain checkstyleTest`）
- [ ] 前端建構成功（`npm run build`）
- [ ] 效能測試達成門檻（p95 < 500ms）
- [ ] 安全掃描无高風險項目
- [ ] 資料庫遷移腳本驗證（Flyway）
- [ ] .env 與機密值已配置

### 部署後健康檢查
- [ ] `/actuator/health` 回應成功
- [ ] 可登入測試帳號
- [ ] 重要 API 端點回應正常
- [ ] 資料庫連線正常
- [ ] 應用日誌無異常
- [ ] 監控告警未觸發

---

## 聯絡方式

- 📧 Issue & PR：https://github.com/TH0nnz/sdd-demo-v3
- 📋 開發流程：參見本檔案與 README.md
- 🐛 Bug 回報：新增 Issue 並標記 `bug` 標籤

---

**最後更新：2026-04-07**
