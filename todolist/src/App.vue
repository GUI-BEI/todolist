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
      <router-link to="/login" style="margin-left: auto;">登录</router-link>
    </nav>

    <main class="page-container">
      <router-view />
    </main>
  </div>
</template>

<script setup>
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
</style>
