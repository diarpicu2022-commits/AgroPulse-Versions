const API_URL = import.meta.env.VITE_API_URL || 'https://agropulse-versions-production.up.railway.app';

// Almacén de contexto de usuario para headers (se actualiza desde App)
let _userCtx: { id?: number; role?: string; adminEmail?: string } = {};
export const setUserContext = (ctx: { id?: number; role?: string; adminEmail?: string }) => {
  _userCtx = ctx;
};

async function request(endpoint: string, options: any = {}) {
  const url = `${API_URL}${endpoint}`;
  const config = {
    headers: {
      'Content-Type': 'application/json',
      ..._userCtx.id       ? { 'X-User-Id': String(_userCtx.id) }             : {},
      ..._userCtx.role     ? { 'X-User-Role': _userCtx.role }                  : {},
      ..._userCtx.adminEmail ? { 'X-Admin-Email': _userCtx.adminEmail }        : {},
      ...options.headers,
    },
    ...options,
  };

  try {
    const res  = await fetch(url, config);
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Error en la petición');
    return data;
  } catch (error) {
    console.error('API Error:', error);
    throw error;
  }
}

// Auth
export const auth = {
  login: (username: string, password: string) =>
    request('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),

  googleLogin: (email: string, name: string, googleId: string) =>
    request('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, name, googleId }) }),

  register: (username: string, password: string, fullName: string) =>
    request('/api/auth/register', { method: 'POST', body: JSON.stringify({ username, password, fullName }) }),

  me: () => request('/api/auth/me'),
};

// Sensors
export const sensors = {
  list:   ()         => request('/api/sensors'),
  get:    (id: number) => request(`/api/sensors/${id}`),
  create: (data: any)  => request('/api/sensors',     { method: 'POST',   body: JSON.stringify(data) }),
  update: (id: number, data: any) => request(`/api/sensors/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: number) => request(`/api/sensors/${id}`, { method: 'DELETE' }),
};

// Crops
export const crops = {
  list:   ()         => request('/api/crops'),
  get:    (id: number) => request(`/api/crops/${id}`),
  create: (data: any)  => request('/api/crops',       { method: 'POST',   body: JSON.stringify(data) }),
  update: (id: number, data: any) => request(`/api/crops/${id}`,   { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: number) => request(`/api/crops/${id}`,   { method: 'DELETE' }),
};

// Greenhouses
export const greenhouses = {
  list:        ()                    => request('/api/greenhouses'),
  get:         (id: number)          => request(`/api/greenhouses/${id}`),
  create:      (data: any)           => request('/api/greenhouses',              { method: 'POST',   body: JSON.stringify(data) }),
  update:      (id: number, data: any) => request(`/api/greenhouses/${id}`,      { method: 'PUT',    body: JSON.stringify(data) }),
  delete:      (id: number)          => request(`/api/greenhouses/${id}`,        { method: 'DELETE' }),
  listUsers:   (id: number)          => request(`/api/greenhouses/${id}/users`),
  assignUser:  (id: number, userId: number) => request(`/api/greenhouses/${id}/users`, { method: 'POST', body: JSON.stringify({ userId }) }),
  removeUser:  (id: number, userId: number) => request(`/api/greenhouses/${id}/users/${userId}`, { method: 'DELETE' }),
};

// Actuators
export const actuators = {
  list:   ()         => request('/api/actuators'),
  get:    (id: number) => request(`/api/actuators/${id}`),
  create: (data: any)  => request('/api/actuators',    { method: 'POST',   body: JSON.stringify(data) }),
  update: (id: number, data: any) => request(`/api/actuators/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: number) => request(`/api/actuators/${id}`, { method: 'DELETE' }),
};

// Users (admin)
export const users = {
  list:   ()         => request('/api/users'),
  create: (data: any)  => request('/api/users',        { method: 'POST',   body: JSON.stringify(data) }),
  update: (id: number, data: any) => request(`/api/users/${id}`,    { method: 'PUT',    body: JSON.stringify(data) }),
  delete: (id: number) => request(`/api/users/${id}`,    { method: 'DELETE' }),
};

// Readings
export const readings = {
  list: (sensorId?: number | null, limit = 100) => {
    if (!sensorId) return request(`/api/readings?limit=${limit}`);
    return request(`/api/readings?sensor=${sensorId}&limit=${limit}`);
  },
  create: (data: any) => request('/api/readings', { method: 'POST', body: JSON.stringify(data) }),
};

// Alerts
export const alerts = {
  list:     ()         => request('/api/alerts'),
  create:   (data: any)  => request('/api/alerts',        { method: 'POST', body: JSON.stringify(data) }),
  markRead: (id: number) => request(`/api/alerts/${id}/read`, { method: 'PUT' }),
  delete:   (id: number) => request(`/api/alerts/${id}`,  { method: 'DELETE' }),
};

// Logs
export const logs = {
  list: (limit = 100) => request(`/api/logs?limit=${limit}`),
};

// Automation Rules
export const rules = {
  list:   ()         => request('/api/rules'),
  create: (data: any)  => request('/api/rules',        { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: any) => request(`/api/rules/${id}`, { method: 'PUT',  body: JSON.stringify(data) }),
  delete: (id: number) => request(`/api/rules/${id}`,  { method: 'DELETE' }),
};

// Reports
export const reports = {
  dailyCsv:   ()         => request('/api/reports/daily-csv'),
  weeklyStats: ()        => request('/api/reports/weekly-stats'),
  sendEmail:  (data: any)  => request('/api/reports/send-email', { method: 'POST', body: JSON.stringify(data) }),
  schedule:   (data: any)  => request('/api/reports/schedule',   { method: 'POST', body: JSON.stringify(data) }),
  history:    (limit = 10) => request(`/api/reports/history?limit=${limit}`),
};

// Support Tickets
export const tickets = {
  list:   ()         => request('/api/tickets'),
  get:    (id: number) => request(`/api/tickets/${id}`),
  create: (data: any)  => request('/api/tickets',        { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: any) => request(`/api/tickets/${id}`, { method: 'PUT',  body: JSON.stringify(data) }),
  delete: (id: number) => request(`/api/tickets/${id}`,  { method: 'DELETE' }),
};

export default { auth, sensors, crops, greenhouses, actuators, users, readings, alerts, logs, rules, reports, tickets };
