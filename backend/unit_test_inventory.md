# Unit Test Inventory（覆蓋率 90% 目標）

JaCoCo 納入覆蓋的範圍：`service/**`、`exception/**`、`entity/**`、`enums/**`、`util/**`、`dto/auth|user|workentry|project|hoursrequest|notification`（排除 controller、config、security、repository、dto/department|task|common、WorkReportApplication）。

## 已有對應單元測試的檔案

| 狀態 | 被測類 | 測試檔 |
|------|--------|--------|
| ✅ | `exception.GlobalExceptionHandler` | `unit/exception/GlobalExceptionHandlerTest.java` |
| ✅ | `util.WorkDayUtils` | `util/WorkDayUtilsTest.java` |
| ✅ | `service.AuthService` | `unit/service/AuthServiceTest.java` |
| ✅ | `service.WorkEntryService` | `unit/service/WorkEntryServiceTest.java` |
| ✅ | `service.ProjectService` | `unit/service/ProjectServiceTest.java` |
| ✅ | `service.HoursRequestService` | `unit/service/HoursRequestServiceTest.java` |
| ✅ | `service.DeptOverviewService` | `unit/service/DeptOverviewServiceTest.java` |
| ✅ | `service.HoursRequestReviewService` | `unit/service/HoursRequestReviewServiceTest.java` |
| ✅ | `service.ExecutorTaskService` | `unit/service/ExecutorTaskServiceTest.java` |
| ✅ | `service.AuditLogService` | `unit/service/AuditLogServiceTest.java` |
| ✅ | `service.TaskService` | `unit/service/TaskServiceTest.java` |
| ✅ | `service.NotificationService` | `unit/service/NotificationServiceTest.java` |
| ✅ | `service.DepartmentService` | `unit/service/DepartmentServiceTest.java` |
| ✅ | `service.PmProjectService` | `unit/service/PmProjectServiceTest.java` |
| ✅ | `service.UserService` | `unit/service/UserServiceTest.java` |

## Todo：尚無專屬單元測試（需補齊以達 90%）

- [x] **1** `service.DeptOverviewService` → `unit/service/DeptOverviewServiceTest.java`
- [x] **2** `service.HoursRequestService` → `unit/service/HoursRequestServiceTest.java`
- [x] **3** `service.HoursRequestReviewService` → `unit/service/HoursRequestReviewServiceTest.java`
- [x] **4** `service.ExecutorTaskService` → `unit/service/ExecutorTaskServiceTest.java`
- [x] **5** `service.AuditLogService` → `unit/service/AuditLogServiceTest.java`
- [x] **6** `service.NotificationService` → `unit/service/NotificationServiceTest.java`
- [x] **7** `service.DepartmentService` → `unit/service/DepartmentServiceTest.java`
- [x] **8** `service.PmProjectService` → `unit/service/PmProjectServiceTest.java`
- [x] **9** `service.TaskService` → `unit/service/TaskServiceTest.java`
- [x] **10** `service.UserService` → `unit/service/UserServiceTest.java`

## 說明

- 外部依賴請用 **Mock**（Repository、其他 Service 等），不啟動 Spring。
- 每完成一個測試類且 `./gradlew test --tests '...'` 通過，請將上表對應項改為 `[x]`。
- 目標：整體 `./gradlew test jacocoTestReport jacocoTestCoverageVerification` 通過（覆蓋率 ≥ 90%）。
