# Specification Analysis Report — 報工系統

**Date**: 2026-02-24  
**Artifacts**: spec.md, plan.md, tasks.md  
**Constitution**: `.specify/memory/constitution.md` v1.0.0  

---

## Findings

| ID | Category | Severity | Location(s) | Summary | Recommendation |
|----|----------|----------|-------------|---------|----------------|
| C1 | Coverage Gap | CRITICAL | spec.md FR-025, ProjectService.java | `closeProject()` 未檢查非終態 Task（FR-025 要求「請先關閉所有進行中的 task 後再關閉專案」）；目前實作僅檢查 `status == CLOSED` 即直接關閉 | 在 `closeProject()` 加入 `taskRepository.countByProjectIdAndStatusNotIn(projectId, List.of(COMPLETED, CLOSED))` 檢查 |
| C2 | Coverage Gap | CRITICAL | spec.md FR-026, UserService.java | `disableUser()` 未將該使用者的非終態 Task 轉為「未指派」並凍結工時填報（FR-026 要求停用帳號時自動處理）；目前僅設 `active=false` | 在 `disableUser()` 加入：查詢該使用者所有非終態 Task → 設 assignee=null → 通知對應 PM |
| C3 | Coverage Gap | CRITICAL | spec.md FR-003, ProjectService.java | `deleteProject()` 檢查的是「有無 Task」而非「有無工時紀錄」（FR-003 要求「已有工時紀錄的專案不能刪除」）；無 Task 但無工時紀錄的專案應可刪除，有 Task 但無工時紀錄的專案也應可刪除 | 改為檢查 `workEntryRepository.countByTask_ProjectId(projectId) > 0` |
| C4 | Constitution | CRITICAL | constitution II, tasks.md | Constitution 要求 E2E 測試覆蓋所有 P1 user story 驗收情境（Playwright），但 tasks.md 無任何 Playwright E2E 測試任務；`frontend/tests/e2e/` 目錄不存在 | 新增 E2E 測試任務覆蓋 US1 的 4 個驗收情境 |
| C5 | Constitution | CRITICAL | constitution II, frontend/ | Constitution 要求「單元測試覆蓋率 ≥ 80%」，但前端完全無任何測試檔案（Vitest 單元測試、Vue Test Utils）；`frontend/tests/` 目錄不存在 | 新增前端單元測試任務：至少覆蓋 stores、utils、關鍵 composables |
| H1 | Inconsistency | HIGH | spec.md FR-003 vs ProjectService.java | spec 中「刪除」條件為「已有工時紀錄」，但 plan/tasks 中描述為「刪除前檢查工時紀錄」(T082)，而實作卻檢查「Task 數量」——三者用語不一致 | 統一為 FR-003 的定義：以 WorkEntry 記錄為判斷依據 |
| H2 | Underspecification | HIGH | spec.md US5, DeptOverviewService.java, MemberSummaryDto.java | spec 要求「本週、本月已填報小時數」，但實作 DTO 只有 `totalHoursThisMonth` 和 `todayHours`，缺少「本週」(thisWeek) 統計欄位 | DTO 加入 `totalHoursThisWeek` 欄位；Service 加入本週計算邏輯 |
| H3 | Coverage Gap | HIGH | spec.md US5 Scenario 2, tasks.md T108 | spec 要求部門主管「點擊某位執行人員 → 查看其詳情 → 顯示該人員所有進行中與已完成 task 的清單」；但 Controller 僅有 `/api/dept/overview`，缺少 `/api/department/members/{userId}/tasks` 端點 | 在 DeptOverviewController 新增成員 Task 清單端點 |
| H4 | Coverage Gap | HIGH | tasks.md T111, frontend/ | T111 要求建立 `MemberTaskDetail.vue` 元件，但該檔案不存在（從未被建立） | 建立 MemberTaskDetail.vue 元件並整合至部門總覽頁面 |
| H5 | Inconsistency | HIGH | spec.md FR-024, AuditActionType.java, UserService.java | FR-024 要求「使用者角色變更」觸發 AuditLog（ROLE_CHANGE），但 UserService.updateUser() 未呼叫 AuditLogService 記錄角色變更 | 在 `updateUser()` 中偵測角色變更並記錄 AuditLog |
| H6 | Inconsistency | HIGH | spec.md FR-024, UserService.java | FR-024 要求「帳號啟停」觸發 AuditLog（ACCOUNT_ACTIVATE/DEACTIVATE），但 `disableUser()` 和 `enableUser()` 均未呼叫 AuditLogService | 在 disable/enable 方法中加入 AuditLog 記錄 |
| H7 | Duplication | HIGH | tasks.md T090 vs T073 / HoursRequestController vs HoursRequestReviewController | T090 描述「擴充 HoursRequestController — approve/reject」，但實作建立了另一個 `HoursRequestReviewController`（`/api/admin/hours-requests`）。API 路徑與 tasks.md 描述不一致 | 統一命名，更新 tasks.md 描述；或合併為單一 Controller |
| M1 | Underspecification | MEDIUM | spec.md, plan.md | spec 提及「管理層關閉專案」但未明確定義 ProjectStatus 是否有 `DELETED` 狀態；plan 中 ProjectStatus 列舉未列出所有值 | 確認列舉值清單並在 spec 中補充 |
| M2 | Terminology | MEDIUM | spec.md vs code | spec 使用「總時數預算」/「可用時數」，plan.md 用 `initialBudgetHours`/`currentBudgetHours`，但 Entity 欄位為 `totalBudgetHours`/`consumedHours` — 三處命名不一致 | 選定一組術語並統一至 spec、plan、code |
| M3 | Terminology | MEDIUM | spec.md vs code | spec 使用「登入識別碼為公司電子郵件地址」 → code 中 LoginRequest 使用 `email` 欄位名稱（一致），但 User entity 無 `username` 欄位；部分地方（如 conversation 記錄）提到 `findByUsername` 但實際為 `findByEmail` | 確認不存在殘留的 `username` 引用 |
| M4 | Coverage Gap | MEDIUM | spec.md Edge Case, WorkEntryService.java | spec Edge Case 提及「Task 時數歸零後，執行人員仍嘗試填報工時：系統拒絕填報並顯示 task 時數已用盡」；需確認 WorkEntryService 是否有此檢查邏輯 | 驗證 `createWorkEntry()` 中是否有 `remainingHours <= 0` 的前置檢查 |
| M5 | Coverage Gap | MEDIUM | spec.md SC-008, plan.md | SC-008 要求「100 名使用者同時操作，p95 ≤ 500ms」，但 tasks.md 無負載測試任務 | 考慮新增 JMeter/Gatling 簡易負載測試（非必要但建議） |
| M6 | Inconsistency | MEDIUM | tasks.md T092 vs code | T092 描述為 `frontend/src/api/hours-request-review.ts`，但實際檔案建立為 `frontend/src/api/admin-hours-requests.ts` — 檔案名稱不一致 | 更新 tasks.md 描述或統一檔案命名 |
| M7 | Coverage Gap | MEDIUM | tasks.md T104 vs code | T104 描述建立 `DepartmentServiceTest.java`，但實際檔案為 `DeptOverviewServiceTest.java` — 類別名稱不一致 | 統一命名 |
| M8 | Coverage Gap | MEDIUM | tasks.md T107 vs code | T107 描述實作 `DepartmentService.java`，但實際檔案為 `DeptOverviewService.java` — 類別名稱不一致 | 統一命名 |
| M9 | Coverage Gap | MEDIUM | tasks.md T108 vs code | T108 描述建立 `DepartmentController`，但實際檔案為 `DeptOverviewController.java`，且路徑為 `/api/dept` 而非 `/api/department` | 統一命名與路徑 |
| M10 | Coverage Gap | MEDIUM | tasks.md T109 vs code | T109 描述建立 `frontend/src/api/department.ts`，但實際檔案為 `frontend/src/api/dept.ts` | 統一命名 |
| L1 | Style | LOW | tasks.md T006 | T006 描述建立 `frontend/.eslintrc.cjs`，但前端實際使用 `eslint.config.js`（Flat Config 格式）— 不影響功能但描述過時 | 更新 tasks.md 描述 |
| L2 | Style | LOW | tasks.md T120 | T120 標記為 `[x]` 完成但實際被跳過（「留待未來處理」）；應標記為 skip 而非 complete | 明確標記為 skipped 或保留 `[ ]` 並加註 |

---

## Coverage Summary Table

| Requirement Key | Has Task? | Task IDs | Notes |
|-----------------|-----------|----------|-------|
| FR-001 (管理層 CRUD 專案) | ✅ | T085-T089 | — |
| FR-002 (指派 PM) | ✅ | T087 | — |
| FR-003 (有工時紀錄不可刪除) | ⚠️ | T087 | 實作檢查 Task 數量而非 WorkEntry 數量 (C3) |
| FR-004 (審核增補申請) | ✅ | T088, T090 | — |
| FR-005 (PM CRUD Task) | ✅ | T069, T072 | — |
| FR-006 (Task 指派執行人員) | ✅ | T069 | — |
| FR-007 (PM 儀表板 ≤5min) | ✅ | T071, T078 | — |
| FR-008 (Task 歸零通知 PM) | ✅ | T052 | — |
| FR-009 (增補申請 target_type) | ✅ | T070, T088 | — |
| FR-010 (部門主管唯讀) | ⚠️ | T107-T110 | 缺少成員 Task 清單端點 (H3) |
| FR-011 (執行人員填報) | ✅ | T052, T054 | — |
| FR-012 (終態拒絕填報) | ✅ | T052 | — |
| FR-013 (三工作天限制) | ✅ | T031, T052 | — |
| FR-014 (手動完成 Task) | ✅ | T053, T055 | — |
| FR-015 (HR 管理使用者) | ✅ | T099, T100 | — |
| FR-016 (RBAC 存取控制) | ✅ | T024, T116 | — |
| FR-017 (多角色合併權限) | ✅ | T014, T024 | — |
| FR-018 (Email 登入) | ✅ | T035, T036 | — |
| FR-019 (鎖定帳號) | ✅ | T035, T037 | — |
| FR-020 (終態不可逆) | ✅ | T052, T053 | — |
| FR-021 (WorkEntry 永久保留) | ✅ | T017 | Entity 無 delete 機制 |
| FR-022 (填報即時生效) | ✅ | T052 | — |
| FR-023 (密碼複雜度) | ✅ | T035, T045 | — |
| FR-024 (稽核日誌) | ⚠️ | T029, T069, T088 | UserService 缺少角色變更/帳號啟停的 AuditLog (H5, H6) |
| FR-025 (關閉專案檢查非終態 Task) | ❌ | T087 | 實作未實現此檢查 (C1) |
| FR-026 (停用帳號 → Task 未指派) | ❌ | T099 | 實作未實現此邏輯 (C2) |

---

## Constitution Alignment Issues

| Principle | Status | Detail |
|-----------|--------|--------|
| I. Code Quality | ✅ PASS | Checkstyle, SpotBugs, ESLint 已設定 |
| II. Testing Standards — TDD | ⚠️ PARTIAL | 後端測試結構完整，但前端零測試（C5）；E2E 測試完全缺失（C4） |
| II. Testing Standards — Coverage ≥ 80% | ⚠️ UNKNOWN | 後端有測試骨架但未驗證覆蓋率；前端 0% 覆蓋率 |
| III. UX Consistency | ✅ PASS | Element Plus 統一元件庫、三態已檢查（T119） |
| IV. Performance | ✅ PASS | 效能測試已包含（T117）；API p95 目標已明確 |

---

## Unmapped Tasks

所有 122 個 Task 均已映射至對應需求或基礎設施。無孤立任務。

---

## Metrics

| 指標 | 數值 |
|------|------|
| Total Requirements (FR) | 26 |
| Total Tasks | 122 |
| Coverage % (FR with ≥1 task) | 92.3% (24/26) |
| Ambiguity Count | 1 (M1) |
| Duplication Count | 1 (H7) |
| Critical Issues Count | **5** (C1-C5) |
| High Issues Count | 7 (H1-H7) |
| Medium Issues Count | 10 (M1-M10) |
| Low Issues Count | 2 (L1-L2) |
| Total Findings | 25 |

---

## Next Actions

### 🔴 CRITICAL — 建議在 deploy 前解決

1. **C1**: 修復 `ProjectService.closeProject()` — 加入非終態 Task 檢查（FR-025）
2. **C2**: 修復 `UserService.disableUser()` — 加入 Task 自動轉未指派 + 通知 PM（FR-026）
3. **C3**: 修復 `ProjectService.deleteProject()` — 改為檢查 WorkEntry 而非 Task 數量（FR-003）
4. **C4**: 新增 Playwright E2E 測試任務覆蓋 US1 驗收情境（Constitution II）
5. **C5**: 新增前端 Vitest 單元測試（Constitution II — coverage ≥ 80%）

### 🟡 HIGH — 建議盡快修復

6. **H2**: `DeptOverviewService` / `MemberSummaryDto` 加入「本週」工時統計
7. **H3+H4**: 新增成員 Task 清單 API 端點 + `MemberTaskDetail.vue` 元件
8. **H5+H6**: `UserService` 中角色變更 / 帳號啟停加入 AuditLog 記錄

### 🟢 建議命令

- 修復 C1-C3: 手動編輯 `ProjectService.java` 和 `UserService.java`
- 修復 C4-C5: 執行 `/speckit.tasks` 追加前端測試任務，或手動新增
- 修復 H2-H4: 編輯 `DeptOverviewService`、新增 API 端點與 Vue 元件
- 修復 H5-H6: 編輯 `UserService.java` 加入 AuditLogService 呼叫

---

## Remediation Offer

是否需要我針對上述 Top 5 CRITICAL 問題提供具體的修復程式碼建議？（不會自動套用，僅提供建議供您審閱。）
