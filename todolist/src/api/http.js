import axios from 'axios';
import { getBaseUrl } from '@/utils/urlHelper';

const http = axios.create({
  baseURL: getBaseUrl() + '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
});

// 请求拦截器：自动添加 token
http.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器
http.interceptors.response.use(
  response => {
    return response.data;
  },
  error => {
    console.error('API 请求失败:', error);
    
    let message = '网络错误，请稍后重试';
    if (error.response) {
      const data = error.response.data;
      message = data.message || `请求失败: ${error.response.status}`;
      
      if (error.response.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('username');
        window.location.href = '/login';
        message = '登录已过期，请重新登录';
      }
    } else if (error.request) {
      message = '服务器无响应，请检查后端是否启动';
    }
    
    console.warn(message);
    return Promise.reject(error);
  }
);

export default http;