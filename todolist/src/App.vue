<template>
  <div class="app-container">
    <nav class="nav-bar">
      <router-link to="/">首页 (周视图)</router-link>
      <router-link to="/add">添加新任务</router-link>
    </nav>

    <main>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { watch } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

// 监听路由对象，只要路由变了，就执行这里的代码
watch(
  () => route.meta,
  (newMeta) => {
    // 先清除之前添加过的 bodyClass (如果有的话)
    document.body.className = ''; 
    
    // 如果当前页面定义了 meta.bodyClass，则添加到 body 上
    if (newMeta.bodyClass) {
      document.body.classList.add(newMeta.bodyClass);
    }
  },
  { immediate: true } // 保证刚进入项目时也能触发
);
</script>

<style>
/* 首页背景 */
.home-page-bg {
    background-color: rgb(230, 234, 249);
    padding: 20px;
}

/* 添加页背景 */
.form-page-bg {
  /* 背景颜色 */
  background: rgb(230, 234, 249);
  min-height: 100%;
  /* flex布局 */
  display: flex;
  /* 垂直居中 */
  align-items: center;
  /* 水平居中 */
  justify-content: center;
  /* 内边距 */
  padding: 5%;
  /* 字体 默认值 */
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}
</style>
