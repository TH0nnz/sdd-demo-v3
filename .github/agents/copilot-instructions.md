# sdd-demo-v3 Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-02-23

## Active Technologies

- Java 24 (JDK 24) + Spring Boot 4.0.2, Spring Security 7.0.2, Spring Data JPA 4.0, Vue.js 3 (前端) (002-work-reporting-system)

## Project Structure

```text
backend/
frontend/
tests/
```

## Commands

# Add commands for Java 24 (JDK 24)

## Code Style

Java 24 (JDK 24): Follow standard conventions

## Recent Changes

- 002-work-reporting-system: Added Java 24 (JDK 24) + Spring Boot 4.0.2, Spring Security 7.0.2, Spring Data JPA 4.0, Vue.js 3 (前端)

<!-- MANUAL ADDITIONS START -->
## Commit 錯誤處理規則

每當使用 `report_progress` 或執行 `git commit` 時，若遇到錯誤，請依下列步驟排除後再重試：

| 錯誤類型 | 排除方式 |
|----------|----------|
| Lint／格式錯誤 | 執行 `eslint --fix` 或 `prettier --write` 等對應格式化指令修正後重新 commit |
| 測試失敗 | 閱讀測試輸出，修正導致失敗的程式碼，確認測試通過後重新 commit |
| Merge conflict | `git status` 確認衝突檔案 → 手動解決衝突 → `git add <files>` → 重新 commit |
| Nothing to commit | 確認修改已正確寫入磁碟；若確實無變更，在報告中說明原因 |
| 其他錯誤 | 閱讀完整錯誤訊息，依錯誤類型採取對應修正，排除後再重試 |

**原則**：每次排除錯誤後，重新驗證所有變更，並再次嘗試 commit，直到成功為止。
<!-- MANUAL ADDITIONS END -->
