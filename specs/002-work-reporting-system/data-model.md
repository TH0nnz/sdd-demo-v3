# Data Model: 報工系統

**Feature**: 002-work-reporting-system
**Date**: 2026-02-23

---

## Entity Relationship Overview

```
Department 1──* User
User *──* Role (via user_roles)
User 1──* WorkEntry
User 1──* Notification
User(PM) 1──* Project
Project 1──* Task
Project 1──* HoursRequest
Task 1──* WorkEntry
Task 0..1── User(Executor, nullable)
HoursRequest 0..1── Task (nullable, 補至 task 時填入)
AuditLog (independent, append-only)
```

---

## Entities

### 1. Department（部門）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 主鍵 |
| name | VARCHAR(100) | NOT NULL, UNIQUE | 部門名稱 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | NOT NULL | 最後更新時間 |

**驗證規則**:
- `name` 不可為空、不可重複

---

### 2. User（使用者）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 主鍵 |
| name | VARCHAR(50) | NOT NULL | 姓名 |
| email | VARCHAR(255) | NOT NULL, UNIQUE | 公司 email，作為登入識別碼 |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt 雜湊密碼 |
| password_changed | BOOLEAN | NOT NULL, DEFAULT false | 初始密碼是否已變更 |
| department_id | BIGINT | FK → Department.id, NOT NULL | 所屬部門 |
| active | BOOLEAN | NOT NULL, DEFAULT true | 帳號狀態（啟用/停用） |
| failed_login_count | INT | NOT NULL, DEFAULT 0 | 連續登入失敗次數 |
| last_failed_login | TIMESTAMP | NULLABLE | 最後一次登入失敗時間 |
| locked_until | TIMESTAMP | NULLABLE | 帳號鎖定截止時間 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | NOT NULL | 最後更新時間 |
| created_by | BIGINT | NULLABLE, FK → User.id | 建立者 |
| updated_by | BIGINT | NULLABLE, FK → User.id | 最後更新者 |

**驗證規則**:
- `email` 符合 email 格式，全系統唯一
- `name` 不可為空，最長 50 字元
- 密碼複雜度：至少 8 字元，包含大寫、小寫、數字 → `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$`
- 初始密碼統一為 `Welcome123`（BCrypt 雜湊後儲存）

**索引**:
- `UNIQUE INDEX idx_user_email ON user(email)`

---

### 3. UserRole（使用者角色，多對多關聯表）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| user_id | BIGINT | PK, FK → User.id | 使用者 |
| role | VARCHAR(20) | PK | 角色代碼 |

**角色列舉值**:
- `ADMIN` — 管理層
- `PM` — 專案經理
- `DEPT_MANAGER` — 部門主管
- `EXECUTOR` — 執行人員
- `HR` — 人力資源

**規則**: 每位使用者至少一個角色，可同時擁有多個角色

---

### 4. Project（專案）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 主鍵 |
| name | VARCHAR(200) | NOT NULL | 專案名稱 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | 狀態 |
| total_budget_hours | DECIMAL(10,1) | NOT NULL | 總時數預算 |
| consumed_hours | DECIMAL(10,1) | NOT NULL, DEFAULT 0 | 已消耗時數（計算用快取） |
| pm_id | BIGINT | FK → User.id, NOT NULL | 負責 PM |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | NOT NULL | 最後更新時間 |
| closed_at | TIMESTAMP | NULLABLE | 關閉時間 |
| created_by | BIGINT | FK → User.id | 建立者 |
| updated_by | BIGINT | FK → User.id | 最後更新者 |
| version | BIGINT | NOT NULL, DEFAULT 0 | 樂觀鎖定版本 |

**狀態列舉與轉換**:
```
ACTIVE → CLOSED（僅當所有 Task 為終態）
ACTIVE → DELETED（僅當無任何 WorkEntry）
CLOSED ✕（終態，不可回退）
DELETED ✕（終態，不可回退）
```

**驗證規則**:
- `total_budget_hours` > 0
- 刪除：僅無工時紀錄的專案可刪除（軟刪除，狀態設為 DELETED）
- 關閉：所有 Task 必須為終態（COMPLETED 或 CLOSED）

---

### 5. Task（工作項目）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 主鍵 |
| name | VARCHAR(200) | NOT NULL | Task 名稱 |
| project_id | BIGINT | FK → Project.id, NOT NULL | 所屬專案 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | 狀態 |
| budget_hours | DECIMAL(10,1) | NOT NULL | 配額時數 |
| consumed_hours | DECIMAL(10,1) | NOT NULL, DEFAULT 0 | 已消耗時數 |
| assignee_id | BIGINT | FK → User.id, NULLABLE | 指派的執行人員（NULLABLE = 未指派） |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | NOT NULL | 最後更新時間 |
| created_by | BIGINT | FK → User.id | 建立者 |
| updated_by | BIGINT | FK → User.id | 最後更新者 |
| version | BIGINT | NOT NULL, DEFAULT 0 | 樂觀鎖定版本 |

**狀態列舉與轉換**:
```
PENDING（待開始）→ IN_PROGRESS（填報工時時自動轉換）
PENDING → CLOSED（PM 強制關閉）
IN_PROGRESS → COMPLETED（執行人員手動完成）
IN_PROGRESS → CLOSED（PM 強制關閉）
COMPLETED ✕（終態，不可回退）
CLOSED ✕（終態，不可回退）
```

**驗證規則**:
- `budget_hours` > 0
- 終態 Task 不接受任何工時填報（新增或修改）
- 時數歸零（`consumed_hours >= budget_hours`）時拒絕填報，提示通知 PM
- 指派的執行人員離職或移出專案時 → `assignee_id = NULL`, `status` 不變但凍結填報

**索引**:
- `INDEX idx_task_project_status ON task(project_id, status)`

**衍生欄位**（非儲存，API 回傳時計算）:
- `remaining_hours = budget_hours - consumed_hours`

---

### 6. WorkEntry（工時填報）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 主鍵 |
| user_id | BIGINT | FK → User.id, NOT NULL | 填報人員 |
| task_id | BIGINT | FK → Task.id, NOT NULL | 對應 Task |
| work_date | DATE | NOT NULL | 工作日期 |
| hours | DECIMAL(3,1) | NOT NULL | 填報小時數 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | NOT NULL | 最後更新時間 |

**驗證規則**:
- `hours` 必須為 0.5 的倍數，範圍 0.5 ~ 24.0
- 同一使用者同一天的所有 WorkEntry 累計不得超過 24 小時
- 只能新增或修改「過去三工作天」以內的紀錄（含當天）
- Task 為終態時拒絕新增或修改
- Task 時數已用盡時拒絕新增
- **永久保留，不得刪除**

**索引**:
- `INDEX idx_workentry_task_date ON work_entry(task_id, work_date)`
- `INDEX idx_workentry_user_date ON work_entry(user_id, work_date)`

---

### 7. HoursRequest（時數增補申請）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 主鍵 |
| project_id | BIGINT | FK → Project.id, NOT NULL | 所屬專案 |
| requester_id | BIGINT | FK → User.id, NOT NULL | 申請人（PM） |
| requested_hours | DECIMAL(10,1) | NOT NULL | 申請時數 |
| description | TEXT | NOT NULL | 申請說明 |
| target_type | VARCHAR(20) | NOT NULL | 補充目標層級 |
| target_task_id | BIGINT | FK → Task.id, NULLABLE | 目標 Task（補至 task 時填入） |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | 狀態 |
| reviewer_id | BIGINT | FK → User.id, NULLABLE | 審核人 |
| review_comment | TEXT | NULLABLE | 審核意見 |
| reviewed_at | TIMESTAMP | NULLABLE | 審核時間 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |
| updated_at | TIMESTAMP | NOT NULL | 最後更新時間 |

**target_type 列舉**:
- `TASK` — 補至指定 Task
- `PROJECT` — 補回專案總預算池

**狀態列舉與轉換**:
```
PENDING（待審核）→ APPROVED（已核准）
PENDING → REJECTED（已拒絕）
APPROVED ✕（終態）
REJECTED ✕（終態）
```

**驗證規則**:
- `requested_hours` > 0
- `target_type = TASK` 時 `target_task_id` 必填
- 核准時：`target_type = TASK` → Task.budget_hours += requested_hours，Project.total_budget_hours += requested_hours
- 核准時：`target_type = PROJECT` → Project.total_budget_hours += requested_hours
- 拒絕時：`review_comment` 必填

**索引**:
- `INDEX idx_hoursreq_project_status ON hours_request(project_id, status)`

---

### 8. AuditLog（稽核日誌）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 主鍵 |
| action_type | VARCHAR(50) | NOT NULL | 操作類型 |
| actor_id | BIGINT | FK → User.id, NOT NULL | 操作人員 |
| target_entity | VARCHAR(50) | NOT NULL | 受影響實體類型 |
| target_id | BIGINT | NOT NULL | 受影響實體 ID |
| summary | TEXT | NOT NULL | 內容摘要 |
| created_at | TIMESTAMP | NOT NULL | 操作時間 |

**action_type 列舉**:
- `ROLE_CHANGE` — 使用者角色變更
- `ACCOUNT_ACTIVATE` — 帳號啟用
- `ACCOUNT_DEACTIVATE` — 帳號停用
- `HOURS_REQUEST_APPROVED` — 時數申請核准
- `HOURS_REQUEST_REJECTED` — 時數申請拒絕
- `TASK_FORCE_CLOSED` — PM 強制關閉 Task

**規則**:
- **Append-only，永久保留，不得刪除或修改**
- Entity 不提供 UPDATE / DELETE 操作
- Repository 僅公開 `save()` 與 `find*()` 方法

**索引**:
- `INDEX idx_auditlog_created_at ON audit_log(created_at)`

---

### 9. Notification（站內通知）

| 欄位 | 型別 | 約束 | 說明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO | 主鍵 |
| user_id | BIGINT | FK → User.id, NOT NULL | 通知對象 |
| type | VARCHAR(50) | NOT NULL | 通知類型 |
| title | VARCHAR(200) | NOT NULL | 通知標題 |
| content | TEXT | NOT NULL | 通知內容 |
| is_read | BOOLEAN | NOT NULL, DEFAULT false | 是否已讀 |
| created_at | TIMESTAMP | NOT NULL | 建立時間 |

**type 列舉**:
- `TASK_HOURS_EXHAUSTED` — Task 時數歸零
- `HOURS_REQUEST_SUBMITTED` — 收到時數申請（通知管理層）
- `HOURS_REQUEST_APPROVED` — 時數申請已核准（通知 PM）
- `HOURS_REQUEST_REJECTED` — 時數申請已拒絕（通知 PM）
- `TASK_COMPLETED` — Task 標記完成（通知 PM）
- `TASK_UNASSIGNED` — Task 轉為未指派（通知 PM）

**索引**:
- `INDEX idx_notification_user_read ON notification(user_id, is_read)`

---

## Relationship Summary

| 關係 | 型態 | 說明 |
|------|------|------|
| Department → User | 1:N | 一個部門有多位使用者 |
| User → UserRole | 1:N | 一位使用者可有多個角色 |
| User(PM) → Project | 1:N | 一位 PM 可負責多個專案 |
| Project → Task | 1:N | 一個專案有多個 Task |
| Task → User(Executor) | N:1 | 一個 Task 指派給一位執行人員（可為 NULL） |
| Task → WorkEntry | 1:N | 一個 Task 有多筆工時紀錄 |
| User → WorkEntry | 1:N | 一位使用者有多筆工時紀錄 |
| Project → HoursRequest | 1:N | 一個專案有多筆時數申請 |
| HoursRequest → Task | N:1 | 申請可指定目標 Task（NULLABLE） |
| User → Notification | 1:N | 一位使用者有多則通知 |
| User → AuditLog | 1:N | 一位使用者可產生多筆稽核日誌 |
