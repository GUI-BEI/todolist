<template>
  <div class="content-wrapper">
    <div class="task-form">
      <div class="inputBar">
        <span>标题</span>
        <input type="text" v-model="form.title" placeholder="标题">
      </div>

      <div class="inputBar">
        <span>描述</span>
        <input type="text" v-model="form.description" placeholder="描述">
      </div>

      <div class="inputBar">
        <span>优先级</span>
        <select class="priority" v-model="form.priority">
          <option value="3">高</option>
          <option value="2">中</option>
          <option value="1">低</option>
        </select>
      </div>

      <div class="inputBar">
        <span>时间</span>
        <input type="date" v-model="form.start">
        <span>—</span>
        <input type="date" v-model="form.end">
      </div>

      <!-- 标签栏 -->
      <div class="inputBar">
        <span>标签</span>
        <div class="tag-bar">
          <div class="tag-list">
            <span 
              v-for="tag in tags" 
              :key="tag.tagName"
              class="tag-item"
              :style="{ backgroundColor: tag.tagColor || '#e0e0e0' }"
              @click="selectTag(tag.tagName)"
            >
              {{ tag.tagName }}
              <button class="tag-delete" @click.stop="removeTag(tag.tagName)">×</button>
            </span>
          </div>
          <div class="tag-add">
            <input 
              type="text" 
              v-model="newTagName" 
              placeholder="新标签"
              @keyup.enter="addNewTag"
              maxlength="20"
            />
            <button class="add-tag-btn" @click="addNewTag">+</button>
          </div>
        </div>
      </div>

      <div class="inputBar">
        <span>分类</span>
        <input type="text" v-model="form.type" placeholder="分类">
      </div>

      <button class="addBtn" @click="submitTask">ADD</button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import { addTask, getTags, addTag, deleteTag } from '@/api/task';
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

const tags = ref([]);
const newTagName = ref('');

// 获取标签列表
const fetchTags = async () => {
  try {
    const result = await getTags();
    if (result.code === 200) {
      tags.value = result.data;
    }
  } catch (error) {
    console.error('获取标签失败', error);
  }
};

// 选择标签（点击复制到分类栏）
const selectTag = (tagName) => {
  form.type = tagName;
};

// 添加新标签
const addNewTag = async () => {
  const name = newTagName.value.trim();
  if (!name) return;
  
  try {
    const result = await addTag(name);
    if (result.code === 200) {
      tags.value.push(result.data);
      newTagName.value = '';
    } else {
      alert(result.message || '添加失败');
    }
  } catch (error) {
    console.error('添加标签失败', error);
    alert('添加失败');
  }
};

// 删除标签
const removeTag = async (tagName) => {
  if (!confirm(`确定要删除标签「${tagName}」吗？`)) return;
  
  try {
    const result = await deleteTag(tagName);
    if (result.code === 200) {
      tags.value = tags.value.filter(t => t.tagName !== tagName);
      // 如果当前分类是删除的标签，清空
      if (form.type === tagName) {
        form.type = '';
      }
    } else {
      alert(result.message || '删除失败');
    }
  } catch (error) {
    console.error('删除标签失败', error);
    alert('删除失败');
  }
};

// 提交任务
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
    const taskData = {
      title: form.title,
      description: form.description,
      priority: parseInt(form.priority),
      start: form.start,
      end: form.end,
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

onMounted(() => {
  fetchTags();
});
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
  /* 透明背景 */
  background: transparent;
  /* 内边距 */
  padding: 15px 5px;
  /* 颜色 */
  color: #222e41;
  /* 属性变化时 在0.2秒内平滑过渡 */
  transition: all 0.2s;
  /* 宽度 */
  width: 60vw;
  /* 高度 */
  height: auto;

  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
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
  width: 100%;
  padding: 14px;
  background-color: #5c83d8;
  color: white;
  border: none;
  border-radius: 40px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 20px;
  transition: all 0.2s;
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

.tag-bar {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top:8px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 8px;
  padding: 8px;
  background: #f5f5f5;
  border-radius: 12px;
  min-height: 50px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 5px 4px 12px;
  background-color: #e0e0e0;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.tag-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.tag-delete {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: rgba(0,0,0,0.2);
  color: white;
  font-size: 12px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.tag-delete:hover {
  background: rgba(0,0,0,0.4);
}

.tag-add {
  display: flex;
  gap: 8px;
  align-items: center;
}

.tag-add input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
  margin: 0;
}

.add-tag-btn {
  padding: 6px 16px;
  background-color: #5c83d8;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.add-tag-btn:hover {
  background-color: #456f9d;
}

.inputBar input[type="text"] {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}
</style>