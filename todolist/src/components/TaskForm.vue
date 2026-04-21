<template>
  <div class="task-form">
    <div>
      <!-- <button class="return" @click="$router.back()"> < </button> -->
      <h3>添加新任务</h3>
    </div>

    <div class="inputBar">
      <span>title</span>
      <input type="text" v-model="form.title" placeholder="标题">
    </div>

    <div class="inputBar">
      <span>description</span>
      <input type="text" v-model="form.description" placeholder="描述">
    </div>

    <div class="inputBar">
      <span>priority</span>
      <select v-model="form.priority">
        <option value="3">高</option>
        <option value="2">中</option>
        <option value="1">低</option>
      </select>
    </div>

    <div class="inputBar">
      <span>time</span>
      <input type="date" v-model="form.start">
      <span>—</span>
      <input type="date" v-model="form.end">
    </div>

    <div class="inputBar">
      <span>type</span>
      <input type="text" v-model="form.type" placeholder="分类">
    </div>

    <button class="addBtn" @click="submitTask">ADD</button>
  </div>
</template>

<script setup>
    import { reactive } from 'vue';

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

        // 发送请求给 Java 后端
        try {
            const response = await fetch('http://localhost:8080/api/addTask', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(form)
            });
            const result = await response.json();
            console.log('Success:', result);
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
        padding: 20px 15px;
        /* 字体大小 */
        font-size: 25px;
        /* 颜色 */
        color: #222e41;
        /* 属性变化时 在0.2秒内平滑过渡 */
        transition: all 0.2s;
        /* 圆角设置，左上尖，其余三个圆 */
        border-radius: 0px 15px 15px 15px;
    }

    #pri {
        /* 提升为块级元素 */
        display: block;
        /* 宽度  父类的100% */
        width: 100%;
        /* 高度 视口高度的5% */
        height: 5vh;
        /* 上下外边距 */
        margin-top: 4%;
        margin-bottom: 10%;
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
        padding: 5%;
        /* 宽度：视口宽度的33% */
        width: 100%;
        /* 圆角 */
        border-radius: 20px;
    }

    .return {
        width: 30px;
        height: 30px;
        border-radius: 50%;
        /* 圆形 */
        background-color: #f0f0f0;
        /* 鲜艳底色 */
        color: rgb(0, 0, 0);
        font-size: 18px;
        font-weight: bold;
        display: inline-flex;
        /*文字居中*/
        align-items: center;
        justify-content: center;
        border: 3px solid rgb(63, 63, 63);
        cursor: pointer;
        transition: opacity 0.2s ease, transform 0.1s ease;
        /* 过渡效果 */
        outline: none;
        /* 去掉默认焦点轮廓 */
    }
</style>