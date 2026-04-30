<template>
  <div class="login-wrapper">
    <div class="login-card">
      <h2>登录</h2>
      
      <div class="inputBar">
        <span>用户名</span>
        <input type="text" v-model="form.username" placeholder="请输入用户名">
      </div>

      <div class="inputBar">
        <span>密码</span>
        <input type="password" v-model="form.password" placeholder="请输入密码">
      </div>

      <button class="loginBtn" :disabled="isLoading" @click="handleLogin">
        {{ isLoading ? '登录中...' : '登录' }}
      </button>

      <div class="action-buttons">
        <button class="registerBtn" :disabled="isLoading" @click="handleRegister">注册</button>
        <button class="visitorLoginBtn" @click="handleVisitorLogin">游客登录</button>
      </div>

      <!-- 添加忘记密码链接 -->
      <div class="forgot-link">
        <router-link to="/forgot-password">忘记密码？</router-link>
      </div>
      
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { login, register } from '@/api/user';

const router = useRouter();
const route = useRoute();
const isLoading = ref(false);

const form = reactive({
  username: '',
  password: ''
});

const handleLogin = async () => {
  if (!form.username || !form.password) {
    alert('请输入用户名和密码');
    return;
  }

  isLoading.value = true;
  
  try {
    const result = await login(form.username, form.password);
    
    if (result.code === 200) {
      localStorage.setItem('token', result.data.token);
      localStorage.setItem('userId', result.data.userId);
      localStorage.setItem('username', result.data.username);
      
      alert('登录成功！');
      
      const redirectPath = route.query.redirect;
      if (redirectPath) {
        router.push(redirectPath);
      } else {
        router.push('/');
      }
    } else {
      alert(result.message || '登录失败');
    }
  } catch (error) {
    console.error('登录请求错误:', error);
    alert('服务器连接失败');
  } finally {
    isLoading.value = false;
  }
};

const handleRegister = async () => {
  if (!form.username || !form.password) {
    alert('请输入用户名和密码');
    return;
  }

  if (form.password.length < 6) {
    alert('密码长度至少6位');
    return;
  }

  isLoading.value = true;

  try {
    const result = await register(form.username, form.password);
    
    if (result.code === 200) {
      alert('注册成功！请登录');
      form.password = '';
    } else {
      alert(result.message || '注册失败');
    }
  } catch (error) {
    console.error('注册请求错误:', error);
    alert('服务器连接失败');
  } finally {
    isLoading.value = false;
  }
};

const handleVisitorLogin = async () => {
  isLoading.value = true;
  
  try {
    const result = await login('visitor', 'visitor123');
    if (result.code === 200) {
      localStorage.setItem('token', result.data.token);
      localStorage.setItem('userId', result.data.userId);
      localStorage.setItem('username', '游客');
      alert('以游客身份进入');
      
      const redirectPath = route.query.redirect;
      if (redirectPath) {
        router.push(redirectPath);
      } else {
        router.push('/');
      }
    }
  } catch (error) {
    alert('游客登录失败，请先注册账号');
  } finally {
    isLoading.value = false;
  }
};
</script>

<style scoped>
/* 样式保持不变，添加忘记密码样式 */
.login-wrapper {
  background: rgb(253, 253, 255);
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  font-family: system-ui, sans-serif;
}

.login-card {
  background: white;
  padding: 40px;
  border-radius: 20px;
  box-shadow: 0 10px 25px rgba(25, 59, 185, 0.1);
  width: 100%;
  max-width: 400px;
}

.inputBar { margin-bottom: 20px; }

.inputBar span {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.inputBar input {
  width: 100%;
  padding: 12px;
  border: 1px solid #ccc;
  border-radius: 10px;
  font-size: 16px;
  box-sizing: border-box;
}

.loginBtn {
  width: 100%;
  padding: 15px;
  background-color: #456f9d;
  color: white;
  border: none;
  border-radius: 15px;
  font-size: 18px;
  cursor: pointer;
  margin-top: 10px;
  transition: all 0.2s;
}

.loginBtn:hover:not(:disabled) { 
  background-color: #355a82; 
  transform: translateY(-2px);
}

.loginBtn:disabled {
  background-color: #999;
  cursor: not-allowed;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
}

.registerBtn,
.visitorLoginBtn {
  border: none;
  background-color: transparent;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  color: #456f9d;
  transition: all 0.2s;
  border-radius: 8px;
}

.registerBtn:hover:not(:disabled),
.visitorLoginBtn:hover {
  text-decoration: underline;
  background-color: rgba(69, 111, 157, 0.1);
}

.registerBtn:disabled {
  color: #999;
  cursor: not-allowed;
}

/* 新增忘记密码样式 */
.forgot-link {
  text-align: center;
  margin-top: 16px;
}

.forgot-link a {
  color: #999;
  text-decoration: none;
  font-size: 13px;
  transition: all 0.2s;
}

.forgot-link a:hover {
  color: #456f9d;
  text-decoration: underline;
}
</style>