# SC-001 Validation Checklist

Objective: 工時填報操作（選擇 task -> 輸入時數 -> 確認）<= 2 分鐘

## Test Setup

- Roles: EXECUTOR
- Browser: Chrome latest
- Network: office LAN baseline

## Procedure

1. Login as executor.
2. Open `/executor/work-entries`.
3. Select task.
4. Select date.
5. Input hours and submit.
6. Record elapsed time from step 2 to success message.

## Acceptance

- PASS if elapsed time <= 120 seconds for at least 95% of attempts.
- Sample size target: >= 30 attempts.
