# 技術研究報告：報工系統全端技術選型分析

**建立日期**: 2026-02-23  
**專案**: 002-work-reporting-system  
**目的**: 研究純參考 — 不產生任何程式碼  

---

## 目錄

1. [主題一：JDK 24 + Spring Boot 4.0 後端基礎](#主題一jdk-24--spring-boot-40-後端基礎)
2. [主題二：Spring Security 7 RBAC 與認證機制](#主題二spring-security-7-rbac-與認證機制)
3. [主題三：Spring Data JPA 4 + PostgreSQL 資料層](#主題三spring-data-jpa-4--postgresql-資料層)
4. [主題四：Vue 3 + TypeScript 前端架構](#主題四vue-3--typescript-前端架構)
5. [主題五：測試策略](#主題五測試策略)

---

## 主題一：JDK 24 + Spring Boot 4.0 後端基礎

### 決策：採用 JDK 24 + Spring Boot 4.0.x + Gradle 9

### 理由

**JDK 24**（2025 年 3 月 GA）帶來多項重要特性，對報工系統這類企業內部應用有直接價值：

| JEP | 特性 | 對本系統的影響 |
|-----|------|--------------|
| **JEP 491** | Virtual Threads 不再 Pin 於 `synchronized` | 解除了 Virtual Threads 在 `synchronized` 方法中阻塞平台執行緒的限制。Hibernate / JDBC 驅動程式中大量使用 `synchronized`，此改進使 Virtual Threads 在資料庫 I/O 密集的報工系統中真正可用。 |
| **JEP 485** | Stream Gatherers（正式版） | 提供自訂中間操作（如 `windowFixed`、`windowSliding`、`mapConcurrent`、`fold`、`scan`），對工時統計的串流處理直接有用。 |
| **JEP 484** | Class-File API（正式版） | 框架層級的效能提升（Spring 可用於替代 ASM），對系統不需直接操作。 |
| **JEP 499** | Structured Concurrency（第四次預覽） | `StructuredTaskScope` 提供 fork/join 模式，可安全管理並行子任務的生命週期。目前仍為預覽，建議觀望至正式版再導入生產環境。 |
| **JEP 487** | Scoped Values（第四次預覽） | 比 ThreadLocal 更安全的執行緒區域共享機制，適用於請求上下文傳遞。同為預覽，暫不採用。 |
| **JEP 492** | Flexible Constructor Bodies（第三次預覽） | `super()` 前可執行語句，減少建構子限制。預覽狀態，不影響核心設計。 |

**Spring Boot 4.0**（2025 年 5 月 GA）重大變更：

- **基礎需求**：Java 17+，Jakarta EE 11（Servlet 6.1）
- **核心相依升級**：Spring Framework 7.0、Spring Security 7.0、Spring Data 2025.1、Hibernate 7.1、Jackson 3.0、Flyway 11.11、Testcontainers 2.0、HikariCP 7.0、Tomcat 11.0
- **模組化 Starter 重新命名**（重大破壞性變更）：
  - `spring-boot-starter-web` → `spring-boot-starter-webmvc`
  - `spring-boot-starter-oauth2-resource-server` → `spring-boot-starter-security-oauth2-resource-server`
  - `spring-boot-starter-data-jpa` → `spring-boot-starter-data-jpa-hibernate`
  - 原名稱仍可用（classic starter POMs），`spring-boot-starter-web` 現在為 WebMVC + WebFlux 的超集
- **Jackson 3.0 為預設**：group ID 從 `com.fasterxml.jackson` 改為 `tools.jackson`；Jackson 2 仍受支援但已標記棄用
- **JSpecify 空值標註**：全面加入 `@Nullable` / `@NonNull` 標註
- **新功能**：HTTP Service Clients、API Versioning、OpenTelemetry starter、`RestTestClient`
- **Flyway 需獨立 starter**：`spring-boot-starter-flyway`
- **Liveness/Readiness 探針預設啟用**

### 替代方案考量

| 方案 | 優缺點 |
|------|--------|
| JDK 21（LTS） | 更穩定的 LTS 版本，但缺少 JEP 491（Virtual Threads 解除 pinning），對 JDBC 驅動效能有影響 |
| JDK 17 | Spring Boot 4.0 最低需求，但錯失 Virtual Threads、Record Patterns、Stream Gatherers 等重要特性 |
| Spring Boot 3.x | 成熟穩定，但會錯失 Jackson 3、Hibernate 7.1、HTTP Service Clients 等改進 |
| Maven | 穩定且社群生態大，但 Spring Boot 4.0 明確推薦 Gradle 9，且 Gradle 的建構速度較快 |

### 關鍵實作注意事項

1. **Gradle 9 設定**：採用 Kotlin DSL (`build.gradle.kts`)；Spring Boot 4.0 原生支援 Gradle 9
2. **Jackson 3 遷移**：套件前綴從 `com.fasterxml.jackson` 改為 `tools.jackson`；相關 import 全部需更新
3. **Starter 命名**：新專案直接使用新名稱（`spring-boot-starter-webmvc`）；若需漸進遷移可暫用 classic 名稱
4. **Virtual Threads 啟用**：在 `application.yml` 中設定 `spring.threads.virtual.enabled: true`，Tomcat 與排程器將自動使用 Virtual Threads
5. **`@MockBean` 棄用**：測試中改用 `@MockitoBean` / `@MockitoSpyBean`
6. **`@SpringBootTest` 不再自動提供 MockMVC**：需顯式加上 `@AutoConfigureMockMvc`

---

## 主題二：Spring Security 7 RBAC 與認證機制

### 決策：採用 Spring Security 7.0 + JWT + BCrypt + 自建角色權限體系

### 理由

本系統需支援五種角色（管理層、PM、部門主管、執行人員、HR），且同一使用者可擁有多角色。Spring Security 7.0 針對此場景提供完整的解決方案：

**認證流程設計**：
- 登入端點接受 email + 密碼，驗證成功後核發 JWT Access Token
- JWT 內嵌使用者 ID、角色列表、過期時間
- 後續所有 API 請求透過 `Authorization: Bearer <token>` 驗證
- 密碼以 BCrypt（Spring Security 內建 `BCryptPasswordEncoder`）加鹽雜湊儲存

**RBAC 實作模式（基於 `@PreAuthorize`）**：
- 使用 `JwtAuthenticationConverter` 從 JWT claims 解析角色
- 設定 `JwtGrantedAuthoritiesConverter`：`setAuthorityPrefix("ROLE_")`、`setAuthoritiesClaimName("roles")`
- Controller 方法上使用 `@PreAuthorize("hasRole('MANAGEMENT')")` 等宣告式控制
- 支援複雜表達式如 `@PreAuthorize("hasAnyRole('PM', 'MANAGEMENT')")` 處理多角色存取

**帳號鎖定機制**（FR-019）：
- 5 分鐘內連續 15 次登入失敗 → 鎖定帳號
- 需自建 `AuthenticationFailureHandler` + 資料庫記錄失敗次數與時間戳
- 15 分鐘後自動解鎖：查詢時檢查距上次失敗是否已超過 15 分鐘

**首次登入強制改密碼**（FR-018）：
- User 實體加入 `mustChangePassword` 旗標（初始值 `true`）
- 登入成功但 `mustChangePassword=true` 時，JWT 僅含有限權限（只允許呼叫改密碼 API）
- 密碼複雜度驗證（FR-023）：≥ 8 字元，含大寫 + 小寫 + 數字，使用正則表達式 `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$`

**Spring Security 7.0 重大變更**：
- Access API（`AccessDecisionManager`、`AccessDecisionVoter` 等）已移至 legacy 模組 `spring-security-access`
- 新版一律使用 `AuthorizationManager` API
- Starter 名稱：`spring-boot-starter-security-oauth2-resource-server`

### 替代方案考量

| 方案 | 優缺點 |
|------|--------|
| Session-based 認證 | 更簡單，但不適合前後端分離架構，且 100 人以下規模差異不大 |
| Keycloak / Auth0 等外部 IdP | 功能強大但增加運維複雜度，對 100 人內部系統而言過重 |
| 自建 Token（非 JWT） | 需額外維護 Token 儲存與查詢，JWT 自帶驗證即可 |
| 資料庫角色表 + 動態查詢 | 可提供更細粒度的權限管理，但本系統五個固定角色不需要此彈性 |

### 關鍵實作注意事項

1. **JWT 金鑰管理**：使用非對稱金鑰（RSA-256），私鑰簽發、公鑰驗證；金鑰以環境變數或 secret 注入
2. **Token 過期策略**：Access Token 有效期 1 小時；Refresh Token 有效期 8 小時（對應工作日時段 08:00–20:00）
3. **資料隔離**：在 Service 層加入角色相關的資料過濾邏輯
   - 執行人員：只查詢 `assignee = currentUser` 的 Task
   - 部門主管：只查詢 `department = currentUser.department` 的執行人員資料
   - PM：只查詢 `pm = currentUser` 的專案
4. **稽核日誌**（FR-024）：使用 AOP `@Aspect` 攔截關鍵操作，自動寫入 AuditLog 表
5. **密碼雜湊**：`BCryptPasswordEncoder` 預設 strength=10，對 100 人規模已足夠

---

## 主題三：Spring Data JPA 4 + PostgreSQL 資料層

### 決策：採用 Spring Data JPA 4.0 + Hibernate 7.1 + PostgreSQL 17+ + Flyway 11

### 理由

**Spring Data JPA 4.0 / Hibernate 7.1 組合特性**：
- Hibernate 7.1 為 Spring Boot 4.0 內建版本，相容 Jakarta Persistence 3.2
- 支持 `@OptimisticLocking` 防止並行寫入衝突（重要：多位執行人員同時填報工時時防止數據不一致）
- JPA Auditing（`@CreatedDate`、`@LastModifiedDate`、`@CreatedBy`、`@LastModifiedBy`）自動填充稽核欄位
- JPQL 增強與更好的 Native Query 支援

**PostgreSQL 選型理由**：
- 免費開源，企業級穩定性
- 原生 JSON/JSONB 支援（稽核日誌的內容摘要可存為 JSONB）
- 強大的日期/時間函式（對工作天計算有幫助）
- 小規模（100 使用者）場景效能綽綽有餘

**Flyway 資料庫遷移**：
- Spring Boot 4.0 需使用獨立 starter：`spring-boot-starter-flyway`
- 版本化腳本命名：`V1__create_users.sql`、`V2__create_projects.sql`…
- 確保 schema 變更可追蹤、可重複、可回滾

**實體設計核心重點**：

```
Project ──(1:N)── Task ──(1:N)── WorkEntry
   │                │
   └──(N:1)── User(PM)   └──(N:1)── User(Assignee)
   
User ──(N:N)── Role
User ──(N:1)── Department ──(1:1)── User(Head)
HoursRequest ──(N:1)── Project
AuditLog（獨立）
```

**N+1 查詢防治策略**：
- 使用 `@EntityGraph` 或 `JOIN FETCH` 在 Repository 方法上明確指定載入關聯
- PM 儀表板查詢（需載入專案 + 所有 Task + 各 Task 的累計工時）使用自訂 JPQL 搭配 `@EntityGraph`
- 不使用 `FetchType.EAGER`，所有關聯預設 `LAZY`，需要時才 fetch

### 替代方案考量

| 方案 | 優缺點 |
|------|--------|
| MyBatis | 更精細的 SQL 控制，但對 CRUD 為主的報工系統而言 JPA 的開發效率更高 |
| jOOQ | 類型安全 SQL DSL，但學習曲線較陡，社群資源較 JPA 少 |
| MySQL | 可行但 PostgreSQL 的 JSONB、日期函式、標準 SQL 相容性較佳 |
| Liquibase | 功能與 Flyway 相當，但 Flyway 的 SQL 腳本方式更直觀 |
| R2DBC (Reactive) | 100 人規模不需要響應式資料存取，增加複雜度無收益 |

### 關鍵實作注意事項

1. **樂觀鎖定**：Task 和 WorkEntry 實體加入 `@Version` 欄位，防止並行填報造成時數計算錯誤
2. **「三工作天」邏輯**：
   - 在 Service 層實作 `isWithinEditableWindow(LocalDate entryDate)` 方法
   - 排除週六、週日（本版本不處理國定假日）
   - 使用 `java.time.DayOfWeek` 判斷
3. **軟刪除 vs 狀態欄位**：不採用軟刪除。WorkEntry 永久保留（FR-021），Project 和 Task 使用狀態欄位控制生命週期
4. **索引**：
   - `work_entry(task_id, entry_date)` — 工時查詢主要路徑
   - `task(project_id, status)` — PM 儀表板篩選
   - `user(email)` — 登入查詢（唯一索引）
   - `user(department_id)` — 部門主管查詢
5. **交易管理**：Service 方法加上 `@Transactional`；工時填報需在同一交易中「建立 WorkEntry + 更新 Task 已消耗時數」
6. **連線池**：HikariCP 7.0（Spring Boot 4.0 內建），100 使用者預設 `maximumPoolSize=10` 即可

---

## 主題四：Vue 3 + TypeScript 前端架構

### 決策：採用 Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus

### 理由

**Vue 3 + TypeScript 組合**：
- Vue 3 的 Composition API (`<script setup lang="ts">`) 提供一流的 TypeScript 支援
- 使用 `create-vue` 腳手架，內建 TypeScript + Vite 設定
- `vue-tsc` 用於型別檢查，`@vue/tsconfig` 提供推薦的 tsconfig 設定
- `compilerOptions.isolatedModules: true` 確保與 Vite 的 esbuild 轉換相容

**Pinia 狀態管理**：
- Vue 3 官方推薦的狀態管理函式庫，取代 Vuex
- 扁平化 Store 架構（無巢狀模組），每個業務領域一個 Store
- 支援 Composition API 風格的 `defineStore` 定義
- 建議的 Store 拆分：
  - `useAuthStore` — 登入狀態、JWT Token、使用者資訊
  - `useProjectStore` — 專案 CRUD
  - `useTaskStore` — Task CRUD、狀態管理
  - `useWorkEntryStore` — 工時填報
  - `useNotificationStore` — 站內通知

**Element Plus 元件函式庫**：
- Vue 3 最成熟的企業級 UI 元件函式庫
- 完整的 i18n 支援，內建繁體中文 (`zh-tw`) 語系
- 豐富的表單元件（DatePicker、TimePicker、Select、Table、Pagination）直接對應報工系統需求
- `ElConfigProvider` 全域設定語系：
  ```ts
  import zhTw from 'element-plus/es/locale/lang/zh-tw'
  ```
- 支援深色模式、主題客製化、SSR
- GitHub 25k+ stars，活躍維護，Discord 社群活躍

**路由架構**（Vue Router）：
- 基於角色的路由守衛（Navigation Guard）
- 路由結構隨角色動態載入
- 核心路由規劃：
  - `/login` — 登入頁
  - `/change-password` — 首次強制改密碼
  - `/executor/*` — 執行人員介面（工時填報、Task 列表）
  - `/pm/*` — PM 介面（專案儀表板、Task 管理、時數申請）
  - `/management/*` — 管理層介面（專案管理、申請審核）
  - `/hr/*` — HR 介面（使用者管理）
  - `/dept-head/*` — 部門主管介面（部門工時概覽）

### 替代方案考量

| 方案 | 優缺點 |
|------|--------|
| React + Next.js | 生態更大，但 Vue 3 的學習曲線較低，Element Plus 的中文化支援更成熟 |
| Angular | 全功能框架，但過於龐大；100 人內部系統不需要 |
| Vuetify 3 | Material Design 風格，但元件成熟度與穩定性不如 Element Plus |
| PrimeVue | 功能豐富但社群規模較小，中文本地化文件較少 |
| Ant Design Vue | 選項之一，但 Element Plus 在亞洲企業圈採用率更高 |
| Vuex | Vue 3 中已被 Pinia 取代為官方推薦方案 |

### 關鍵實作注意事項

1. **Axios 攔截器**：
   - Request 攔截器：自動附加 `Authorization: Bearer <token>` header
   - Response 攔截器：捕捉 401 → 嘗試 refresh token 或導向登入頁；捕捉 403 → 顯示權限不足訊息
2. **TypeScript 嚴格模式**：`tsconfig.json` 啟用 `strict: true`；API 回應定義完整的 interface/type
3. **環境變數**：Vite 的 `.env` 設定 `VITE_API_BASE_URL` 指向後端 API
4. **日期處理**：使用 Day.js（Element Plus 相依）處理日期格式化與「三工作天」可視化邏輯
5. **Element Plus 按需引入**：使用 `unplugin-vue-components` + `unplugin-auto-import` 實現自動按需載入，減少建構產出大小
6. **多角色切換**：若使用者同時有 PM 和執行人員角色，側邊欄動態合併顯示所有可存取的功能模組

---

## 主題五：測試策略

### 決策：四層測試架構 — 單元測試 + 整合測試 + 元件測試 + E2E 測試

### 理由

報工系統涉及工時計算、角色權限、狀態機轉換等核心業務邏輯，需要嚴謹的測試覆蓋。

### 5.1 後端：JUnit 5 + Testcontainers 2.0

**決策**：使用 JUnit 5 + Spring Boot Test + Testcontainers 2.0（PostgreSQL）

**Spring Boot 4.0 測試變更**：
- `@MockBean` / `@SpyBean` 已棄用 → 改用 `@MockitoBean` / `@MockitoSpyBean`
- `@SpringBootTest` 不再自動設定 MockMVC → 需加 `@AutoConfigureMockMvc`
- 新增 `RestTestClient` 用於 REST API 測試

**Testcontainers 2.0 + Spring Boot 整合**：
- 使用 `@ServiceConnection` 註解自動設定資料庫連線：
  ```java
  @Container
  @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");
  ```
- Spring Boot 自動偵測容器並覆寫 `spring.datasource.*` 設定
- 使用 `@TestConfiguration` + `@ImportTestcontainers` 跨測試類別共享容器設定
- 容器生命週期由 Spring 管理，保證 Bean 正確初始化順序

**測試分層**：

| 層級 | 工具 | 覆蓋範圍 | 範例 |
|------|------|---------|------|
| 單元測試 | JUnit 5 + Mockito | Service 層業務邏輯 | 三工作天計算、時數扣減、狀態轉換驗證 |
| 整合測試 | `@SpringBootTest` + Testcontainers | Repository + Service + 真實 DB | 工時填報完整流程、樂觀鎖定衝突處理 |
| API 測試 | `RestTestClient` / MockMVC | Controller + Security | 角色權限驗證（401/403）、RBAC 資料隔離 |
| 安全測試 | `@WithMockUser` | Security 規則 | 執行人員不能存取 PM 端點、帳號鎖定流程 |

### 5.2 前端：Vitest + Vue Test Utils

**決策**：使用 Vitest + Vue Test Utils 進行元件單元/整合測試

**Vitest 優勢**：
- 基於 Vite，與專案建構管線完全共用設定（resolve.alias、plugins）
- 相容 Jest API（`describe`、`it`、`expect`），遷移成本低
- 原生 TypeScript 支援，無需額外設定
- 內建覆蓋率報告 (`vitest run --coverage`)
- VS Code 擴充套件支援直接在編輯器中執行與除錯測試

**Vue Test Utils 模式**：
- `mount()` 完整渲染元件；`shallowMount()` 隔離子元件
- 支援 Props 注入、Event 觸發驗證、Slot 測試
- 搭配 Pinia 測試：使用 `createTestingPinia()` 注入模擬 Store
- 搭配 Vue Router 測試：使用 `RouterLinkStub` 或模擬路由物件

**前端測試分層**：

| 層級 | 工具 | 覆蓋範圍 | 範例 |
|------|------|---------|------|
| 元件單元測試 | Vitest + Vue Test Utils | 單一元件邏輯 | 工時填報表單驗證、角色切換選單 |
| Store 測試 | Vitest + Pinia Testing | 狀態管理邏輯 | `useWorkEntryStore` 的 actions 與 getters |
| Composable 測試 | Vitest | 共用邏輯 | `useEditableWindow()` — 三工作天計算 |
| API Mock 測試 | Vitest + MSW | 前後端介面 | API 回應處理、錯誤狀態碼處理 |

### 5.3 E2E 測試：Playwright

**決策**：使用 Playwright 進行端對端測試

**選型理由**：
- 支援 Chromium、Firefox、WebKit 三引擎跨瀏覽器測試
- 原生 TypeScript 支援（`.ts` 檔案直接撰寫測試）
- 內建 Auto-waiting 與重試機制，測試穩定性高
- Trace Viewer 提供完整的測試追蹤（時間軸、DOM 快照、網路請求）
- Codegen 工具可自動產生測試程式碼
- 支援平行化與 Sharding（CI 中加速）

**E2E 測試最佳實踐**：
- **使用 Locators**：優先使用 `page.getByRole()`、`page.getByText()`、`page.getByLabel()` 等語義化定位器，避免 CSS selector
- **測試隔離**：每個測試獨立的 browser context，不依賴其他測試的狀態
- **不測試第三方**：使用 `page.route()` 模擬外部 API 回應
- **Web-first Assertions**：使用 `await expect(locator).toBeVisible()` 而非手動 `isVisible()` 判斷

**E2E 測試涵蓋的核心場景**：

| 場景 | 驗證重點 |
|------|---------|
| 登入與首次改密碼 | 初始密碼登入 → 強制改密碼 → 密碼複雜度驗證 → 改密碼後進入主畫面 |
| 工時填報完整流程 | 執行人員登入 → 選 Task → 填報工時 → 確認時數正確扣減 |
| 超過三工作天紀錄不可編輯 | 嘗試修改舊紀錄 → 確認系統拒絕 |
| PM 建立 Task 並指派 | PM 登入 → 建立 Task → 指派執行人員 → 確認執行人員看到 Task |
| RBAC 隔離 | 以執行人員帳號嘗試存取 PM 頁面 → 確認重新導向或 403 |
| 帳號鎖定 | 連續 15 次錯誤密碼 → 確認帳號被鎖定 → 15 分鐘後解鎖 |

### 5.4 覆蓋率目標與 CI 整合

**覆蓋率工具**：
- 後端：JaCoCo（Gradle 外掛），產生 HTML + XML 報告
- 前端：Vitest 內建覆蓋率（基於 v8 或 istanbul）

**覆蓋率目標**：

| 範圍 | 目標 | 說明 |
|------|------|------|
| 後端 Service 層 | ≥ 80% 行覆蓋率 | 核心業務邏輯必須充分覆蓋 |
| 後端 Security 層 | 100% 端點覆蓋 | 每個端點都需驗證 RBAC 規則 |
| 前端元件 | ≥ 70% 行覆蓋率 | 表單驗證、條件渲染邏輯 |
| E2E 場景 | 覆蓋所有 P1-P3 User Story | 核心業務流程端對端驗證 |

**JaCoCo Gradle 設定重點**：
```kotlin
// build.gradle.kts
plugins {
    jacoco
}
jacoco {
    toolVersion = "0.8.12"
}
tasks.jacocoTestReport {
    reports {
        xml.required = true  // CI 解析用
        html.required = true // 人工檢視用
    }
}
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
```

### 替代方案考量

| 方案 | 優缺點 |
|------|--------|
| Cypress（E2E） | 成熟穩定，但僅支援 Chromium/Firefox，無 WebKit；Playwright 多瀏覽器支援更完整 |
| Jest（前端） | 成熟生態，但非 Vite 原生；Vitest 共用 Vite 設定且效能更佳 |
| TestNG（後端） | Spring 生態中 JUnit 5 是主流，社群資源更多 |
| Selenium（E2E） | 歷史悠久但 API 較舊，無 auto-waiting，測試穩定性較差 |
| Cobertura（覆蓋率） | JaCoCo 是 Spring / Gradle 生態的事實標準 |

---

## 總結建議

| 層面 | 技術選型 | 版本 |
|------|---------|------|
| JDK | OpenJDK | 24 |
| 後端框架 | Spring Boot | 4.0.x |
| 安全框架 | Spring Security | 7.0.x |
| ORM | Spring Data JPA + Hibernate | 4.0 / 7.1 |
| 資料庫 | PostgreSQL | 17+ |
| DB 遷移 | Flyway | 11.x |
| 建構工具 | Gradle (Kotlin DSL) | 9.x |
| JSON 處理 | Jackson | 3.0 |
| 前端框架 | Vue 3 (Composition API) | 3.5+ |
| 類型系統 | TypeScript | 5.x |
| 建構/打包 | Vite | 6.x |
| 狀態管理 | Pinia | 3.x |
| UI 元件 | Element Plus | 2.x |
| 路由 | Vue Router | 4.x |
| 後端測試 | JUnit 5 + Testcontainers 2.0 | — |
| 前端測試 | Vitest + Vue Test Utils | — |
| E2E 測試 | Playwright | latest |
| 覆蓋率 (後端) | JaCoCo | 0.8.x |
| 覆蓋率 (前端) | Vitest (v8/istanbul) | — |
