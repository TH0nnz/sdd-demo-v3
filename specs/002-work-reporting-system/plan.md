# Implementation Plan: 報工系統

**Branch**: `002-work-reporting-system` | **Date**: 2026-02-23 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-work-reporting-system/spec.md`

## Summary

建構一套報工系統（Work Reporting System），支援五種角色（管理層、PM、部門主管、執行人員、HR）的工時管理流程。後端採用 JDK 24 + Spring Boot 4.0.2 提供 RESTful API，前端採用 Vue.js SPA 實現前後端分離架構，資料庫使用 PostgreSQL 18.1。核心功能涵蓋：專案與 Task 的生命週期管理、執行人員工時填報（即時扣減、三工作天可編輯限制）、時數增補申請審核機制、RBAC 角色存取控制、稽核日誌，以及部門工時唯讀檢視。

## Technical Context

**Language/Version**: Java 24 (JDK 24)
**Primary Dependencies**: Spring Boot 4.0.2, Spring Security 7.0.2, Spring Data JPA 4.0, Vue.js 3 (前端)
**Storage**: PostgreSQL 18.1
**Testing**: JUnit 5 + Mockito（後端單元 / 整合測試）、Vitest + Vue Test Utils（前端單元測試）、Playwright（E2E 測試）
**Target Platform**: 內部網路 Web 應用程式（Linux/Docker 部署）
**Project Type**: Web service (REST API) + SPA frontend（前後端分離）
**Performance Goals**: API p95 ≤ 200ms、p99 ≤ 500ms；前端 FCP ≤ 1.5s、TTI ≤ 3.0s（4G 基準）
**Constraints**: < 100 同時線上使用者、單一部署（不需水平擴展）、工時填報後即時生效（無審核延遲）
**Scale/Scope**: ~100 使用者、5 種角色、7 核心實體、~15 個頁面

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. 程式碼品質 — ✅ 通過

| 要求 | 計畫對應 |
|------|---------|
| Linting / 靜態分析零警告 | 後端：Checkstyle + SpotBugs（CI 強制）；前端：ESLint + Prettier |
| 函式單一職責、cyclomatic complexity ≤ 10 | 透過 SonarQube 或 Checkstyle 規則限制 |
| 無魔術數字 / 硬編碼字串 | 提取為常數或 application.yml 設定 |
| PR 需至少一人 code review | Git 分支策略與 PR 規範 |
| 禁止 dead code 與 untracked TODO | Linting 與 CI 檢查 |

### II. 測試標準 — ✅ 通過

| 要求 | 計畫對應 |
|------|---------|
| TDD: Red → Green → Refactor | 開發流程規範 |
| 單元測試覆蓋率 ≥ 80% | JaCoCo（後端）+ Istanbul/c8（前端），CI 門檻強制 |
| 整合測試覆蓋所有 API 合約 | Spring Boot Test + Testcontainers (PostgreSQL) |
| 測試必須確定性 | 禁止依賴時間或外部服務的非確定性測試 |
| E2E 覆蓋所有 P1 user story 驗收情境 | Playwright 測試對應 User Story 1 的所有驗收情境 |

### III. 使用者體驗一致性 — ✅ 通過

| 要求 | 計畫對應 |
|------|---------|
| 統一設計系統元件 | 採用 Element Plus 或 PrimeVue 元件庫 |
| 錯誤訊息格式：context + what + action | 全域錯誤處理中介層統一格式 |
| 平台導覽慣例 | Vue Router SPA 導覽 + 角色分流 |
| WCAG 2.1 AA 無障礙 | 元件庫內建支援 + a11y 自動掃描 |
| Loading / Empty / Error 狀態 | 每個頁面 / 元件明確設計三態 |

### IV. 效能要求 — ✅ 通過

| 要求 | 計畫對應 |
|------|---------|
| API p95 ≤ 200ms, p99 ≤ 500ms | Spring Boot Actuator 監控 + 效能測試 |
| FCP ≤ 1.5s, TTI ≤ 3.0s | Vue 3 lazy loading + code splitting |
| 資料庫查詢驗證（EXPLAIN ANALYZE） | 所有新增查詢須附 query plan；>10K row 禁止全表掃描 |
| 記憶體消耗不退化 > 5% | 效能基準測試對比 |
| Hot path 變更附效能基準報告 | PR 描述中強制要求 |

**Constitution Check 結論**: 四項核心原則全部通過，無需違規豁免。

## Project Structure

### Documentation (this feature)

```text
specs/002-work-reporting-system/
├── plan.md              # 本文件（/speckit.plan 產出）
├── research.md          # Phase 0 產出（/speckit.plan）
├── data-model.md        # Phase 1 產出（/speckit.plan）
├── quickstart.md        # Phase 1 產出（/speckit.plan）
├── contracts/           # Phase 1 產出（/speckit.plan）
└── tasks.md             # Phase 2 產出（/speckit.tasks — 非本指令建立）
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/workreport/
│   ├── config/          # Spring Security、CORS、審計設定
│   ├── controller/      # REST API 控制器（按角色 / 領域分模組）
│   ├── dto/             # 請求 / 回應 DTO
│   ├── entity/          # JPA Entity（Project, Task, WorkEntry, User, Department, HoursRequest, AuditLog）
│   ├── enums/           # 列舉型別（ProjectStatus, TaskStatus, Role, HoursRequestStatus 等）
│   ├── exception/       # 全域例外處理 + 自訂例外
│   ├── repository/      # Spring Data JPA Repository
│   ├── security/        # JWT 驗證、密碼策略、帳號鎖定
│   └── service/         # 業務邏輯層
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/    # Flyway 資料庫遷移腳本
├── src/test/java/com/workreport/
│   ├── unit/            # 單元測試
│   ├── integration/     # 整合測試（Testcontainers）
│   └── contract/        # API 合約測試
├── build.gradle         # Gradle 建構檔
└── Dockerfile

frontend/
├── src/
│   ├── api/             # Axios HTTP client + API 封裝
│   ├── assets/          # 靜態資源
│   ├── components/      # 共用 UI 元件
│   ├── composables/     # Vue Composables（共用邏輯）
│   ├── layouts/         # 頁面佈局元件
│   ├── pages/           # 頁面元件（按角色 / 功能分區）
│   ├── router/          # Vue Router 路由設定 + 角色守衛
│   ├── stores/          # Pinia 狀態管理
│   ├── types/           # TypeScript 型別定義
│   └── utils/           # 工具函式
├── tests/
│   ├── unit/            # Vitest 單元測試
│   └── e2e/             # Playwright E2E 測試
├── package.json
├── vite.config.ts
└── Dockerfile
```

**Structure Decision**: 採用前後端分離架構（Option 2）。後端為 Spring Boot REST API 專案，前端為 Vue 3 + TypeScript SPA。兩者獨立部署，透過 REST API 通訊。

## Complexity Tracking

> 四項 Constitution 原則全數通過，無違規項目需追蹤。
