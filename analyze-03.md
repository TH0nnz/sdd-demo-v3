# Specification Analysis Report

| ID | Category | Severity | Location(s) | Summary | Recommendation |
|----|----------|----------|-------------|---------|----------------|
| C1 | Constitution Alignment | CRITICAL | `specs/002-work-reporting-system/tasks.md:286`, `.specify/memory/constitution.md` | `T120` 無障礙掃描被標記為 `skipped`，但 Constitution III 與 Quality Gate 5 要求 WCAG 2.1 AA 與可近用性掃描必須通過。 | 補回 `T120` 實作並新增可驗證輸出（掃描報告、CI gate）；未完成前不應視為完成。 |
| I1 | Inconsistency | HIGH | `specs/002-work-reporting-system/spec.md:121`, `specs/002-work-reporting-system/spec.md:181` | `FR-013` 寫「不含公假」，但 Assumption 又寫「本版本不處理國定假日排除」，規格自相矛盾。 | 二選一明確化：1) 明確不支援公假；或 2) 補公假日曆規則與任務。 |
| I2 | Inconsistency | HIGH | `specs/002-work-reporting-system/spec.md:157`, `specs/002-work-reporting-system/plan.md:18` | spec 成功標準寫一般頁面 `p95 <= 500ms`，plan 寫 API `p95 <= 200ms`；目標層級與門檻未對齊。 | 明確拆分成「API SLO」與「UI SLO」，並在 spec/plan 同步。 |
| U1 | Underspecification | MEDIUM | `specs/002-work-reporting-system/spec.md:154` | `SC-005`（95% 首次嘗試成功）缺少量測方法、樣本、驗收程序，難以驗證。 | 補充：量測方式（可用性測試/事件追蹤）、樣本數、通過門檻。 |
| C2 | Coverage Gap | MEDIUM | `specs/002-work-reporting-system/spec.md:150-157`, `specs/002-work-reporting-system/tasks.md` | 多個成功準則（如 `SC-001`, `SC-005`）沒有明確對應任務或驗證步驟。 | 在 Phase 9 加入可驗證任務（可用性測試、操作時間量測）。 |
| D1 | Duplication | LOW | `specs/002-work-reporting-system/spec.md:120`, `specs/002-work-reporting-system/spec.md:122` | `FR-012` 與 `FR-014` 都描述終態 task 禁止工時新增/修改，語意有重疊。 | 保留一條主規則，另一條改成「手動完成」專屬行為描述，避免重複。 |

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
| `fr-013` | Yes | T031, T032, T047, T052 | 三工作天限制（但公假規則矛盾） |
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

- `CRITICAL`: 可近用性掃描被標示 skipped（`T120`）與 Constitution 衝突。
- 其餘原則：有對應任務，但 `Coverage >= 80%` 仍需 CI 報告證據。

## Unmapped Tasks

- 無明顯 unmapped tasks（T001-T125 均可映射到需求或基礎設施）。

## Metrics

- Total Requirements: `26`
- Total Tasks: `125`
- Coverage % (requirements with >=1 task): `100%` (26/26)
- Ambiguity Count: `1`
- Duplication Count: `1`
- Critical Issues Count: `1`

## Next Actions

- 有 `CRITICAL`（C1），建議先修復再進 `/speckit.implement` 收尾。
- 建議順序：
1. 補完 `T120`（WCAG 掃描與報告）
2. 解決 FR-013 公假規則矛盾（I1）
3. 對齊效能門檻描述（I2）
4. 補 `SC-001` / `SC-005` 可驗證任務（C2, U1）
5. 清理 FR 重複語義（D1）
