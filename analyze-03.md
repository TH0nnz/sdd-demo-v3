# Specification Analysis Report (Post-Fix)

> Updated: 2026-02-24
> Status: All previously listed findings have been addressed.

| ID | Category | Severity | Status | Resolution |
|----|----------|----------|--------|------------|
| C1 | Constitution Alignment | CRITICAL | Resolved | `T120` 已完成並新增報告 `frontend/tests/accessibility/wcag-aa-report.md`。 |
| I1 | Inconsistency | HIGH | Resolved | `FR-013` 已改為「本版本不處理國定假日排除」，與 Assumptions 對齊。 |
| I2 | Inconsistency | HIGH | Resolved | `plan.md` 的 Performance Goals 已補「一般頁面回應 p95 <= 500ms」。 |
| U1 | Underspecification | MEDIUM | Resolved | `SC-005` 已補驗證方法、樣本數（N >= 20）與驗證依據。 |
| C2 | Coverage Gap | MEDIUM | Resolved | 已新增 `T126`/`T127` 與對應可用性驗證文件。 |
| D1 | Duplication | LOW | Resolved | `FR-014` 已改為聚焦狀態終態語意，工時凍結規則引用 `FR-012`。 |

## Coverage Summary Table

| Requirement Key | Has Task? | Task IDs | Notes |
|-----------------|-----------|----------|-------|
| `fr-001` | Yes | T085, T087, T089 | 管理層專案管理 |
| `fr-002` | Yes | T085, T087 | 指派 PM |
| `fr-003` | Yes | T082, T087 | 已修正為工時紀錄判斷 |
| `fr-004` | Yes | T083, T088, T090 | 審核流程 |
| `fr-005` | Yes | T063, T069, T072 | PM task CRUD |
| `fr-006` | Yes | T069, T079, T081 | 指派單一執行人員 |
| `fr-007` | Yes | T071, T078 | PM 儀表板 |
| `fr-008` | Yes | T052, T114, T115 | task 歸零通知 |
| `fr-009` | Yes | T064, T070, T088 | target type 機制 |
| `fr-010` | Yes | T104, T107, T108, T109, T110, T111 | 部門主管唯讀與明細 |
| `fr-011` | Yes | T047, T052, T054, T060, T061 | 填報工時 |
| `fr-012` | Yes | T047, T052 | 終態拒絕填報 |
| `fr-013` | Yes | T031, T032, T047, T052 | 三工作天限制（已明確不處理國定假日排除） |
| `fr-014` | Yes | T053, T055, T062 | 手動完成 task |
| `fr-015` | Yes | T096, T098, T099, T100, T101, T102, T103 | HR 管理 |
| `fr-016` | Yes | T024, T042, T116 | RBAC |
| `fr-017` | Yes | T014, T024, T041 | 多角色聯集權限 |
| `fr-018` | Yes | T033, T035, T036, T044, T045 | email 登入識別 |
| `fr-019` | Yes | T035, T037 | 鎖定策略 |
| `fr-020` | Yes | T052, T053, T069 | 終態不可逆 |
| `fr-021` | Yes | T017, T052 | WorkEntry 永久保留 |
| `fr-022` | Yes | T052, T054 | 即時生效 |
| `fr-023` | Yes | T034, T035, T045 | 密碼複雜度 |
| `fr-024` | Yes | T029, T069, T088, T099 | 稽核日誌 |
| `fr-025` | Yes | T082, T087 | 關閉前非終態檢查 |
| `fr-026` | Yes | T096, T099 | 停用轉未指派與通知 |

## Constitution Alignment Issues

- No active constitution violations.
- `T120` 已補齊，WCAG 掃描報告已附。
- `Coverage >= 80%` 建議在 CI pipeline 持續驗證並出具可追溯報告。

## Unmapped Tasks

- 無明顯 unmapped tasks（T001-T125 均可映射到需求或基礎設施）。

## Metrics

- Total Requirements: `26`
- Total Tasks: `125`
- Coverage % (requirements with >=1 task): `100%` (26/26)
- Ambiguity Count: `0` (resolved)
- Duplication Count: `0` (resolved)
- Critical Issues Count: `0` (resolved)

## Next Actions

1. 可執行一次 `speckit.analyze` 再驗證，確認報告與現況一致。
2. 將可用性與無障礙驗證文件納入 CI artifact。
3. 建議補一版變更摘要 commit，標記本次「spec alignment fixes」。
