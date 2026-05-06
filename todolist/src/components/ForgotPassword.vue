<template>
  <div class="forgot-wrapper">
    <div class="forgot-card">
      <h2>找回密码</h2>
      
      <!-- 步骤1：输入用户名 -->
      <div v-if="step === 1" class="step">
        <div class="inputBar">
          <span>用户名</span>
          <input type="text" v-model="username" placeholder="请输入用户名" @keyup.enter="fetchSecurityQuestion">
        </div>
        <button class="next-btn" :disabled="isLoading || !username.trim()" @click="fetchSecurityQuestion">
          {{ isLoading ? '验证中...' : '下一步' }}
        </button>
        <div class="back-link">
          <router-link to="/login">返回登录</router-link>
        </div>
      </div>

      <!-- 步骤2：回答密保问题 -->
      <div v-if="step === 2" class="step">
        <div class="question-box">
          <p class="question-label">密保问题：</p>
          <p class="question-text">{{ securityQuestion }}</p>
        </div>
        <div class="inputBar">
          <span>密保答案</span>
          <input type="text" v-model="securityAnswer" placeholder="请输入密保答案" @keyup.enter="verifyAnswer">
        </div>
        <button class="next-btn" :disabled="isLoading || !securityAnswer.trim()" @click="verifyAnswer">
          {{ isLoading ? '验证中...' : '验证答案' }}
        </button>
        <button class="back-btn" @click="goBack">返回</button>
      </div>

      <!-- 步骤3：重置密码 -->
      <div v-if="step === 3" class="step">
        <div class="success-message">✓ 验证成功！请设置新密码</div>
        <div class="inputBar">
          <span>新密码</span>
          <input type="password" v-model="newPassword" placeholder="请输入新密码（至少6位）" @keyup.enter="resetPassword">
        </div>
        <div class="inputBar">
          <span>确认密码</span>
          <input type="password" v-model="confirmPassword" placeholder="请再次输入新密码" @keyup.enter="resetPassword">
        </div>
        <button class="reset-btn" :disabled="isLoading || !newPassword || !confirmPassword" @click="resetPassword">
          {{ isLoading ? '重置中...' : '重置密码' }}
        </button>
        <div class="back-link">
          <router-link to="/login">返回登录</router-link>
        </div>
      </div>

      <div v-if="message" class="message" :class="{ error: isError }">
        {{ message }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { getSecurityQuestion, resetPassword as resetPasswordApi, verifySecurityAnswer } from '@/api/user';

const router = useRouter();

const step = ref(1);
const username = ref('');
const securityQuestion = ref('');
const securityAnswer = ref('');
const newPassword = ref('');
const confirmPassword = ref('');
const isLoading = ref(false);
const message = ref('');
const isError = ref(false);

const showMessage = (text, error = false) => {
  message.value = text;
  isError.value = error;
  setTimeout(() => {
    message.value = '';
  }, 3000);
};

const goBack = () => {
  step.value = 1;
  securityAnswer.value = '';
  message.value = '';
};

// 步骤1：获取密保问题
const fetchSecurityQuestion = async () => {
  if (!username.value.trim()) {
    showMessage('请输入用户名', true);
    return;
  }

  isLoading.value = true;
  message.value = '';

  try {
    const result = await getSecurityQuestion(username.value);
    console.log('getSecurityQuestion 返回:', result);
    
    if (result.code === 200 && result.data) {
      securityQuestion.value = result.data;
      step.value = 2;
      showMessage('请回答密保问题');
    } else {
      showMessage(result.message || '获取密保问题失败', true);
    }
  } catch (err) {
    console.error('获取密保问题失败', err);
    if (err.response && err.response.data) {
      showMessage(err.response.data.message || '用户不存在或未设置密保', true);
    } else {
      showMessage('服务器连接失败，请稍后重试', true);
    }
  } finally {
    isLoading.value = false;
  }
};

// 步骤2：验证密保答案
const verifyAnswer = async () => {
  if (!securityAnswer.value.trim()) {
    showMessage('请输入密保答案', true);
    return;
  }

  isLoading.value = true;
  message.value = '';

  try {
    console.log('========== 开始验证答案 ==========');
    console.log('用户名:', username.value);
    console.log('答案:', securityAnswer.value);
    
    const result = await verifySecurityAnswer(username.value, securityAnswer.value);
    
    console.log('verifySecurityAnswer 返回:', result);
    console.log('返回码:', result.code);
    console.log('返回消息:', result.message);
    
    if (result.code === 200) {
      step.value = 3;
      showMessage('验证成功，请设置新密码');
    } else {
      showMessage(result.message || '密保答案错误', true);
      securityAnswer.value = '';
    }
  } catch (err) {
    console.error('========== 验证失败 ==========');
    console.error('错误对象:', err);
    
    if (err.response) {
      console.error('HTTP状态码:', err.response.status);
      console.error('响应数据:', err.response.data);
      
      if (err.response.status === 401) {
        showMessage('密保答案错误，请重新输入', true);
        securityAnswer.value = '';
      } else if (err.response.data && err.response.data.message) {
        showMessage(err.response.data.message, true);
      } else {
        showMessage(`请求失败: ${err.response.status}`, true);
      }
    } else if (err.request) {
      console.error('没有收到响应:', err.request);
      showMessage('服务器无响应，请检查后端是否运行', true);
    } else {
      console.error('请求配置错误:', err.message);
      showMessage(err.message || '请求失败', true);
    }
  } finally {
    isLoading.value = false;
  }
};

// 步骤3：重置密码
const resetPassword = async () => {
  if (!newPassword.value) {
    showMessage('请输入新密码', true);
    return;
  }
  
  if (newPassword.value.length < 6) {
    showMessage('密码长度至少6位', true);
    return;
  }

  if (newPassword.value !== confirmPassword.value) {
    showMessage('两次输入的密码不一致', true);
    return;
  }

  isLoading.value = true;
  message.value = '';

  try {
    const result = await resetPasswordApi(username.value, securityAnswer.value, newPassword.value);
    console.log('resetPassword 返回:', result);
    
    if (result.code === 200) {
      showMessage('密码重置成功！请重新登录');
      setTimeout(() => {
        router.push('/login');
      }, 2000);
    } else {
      showMessage(result.message || '重置失败，请重试', true);
    }
  } catch (err) {
    console.error('重置密码失败', err);
    if (err.response && err.response.data) {
      showMessage(err.response.data.message || '重置失败', true);
    } else {
      showMessage('服务器连接失败，请稍后重试', true);
    }
  } finally {
    isLoading.value = false;
  }
};
</script>

<style>
/* 黑夜模式 - ForgotPassword */
body.dark-mode .forgot-wrapper {
  background: #1a1a2e;
}

body.dark-mode .forgot-card {
  background: #16213e;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
}

body.dark-mode .forgot-card h2 {
  color: #8ab3ff;
}

body.dark-mode .inputBar span {
  color: #e0e0e0;
}

body.dark-mode .inputBar input {
  background: #2c2c3e;
  border-color: #3a3a4e;
  color: #e0e0e0;
}

body.dark-mode .inputBar input:focus {
  border-color: #5c83d8;
  outline: none;
}

body.dark-mode .question-box {
  background: #0f0f1a;
}

body.dark-mode .question-label {
  color: #aaaaaa;
}

body.dark-mode .question-text {
  color: #8ab3ff;
}

body.dark-mode .success-message {
  background: #0a2e1a;
  color: #6fbf6f;
}

body.dark-mode .next-btn,
body.dark-mode .reset-btn {
  background: #4a6fb8;
}

body.dark-mode .next-btn:hover:not(:disabled),
body.dark-mode .reset-btn:hover:not(:disabled) {
  background: #5c83d8;
}

body.dark-mode .next-btn:disabled,
body.dark-mode .reset-btn:disabled {
  background: #2a2a3e;
  color: #888888;
}

body.dark-mode .back-btn {
  background: transparent;
  border-color: #3a3a4e;
  color: #aaaaaa;
}

body.dark-mode .back-btn:hover {
  background: #1a1a2e;
}

body.dark-mode .back-link a {
  color: #888888;
}

body.dark-mode .back-link a:hover {
  color: #8ab3ff;
}

body.dark-mode .message {
  background: #0a2e1a;
  color: #6fbf6f;
}

body.dark-mode .message.error {
  background: #4a1a1a;
  color: #f08080;
}
</style>

<style scoped>
.forgot-wrapper {
  background: rgb(255, 255, 255);
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  font-family: system-ui, sans-serif;
}

.forgot-card {
  background: white;
  padding: 40px;
  border-radius: 20px;
  box-shadow: 0 10px 25px rgba(25, 59, 185, 0.1);
  width: 100%;
  max-width: 400px;
}

.forgot-card h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #2c4c96;
}

.step {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.inputBar span {
  display: block;
  margin-bottom: 8px;
  font-weight: bold;
  color: #333;
}

.inputBar input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 10px;
  font-size: 16px;
  box-sizing: border-box;
  transition: all 0.2s;
}

.inputBar input:focus {
  outline: none;
  border-color: #5c83d8;
  box-shadow: 0 0 0 2px rgba(92, 131, 216, 0.1);
}

.question-box {
  background: #f5f5f5;
  padding: 16px;
  border-radius: 12px;
  text-align: center;
}

.question-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.question-text {
  font-size: 18px;
  font-weight: 600;
  color: #2c4c96;
}

.success-message {
  text-align: center;
  padding: 16px;
  background: #e8f5e9;
  color: #4caf50;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
}

.next-btn, .reset-btn {
  width: 100%;
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

.next-btn:hover:not(:disabled),
.reset-btn:hover:not(:disabled) {
  background-color: #456f9d;
  transform: translateY(-2px);
}

.next-btn:disabled,
.reset-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
  transform: none;
}

.back-btn {
  width: 100%;
  padding: 12px;
  background-color: transparent;
  color: #666;
  border: 1px solid #ddd;
  border-radius: 40px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  background-color: #f5f5f5;
}

.back-link {
  text-align: center;
  margin-top: 16px;
}

.back-link a {
  color: #5c83d8;
  text-decoration: none;
  font-size: 14px;
}

.back-link a:hover {
  text-decoration: underline;
}

.message {
  margin-top: 20px;
  padding: 10px;
  border-radius: 8px;
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