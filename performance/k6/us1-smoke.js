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
