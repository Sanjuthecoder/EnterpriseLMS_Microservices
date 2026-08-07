import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    const tenantId = localStorage.getItem('tenant_id'); 
    // Only attach token if it's a valid JWT (contains at least 2 dots)
    if (token && token !== 'undefined' && token !== 'null' && token.includes('.')) {
      config.headers['Authorization'] = `Bearer ${token}`;
    } else if (token && (!token.includes('.') || token === 'undefined' || token === 'null')) {
      // Invalid token found in storage — clean it up
      console.warn('Invalid JWT token found in localStorage, removing it.');
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
    if (tenantId && tenantId !== 'undefined' && tenantId !== 'null') {
      config.headers['X-Tenant-ID'] = tenantId;
    }
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type'];
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      if (error.response.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        // If not already on auth page, redirect
        if (!window.location.pathname.startsWith('/auth')) {
          window.location.href = '/auth'; 
        }
      } else if (error.response.status === 403) {
        console.error('Forbidden access - You lack permissions for this resource.');
      } else if (error.response.status >= 500) {
        console.error('Server error - Please try again later.');
      }
    }
    return Promise.reject(error);
  }
);

export default api;
