const output = document.getElementById('output');
const productsList = document.getElementById('productsList');

function render(data) {
  output.textContent = JSON.stringify(data, null, 2);
}

async function request(url, options) {
  const res = await fetch(url, options);
  const data = await res.json();
  render(data);
  return data;
}

document.getElementById('loadProductsBtn').addEventListener('click', async () => {
  const data = await request('/api/products');
  productsList.innerHTML = '';

  const products = Array.isArray(data) ? data : data.value || [];
  products.forEach((p) => {
    const li = document.createElement('li');
    li.textContent = `商品ID=${p.id}，名称=${p.name}，库存=${p.stock}，价格=${p.price}`;
    productsList.appendChild(li);
  });
});

document.getElementById('searchForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = new FormData(e.target);
  const keyword = (form.get('keyword') || '').toString().trim();
  const url = keyword ? `/api/search/products?keyword=${encodeURIComponent(keyword)}` : '/api/search/products';
  const data = await request(url);
  productsList.innerHTML = '';

  const products = data.data || [];
  products.forEach((p) => {
    const li = document.createElement('li');
    li.textContent = `商品ID=${p.id}，名称=${p.name}，库存=${p.stock}，价格=${p.price}`;
    productsList.appendChild(li);
  });
});

document.getElementById('registerForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = new FormData(e.target);
  await request('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: form.get('username'),
      email: form.get('email'),
      password: form.get('password')
    })
  });
});

document.getElementById('loginForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = new FormData(e.target);
  await request('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      username: form.get('username'),
      password: form.get('password')
    })
  });
});

document.getElementById('seckillForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const form = new FormData(e.target);
  const productId = form.get('productId');
  const quantity = form.get('quantity');
  await request(`/api/seckill/${productId}?quantity=${quantity}`, { method: 'POST' });
});
