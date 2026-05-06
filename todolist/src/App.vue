<template>
  <div class="app-wrapper">
    <nav class="nav-bar">
      <router-link to="/week">首页 (周视图)</router-link>
      <router-link to="/month">首页 (月视图)</router-link>
      <router-link to="/add">添加新任务</router-link>
      <router-link to="/recurring">添加周期任务</router-link>
      <router-link to="/history">足迹</router-link>
      <router-link to="/sign">签到</router-link>
      <router-link to="/settings">设置</router-link>

      <button class="dark-mode-btn" @click="toggleDarkMode">
        {{ isDarkMode ? '☀' : '☽' }}
      </button>

      <!-- 未登录：显示登录按钮 -->
      <router-link v-if="!isLoggedIn" to="/login" class="login-link">登录</router-link>
      
      <!-- 已登录：显示用户信息 -->
      <div v-else class="user-info" @click="goToSettings">
        <div class="avatar-small">
          <img v-if="avatarUrl" :src="getFullAvatarUrl(avatarUrl)" alt="头像">
          <span v-else class="avatar-small-placeholder">{{ usernameInitial }}</span>
        </div>
        <span class="username">{{ username }}</span>
      </div>
    </nav>

    <main class="page-container">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { getUserInfo } from '@/api/user';
import { on, off } from '@/utils/eventBus';
import { getFullAvatarUrl } from '@/utils/urlHelper';

const router = useRouter();
const username = ref('');
const avatarUrl = ref('');
const isDarkMode = ref(false);

// 添加：判断是否已登录（使用 ref 而不是 computed，以便手动更新）
const isLoggedIn = ref(false);

// 检查登录状态
const checkLoginStatus = () => {
  const token = localStorage.getItem('token');
  isLoggedIn.value = !!token;
  console.log('登录状态检查:', isLoggedIn.value, 'token:', token);
  return isLoggedIn.value;
};

// 跳转到设置页面的方法
const goToSettings = () => {
  router.push('/settings');
};

// 切换黑夜模式
const toggleDarkMode = () => {
  isDarkMode.value = !isDarkMode.value;
  localStorage.setItem('darkMode', isDarkMode.value);
  if (isDarkMode.value) {
    document.body.classList.add('dark-mode');
  } else {
    document.body.classList.remove('dark-mode');
  }
};

// 加载黑夜模式偏好
const loadDarkModePreference = () => {
  const saved = localStorage.getItem('darkMode');
  if (saved !== null) {
    isDarkMode.value = saved === 'true';
  } else {
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    isDarkMode.value = prefersDark;
  }
  
  if (isDarkMode.value) {
    document.body.classList.add('dark-mode');
  }
};

const usernameInitial = computed(() => {
  return username.value ? username.value.charAt(0).toUpperCase() : '?';
});

const fetchUserInfo = async () => {
  const token = localStorage.getItem('token');
  console.log('fetchUserInfo - token:', token);
  
  if (!token) {
    console.log('无token，跳过获取用户信息');
    isLoggedIn.value = false;
    return;
  }
  
  try {
    const result = await getUserInfo();
    console.log('getUserInfo 返回:', result);
    
    if (result.code === 200) {
      username.value = result.data.username;
      avatarUrl.value = result.data.avatarUrl || '';
      isLoggedIn.value = true;
      console.log('用户信息获取成功:', username.value, avatarUrl.value);
    } else {
      // token 无效，清除登录状态
      console.log('获取用户信息失败，清除token');
      localStorage.removeItem('token');
      isLoggedIn.value = false;
    }
  } catch (err) {
    console.error('获取用户信息失败', err);
    isLoggedIn.value = false;
  }
};

const handleUserInfoUpdate = () => {
  console.log('收到 userLoggedIn 事件，刷新用户信息');
  fetchUserInfo();
};

// 监听 localStorage 变化
const handleStorageChange = (e) => {
  console.log('storage 事件:', e.key, e.newValue);
  if (e.key === 'avatarUpdated') {
    fetchUserInfo();
  }
  if (e.key === 'token') {
    checkLoginStatus();
    if (e.newValue) {
      fetchUserInfo();
    } else {
      // token 被清除
      username.value = '';
      avatarUrl.value = '';
      isLoggedIn.value = false;
    }
  }
};

// 定时检查（备用方案）
let checkTimer;
const startCheck = () => {
  checkTimer = setInterval(() => {
    const token = localStorage.getItem('token');
    if (!!token !== isLoggedIn.value) {
      console.log('定时检查发现登录状态变化');
      checkLoginStatus();
      if (token) {
        fetchUserInfo();
      } else {
        username.value = '';
        avatarUrl.value = '';
      }
    }
  }, 2000);
};

// 监听自定义事件
const handleAvatarChanged = () => {
  console.log('收到 avatarChanged 事件');
  fetchUserInfo();
};

onMounted(() => {
  console.log('App.vue mounted');
  loadDarkModePreference();
  
  // 先检查登录状态
  checkLoginStatus();
  
  // 如果已登录，获取用户信息
  if (isLoggedIn.value) {
    fetchUserInfo();
  }
  
  // 注册事件监听
  on('userLoggedIn', handleUserInfoUpdate);
  window.addEventListener('avatarChanged', handleAvatarChanged);
  window.addEventListener('storage', handleStorageChange);
  
  startCheck();
});

onUnmounted(() => {
  off('userLoggedIn', handleUserInfoUpdate);
  window.removeEventListener('avatarChanged', handleAvatarChanged);
  window.removeEventListener('storage', handleStorageChange);
  clearInterval(checkTimer);
});
</script>

<style>
/* ===== 全局样式（影响所有组件） ===== */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: system-ui, -apple-system, sans-serif;
  background: #f5f7fe;
}

/* ===== 黑夜模式全局 ===== */
body.dark-mode {
  background: #1a1a2e;
  color: #e0e0e0;
}
</style>

<style scoped>
/* ===== App.vue 专属样式 ===== */

.app-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* 导航栏样式 */
.nav-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;
  z-index: 100;
  flex-wrap: wrap;
}

.nav-bar a {
  text-decoration: none;
  color: #555;
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
}

.nav-bar a:hover {
  background: #e8ecf8;
  color: #2c4c96;
}

.nav-bar a.router-link-active {
  background: #5c83d8;
  color: white;
}

/* 黑夜模式按钮 */
.dark-mode-btn {
  background: none;
  border: 1px solid #ddd;
  border-radius: 20px;
  padding: 6px 10px;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dark-mode-btn:hover {
  background: #f0f0f0;
}

/* 登录按钮 */
.login-link {
  margin-left: auto !important;
  background: #5c83d8;
  color: white !important;
  padding: 8px 20px !important;
  border-radius: 20px !important;
}

.login-link:hover {
  background: #456f9d !important;
  color: white !important;
}

/* 用户信息 - 确保显示 */
.user-info {
  display: flex !important;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 20px;
  transition: all 0.2s;
}

.user-info:hover {
  background: #f0f0f0;
}

.avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background: #e0e0e0;
  flex-shrink: 0;
}

.avatar-small img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-small-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #5c83d8;
  color: white;
  font-size: 14px;
  font-weight: bold;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
}

/* 主内容区域 */
.page-container {
  flex: 1;
  padding: 0;
}

/* ===== 黑夜模式 ===== */
body.dark-mode .nav-bar {
  background: #0f0f1a;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

body.dark-mode .nav-bar a {
  color: #cccccc;
}

body.dark-mode .nav-bar a:hover {
  background: #1a1a2e;
  color: #8ab3ff;
}

body.dark-mode .nav-bar a.router-link-active {
  background: #4a6fb8;
  color: white;
}

body.dark-mode .dark-mode-btn {
  border-color: #2c2c3e;
  color: #e0e0e0;
}

body.dark-mode .dark-mode-btn:hover {
  background: #1a1a2e;
}

body.dark-mode .login-link {
  background: #4a6fb8;
  color: white !important;
}

body.dark-mode .user-info:hover {
  background: #1a1a2e;
}

body.dark-mode .username {
  color: #e0e0e0;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .nav-bar {
    padding: 10px 16px;
    gap: 8px;
  }
  
  .nav-bar a {
    padding: 6px 12px;
    font-size: 13px;
  }
  
  .username {
    display: none;
  }
}
</style>