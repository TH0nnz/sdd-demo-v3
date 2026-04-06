import http from 'k6/http'
import { check, sleep } from 'k6'

export const options = {
  vus: 100,
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    checks: ['rate>0.99'],
  },
}

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'
const TOKEN = __ENV.TOKEN || ''

export default function () {
  const headers = TOKEN
    ? { Authorization: `Bearer ${TOKEN}` }
    : undefined

  const res = http.get(`${BASE_URL}/api/work-entries`, { headers })
  check(res, {
    'status is 200 or 401': (r) => r.status === 200 || r.status === 401,
  })
  sleep(1)
}


// 你可以用 K6 工具執行 us1-smoke.js 來對 API 進行壓力測試。步驟如下：
//
// 1. **安裝 K6**（如尚未安裝）
//    在終端機執行：
//    ```
//    brew install k6
//    ```
//    或參考官方安裝文件：https://k6.io/docs/getting-started/installation/
//
// 2. **執行測試腳本**
//    進入專案根目錄，執行：
//    ```
//    k6 run performance/k6/us1-smoke.js
//    ```
//    預設會對 http://localhost:8080/api/work-entries 發送請求。
//
// 3. **自訂 API 位置或 Token**
//    若要測試不同 API 或帶入 JWT Token，可加上環境變數：
//    ```
//    BASE_URL="http://your-api-url" TOKEN="your-jwt-token" k6 run performance/k6/us1-smoke.js
//    ```
//
// 4. **查看結果**
//    執行後，K6 會在終端機顯示請求成功率、延遲分佈等效能資訊。
//
// 如需進階參數或報表，可參考 K6 官方文件。