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

      <div style="
        display: flex; 
        justify-content: flex-end;
        margin-top: 5%;
      ">
        <button class="visitorLoginBtn">游客登录</button>
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

.loginBtn:hover { background-color: #355a82; }

.visitorLoginBtn{
    /* 无边框 */
    border: none;
    /* 背景颜色 */
    background-color: transparent;
}

.visitorLoginBtn:hover {
  /* 文字样式 */
  text-decoration: underline;
}
</style>