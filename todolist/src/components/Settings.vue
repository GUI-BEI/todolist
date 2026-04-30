<template>
  <div class="settings-wrapper">
    <div class="settings-card">
      <h2>账户设置</h2>
      
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
import { getUserInfo, updateUser, setSecurityQuestion } from '@/api/user';

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
      // 检查是否已设置密保
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
  router.push('/login');
};

onMounted(() => {
  fetchUserInfo();
});
</script>

<style scoped>
/* 添加密保区域样式 */
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

.info-message {
  padding: 10px;
  background-color: #e8f5e9;
  border-radius: 8px;
  color: #4caf50;
  font-size: 14px;
}

select {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #ddd;
  border-radius: 12px;
  font-size: 15px;
  font-family: inherit;
  background-color: white;
}

/* 其他样式保持不变 */
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

.settings-form {
  margin-bottom: 28px;
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