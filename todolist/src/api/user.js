import http from './http';

// ========== 用户相关 ==========

// 用户登录
export const login = (username, password) => {
  return http.post('/login', { username, password });
};

// 用户注册
export const register = (username, password) => {
  return http.post('/register', { username, password });
};

// 获取当前用户信息
export const getUserInfo = () => {
  return http.get('/user/info');
};

// 更新用户信息
export const updateUser = (username, password) => {
  return http.post('/user/update', { username, password });
};

// 验证 token
export const verifyToken = () => {
  return http.get('/verify');
};