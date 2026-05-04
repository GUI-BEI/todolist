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

// ========== 密保相关 ==========

// 设置密保问题
export const setSecurityQuestion = (securityQuestion, securityAnswer) => {
  return http.post('/user/security', { securityQuestion, securityAnswer });
};

// 获取密保问题（忘记密码时）
export const getSecurityQuestion = (username) => {
  return http.get('/user/security/question', { params: { username } });
};

// 重置密码
export const resetPassword = (username, securityAnswer, newPassword) => {
  return http.post('/user/reset-password', { username, securityAnswer, newPassword });
};

// ========== 头像相关 ==========

// 上传头像
export const uploadAvatar = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return http.post('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};