import 'dotenv/config';

const baseUrl = process.env.SMOKE_BASE_URL || `http://127.0.0.1:${process.env.PORT || 8080}`;

async function request(path, options = {}) {
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {})
  };

  const response = await fetch(`${baseUrl}${path}`, {
    ...options,
    headers
  });

  const text = await response.text();
  let body;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = text;
  }

  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}: ${JSON.stringify(body)}`);
  }

  return body;
}

async function main() {
  const email = `smoke_${Date.now()}@gymeye.local`;
  const password = 'smoke123';

  const login = await request('/api/user/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  });
  const token = login.token;

  const equipmentList = await request('/api/equipment?limit=5');
  const firstEquipment = equipmentList.data[0];
  if (!firstEquipment) {
    throw new Error('No equipment found. Run db:init and seed scripts first.');
  }

  await request('/api/history/add', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: JSON.stringify({
      userId: login.userId,
      equipmentName: firstEquipment.name
    })
  });

  const history = await request(`/api/history/list?userId=${login.userId}`, {
    headers: { Authorization: `Bearer ${token}` }
  });

  console.log(
    JSON.stringify(
      {
        ok: true,
        userId: login.userId,
        equipmentSample: firstEquipment.name,
        historyCount: Array.isArray(history.data) ? history.data.length : 0
      },
      null,
      2
    )
  );
}

main().catch(error => {
  console.error(error.message);
  process.exit(1);
});
