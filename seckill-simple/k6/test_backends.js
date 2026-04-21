import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const backend1Duration = new Trend('backend1_duration', true);
const backend2Duration = new Trend('backend2_duration', true);
const backend1Failures = new Counter('backend1_failures');
const backend2Failures = new Counter('backend2_failures');
const backend1Requests = new Counter('backend1_requests');
const backend2Requests = new Counter('backend2_requests');

const host = __ENV.HOST || 'http://host.docker.internal';
const backend1 = `${host}:8081/api/products`;
const backend2 = `${host}:8082/api/products`;

export const options = {
  vus: 20,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    backend1_duration: ['p(95)<500'],
    backend2_duration: ['p(95)<500'],
    backend1_failures: ['count<1'],
    backend2_failures: ['count<1'],
    backend1_requests: ['count>0'],
    backend2_requests: ['count>0']
  }
};

export default function () {
  const res1 = http.get(backend1, { tags: { backend: 'backend-1' } });
  backend1Requests.add(1);
  backend1Duration.add(res1.timings.duration);
  if (!check(res1, { 'backend-1 status is 200': (r) => r.status === 200 })) {
    backend1Failures.add(1);
  }

  const res2 = http.get(backend2, { tags: { backend: 'backend-2' } });
  backend2Requests.add(1);
  backend2Duration.add(res2.timings.duration);
  if (!check(res2, { 'backend-2 status is 200': (r) => r.status === 200 })) {
    backend2Failures.add(1);
  }

  sleep(0.1);
}
