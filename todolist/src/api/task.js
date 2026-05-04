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

// ========== 标签管理 ==========
export const getTags = () => http.get('/tags');
export const addTag = (tagName) => http.post('/tags', { tagName });
export const deleteTag = (tagName) => http.delete(`/tags/${tagName}`);

// ========== 附件管理 ==========

// 上传附件
export const uploadAttachment = (taskId, file) => {
  const formData = new FormData();
  formData.append('file', file);
  return http.post(`/task/${taskId}/attachments`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};

// 获取附件列表
export const getAttachments = (taskId) => {
  return http.get(`/task/${taskId}/attachments`);
};

// 删除附件
export const deleteAttachment = (attachmentId) => {
  return http.delete(`/attachments/${attachmentId}`);
};