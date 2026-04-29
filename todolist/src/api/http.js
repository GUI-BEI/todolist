import axios from 'axios';

const http = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json'
  }
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

// 响应拦截器：统一处理错误和 token 过期
http.interceptors.response.use(
  response => {
    // 直接返回 response.data
    return response.data;
  },
  error => {
    console.error('API 请求失败:', error);
    
    let message = '网络错误，请稍后重试';
    if (error.response) {
      const data = error.response.data;
      message = data.message || `请求失败: ${error.response.status}`;
      
      // token 过期或无效，跳转到登录页
      if (error.response.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('username');
        // 跳转到登录页
        window.location.href = '/login';
        message = '登录已过期，请重新登录';
      }
    } else if (error.request) {
      message = '服务器无响应，请检查后端是否启动';
    }
    
    // 可以在这里用全局提示组件
    console.warn(message);
    
    return Promise.reject(error);
  }
);

export default http;