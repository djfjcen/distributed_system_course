import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://seckill-nginx';

export const options = {
  vus: 30,
  duration: '15s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500']
  }
};

export default function () {
  const indexRes = http.get(`${baseUrl}/`);
  check(indexRes, { 'index status 200': (r) => r.status === 200 });

  const cssRes = http.get(`${baseUrl}/styles.css`);
  check(cssRes, { 'css status 200': (r) => r.status === 200 });

  const jsRes = http.get(`${baseUrl}/app.js`);
  check(jsRes, { 'js status 200': (r) => r.status === 200 });

  sleep(0.1);
}
