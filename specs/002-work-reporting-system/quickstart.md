# Quickstart: 報工系統

**Feature**: 002-work-reporting-system
**Date**: 2026-02-23

---

## 前置需求

| 工具 | 版本 | 說明 |
|------|------|------|
| JDK | 24 | 後端執行環境 |
| Gradle | 8.x | 後端建構工具（使用 Wrapper） |
| Node.js | 20 LTS+ | 前端建構環境 |
| pnpm | 9.x | 前端套件管理 |
| Docker & Docker Compose | 最新 | 本地開發資料庫 + 部署 |
| PostgreSQL | 18.1 | 透過 Docker 執行即可 |

---

## 快速啟動（本地開發）

### 1. 複製並切換分支

```bash
git checkout 002-work-reporting-system
```

### 2. 啟動 PostgreSQL（透過 Docker）

```bash
docker compose up -d db
```

預設連線資訊（定義於 `docker-compose.yml`）：
- Host: `localhost:5432`
- Database: `workreport`
- User: `workreport`
- Password: `workreport`

### 3. 啟動後端

```bash
cd backend
./gradlew bootRun
```

後端啟動於 `http://localhost:8080`。
Flyway 將自動執行資料庫遷移。

### 4. 啟動前端

```bash
cd frontend
pnpm install
pnpm dev
```

前端啟動於 `http://localhost:5173`。
Vite dev server 自動代理 `/api` 請求至後端。

### 5. 初始測試帳號

系統啟動後，透過 Flyway seed 資料自動建立以下測試帳號：

| 角色 | Email | 初始密碼 | 備註 |
|------|-------|---------|------|
| 管理層 | admin@company.com | Welcome123 | 首次登入須改密碼 |
| PM | pm@company.com | Welcome123 | 首次登入須改密碼 |
| 部門主管 | manager@company.com | Welcome123 | 首次登入須改密碼 |
| 執行人員 | executor@company.com | Welcome123 | 首次登入須改密碼 |
| HR | hr@company.com | Welcome123 | 首次登入須改密碼 |

---

## 建構指令

### 後端

```bash
cd backend

# 執行所有測試
./gradlew test

# 測試覆蓋率報告（JaCoCo）
./gradlew jacocoTestReport
# 報告位於 build/reports/jacoco/test/html/index.html

# 覆蓋率門檻檢查（≥ 80%）
./gradlew jacocoTestCoverageVerification

# 建構 JAR
./gradlew bootJar

# Lint / 靜態分析
./gradlew checkstyleMain spotbugsMain
```

### 前端

```bash
cd frontend

# 安裝相依套件
pnpm install

# 開發模式
pnpm dev

# 執行單元測試
pnpm test

# 測試覆蓋率
pnpm test:coverage

# Lint
pnpm lint

# 建構生產版本
pnpm build
```

### E2E 測試

```bash
cd frontend

# 安裝 Playwright 瀏覽器
pnpm exec playwright install

# 執行 E2E 測試（需先啟動前後端）
pnpm test:e2e
```

---

## Docker Compose 完整部署

```bash
# 建構並啟動所有服務
docker compose up -d --build

# 查看日誌
docker compose logs -f
```

服務對應：
- 前端（Nginx）：`http://localhost`
- 後端 API：`http://localhost:8080`
- PostgreSQL：`localhost:5432`

---

## 環境變數

### 後端（application.yml / 環境變數）

| 變數 | 預設值 | 說明 |
|------|--------|------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/workreport` | 資料庫連線 URL |
| `DB_USERNAME` | `workreport` | 資料庫使用者 |
| `DB_PASSWORD` | `workreport` | 資料庫密碼 |
| `JWT_SECRET` | （開發環境預設值） | JWT 簽章密鑰 |
| `JWT_EXPIRATION_MS` | `1800000` | JWT 有效期（毫秒，預設 30 分鐘） |

### 前端（.env）

| 變數 | 預設值 | 說明 |
|------|--------|------|
| `VITE_API_BASE_URL` | `/api` | API 基礎路徑 |

---

## 目錄結構概覽

```
project-root/
├── backend/                 # Spring Boot 後端
│   ├── src/main/java/       # Java 原始碼
│   ├── src/main/resources/  # 設定檔 + Flyway 遷移
│   ├── src/test/java/       # 測試
│   ├── build.gradle         # Gradle 建構檔
│   └── Dockerfile
├── frontend/                # Vue 3 前端
│   ├── src/                 # TypeScript/Vue 原始碼
│   ├── tests/               # 測試
│   ├── package.json
│   ├── vite.config.ts
│   └── Dockerfile
├── docker-compose.yml       # 本地開發 + 部署
└── specs/                   # 規格文件
    └── 002-work-reporting-system/
        ├── spec.md
        ├── plan.md
        ├── research.md
        ├── data-model.md
        ├── quickstart.md    # 本文件
        └── contracts/
```
