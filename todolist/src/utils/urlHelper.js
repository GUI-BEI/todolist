// src/utils/urlHelper.js

/**
 * 获取 API 基础 URL
 * 统一返回后端服务地址
 */
export const getBaseUrl = () => {
  return 'http://localhost:8080';
};

/**
 * 获取完整的资源 URL（头像、附件等）
 * @param {string} url - 后端返回的相对路径（如 /avatars/xxx.jpg）
 * @returns {string} 完整的 URL
 */
export const getFullUrl = (url) => {
  if (!url) return '';
  
  // 已经是完整 URL（http/https 开头），直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }
  
  // 相对路径，拼接基础 URL
  if (url.startsWith('/')) {
    return `http://localhost:8080${url}`;
  }
  
  return `http://localhost:8080/${url}`;
};

/**
 * 获取头像完整 URL
 */
export const getFullAvatarUrl = (avatarUrl) => {
  return getFullUrl(avatarUrl);
};

/**
 * 获取附件完整 URL
 */
export const getFullAttachmentUrl = (attachmentUrl) => {
  return getFullUrl(attachmentUrl);
};