import http from 'k6/http';
import { check, sleep } from 'k6';

const backendApiUrl = __ENV.BACKEND_API_URL || 'http://backend-1:8080/api/products';

export const options = {
  vus: 30,
  duration: '15s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500']
  }
};

export default function () {
  const res = http.get(backendApiUrl);
  check(res, { 'api status 200': (r) => r.status === 200 });
  sleep(0.1);
}
