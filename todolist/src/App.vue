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


      <!-- 头像和用户名 -->
      <div class="user-info" @click="router.push('/settings')">
        <div class="avatar-small">
          <img v-if="avatarUrl" :src="getFullAvatarUrl(avatarUrl)" alt="头像">
          <span v-else class="avatar-small-placeholder">{{ usernameInitial }}</span>
        </div>
        <span class="username">{{ username }}</span>
      </div>

      <router-link to="/login" style="margin-left: auto;">登录</router-link>
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

const router = useRouter();
const username = ref('');
const avatarUrl = ref('');
const isDarkMode = ref(false);

// 切换黑夜模式
const toggleDarkMode = () => {
  isDarkMode.value = !isDarkMode.value;
  localStorage.setItem('darkMode', isDarkMode.value);
  // 应用到 body，方便全局样式
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
    // 可选：根据系统主题自动判断
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
  if (!token) return;
  
  try {
    const result = await getUserInfo();
    if (result.code === 200) {
      username.value = result.data.username;
      avatarUrl.value = result.data.avatarUrl || '';
    }
  } catch (err) {
    console.error('获取用户信息失败', err);
  }
};

// 获取完整头像URL的方法
const getFullAvatarUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  return `http://localhost:8080${url}`;
};

// 监听用户信息更新事件
const handleUserInfoUpdate = () => {
  fetchUserInfo();
};

onMounted(() => {
  fetchUserInfo();
  on('userLoggedIn', handleUserInfoUpdate);
});

onUnmounted(() => {
  off('userLoggedIn', handleUserInfoUpdate);
});
</script>

<style>

/* 全局黑夜模式变量 */
:root {
  --bg-primary: #ffffff;
  --bg-secondary: #f5f7fe;
  --bg-card: #ffffff;
  --text-primary: #333333;
  --text-secondary: #666666;
  --border-color: #ddd;
  --shadow-color: rgba(0, 0, 0, 0.1);
  --nav-bg: #f0f0f0;
  --modal-bg: #ffffff;
  --input-bg: #ffffff;
  --scheduler-header: #f8f9fa;
  --scheduler-border: #dee2e6;
}

body.dark-mode {
  --bg-primary: #1a1a2e;
  --bg-secondary: #16213e;
  --bg-card: #0f3460;
  --text-primary: #e0e0e0;
  --text-secondary: #aaaaaa;
  --border-color: #2c2c3e;
  --shadow-color: rgba(0, 0, 0, 0.3);
  --nav-bg: #0f0f1a;
  --modal-bg: #16213e;
  --input-bg: #2c2c3e;
  --scheduler-header: #1a1a2e;
  --scheduler-border: #2c2c3e;
}

/* 全局样式使用 CSS 变量 */
body {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  transition: background-color 0.3s ease, color 0.3s ease;
}

.app-wrapper {
    display: flex;
    flex-direction: column;
}

.nav-bar {
    height: 60px; /* 固定高度 */
    background: #f0f0f0;
    display: flex;
    align-items: center;
    padding: 0 20px;
    flex-shrink: 0; /* 禁止被压缩 */
}

.page-container {
    flex: 1; /* 自动撑开剩余所有空间 */
    overflow-y: auto; /* 内容多了自动出现滚动条，不会挤压导航栏 */
    width: 100%;
}

/* 导航栏link效果 */
.nav-bar a{
  /* 去除下划线 */
  text-decoration: none;
  /* 左边距 */
  margin-left: 4vw;
  /* 内边距 */
  padding: 5px 8px 5px 8px;
  /* 圆角 */
  border-radius: 15px;
  /* 颜色 */
  color: rgb(39, 73, 151);
  /* 粗体 */
  font-weight: 1000;
  /* 平滑过渡 */
  transition: all 0.3s ease;
}

/* 鼠标悬停效果 */
.nav-bar a:hover {
  background-color: #dddfe7;
   /* 轻微上浮 */
  transform: translateY(-2px);
}

/* 激活状态的样式 */
.nav-bar a.router-link-active {
  background-color: #5c83d8;
  color: white;
}

/* 导航栏右侧用户信息 */
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  cursor: pointer;
  padding: 5px 12px;
  border-radius: 30px;
  transition: all 0.3s;
}

.user-info:hover {
  background-color: #dddfe7;
}

.avatar-small {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  background-color: #5c83d8;
  display: flex;
  align-items: center;
  justify-content: center;
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
  font-size: 16px;
  font-weight: bold;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #2c4c96;
}

/* 黑夜模式切换按钮 */
.dark-mode-btn {
  margin-left: auto;
  padding: 6px 16px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-primary);
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  white-space: nowrap;
  margin-right: 16px;
}

.dark-mode-btn:hover {
  background: #5c83d8;
  color: white;
  border-color: #5c83d8;
}
</style>
