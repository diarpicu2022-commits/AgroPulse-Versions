const API_URL = import.meta.env.VITE_API_URL || 'https://agropulse-versions-production.up.railway.app';

async function request(endpoint, options = {}) {
  const url = `${API_URL}${endpoint}`;
  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  };
  
  try {
    const res = await fetch(url, config);
    const data = await res.json();
    
    if (!res.ok) {
      throw new Error(data.error || 'Error en la petición');
    }
    
    return data;
  } catch (error) {
    console.error('API Error:', error);
    throw error;
  }
}

// Auth
export const auth = {
  login: (username, password) => 
    request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    }),
  
  googleLogin: (email, name, googleId) =>
    request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, name, googleId }),
    }),
  
  me: () => request('/api/auth/me'),
};

// Sensors
export const sensors = {
  list: () => request('/api/sensors'),
  
  get: (id) => request(`/api/sensors/${id}`),
  
  create: (data) => 
    request('/api/sensors', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  
  update: (id, data) => 
    request(`/api/sensors/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  
  delete: (id) => 
    request(`/api/sensors/${id}`, {
      method: 'DELETE',
    }),
};

// Crops
export const crops = {
  list: () => request('/api/crops'),
  
  get: (id) => request(`/api/crops/${id}`),
  
  create: (data) => 
    request('/api/crops', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  
  update: (id, data) => 
    request(`/api/crops/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    }),
  
  delete: (id) => 
    request(`/api/crops/${id}`, {
      method: 'DELETE',
    }),
};

// Greenhouses
export const greenhouses = {
  list: () => request('/api/greenhouses'),
  get: (id) => request(`/api/greenhouses/${id}`),
  create: (data) => request('/api/greenhouses', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/api/greenhouses/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/api/greenhouses/${id}`, { method: 'DELETE' }),
};

// Actuators
export const actuators = {
  list: () => request('/api/actuators'),
  get: (id) => request(`/api/actuators/${id}`),
  create: (data) => request('/api/actuators', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/api/actuators/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/api/actuators/${id}`, { method: 'DELETE' }),
};

// Users (admin)
export const users = {
  list: () => request('/api/users'),
  create: (data) => request('/api/users', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/api/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/api/users/${id}`, { method: 'DELETE' }),
};

// Readings
export const readings = {
  list: (sensorId, limit = 100) => {
    // Si no hay sensorId, devolver TODAS las lecturas (últimas 100)
    if (!sensorId) {
      return request(`/api/readings?limit=${limit}`);
    }
    // Si hay sensorId, filtrar por ese sensor
    return request(`/api/readings?sensor=${sensorId}&limit=${limit}`);
  },
  create: (data) => request('/api/readings', { method: 'POST', body: JSON.stringify(data) }),
};

// Alerts
export const alerts = {
  list: () => request('/api/alerts'),
  create: (data) => request('/api/alerts', { method: 'POST', body: JSON.stringify(data) }),
  markRead: (id) => request(`/api/alerts/${id}/read`, { method: 'PUT' }),
  delete: (id) => request(`/api/alerts/${id}`, { method: 'DELETE' }),
};

// Logs
export const logs = {
  list: (limit = 100) => request(`/api/logs?limit=${limit}`),
};

// Automation Rules
export const rules = {
  list: () => request('/api/rules'),
  create: (data) => request('/api/rules', { method: 'POST', body: JSON.stringify(data) }),
  update: (id, data) => request(`/api/rules/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id) => request(`/api/rules/${id}`, { method: 'DELETE' }),
};

// Reports
export const reports = {
  dailyCsv: () => request('/api/reports/daily-csv'),
  weeklyStats: () => request('/api/reports/weekly-stats'),
  sendEmail: (data) => request('/api/reports/send-email', { method: 'POST', body: JSON.stringify(data) }),
  schedule: (data) => request('/api/reports/schedule', { method: 'POST', body: JSON.stringify(data) }),
  history: (limit = 10) => request(`/api/reports/history?limit=${limit}`),
};

export default { auth, sensors, crops, greenhouses, actuators, users, readings, alerts, logs, rules, reports };