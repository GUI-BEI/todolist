import http from './http';

// ========== 任务相关 ==========

// 获取所有任务（无筛选）
export const getTasks = () => http.get('/getTasks');

// 获取筛选后的任务（带过滤条件）
export const getFilteredTasks = (params) => {
  return http.get('/getFilteredTasks', { params });
};

// 新增任务
export const addTask = (data) => http.post('/addTask', data);

// 删除任务
export const deleteTask = (id) => http.delete(`/deleteTask/${id}`);

// 更新任务（完整更新）
export const updateTask = (id, data) => http.put(`/updateTask/${id}`, data);

// 单独更新任务时间（拖拽时使用）
export const updateTaskTime = (id, start, end) => {
  return http.post('/updateTaskTime', { id, start, end });
};