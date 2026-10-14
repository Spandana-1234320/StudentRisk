import axios from 'axios';

const API = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8086/api'
});

// Attach JWT token to every request
API.interceptors.request.use((config) => {
  const isAuthRequest = config.url?.startsWith('/auth/');
  const token = localStorage.getItem('token');
  if (token && !isAuthRequest) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default API;
