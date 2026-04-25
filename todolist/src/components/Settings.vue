<template>
  <div class="settings-wrapper">
    <div class="settings-card">
      <h2>账户设置</h2>
      
      <div class="settings-form">
        <!-- 用户名修改 -->
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

        <!-- 密码修改 -->
        <div class="form-group">
          <label>密码</label>
          <div class="input-wrapper">
            <input 
              :type="showPassword ? 'text' : 'password'" 
              v-model="form.password" 
              :disabled="isLoading"
              placeholder="请输入新密码"
            />
            <button 
              type="button" 
              class="toggle-pwd" 
              @click="showPassword = !showPassword"
            >
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
          <p class="field-desc">建议使用字母、数字组合，长度6-20位</p>
        </div>

        <!-- 确认密码（可选，增强安全性） -->
        <div class="form-group">
          <label>确认密码</label>
          <div class="input-wrapper">
            <input 
              :type="showConfirmPwd ? 'text' : 'password'" 
              v-model="confirmPassword" 
              :disabled="isLoading"
              placeholder="请再次输入新密码"
            />
            <button 
              type="button" 
              class="toggle-pwd" 
              @click="showConfirmPwd = !showConfirmPwd"
            >
              {{ showConfirmPwd ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>
      </div>

      <div class="form-actions">
        <button 
          class="save-btn" 
          :disabled="isLoading || !hasChanges"
          @click="handleSubmit"
        >
          {{ isLoading ? '提交中...' : '保存修改' }}
        </button>
      </div>

      <!-- 提示信息 -->
      <div v-if="message" class="message" :class="{ error: isError }">
        {{ message }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';

// 表单数据
const form = reactive({
  username: '',
  password: ''
});

const confirmPassword = ref('');
const showPassword = ref(false);
const showConfirmPwd = ref(false);
const isLoading = ref(false);
const message = ref('');
const isError = ref(false);
const originalUsername = ref(''); // 用于判断是否有修改

// 判断是否有修改
const hasChanges = computed(() => {
  return form.username !== originalUsername.value || form.password.length > 0;
});

// 显示消息
const showMessage = (text, error = false) => {
  message.value = text;
  isError.value = error;
  setTimeout(() => {
    message.value = '';
  }, 3000);
};

// 获取用户信息
const fetchUserInfo = async () => {
  isLoading.value = true;
  
  try {
    const res = await fetch('http://localhost:8080/api/user/info', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    });
    const result = await res.json();
    
    if (result.code === 200) {
      form.username = result.data.username;
      originalUsername.value = result.data.username;
      form.password = '';  // 密码清空，不显示原密码
      confirmPassword.value = '';
    } else {
      throw new Error(result.message || '获取用户信息失败');
    }
  } catch (err) {
    console.error('获取用户信息失败', err);
    // 测试数据
    form.username = 'test_user';
    originalUsername.value = 'test_user';
    form.password = '';
    confirmPassword.value = '';
    showMessage('使用测试数据，后端连接失败', true);
  } finally {
    isLoading.value = false;
  }
};

// 提交修改
const handleSubmit = async () => {
  // 验证
  if (!form.username.trim()) {
    showMessage('用户名不能为空', true);
    return;
  }
  
  if (form.username.length < 3) {
    showMessage('用户名长度至少3位', true);
    return;
  }
  
  // 如果密码不为空，才进行密码验证
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
    const updateData = {
      username: form.username.trim()
    };
    
    // 只有输入了新密码才提交密码字段
    if (form.password) {
      updateData.password = form.password;
    }
    
    const res = await fetch('http://localhost:8080/api/user/update', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(updateData)
    });
    
    const result = await res.json();
    
    if (result.code === 200) {
      originalUsername.value = form.username;
      form.password = '';
      confirmPassword.value = '';
      showMessage('修改成功！');
    } else {
      throw new Error(result.message || '修改失败');
    }
  } catch (err) {
    console.error('修改失败', err);
    showMessage('修改失败，请稍后重试', true);
    // 测试模式
    if (err.message === 'Failed to fetch') {
      originalUsername.value = form.username;
      form.password = '';
      confirmPassword.value = '';
      showMessage('修改成功！(测试模式)');
    }
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchUserInfo();
});
</script>

<style scoped>
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

.input-wrapper input {
  flex: 1;
  padding: 12px 14px;
  border: 1px solid #ddd;
  border-radius: 12px;
  font-size: 15px;
  transition: all 0.2s;
  font-family: inherit;
}

.input-wrapper input:focus {
  outline: none;
  border-color: #5c83d8;
  box-shadow: 0 0 0 2px rgba(92, 131, 216, 0.1);
}

.input-wrapper input:disabled {
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
</style>