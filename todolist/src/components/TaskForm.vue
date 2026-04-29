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
        <select style="border-color: rgb(114, 130, 156);" class="priority" v-model="form.priority">
          <option value="3">高</option>
          <option value="2">中</option>
          <option value="1">低</option>
        </select>
      </div>

      <!-- 改为 datetime-local -->
      <div class="inputBar">
        <span>开始时间</span>
        <input type="datetime-local" v-model="form.start">
      </div>
      
      <div class="inputBar">
        <span>结束时间</span>
        <input type="datetime-local" v-model="form.end">
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
import { useRouter } from 'vue-router';

const router = useRouter();

const form = reactive({
  title: '',
  description: '',
  priority: 3,
  start: '',
  end: '',
  type: ''
});

const submitTask = async () => {
  const token = localStorage.getItem('token');
  if (!token) {
    router.push('/login');
    return;
  }
  
  if (!form.title || !form.start || !form.end) {
    alert('请填写完整信息');
    return;
  }

  try {
    // 将 datetime-local 格式转换为后端期望的格式
    const startDateTime = form.start.replace('T', ' ') + ':00';
    const endDateTime = form.end.replace('T', ' ') + ':00';
    
    const taskData = {
      title: form.title,
      description: form.description,
      priority: parseInt(form.priority),
      start: startDateTime,
      end: endDateTime,
      type: form.type
    };
    
    const result = await addTask(taskData);
    if (result.code === 200) {
      alert('添加成功！');
      form.title = '';
      form.description = '';
      form.priority = 3;
      form.start = '';
      form.end = '';
      form.type = '';
    } else {
      alert(result.message || '添加失败');
    }
  } catch (error) {
    console.error('Error:', error);
    alert('添加失败，请稍后重试');
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
  /* 边框颜色 */
  border-color: rgb(114, 130, 156);
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
  /* 边框颜色 */
  border-color: rgb(114, 130, 156);
}

/* 激活状态的样式 */
.addBtn:active {
  border-color: rgb(106, 121, 145);
  background-color: rgba(220, 224, 234, 0.616);
}

.content-wrapper {
  /* 背景颜色 */
  background: rgb(255, 255, 255);
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

input[type="datetime-local"] {
  /* 字体大小 */
  font-size: 100%;
  /* 粗体 */
  font-weight: bold;
  /* 继承页面字体 */
  font-family: inherit;
  /* 与其他输入框保持一致的内边距和圆角 */
  padding: 15px 5px;
  border-radius: 0px 15px 15px 15px;
  border-color: rgb(114, 130, 156);
  background: transparent;
  width: 60vw;
  height: auto;
  color: #222e41;
  transition: all 0.2s;
}
</style>