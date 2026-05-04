<template>
  <div class="settings-wrapper">
    <div class="settings-card">
      <h2>账户设置</h2>
      
      <!-- 头像区域 -->
      <div class="avatar-section">
        <div class="avatar-container" @click="triggerFileInput">
          <img v-if="avatarUrl" :src="getFullAvatarUrl(avatarUrl)" class="avatar-img" alt="头像">
          <div v-else class="avatar-placeholder">
            <span>{{ usernameInitial }}</span>
          </div>
          <div class="avatar-overlay">
            <span>✚</span>
          </div>
        </div>
        <input 
          type="file" 
          ref="fileInput" 
          accept="image/jpeg,image/png,image/gif"
          style="display: none"
          @change="handleAvatarUpload"
        />
        <p class="avatar-hint">点击上传头像（支持 JPG、PNG，最大1MB）</p>
      </div>

      <!-- 基本信息设置 -->
      <div class="settings-form">
        <div class="form-group">
          <label>用户名</label>
          <div class="input-wrapper">
            <input 
              type="text" 
              v-model="form.username" 
              :disabled="isLoading"
              placeholder="请输入用户名"
            />
          </div>
          <p class="field-desc">用于登录的唯一标识</p>
        </div>

        <div class="form-group">
          <label>密码</label>
          <div class="input-wrapper">
            <input 
              :type="showPassword ? 'text' : 'password'" 
              v-model="form.password" 
              :disabled="isLoading"
              placeholder="请输入新密码"
            />
            <button type="button" class="toggle-pwd" @click="showPassword = !showPassword">
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
          <p class="field-desc">建议使用字母、数字组合，长度6-20位</p>
        </div>

        <div class="form-group">
          <label>确认密码</label>
          <div class="input-wrapper">
            <input 
              :type="showConfirmPwd ? 'text' : 'password'" 
              v-model="confirmPassword" 
              :disabled="isLoading"
              placeholder="请再次输入新密码"
            />
            <button type="button" class="toggle-pwd" @click="showConfirmPwd = !showConfirmPwd">
              {{ showConfirmPwd ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 密保问题设置 -->
      <div class="settings-form security-section">
        <h3>密保设置</h3>
        
        <div class="form-group">
          <label>密保问题</label>
          <div class="input-wrapper">
            <select v-model="securityForm.question" :disabled="isLoading || isSecuritySet">
              <option value="">请选择密保问题</option>
              <option value="你的小学名称是什么？">你的小学名称是什么？</option>
              <option value="你的出生城市是哪里？">你的出生城市是哪里？</option>
              <option value="你的宠物名字是什么？">你的宠物名字是什么？</option>
              <option value="你最喜欢的书籍是什么？">你最喜欢的书籍是什么？</option>
              <option value="你母亲的生日是什么？">你母亲的生日是什么？</option>
            </select>
          </div>
        </div>

        <div class="form-group">
          <label>密保答案</label>
          <div class="input-wrapper">
            <input 
              type="text" 
              v-model="securityForm.answer" 
              :disabled="isLoading || isSecuritySet"
              placeholder="请输入密保答案"
            />
          </div>
          <p class="field-desc">请牢记答案，用于找回密码</p>
        </div>

        <div class="form-group" v-if="isSecuritySet">
          <div class="info-message">
            已设置密保问题
          </div>
        </div>

        <div class="form-actions">
          <button 
            class="save-btn" 
            :disabled="isLoading || !securityForm.question || !securityForm.answer || isSecuritySet" 
            @click="handleSetSecurity"
          >
            {{ isLoading ? '提交中...' : '设置密保' }}
          </button>
        </div>
      </div>

      <div class="form-actions">
        <button class="save-btn" :disabled="isLoading || !hasChanges" @click="handleSubmit">
          {{ isLoading ? '提交中...' : '保存修改' }}
        </button>
      </div>

      <div v-if="message" class="message" :class="{ error: isError }">
        {{ message }}
      </div>
      
      <div class="logout-section">
        <button class="logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getUserInfo, updateUser, setSecurityQuestion, uploadAvatar } from '@/api/user';
import { getFullAvatarUrl } from '@/utils/urlHelper';

const router = useRouter();

const form = reactive({
  username: '',
  password: ''
});

const securityForm = reactive({
  question: '',
  answer: ''
});

const confirmPassword = ref('');
const showPassword = ref(false);
const showConfirmPwd = ref(false);
const isLoading = ref(false);
const message = ref('');
const isError = ref(false);
const originalUsername = ref('');
const isSecuritySet = ref(false);
const avatarUrl = ref('');
const fileInput = ref(null);

// 用户名首字母（用于默认头像）
const usernameInitial = computed(() => {
  return form.username ? form.username.charAt(0).toUpperCase() : '?';
});

const hasChanges = computed(() => {
  return form.username !== originalUsername.value || form.password.length > 0;
});

const showMessage = (text, error = false) => {
  message.value = text;
  isError.value = error;
  setTimeout(() => {
    message.value = '';
  }, 3000);
};

// 触发文件选择
const triggerFileInput = () => {
  fileInput.value.click();
};

// 上传头像
const handleAvatarUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    showMessage('请选择图片文件', true);
    return;
  }
  
  // 验证文件大小（2MB）
  if (file.size > 2 * 1024 * 1024) {
    showMessage('图片大小不能超过2MB', true);
    return;
  }
  
  isLoading.value = true;
  
  try {
    const result = await uploadAvatar(file);
    if (result.code === 200) {
      avatarUrl.value = result.data;
      showMessage('头像上传成功');
      window.dispatchEvent(new CustomEvent('avatarChanged'));
      localStorage.setItem('avatarUpdated', Date.now().toString());
    } else {
      throw new Error(result.message || '上传失败');
    }
  } catch (err) {
    console.error('上传头像失败', err);
    showMessage('上传失败，请稍后重试', true);
  } finally {
    isLoading.value = false;
    event.target.value = '';
  }
};

const fetchUserInfo = async () => {
  const token = localStorage.getItem('token');
  if (!token) {
    router.push('/login');
    return;
  }
  
  isLoading.value = true;
  
  try {
    const result = await getUserInfo();
    if (result.code === 200) {
      form.username = result.data.username;
      originalUsername.value = result.data.username;
      form.password = '';
      confirmPassword.value = '';
      avatarUrl.value = result.data.avatarUrl || '';
      isSecuritySet.value = result.data.securityQuestion && result.data.securityQuestion.length > 0;
      if (isSecuritySet.value) {
        securityForm.question = result.data.securityQuestion;
        securityForm.answer = '********';
      }
    } else {
      throw new Error(result.message || '获取用户信息失败');
    }
  } catch (err) {
    console.error('获取用户信息失败', err);
    showMessage('获取用户信息失败', true);
  } finally {
    isLoading.value = false;
  }
};

const handleSubmit = async () => {
  if (!form.username.trim()) {
    showMessage('用户名不能为空', true);
    return;
  }
  
  if (form.username.length < 3) {
    showMessage('用户名长度至少3位', true);
    return;
  }
  
  if (form.password) {
    if (form.password.length < 6) {
      showMessage('密码长度至少6位', true);
      return;
    }
    
    if (form.password !== confirmPassword.value) {
      showMessage('两次输入的密码不一致', true);
      return;
    }
  }
  
  isLoading.value = true;
  
  try {
    const updateData = {};
    if (form.username !== originalUsername.value) {
      updateData.username = form.username.trim();
    }
    if (form.password) {
      updateData.password = form.password;
    }
    
    const result = await updateUser(updateData.username, updateData.password);
    
    if (result.code === 200) {
      originalUsername.value = form.username;
      form.password = '';
      confirmPassword.value = '';
      localStorage.setItem('username', result.data.username);
      showMessage('修改成功！');
    } else {
      throw new Error(result.message || '修改失败');
    }
  } catch (err) {
    console.error('修改失败', err);
    showMessage('修改失败，请稍后重试', true);
  } finally {
    isLoading.value = false;
  }
};

const handleSetSecurity = async () => {
  if (!securityForm.question || !securityForm.answer) {
    showMessage('请填写密保问题和答案', true);
    return;
  }

  isLoading.value = true;

  try {
    const result = await setSecurityQuestion(securityForm.question, securityForm.answer);
    if (result.code === 200) {
      showMessage('密保设置成功！');
      isSecuritySet.value = true;
    } else {
      throw new Error(result.message || '设置失败');
    }
  } catch (err) {
    console.error('设置密保失败', err);
    showMessage('设置失败，请稍后重试', true);
  } finally {
    isLoading.value = false;
  }
};

const handleLogout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('userId');
  localStorage.removeItem('username');
  localStorage.removeItem('avatarUrl');
  router.push('/login');
};

onMounted(() => {
  fetchUserInfo();
});
</script>

<style>
/* 黑夜模式 - Settings */
body.dark-mode .settings-wrapper {
  background: #1a1a2e;
}

body.dark-mode .settings-card {
  background: #16213e;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

body.dark-mode .settings-card h2 {
  color: #8ab3ff;
}

body.dark-mode .avatar-section {
  border-bottom-color: #2c2c3e;
}

body.dark-mode .avatar-container {
  background-color: #2a2a3e;
}

body.dark-mode .avatar-hint {
  color: #888888;
}

body.dark-mode .settings-form {
  border-color: #2c2c3e;
}

body.dark-mode .security-section h3 {
  color: #8ab3ff;
}

body.dark-mode .form-group label {
  color: #e0e0e0;
}

body.dark-mode .input-wrapper input,
body.dark-mode .input-wrapper select {
  background: #2c2c3e;
  border-color: #3a3a4e;
  color: #e0e0e0;
}

body.dark-mode .input-wrapper input:disabled,
body.dark-mode .input-wrapper select:disabled {
  background-color: #1a1a2e;
  color: #888888;
}

body.dark-mode .toggle-pwd {
  background: #2a2a3e;
  color: #e0e0e0;
}

body.dark-mode .toggle-pwd:hover {
  background: #3a3a4e;
}

body.dark-mode .field-desc {
  color: #888888;
}

body.dark-mode .info-message {
  background: #0a2e1a;
  color: #6fbf6f;
}

body.dark-mode .save-btn {
  background: #4a6fb8;
}

body.dark-mode .save-btn:hover:not(:disabled) {
  background: #5c83d8;
}

body.dark-mode .save-btn:disabled {
  background: #2a2a3e;
  color: #888888;
}

body.dark-mode .message {
  background: #0a2e1a;
  color: #6fbf6f;
}

body.dark-mode .message.error {
  background: #4a1a1a;
  color: #f08080;
}

body.dark-mode .logout-section {
  border-top-color: #2c2c3e;
}

body.dark-mode .logout-btn {
  color: #f08080;
  border-color: #f08080;
}

body.dark-mode .logout-btn:hover {
  background-color: #f08080;
  color: white;
}
</style>

<style scoped>
/* 样式与之前相同，省略以节省空间 */
.settings-wrapper {
  background: rgb(255, 255, 255);
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.settings-card {
  background: white;
  border-radius: 24px;
  padding: 32px 40px;
  box-shadow: 0 10px 30px rgba(35, 19, 207, 0.1);
  width: 100%;
  max-width: 500px;
}

.settings-card h2 {
  margin: 0 0 24px 0;
  color: #2c4c96;
  font-size: 24px;
  font-weight: 600;
  text-align: center;
}

/* 头像区域 */
.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.avatar-container {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  cursor: pointer;
  overflow: hidden;
  background-color: #f0f0f0;
  transition: all 0.3s;
}

.avatar-container:hover .avatar-overlay {
  opacity: 1;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #5c83d8;
  color: white;
  font-size: 40px;
  font-weight: bold;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  font-size: 24px;
  color: white;
}

.avatar-hint {
  margin-top: 12px;
  font-size: 12px;
  color: #888;
  text-align: center;
}

/* 表单样式 */
.settings-form {
  margin-bottom: 28px;
}

.security-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.security-section h3 {
  margin: 0 0 20px 0;
  color: #2c4c96;
  font-size: 18px;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-wrapper input,
.input-wrapper select {
  flex: 1;
  padding: 12px 14px;
  border: 1px solid #ddd;
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.2s;
  font-family: inherit;
}

.input-wrapper input:focus,
.input-wrapper select:focus {
  outline: none;
  border-color: #5c83d8;
  box-shadow: 0 0 0 2px rgba(92, 131, 216, 0.1);
}

.input-wrapper input:disabled,
.input-wrapper select:disabled {
  background-color: #f5f5f5;
  color: #999;
}

.toggle-pwd {
  padding: 12px 16px;
  background: #f0f0f0;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
  white-space: nowrap;
}

.toggle-pwd:hover {
  background: #e0e0e0;
}

.field-desc {
  margin: 6px 0 0 0;
  font-size: 12px;
  color: #888;
}

.info-message {
  padding: 10px;
  background-color: #e8f5e9;
  border-radius: 8px;
  color: #4caf50;
  font-size: 14px;
  text-align: center;
}

.form-actions {
  margin-top: 8px;
}

.save-btn {
  width: 100%;
  padding: 14px 24px;
  background-color: #5c83d8;
  color: white;
  border: none;
  border-radius: 40px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.save-btn:hover:not(:disabled) {
  background-color: #456f9d;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(92, 131, 216, 0.3);
}

.save-btn:active:not(:disabled) {
  transform: translateY(0);
}

.save-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.message {
  margin-top: 20px;
  padding: 10px 16px;
  border-radius: 12px;
  text-align: center;
  font-size: 14px;
  background-color: #e8f5e9;
  color: #4caf50;
}

.message.error {
  background-color: #ffebee;
  color: #f44336;
}

.logout-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.logout-btn {
  width: 100%;
  padding: 12px 24px;
  background-color: transparent;
  color: #f44336;
  border: 1px solid #f44336;
  border-radius: 40px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.logout-btn:hover {
  background-color: #f44336;
  color: white;
}
</style>