<template>
  <div class="content-wrapper">
      <div class="task-form">

        <div class="inputBar">
          <span>title</span>
          <input type="text" v-model="form.title" placeholder=" 标题">
        </div>

        <div class="inputBar">
          <span>description</span>
          <input type="text" v-model="form.description" placeholder=" 描述">
        </div>

        <div class="inputBar">
          <span>priority</span>
          <select  class="priority" v-model="form.priority">
            <option value="3">高</option>
            <option value="2">中</option>
            <option value="1">低</option>
          </select>
        </div>

        <!-- 如果需要精确到小时，type改为"datetime-local" 此时 YYYY-MM-DDTHH:mm  2026-04-08T14:30-->
        <div class="inputBar">
          <span>time</span>
          <input type="date" v-model="form.start">
          <span>—</span>
          <input type="date" v-model="form.end">
        </div>

        <div class="inputBar">
          <span>type</span>
          <input type="text" v-model="form.type" placeholder=" 分类">
        </div>

        <button class="addBtn" @click="submitTask">ADD</button>
    </div>
  </div>

  
</template>

<script setup>
import { reactive } from 'vue';
import { addTask } from '@/api/task';

// 数据绑定
const form = reactive({
    title: '',
    description: '',
    priority: 3,
    start: '',
    end: '',
    type: ''
});

// 逻辑函数
const submitTask = async () => {
    // 简单的校验  标题和起止时间不能为空
    if (!form.title || !form.start || !form.end) {
      alert('请填写完整信息');
      return;
    }

    const taskData = {
      id: Date.now(),       // 使用当前时间戳作为唯一 ID
      ...form
    };

    // 发送请求给 Java 后端（标准 axios 封装方式）
    try {
      const result = await addTask(form);
      console.log('Success:', result.data);
      alert('添加成功！');
    } catch (error) {
      console.error('Error:', error);
    }
};

</script>

<style scoped>
/* 输入框上方标签 */
.inputBar span {
  /* 提升为块级元素，独占一行 */
  display: block;
  /* 居中 */
  align-items: center;
  flex-wrap: wrap;
  /* 上边距，这样可以紧挨着下方的输入框而远离上方的输入框 */
  margin-top: 3%;
  /* 无边框 */
  border: transparent;
  /* 加粗 */
  font: bold;
  /* 字体大小 */
  font-size: 15px;
}

.inputBar input {
  /* 上 外边距 */
  margin-top: 3px;
  /* 边框灰 */
  border: solid gray;
  /* 透明背景 */
  background: transparent;
  /* 内边距 */
  padding: 15px 5px;
  /* 字体大小 */
  font-size: 20px;
  /* 颜色 */
  color: #222e41;
  /* 属性变化时 在0.2秒内平滑过渡 */
  transition: all 0.2s;
  /* 圆角设置，左上尖，其余三个圆 */
  border-radius: 0px 15px 15px 15px;
  /* 宽度 */
  width: 60vw;
  /* 高度 */
  height: auto;
}

.priority {
  /* 提升为块级元素 */
  display: block;
  /* 宽度  父类的100% */
  width: 100%;
  /* 高度 视口高度的5% */
  height: 5vh;
  /* 上 外边距 */
  margin-top: 15px;
  /* 背景颜色 比大的背景稍白一点 */
  background-color: rgb(240, 242, 249);
  /* 圆角 */
  border-radius: 0px 15px 15px 15px;
  /* 文本居中 */
  text-align-last: center;
}

input[type="date"] {
  /* 字体大小 */
  font-size: 100%;
  /* 粗体 */
  font: bold;
  /* 继承页面字体 */
  font-family: inherit;
}

.addBtn {
  /* 上 外边距 */
  margin-top: 40px;
  /* flex布局 暂时没什么作用 */
  display: flex;
  /* 文本水平居中 */
  justify-content: center;
  /* 内边距 */
  padding: 3%;
  /* 宽度 */
  width: 100%;
  /* 圆角 */
  border-radius: 20px;
  /* 背景颜色 */
  background-color: rgb(231, 235, 242);
}

/* 激活状态的样式 */
.addBtn:active {
  border-color: rgb(19, 24, 41);
  background-color: rgba(208, 212, 218, 0.616)
}

.content-wrapper {
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
  padding: 0 3% 1% 3%;
  /* 字体 默认值 */
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

</style>