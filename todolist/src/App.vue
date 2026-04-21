<template>
  <div class="app-wrapper">
    <nav class="nav-bar">
      <router-link to="/">首页 (周视图)</router-link>
      <router-link to="/add">添加新任务</router-link>
      <router-link to="/history">足迹</router-link>
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

</style>
