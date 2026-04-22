import http from './http';

// 获取任务列表
export const getTasks = () => http.get('/getTasks');

// 新增任务	
export const addTask = (data) => http.post('/addTask', data);

// 删除任务
export const deleteTask = (id) => http.delete(`/deleteTask/${id}`);

// 更新任务
export const updateTask = (id, data) => http.put(`/updateTask/${id}`, data);
