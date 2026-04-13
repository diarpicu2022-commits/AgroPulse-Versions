const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

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
  list: (sensorId, limit = 100) => request(`/api/readings?sensor=${sensorId}&limit=${limit}`),
  create: (data) => request('/api/readings', { method: 'POST', body: JSON.stringify(data) }),
};

// Alerts
export const alerts = {
  list: () => request('/api/alerts'),
  markRead: (id) => request(`/api/alerts/${id}/read`, { method: 'PUT' }),
};

// Logs
export const logs = {
  list: (limit = 100) => request(`/api/logs?limit=${limit}`),
};

export default { auth, sensors, crops, greenhouses, actuators, users, readings, alerts, logs };