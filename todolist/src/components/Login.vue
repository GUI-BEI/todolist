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

      <button class="loginBtn" @click="handleLogin">登录</button>

      <div class="action-buttons">
        <button class="registerBtn" @click="handleRegister">注册</button>
        <button class="visitorLoginBtn" @click="handleVisitorLogin">游客登录</button>
      </div>
      
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();
const form = reactive({
  username: '',
  password: ''
});

const handleLogin = async () => {
  if (!form.username || !form.password) {
    alert('请输入用户名和密码');
    return;
  }

  try {
    const result = await signin(form);
    console.log('Success:', result.data);
    alert('登录成功！');
  } catch (error) {
    console.error('Error:', error);
    alert('登录失败！')
  }
};

// 注册函数
const handleRegister = async () => {
  if (!form.username || !form.password) {
    alert('请输入用户名和密码');
    return;
  }

  if (form.password.length < 6) {
    alert('密码长度至少6位');
    return;
  }

  try {
    const response = await fetch('http://localhost:8080/api/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(form)
    });
    const result = await response.json();
    
    if (result.code === 200) {
      alert('注册成功！请登录');
      // 清空密码，保留用户名方便登录
      form.password = '';
    } else {
      alert(result.message || '注册失败');
    }
  } catch (error) {
    console.error('注册请求错误:', error);
    alert('服务器连接失败');
  }
};

// 游客登录
const handleVisitorLogin = () => {
  // 游客可以直接进入，不需要后端验证
  alert('以游客身份进入');
  router.push('/');
};
</script>

<style scoped>
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
  box-sizing: border-box; /* 确保padding不会撑破容器 */
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
}

.loginBtn:hover { 
  background-color: #355a82; 
}

/* 按钮容器 - 同一行 */
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

.registerBtn:hover,
.visitorLoginBtn:hover {
  text-decoration: underline;
  background-color: rgba(69, 111, 157, 0.1);
}
</style>