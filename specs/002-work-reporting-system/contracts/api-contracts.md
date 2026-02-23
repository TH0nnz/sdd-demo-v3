# API Contracts: 報工系統

**Feature**: 002-work-reporting-system
**Date**: 2026-02-23
**Base URL**: `/api`

---

## 通用規範

### 認證
所有 API（除登入外）需攜帶 `Authorization: Bearer {JWT}` 標頭。
- 未認證 → `401 Unauthorized`
- 無權限 → `403 Forbidden`

### 錯誤回應格式
```json
{
  "timestamp": "2026-02-23T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "操作失敗的可讀描述",
  "path": "/api/..."
}
```

### 分頁回應格式
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5
}
```

### 時間格式
- 日期：`YYYY-MM-DD`（例如 `2026-02-23`）
- 日期時間：ISO 8601（例如 `2026-02-23T10:30:00+08:00`）

---

## 1. 認證 API（Auth）

### POST /api/auth/login

登入取得 JWT Token。

**Request Body**:
```json
{
  "email": "user@company.com",
  "password": "Welcome123"
}
```

**Response 200**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "name": "王小明",
    "email": "user@company.com",
    "roles": ["EXECUTOR"],
    "departmentId": 1,
    "departmentName": "研發部"
  },
  "forcePasswordChange": true
}
```

**Error Responses**:
- `401` — 帳號或密碼錯誤
- `423` — 帳號已鎖定，請 {minutes} 分鐘後再試
- `403` — 帳號已停用，請聯絡 HR

---

### POST /api/auth/change-password

變更密碼（首次登入強制改密碼 / 一般改密碼）。

**Request Body**:
```json
{
  "currentPassword": "Welcome123",
  "newPassword": "NewPass123"
}
```

**Response 200**:
```json
{
  "message": "密碼已成功變更"
}
```

**Error Responses**:
- `400` — 密碼不符合複雜度要求：至少 8 個字元，必須包含大寫字母、小寫字母與數字
- `401` — 當前密碼不正確

---

## 2. 專案管理 API（Projects）— 管理層

### GET /api/projects

取得專案列表。

**權限**: `ADMIN`
**Query Parameters**: `status` (可選, ACTIVE/CLOSED), `page`, `size`

**Response 200**:
```json
{
  "content": [
    {
      "id": 1,
      "name": "產品改版",
      "status": "ACTIVE",
      "totalBudgetHours": 200.0,
      "consumedHours": 45.5,
      "remainingHours": 154.5,
      "pmId": 2,
      "pmName": "李PM",
      "createdAt": "2026-02-20T09:00:00+08:00",
      "closedAt": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 5,
  "totalPages": 1
}
```

---

### POST /api/projects

建立新專案。

**權限**: `ADMIN`

**Request Body**:
```json
{
  "name": "產品改版",
  "totalBudgetHours": 200.0,
  "pmId": 2
}
```

**Response 201**:
```json
{
  "id": 1,
  "name": "產品改版",
  "status": "ACTIVE",
  "totalBudgetHours": 200.0,
  "consumedHours": 0.0,
  "remainingHours": 200.0,
  "pmId": 2,
  "pmName": "李PM",
  "createdAt": "2026-02-23T10:00:00+08:00"
}
```

**Error Responses**:
- `400` — 總時數預算必須大於 0
- `404` — 指定的 PM 不存在或無 PM 角色

---

### PUT /api/projects/{id}

修改專案資訊。

**權限**: `ADMIN`

**Request Body**:
```json
{
  "name": "產品改版 v2",
  "totalBudgetHours": 250.0,
  "pmId": 2
}
```

**Response 200**: 更新後的專案物件（同 POST 回傳格式）

**Error Responses**:
- `400` — 專案已關閉，無法修改
- `404` — 專案不存在

---

### POST /api/projects/{id}/close

關閉專案。

**權限**: `ADMIN`

**Response 200**:
```json
{
  "id": 1,
  "status": "CLOSED",
  "closedAt": "2026-02-23T15:00:00+08:00"
}
```

**Error Responses**:
- `409` — 請先關閉所有進行中的 task 後再關閉專案

---

### DELETE /api/projects/{id}

刪除專案（僅無工時紀錄的專案）。

**權限**: `ADMIN`

**Response 204**: No Content

**Error Responses**:
- `409` — 專案已有工時紀錄，請改為關閉

---

## 3. Task 管理 API（Tasks）— PM

### GET /api/projects/{projectId}/tasks

取得專案下所有 Task。

**權限**: `PM`（僅限負責的專案）、`ADMIN`

**Response 200**:
```json
{
  "content": [
    {
      "id": 1,
      "name": "前端登入頁切版",
      "projectId": 1,
      "projectName": "產品改版",
      "status": "IN_PROGRESS",
      "budgetHours": 20.0,
      "consumedHours": 8.5,
      "remainingHours": 11.5,
      "assigneeId": 3,
      "assigneeName": "張執行",
      "createdAt": "2026-02-21T10:00:00+08:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 8,
  "totalPages": 1
}
```

---

### POST /api/projects/{projectId}/tasks

建立新 Task。

**權限**: `PM`（僅限負責的專案）

**Request Body**:
```json
{
  "name": "前端登入頁切版",
  "budgetHours": 20.0,
  "assigneeId": 3
}
```

**Response 201**: Task 物件

**Error Responses**:
- `400` — 專案已關閉，無法建立新 task
- `400` — 配額時數必須大於 0
- `404` — 指定的執行人員不存在或無執行人員角色

---

### PUT /api/projects/{projectId}/tasks/{id}

修改 Task（名稱、時數、指派人員）。

**權限**: `PM`

**Request Body**:
```json
{
  "name": "前端登入頁切版（含忘記密碼）",
  "budgetHours": 25.0,
  "assigneeId": 3
}
```

**Response 200**: 更新後的 Task 物件

**Error Responses**:
- `400` — Task 已為終態，無法修改
- `409` — 資料已被他人修改，請重新整理後再試（樂觀鎖定衝突）

---

### POST /api/projects/{projectId}/tasks/{id}/close

PM 強制關閉 Task。

**權限**: `PM`

**Response 200**: 更新後的 Task 物件（status = CLOSED）

**Error Responses**:
- `400` — Task 已為終態

**Side Effects**:
- 產生 AuditLog（TASK_FORCE_CLOSED）
- 執行人員無法再對此 Task 填報工時

---

### DELETE /api/projects/{projectId}/tasks/{id}

刪除 Task。

**權限**: `PM`

**Response 204**: No Content

**Error Responses**:
- `409` — Task 已有工時紀錄，無法刪除

---

## 4. 工時填報 API（Work Entries）— 執行人員

### GET /api/work-entries

取得當前使用者的工時填報紀錄。

**權限**: `EXECUTOR`
**Query Parameters**: `startDate`, `endDate`, `taskId` (可選)

**Response 200**:
```json
{
  "content": [
    {
      "id": 1,
      "taskId": 1,
      "taskName": "前端登入頁切版",
      "projectName": "產品改版",
      "workDate": "2026-02-23",
      "hours": 2.0,
      "editable": true,
      "createdAt": "2026-02-23T17:00:00+08:00",
      "updatedAt": "2026-02-23T17:00:00+08:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 15,
  "totalPages": 1
}
```

---

### POST /api/work-entries

新增工時填報。

**權限**: `EXECUTOR`

**Request Body**:
```json
{
  "taskId": 1,
  "workDate": "2026-02-23",
  "hours": 2.0
}
```

**Response 201**:
```json
{
  "id": 1,
  "taskId": 1,
  "taskName": "前端登入頁切版",
  "workDate": "2026-02-23",
  "hours": 2.0,
  "taskRemainingHours": 9.5,
  "warning": null
}
```

**Response 201 (時數歸零警告)**:
```json
{
  "id": 2,
  "taskId": 1,
  "taskName": "前端登入頁切版",
  "workDate": "2026-02-23",
  "hours": 3.0,
  "taskRemainingHours": 0.0,
  "warning": "task 時數已用盡，已自動通知 PM 申請增補"
}
```

**Error Responses**:
- `400` — 工時必須為 0.5 的倍數
- `400` — 超過可編輯期限（三工作天）
- `400` — task 時數已用盡，請等待 PM 增補
- `400` — task 已結束，無法新增工時
- `400` — 當日累計工時不得超過 24 小時
- `403` — 此 task 未指派給您

---

### PUT /api/work-entries/{id}

修改工時填報紀錄。

**權限**: `EXECUTOR`（僅限自己的紀錄）

**Request Body**:
```json
{
  "hours": 3.0
}
```

**Response 200**: 更新後的 WorkEntry 物件

**Error Responses**:
- `400` — 超過可編輯期限（三工作天）
- `400` — task 已結束，無法修改工時
- `400` — 修改後當日累計工時不得超過 24 小時

---

## 5. 執行人員 Task 操作 API

### GET /api/my-tasks

取得指派給當前使用者的 Task 列表。

**權限**: `EXECUTOR`
**Query Parameters**: `status` (可選, PENDING/IN_PROGRESS/COMPLETED/CLOSED)

**Response 200**:
```json
{
  "content": [
    {
      "id": 1,
      "name": "前端登入頁切版",
      "projectName": "產品改版",
      "status": "IN_PROGRESS",
      "budgetHours": 20.0,
      "consumedHours": 8.5,
      "remainingHours": 11.5
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 3,
  "totalPages": 1
}
```

---

### POST /api/my-tasks/{id}/complete

執行人員將 Task 標記為完成。

**權限**: `EXECUTOR`（僅限指派給自己的 Task）

**Response 200**:
```json
{
  "id": 1,
  "status": "COMPLETED",
  "remainingHours": 5.0,
  "message": "task 已標記為完成，剩餘時數已鎖定"
}
```

**Error Responses**:
- `400` — Task 已為終態
- `403` — 此 task 未指派給您

---

## 6. 時數增補申請 API（Hours Requests）

### GET /api/hours-requests

取得時數增補申請列表。

**權限**: `PM`（僅限自己提交的）、`ADMIN`（全部）
**Query Parameters**: `projectId` (可選), `status` (可選), `page`, `size`

**Response 200**:
```json
{
  "content": [
    {
      "id": 1,
      "projectId": 1,
      "projectName": "產品改版",
      "requesterId": 2,
      "requesterName": "李PM",
      "requestedHours": 40.0,
      "description": "前端頁面切版工作量超出預期",
      "targetType": "TASK",
      "targetTaskId": 1,
      "targetTaskName": "前端登入頁切版",
      "status": "PENDING",
      "reviewerId": null,
      "reviewerName": null,
      "reviewComment": null,
      "reviewedAt": null,
      "createdAt": "2026-02-23T14:00:00+08:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

---

### POST /api/hours-requests

PM 提交時數增補申請。

**權限**: `PM`

**Request Body**:
```json
{
  "projectId": 1,
  "requestedHours": 40.0,
  "description": "前端頁面切版工作量超出預期",
  "targetType": "TASK",
  "targetTaskId": 1
}
```

**Response 201**: HoursRequest 物件

**Error Responses**:
- `400` — 申請時數必須大於 0
- `400` — targetType 為 TASK 時必須指定 targetTaskId
- `403` — 此專案非您負責

---

### POST /api/hours-requests/{id}/approve

管理層核准時數增補申請。

**權限**: `ADMIN`

**Request Body**:
```json
{
  "comment": "核准，請加快進度"
}
```

**Response 200**: 更新後的 HoursRequest 物件（status = APPROVED）

**Side Effects**:
- `targetType = TASK` → Task.budgetHours += requestedHours, Project.totalBudgetHours += requestedHours
- `targetType = PROJECT` → Project.totalBudgetHours += requestedHours
- 通知 PM
- 產生 AuditLog

---

### POST /api/hours-requests/{id}/reject

管理層拒絕時數增補申請。

**權限**: `ADMIN`

**Request Body**:
```json
{
  "comment": "預算不足，請重新評估工作範圍"
}
```

**Response 200**: 更新後的 HoursRequest 物件（status = REJECTED）

**Error Responses**:
- `400` — 拒絕原因為必填

**Side Effects**:
- 通知 PM
- 產生 AuditLog

---

## 7. 使用者管理 API（Users）— HR

### GET /api/users

取得使用者列表。

**權限**: `HR`
**Query Parameters**: `departmentId` (可選), `active` (可選), `page`, `size`

**Response 200**:
```json
{
  "content": [
    {
      "id": 1,
      "name": "王小明",
      "email": "wang@company.com",
      "departmentId": 1,
      "departmentName": "研發部",
      "roles": ["EXECUTOR"],
      "active": true,
      "createdAt": "2026-02-20T09:00:00+08:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 50,
  "totalPages": 3
}
```

---

### POST /api/users

新增使用者。

**權限**: `HR`

**Request Body**:
```json
{
  "name": "王小明",
  "email": "wang@company.com",
  "departmentId": 1,
  "roles": ["EXECUTOR"]
}
```

**Response 201**: User 物件（不含密碼資訊）

**Error Responses**:
- `400` — email 已被使用
- `400` — 至少需指派一個角色
- `404` — 部門不存在

**Side Effects**:
- 初始密碼設定為 `Welcome123`（BCrypt 雜湊儲存）
- `password_changed = false`

---

### PUT /api/users/{id}

修改使用者資訊（角色、部門）。

**權限**: `HR`

**Request Body**:
```json
{
  "name": "王小明",
  "departmentId": 2,
  "roles": ["EXECUTOR", "PM"]
}
```

**Response 200**: 更新後的 User 物件

**Side Effects**:
- 角色變更時產生 AuditLog

---

### POST /api/users/{id}/deactivate

停用使用者帳號。

**權限**: `HR`

**Response 200**:
```json
{
  "id": 1,
  "active": false,
  "message": "帳號已停用"
}
```

**Side Effects**:
- 該使用者所有非終態 Task 轉為「未指派」並凍結工時填報
- 產生 AuditLog（ACCOUNT_DEACTIVATE）
- 通知相關 PM

---

### POST /api/users/{id}/activate

啟用使用者帳號。

**權限**: `HR`

**Response 200**:
```json
{
  "id": 1,
  "active": true,
  "message": "帳號已啟用"
}
```

**Side Effects**:
- 產生 AuditLog（ACCOUNT_ACTIVATE）

---

## 8. 部門工時 API（Department）— 部門主管

### GET /api/department/members

取得部門成員工時概覽。

**權限**: `DEPT_MANAGER`（僅限本部門）

**Response 200**:
```json
{
  "departmentName": "研發部",
  "members": [
    {
      "userId": 3,
      "userName": "張執行",
      "weeklyHours": 25.0,
      "monthlyHours": 80.5,
      "activeTaskCount": 3
    }
  ]
}
```

---

### GET /api/department/members/{userId}/tasks

取得部門成員的 Task 詳情。

**權限**: `DEPT_MANAGER`（僅限本部門成員）

**Response 200**:
```json
{
  "userId": 3,
  "userName": "張執行",
  "tasks": [
    {
      "taskId": 1,
      "taskName": "前端登入頁切版",
      "projectName": "產品改版",
      "status": "IN_PROGRESS",
      "consumedHours": 8.5
    }
  ]
}
```

**Error Responses**:
- `403` — 該使用者不屬於您的部門

---

## 9. 通知 API（Notifications）

### GET /api/notifications

取得當前使用者的通知列表。

**權限**: 任意已認證使用者
**Query Parameters**: `unread` (可選, boolean), `page`, `size`

**Response 200**:
```json
{
  "content": [
    {
      "id": 1,
      "type": "TASK_HOURS_EXHAUSTED",
      "title": "Task 時數已用盡",
      "content": "「前端登入頁切版」時數已歸零，請盡快申請增補",
      "isRead": false,
      "createdAt": "2026-02-23T17:00:00+08:00"
    }
  ],
  "unreadCount": 3,
  "page": 0,
  "size": 20,
  "totalElements": 10,
  "totalPages": 1
}
```

---

### PATCH /api/notifications/{id}/read

標記通知為已讀。

**權限**: 通知擁有者

**Response 200**:
```json
{
  "id": 1,
  "isRead": true
}
```

---

## 10. PM 專案儀表板 API

### GET /api/pm/projects

取得 PM 負責的專案列表（含儀表板摘要）。

**權限**: `PM`

**Response 200**:
```json
{
  "content": [
    {
      "id": 1,
      "name": "產品改版",
      "status": "ACTIVE",
      "totalBudgetHours": 200.0,
      "consumedHours": 45.5,
      "remainingHours": 154.5,
      "usageRate": 22.75,
      "taskSummary": {
        "total": 8,
        "pending": 2,
        "inProgress": 4,
        "completed": 1,
        "closed": 1
      }
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```
