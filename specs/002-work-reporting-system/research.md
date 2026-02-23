# Research: 報工系統

**Feature**: 002-work-reporting-system
**Date**: 2026-02-23

---

## 1. JDK 24 + Spring Boot 4.0.2

### Decision
採用 JDK 24 + Spring Boot 4.0.2 + Gradle 建構。啟用 Virtual Threads 以提升並行處理效能。

### Rationale
- JDK 24 的 JEP 491 解除 Virtual Threads 在 `synchronized` 上的 pinning 問題，對 JDBC 驅動程式特別重要——Spring Data JPA 的資料庫操作將直接受益
- Stream Gatherers (JEP 485) 正式化，可用於複雜的資料轉換管道
- Spring Boot 4.0.2 基於 Spring Framework 7.0，完全支援 JDK 24

### Alternatives Considered
- **JDK 21 LTS**: 更穩定，但 Virtual Threads pinning 問題未解決，且使用者明確要求 JDK 24
- **Maven**: Spring Boot 官方同等支援，但 Gradle 的增量建構更快速，適合前後端分離的多模組專案

### Key Implementation Notes
- `build.gradle` 中設定 `java.toolchain.languageVersion = JavaLanguageVersion.of(24)`
- `application.yml` 中啟用 `spring.threads.virtual.enabled: true`
- Spring Boot 4.0 破壞性變更：
  - Starter 模組命名可能調整（確認 `spring-boot-starter-data-jpa` 等命名）
  - Jackson 3.0 遷移：`com.fasterxml.jackson` → 檢查套件路徑變化
  - `@MockBean` 已棄用，改用 `@MockitoBean`
  - `jakarta.` 命名空間全面取代 `javax.`

---

## 2. Spring Security 7.0.2 — RBAC 與認證

### Decision
採用 JWT 無狀態認證 + BCrypt 密碼雜湊 + 自訂帳號鎖定機制 + 首次登入強制改密碼。

### Rationale
- JWT 無狀態模式適合前後端分離架構，前端每次請求攜帶 Bearer Token
- BCrypt 自帶鹽值，是 Spring Security 預設且成熟的密碼雜湊演算法
- Spring Security 7 的 `AuthorizationManager` API 取代舊版 `AccessDecisionManager`，更加簡潔

### Alternatives Considered
- **Session-based 認證**: 需要 session 共享機制，不適合前後端分離
- **OAuth2 / OIDC**: 過度複雜，系統不需外部 IdP 整合
- **Argon2 密碼雜湊**: 安全性更高但 CPU 開銷大，BCrypt 對內部系統已足夠

### Key Implementation Notes

#### RBAC 實作
```
角色定義：ROLE_ADMIN（管理層）, ROLE_PM, ROLE_DEPT_MANAGER（部門主管）, 
          ROLE_EXECUTOR（執行人員）, ROLE_HR
```
- 使用 `@PreAuthorize("hasRole('ADMIN')")` 方法層級授權
- 多角色使用者的權限取聯集（Spring Security 預設行為）
- 資料層級隔離透過 Service 層實現（非 Spring Security 範疇）

#### JWT 流程
1. 登入 → POST `/api/auth/login` → 回傳 JWT Access Token（有效期 30 分鐘）
2. 前端將 Token 存於記憶體（非 localStorage 以避免 XSS）
3. 每次請求 Authorization: Bearer {token}
4. Token 過期 → 回傳 401 → 前端導向登入頁

#### 帳號鎖定
- 資料庫欄位：`failed_login_count`, `last_failed_login`, `locked_until`
- 登入失敗時更新計數；5 分鐘內累計 15 次 → 設定 `locked_until = now + 15min`
- 登入成功 → 重設計數
- 鎖定期間的登入嘗試回傳「帳號已鎖定，請稍後再試」

#### 首次登入強制改密碼
- User Entity 新增 `password_changed` (boolean, default false)
- 首次登入成功後，若 `password_changed = false`，JWT 回傳包含 `forcePasswordChange: true` 標記
- 前端攔截此標記，導向強制改密碼頁面
- 改密碼 API 成功後設定 `password_changed = true`

#### 密碼複雜度
- 正規表達式：`^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$`
- 在 Service 層驗證，不符合時拋出 `PasswordPolicyException`

---

## 3. Spring Data JPA 4.0 + PostgreSQL 18.1

### Decision
使用 Spring Data JPA 4.0 + Hibernate 7.x + Flyway 進行資料庫管理。Entity 設計包含審計欄位，Task 時數操作採用樂觀鎖定。

### Rationale
- Spring Data JPA 4.0 基於 Hibernate 7.x，支援新的 entity lifecycle 事件
- Flyway 提供版本化的資料庫遷移，確保各環境一致
- 樂觀鎖定 (`@Version`) 避免並行工時填報的競爭條件

### Alternatives Considered
- **Liquibase**: 功能與 Flyway 類似，但 Flyway 在 Spring Boot 生態系中整合更緊密
- **悲觀鎖定**: 會降低並行效能，不適合多名執行人員同時填報的場景
- **MyBatis**: 靈活但缺少 JPA 的 entity lifecycle 管理與查詢方法自動產生

### Key Implementation Notes

#### 審計欄位
- 所有 Entity 繼承 `BaseEntity`，包含 `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
- 使用 `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate`, `@LastModifiedDate`
- `AuditorAware` 實作從 SecurityContext 取得當前使用者

#### 樂觀鎖定
- Task Entity 添加 `@Version private Long version`
- 工時填報時，先讀取 Task → 扣減時數 → 儲存（若 version 衝突則 `OptimisticLockException` → 重試）

#### N+1 防治
- 使用 `@EntityGraph` 或 `JOIN FETCH` 在必要的查詢中預載入關聯
- 專案儀表板查詢：自訂 JPQL 一次載入 Project + Tasks 摘要
- 禁止 lazy loading 的意外觸發，開發環境啟用 `spring.jpa.open-in-view: false`

#### 索引策略
- `work_entry(task_id, work_date)` — 複合索引，加速工時查詢
- `work_entry(user_id, work_date)` — 複合索引，加速個人工時查詢
- `user(email)` — 唯一索引，加速登入查詢
- `task(project_id, status)` — 複合索引，加速專案儀表板
- `audit_log(created_at)` — 索引，加速日誌查詢
- `hours_request(project_id, status)` — 複合索引，加速審核列表

#### Flyway
- 遷移腳本路徑：`src/main/resources/db/migration/`
- 命名規則：`V{版本號}__{描述}.sql`（例如 `V1__init_schema.sql`）

---

## 4. Vue 3 + TypeScript 前端架構

### Decision
採用 Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus 元件庫。

### Rationale
- Vue 3 Composition API + TypeScript 提供良好的型別安全與程式碼組織
- Pinia 是 Vue 3 官方推薦的狀態管理方案，比 Vuex 更簡潔
- Element Plus 提供完整的繁體中文 i18n 支援，適合企業內部應用
- Vite 開發伺服器啟動極快，HMR 即時更新

### Alternatives Considered
- **PrimeVue**: 功能豐富但 CSS 樣式客製化較複雜
- **Ant Design Vue**: 設計語言偏簡約，不如 Element Plus 適合表單密集的企業應用
- **Nuxt 3**: SSR 功能多餘，內部系統不需 SEO

### Key Implementation Notes

#### 角色路由守衛
```typescript
// router/guards.ts
router.beforeEach((to, from) => {
  const authStore = useAuthStore()
  const requiredRoles = to.meta.roles as string[]
  if (requiredRoles && !requiredRoles.some(r => authStore.hasRole(r))) {
    return { name: 'forbidden' }
  }
})
```

#### Axios JWT 攔截器
- Request 攔截器：自動附加 `Authorization: Bearer {token}`
- Response 攔截器：401 → 清除 token → 導向登入頁
- Token 儲存於 Pinia store（記憶體），不存 localStorage

#### 元件庫 i18n
- Element Plus 設定繁體中文 locale
- 自訂 i18n 使用 `vue-i18n` 或直接使用中文字面量（內部系統僅需單一語系）

#### 頁面結構（按角色）
```
pages/
├── auth/               # 登入、強制改密碼
├── executor/           # 執行人員：工時填報、Task 列表
├── pm/                 # PM：Task 管理、專案儀表板、時數申請
├── admin/              # 管理層：專案管理、審核時數申請
├── dept-manager/       # 部門主管：部門工時總覽
└── hr/                 # HR：使用者管理、角色指派
```

---

## 5. 測試策略

### Decision
四層測試架構：單元測試（JUnit 5 + Vitest）→ 整合測試（Testcontainers）→ API 合約測試 → E2E 測試（Playwright）。覆蓋率門檻 80%。

### Rationale
- 四層架構確保從函式層級到使用者層級的完整覆蓋
- Testcontainers 提供真實 PostgreSQL 環境，避免 H2 行為差異
- Playwright 跨瀏覽器支援，適合驗收情境測試

### Alternatives Considered
- **H2 In-Memory DB**: 與 PostgreSQL 語法差異會導致測試不可靠
- **Cypress**: E2E 工具成熟但 Playwright 效能更好、支援多瀏覽器
- **Selenium**: 過時，API 不如 Playwright 現代化

### Key Implementation Notes

#### 後端測試
```groovy
// build.gradle
jacocoTestReport {
    reports { xml.required = true }
}
jacocoTestCoverageVerification {
    violationRules {
        rule { limit { minimum = 0.80 } }
    }
}
```
- `@SpringBootTest` + Testcontainers PostgreSQL 整合測試
- `@WebMvcTest` + `@MockitoBean` 控制器單元測試
- 使用 `@Sql` 載入測試資料

#### 前端測試
- Vitest：元件渲染 + Composable 邏輯測試
- Vue Test Utils：元件互動測試
- `c8` 覆蓋率報告器

#### E2E 測試
- Playwright 測試矩陣：每個角色一個測試套件
- P1 user story（執行人員工時填報）的全部 4 個驗收情境必須有對應 E2E 測試
- 使用 POM（Page Object Model）模式組織

---

## 6. 通知機制

### Decision
站內通知採用資料庫表 + 前端輪詢（30 秒間隔）。

### Rationale
- 規格要求站內訊息（in-app notification），不需電子郵件或簡訊
- < 100 使用者規模下，輪詢比 WebSocket 實作更簡單、可靠
- 30 秒輪詢間隔滿足「5 分鐘內更新」的要求

### Alternatives Considered
- **WebSocket / SSE**: 即時性更好但增加基礎設施複雜度，規模不需要
- **Redis Pub/Sub**: 需額外服務，過度設計

### Key Implementation Notes
- Notification Entity：`id`, `user_id`, `type`, `title`, `content`, `is_read`, `created_at`
- API：GET `/api/notifications?unread=true`（輪詢）、PATCH `/api/notifications/{id}/read`（標記已讀）
- 通知觸發點：Task 時數歸零 → 通知 PM、時數申請狀態變更 → 通知 PM

---

## 7. 工作天計算

### Decision
工作天定義為週一至週五。本版本不處理國定假日（符合規格 Assumptions）。

### Rationale
- 規格明確聲明「本版本不處理國定假日的排除邏輯」
- 使用 `java.time.DayOfWeek` 判斷即可，無需額外假日資料庫

### Key Implementation Notes
- 「過去三工作天」計算邏輯：從今日往回計算，略過週六日
  - 例如：週一 → 可編輯上週五、四、三
  - 例如：週三 → 可編輯當週二、一、上週五
- 封裝為工具方法 `WorkDayUtils.isWithinEditableRange(LocalDate date)`
- 完整的單元測試覆蓋所有跨週情境

---

## 8. 部署架構

### Decision
Docker Compose 單機部署，包含後端、前端（Nginx 靜態伺服）、PostgreSQL。

### Rationale
- 內部網路、< 100 使用者，單機部署足夠
- Docker Compose 便於環境一致性與快速部署

### Alternatives Considered
- **Kubernetes**: 過度複雜，不符合規模需求
- **直接部署 JAR**: 缺少環境一致性保證

### Key Implementation Notes
```yaml
# docker-compose.yml 概要
services:
  db:       PostgreSQL 18.1
  backend:  Spring Boot JAR (JDK 24)
  frontend: Nginx + Vue build artifacts
```
- 前端 Nginx 反向代理 API 請求至後端
- PostgreSQL 資料卷持久化
