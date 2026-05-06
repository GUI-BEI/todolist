<template>
  <div class="content-wrapper">
    <div class="task-form">
      <div class="inputBar">
        <span>标题</span>
        <input type="text" v-model="form.title" placeholder="标题">
      </div>

      <div class="inputBar">
        <span>描述</span>
        <input type="text" v-model="form.description" placeholder="描述">
      </div>

      <div class="inputBar">
        <span>优先级</span>
        <select class="priority" v-model="form.priority">
          <option value="3">高</option>
          <option value="2">中</option>
          <option value="1">低</option>
        </select>
      </div>

      <!-- 时间选择 -->
      <div class="inputBar">
        <span>开始时间</span>
        <input type="datetime-local" v-model="form.start">
      </div>

      <div class="inputBar">
        <span>结束时间</span>
        <input type="datetime-local" v-model="form.end">
      </div>

      <!-- 标签栏 -->
      <div class="inputBar">
        <span>标签</span>
        <div class="tag-bar">
          <div class="tag-list">
            <span 
              v-for="tag in tags" 
              :key="tag.tagName"
              class="tag-item"
              :style="{ backgroundColor: tag.tagColor || '#e0e0e0' }"
              @click="selectTag(tag.tagName)"
            >
              {{ tag.tagName }}
              <button class="tag-delete" @click.stop="removeTag(tag.tagName)">×</button>
            </span>
          </div>
          <div class="tag-add">
            <input 
              type="text" 
              v-model="newTagName" 
              placeholder="新标签"
              @keyup.enter="addNewTag"
              maxlength="20"
            />
            <button class="add-tag-btn" @click="addNewTag">+</button>
          </div>
        </div>
      </div>

      <div class="inputBar">
        <span>分类</span>
        <input type="text" v-model="form.type" placeholder="分类">
      </div>

      <!-- 附件上传区域 -->
      <div class="inputBar">
        <span>附件</span>
        <div class="attachment-area">
          <div class="attachment-list">
            <div v-for="att in attachments" :key="att.id" class="attachment-item">
              <span class="attachment-icon">📎</span>
              <span class="attachment-name" @click="previewAttachment(att)">
                {{ att.fileName }}
              </span>
              <span class="attachment-size">{{ formatFileSize(att.fileSize) }}</span>
              <button class="attachment-delete" @click="deleteAttachmentFile(att.id)">×</button>
            </div>
            <div v-if="attachments.length === 0 && !currentTaskId" class="attachment-empty">
              保存任务后可上传附件
            </div>
            <div v-else-if="attachments.length === 0 && currentTaskId" class="attachment-empty">
              暂无附件
            </div>
          </div>
          <div class="attachment-upload">
            <label class="upload-btn" :class="{ 'btn-disabled': !currentTaskId }">
              {{ currentTaskId ? '选择文件' : '请先保存任务' }}
              <input 
                type="file" 
                @change="uploadNewAttachment" 
                :disabled="!currentTaskId"
                style="display: none;"
              >
            </label>
            <span class="upload-hint">支持图片、文档等，最大50MB</span>
          </div>
          <div v-if="uploading" class="upload-progress">上传中...</div>
        </div>
      </div>

      <!-- 按钮区域 - 两个按钮并排 -->
      <div class="button-group">
        <button class="addBtn" @click="submitTask" :disabled="isSubmitting">
          {{ isSubmitting ? '提交中...' : '保存任务' }}
        </button>
        <button class="clearBtn" @click="clearAll" type="button">
          清空表单
        </button>
      </div>

      <!-- 成功提示条 -->
      <div v-if="showSuccessBar" class="success-bar">
        ✓ 任务保存成功！任务ID: {{ currentTaskId }}，可继续上传附件
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import { addTask, getTags, addTag, deleteTag, uploadAttachment, getAttachments, deleteAttachment } from '@/api/task';
import { useRouter } from 'vue-router';
import { showToast, showConfirm } from '@/utils/message';
import { getFullAttachmentUrl } from '@/utils/urlHelper';

const router = useRouter();

const form = reactive({
  title: '',
  description: '',
  priority: 3,
  start: '',
  end: '',
  type: ''
});

const tags = ref([]);
const newTagName = ref('');
const isSubmitting = ref(false);
const showSuccessBar = ref(false);
let successBarTimer = null;

// 附件相关
const attachments = ref([]);
const uploading = ref(false);
const currentTaskId = ref(null);

const formatFileSize = (bytes) => {
  if (!bytes) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
};

const previewAttachment = (attachment) => {
  const url = getFullAttachmentUrl(attachment.downloadUrl);
  window.open(url, '_blank');
};

const deleteAttachmentFile = async (attachmentId) => {
  const confirmed = await showConfirm('确定要删除此附件吗？');
  if (!confirmed) return;
  
  try {
    const result = await deleteAttachment(attachmentId);
    if (result.code === 200) {
      attachments.value = attachments.value.filter(a => a.id !== attachmentId);
      showToast('删除成功');
    } else {
      throw new Error(result.message || '删除失败');
    }
  } catch (err) {
    console.error('删除失败', err);
    showToast('删除失败', true);
  }
};

const loadAttachments = async (taskId) => {
  try {
    const result = await getAttachments(taskId);
    if (result.code === 200) {
      attachments.value = result.data;
    }
  } catch (err) {
    console.error('加载附件失败', err);
  }
};

const uploadNewAttachment = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  if (!currentTaskId.value) {
    showToast('请先保存任务后再上传附件', true);
    event.target.value = '';
    return;
  }
  
  if (file.size > 50 * 1024 * 1024) {
    showToast('文件大小不能超过50MB', true);
    event.target.value = '';
    return;
  }
  
  uploading.value = true;
  
  try {
    const result = await uploadAttachment(currentTaskId.value, file);
    if (result.code === 200) {
      attachments.value.push(result.data);
      showToast('附件上传成功');
    } else {
      throw new Error(result.message || '上传失败');
    }
  } catch (err) {
    console.error('上传失败:', err);
    showToast('上传失败: ' + (err.message || '未知错误'), true);
  } finally {
    uploading.value = false;
    event.target.value = '';
  }
};

const fetchTags = async () => {
  try {
    const result = await getTags();
    if (result.code === 200) {
      tags.value = result.data;
    }
  } catch (error) {
    console.error('获取标签失败', error);
  }
};

const selectTag = (tagName) => {
  form.type = tagName;
};

const addNewTag = async () => {
  const name = newTagName.value.trim();
  if (!name) return;
  
  try {
    const result = await addTag(name);
    if (result.code === 200) {
      tags.value.push(result.data);
      newTagName.value = '';
      showToast('标签添加成功');
    } else {
      showToast(result.message || '添加失败');
    }
  } catch (error) {
    console.error('添加标签失败', error);
    showToast('添加失败');
  }
};

const removeTag = async (tagName) => {
  const confirmed = await showConfirm(`确定要删除标签「${tagName}」吗？`);
  if (!confirmed) return;
  
  try {
    const result = await deleteTag(tagName);
    if (result.code === 200) {
      tags.value = tags.value.filter(t => t.tagName !== tagName);
      if (form.type === tagName) {
        form.type = '';
      }
      showToast('删除成功');
    } else {
      showToast(result.message || '删除失败');
    }
  } catch (error) {
    console.error('删除标签失败', error);
    showToast('删除失败');
  }
};

const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '';
  return dateTimeStr.replace('T', ' ') + ':00';
};

// 清空表单（不清空 currentTaskId，保留附件关联）
const clearFormOnly = () => {
  form.title = '';
  form.description = '';
  form.priority = 3;
  form.start = '';
  form.end = '';
  form.type = '';
};

// 完全清空（重置所有状态）
const clearAll = async () => {
  // 如果有附件，提示用户
  if (attachments.value.length > 0) {
    const confirmed = await showConfirm('清空表单将清除当前任务的所有附件，确定要清空吗？');
    if (!confirmed) return;
  }
  
  // 清空表单
  clearFormOnly();
  
  // 重置任务ID和附件列表
  currentTaskId.value = null;
  attachments.value = [];
  
  // 隐藏成功提示条
  showSuccessBar.value = false;
  if (successBarTimer) clearTimeout(successBarTimer);
  
  showToast('已清空');
};

const submitTask = async () => {
  const token = localStorage.getItem('token');
  if (!token) {
    router.push('/login');
    return;
  }
  
  if (!form.title || !form.start || !form.end) {
    showToast('请填写完整信息');
    return;
  }

  if (form.start >= form.end) {
    showToast('开始时间不能大于或等于结束时间');
    return;
  }

  isSubmitting.value = true;

  try {
    const taskData = {
      title: form.title,
      description: form.description,
      priority: parseInt(form.priority),
      start: formatDateTime(form.start),
      end: formatDateTime(form.end),
      type: form.type
    };
    
    const result = await addTask(taskData);
    if (result.code === 200) {
      currentTaskId.value = result.data.id;
      
      // 显示成功提示条
      showSuccessBar.value = true;
      if (successBarTimer) clearTimeout(successBarTimer);
      successBarTimer = setTimeout(() => {
        showSuccessBar.value = false;
      }, 5000);
      
      showToast('任务保存成功！可继续上传附件');
      
      // 加载该任务的附件（通常为空）
      await loadAttachments(currentTaskId.value);
      
      // 只清空标题和描述，保留时间和分类（因为创建的是同一个任务的不同版本？）
      // 这里选择不清空任何内容，让用户可以继续调整后保存新任务
      // 但如果用户想保存新任务，需要手动点清空按钮
    } else {
      showToast(result.message || '保存失败');
    }
  } catch (error) {
    console.error('Error:', error);
    showToast('保存失败，请稍后重试');
  } finally {
    isSubmitting.value = false;
  }
};

onMounted(() => {
  fetchTags();
});
</script>

<style>
/* ===== 全局样式（附件相关） ===== */

.attachment-area {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
  background: #fafafa;
  margin-top: 8px;
}

.attachment-list {
  max-height: 150px;
  overflow-y: auto;
  margin-bottom: 12px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: white;
  border-radius: 8px;
  margin-bottom: 6px;
  border: 1px solid #eee;
}

.attachment-icon {
  font-size: 16px;
}

.attachment-name {
  flex: 1;
  color: #2c4c96;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.attachment-name:hover {
  text-decoration: underline;
}

.attachment-size {
  font-size: 11px;
  color: #999;
}

.attachment-delete {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background: #ffebee;
  color: #f44336;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.attachment-delete:hover {
  background: #f44336;
  color: white;
}

.attachment-empty {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
}

.attachment-upload {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.upload-btn {
  padding: 6px 16px;
  background-color: #5c83d8;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.upload-btn.btn-disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.upload-btn:hover:not(.btn-disabled) {
  background-color: #456f9d;
}

.upload-hint {
  font-size: 11px;
  color: #999;
}

.upload-progress {
  margin-top: 8px;
  text-align: center;
  color: #5c83d8;
  font-size: 12px;
}

.success-bar {
  margin-top: 16px;
  padding: 12px;
  background-color: #e8f5e9;
  color: #4caf50;
  border-radius: 8px;
  text-align: center;
  font-size: 14px;
  animation: fadeOut 5s ease forwards;
}

@keyframes fadeOut {
  0% { opacity: 1; }
  80% { opacity: 1; }
  100% { opacity: 0; visibility: hidden; }
}

/* 黑夜模式附件样式 */
body.dark-mode .attachment-area {
  background: #0f0f1a;
  border-color: #2c2c3e;
}

body.dark-mode .attachment-item {
  background: #1a1a2e;
  border-color: #2c2c3e;
}

body.dark-mode .attachment-name {
  color: #8ab3ff;
}

body.dark-mode .upload-btn {
  background: #4a6fb8;
}

body.dark-mode .upload-btn.btn-disabled {
  background: #2a2a3e;
  color: #888;
}

body.dark-mode .success-bar {
  background: #0a2e1a;
  color: #6fbf6f;
}
</style>

<style scoped>
/* ===== TaskForm 原有样式 ===== */
.content-wrapper {
  background: rgb(255, 255, 255);
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 3% 1% 3%;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

.task-form {
  width: 100%;
  max-width: 600px;
}

.inputBar span {
  display: block;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 3%;
  border: transparent;
  font-weight: bold;
  font-size: 15px;
}

.inputBar input {
  margin-top: 3px;
  background: transparent;
  padding: 15px 5px;
  color: #222e41;
  transition: all 0.2s;
  width: 100%;
  height: auto;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}

.inputBar input[type="text"] {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}

input[type="datetime-local"] {
  font-size: 100%;
  font-weight: bold;
  font-family: inherit;
  padding: 15px 5px;
  border-radius: 0px 15px 15px 15px;
  border-color: rgb(114, 130, 156);
  background: transparent;
  width: 100%;
  height: auto;
  color: #222e41;
  transition: all 0.2s;
}

.priority {
  display: block;
  width: 100%;
  height: 5vh;
  margin-top: 15px;
  background-color: rgb(240, 242, 249);
  border-radius: 0px 15px 15px 15px;
  text-align-last: center;
}

/* 按钮组 - 两个按钮并排 */
.button-group {
  display: flex;
  gap: 16px;
  margin-top: 20px;
}

.addBtn {
  flex: 2;
  padding: 14px;
  background-color: #5c83d8;
  color: white;
  border: none;
  border-radius: 40px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.addBtn:active {
  border-color: rgb(106, 121, 145);
  background-color: rgba(220, 224, 234, 0.616);
}

.addBtn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.clearBtn {
  flex: 1;
  padding: 14px;
  background-color: transparent;
  color: #f44336;
  border: 1px solid #f44336;
  border-radius: 40px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.clearBtn:hover {
  background-color: #f44336;
  color: white;
}

/* 标签栏样式 */
.tag-bar {
  margin-top: 8px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 8px;
  padding: 10px;
  background: #f5f5f5;
  border-radius: 12px;
  min-height: 55px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 8px 5px 12px;
  background-color: #e0e0e0;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.tag-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.tag-delete {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: rgba(0,0,0,0.2);
  color: white;
  font-size: 12px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.tag-delete:hover {
  background: rgba(0,0,0,0.4);
}

.tag-add {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 5px;
}

.tag-add input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
  margin: 0;
}

.add-tag-btn {
  padding: 6px 16px;
  background-color: #5c83d8;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.add-tag-btn:hover {
  background-color: #456f9d;
}

/* 黑夜模式 - TaskForm */
body.dark-mode .content-wrapper {
  background: #1a1a2e;
}

body.dark-mode .inputBar span {
  color: #e0e0e0;
}

body.dark-mode .inputBar input,
body.dark-mode input[type="datetime-local"] {
  background: #2c2c3e;
  border-color: #3a3a4e;
  color: #e0e0e0;
}

body.dark-mode .priority {
  background-color: #2a2a3e;
  color: #e0e0e0;
}

body.dark-mode .tag-list {
  background: #0f0f1a;
}

body.dark-mode .tag-item {
  background-color: #4a6fb8 !important;
  color: #ffffff !important;
}

body.dark-mode .tag-delete {
  background: rgba(255, 255, 255, 0.2);
  color: #ff9999;
}

body.dark-mode .tag-add input {
  background: #2c2c3e;
  border-color: #3a3a4e;
  color: #e0e0e0;
}

body.dark-mode .add-tag-btn {
  background: #4a6fb8;
}

body.dark-mode .add-tag-btn:hover {
  background: #5c83d8;
}

body.dark-mode .addBtn {
  background: #4a6fb8;
}

body.dark-mode .addBtn:active {
  background: #3a5a9a;
}

body.dark-mode .clearBtn {
  color: #f08080;
  border-color: #f08080;
}

body.dark-mode .clearBtn:hover {
  background-color: #f08080;
  color: white;
}
</style>