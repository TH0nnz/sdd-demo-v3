# 報工系統 (Work Report System)

報工時間記錄與管理系統，用於工作項目的時間追蹤與報表統計。

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

## Docker Compose 部署（Linux 伺服器）

### 1. 準備環境

```bash
# 安裝 Docker
curl -fsSL https://get.docker.com | sh

# 安裝 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 2. 部署應用

```bash
# 複製專案
git clone <repo-url> sdd-demo-v3
cd sdd-demo-v3

# 切換分支
git checkout 002-work-reporting-system

# 啟動所有服務
sudo docker-compose up -d

# 查看狀態
sudo docker-compose ps
```

### 3. 服務訪問

- 前端（Nginx）：`http://<伺服器IP>`
- 後端 API：`http://<伺服器IP>:8089`
- 資料庫：`<伺服器IP>:5454`

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

## 目錄結構

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
├── docker-compose.yml       # 本地開發 + 部署設定
└── specs/                   # 規格文件
    └── 002-work-reporting-system/
        ├── spec.md
        ├── plan.md
        ├── research.md
        ├── data-model.md
        ├── quickstart.md
        └── contracts/
```

---

## 常用 Docker 命令

```bash
# 查看執行中的服務
docker-compose ps

# 檢視日誌
docker-compose logs -f

# 停止所有服務
docker-compose down

# 完全清除（包括 volume）
docker-compose down -v

# 重啟服務
docker-compose restart

# 進入資料庫
docker-compose exec db psql -U workreport -d workreport
```

---

## 數據庫遷移說明

使用 Flyway 進行資料庫版本管理。遷移文件位於 `backend/src/main/resources/db/migration/`。

首次啟動時，系統會自動執行所有遷移文件：
- `V1__...` - 建立資料庫結構
- `V2__...` - 插入測試資料

**重要**: 如需重置資料庫，請：
1. 停止容器：`docker-compose down -v`
2. 刪除 volume：`docker volume prune`
3. 重新啟動：`docker-compose up -d`

---

## 詳細文件

更多詳細說明請參考 [specs/002-work-reporting-system/quickstart.md](specs/002-work-reporting-system/quickstart.md)

---

### 結語：
切記如果用docker compose在linux上啟動，會自動幫你init db，要砍掉所有table 在使用/Volumes/DOCKER_SSD/workspace/sdd-demo-v3/backend/src/main/resources/db/migration 中的v1 與 v2 重建db


**最後更新**: 2026-02-26
