import http from './http';

// ========== 签到相关 ==========

// 获取签到状态
export const getSignStatus = () => {
  return http.get('/sign/status');
};

// 执行签到
export const signIn = () => {
  return http.post('/sign');
};