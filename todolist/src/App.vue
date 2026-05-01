<template>
  <div class="app-wrapper">
    <nav class="nav-bar">
      <router-link to="/">首页 (周视图)</router-link>
      <router-link to="/month">首页 (月视图)</router-link>
      <router-link to="/add">添加新任务</router-link>
      <router-link to="/recurring">添加周期任务</router-link>
      <router-link to="/history">足迹</router-link>
      <router-link to="/sign">签到</router-link>
      <router-link to="/settings">设置</router-link>

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
</style>
