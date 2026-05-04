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
        <span>周期类型</span>
        <select class="priority" v-model="form.recurringType">
          <option value="weekly">每周</option>
          <option value="monthly">每月</option>
          <option value="daily">每天</option>
        </select>
      </div>

      <!-- 每周重复 -->
      <div class="inputBar" v-if="form.recurringType === 'weekly'">
        <span>重复星期</span>
        <div class="weekday-group">
          <label v-for="day in weekdays" :key="day.value" class="weekday-label">
            <input type="checkbox" :value="day.value" v-model="form.recurringDays">
            <span>{{ day.label }}</span>
          </label>
        </div>
      </div>

      <!-- 每月重复 -->
      <div class="inputBar" v-if="form.recurringType === 'monthly'">
        <span>每月</span>
        <div class="monthly-group">
          <select class="monthly-select" v-model="form.recurringWeek">
            <option value="1">第一周</option>
            <option value="2">第二周</option>
            <option value="3">第三周</option>
            <option value="4">第四周</option>
            <option value="5">最后一周</option>
          </select>
          <select class="monthly-select" v-model="form.recurringDay">
            <option value="1">周一</option>
            <option value="2">周二</option>
            <option value="3">周三</option>
            <option value="4">周四</option>
            <option value="5">周五</option>
            <option value="6">周六</option>
            <option value="7">周日</option>
          </select>
        </div>
      </div>

      <div class="time-group">
        <div class="inputBar half">
          <span>开始时间</span>
          <input type="time" v-model="form.startTime">
        </div>
        <div class="inputBar half">
          <span>结束时间</span>
          <input type="time" v-model="form.endTime">
        </div>
      </div>

      <div class="inputBar">
        <span>结束日期</span>
        <input type="date" v-model="form.endDate">
      </div>

      <div class="inputBar">
        <span>提前生成周数</span>
        <div class="weeks-group">
          <input type="number" v-model.number="form.weeksToGenerate" min="1" max="12" class="weeks-input">
          <span class="weeks-unit">周</span>
        </div>
        <p class="field-hint">将提前生成指定周数的周期任务</p>
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

      <button class="addBtn" @click="submitRecurringTask">创建周期任务</button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import { addTask, getTags, addTag, deleteTag } from '@/api/task';
import { useRouter } from 'vue-router';
import { showToast, showConfirm } from '@/utils/message';

const router = useRouter();

const weekdays = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
];

const form = reactive({
  title: '',
  description: '',
  priority: 3,
  recurringType: 'weekly',
  recurringDays: [1],
  recurringWeek: 1,
  recurringDay: 1,
  startTime: '09:00',
  endTime: '10:00',
  endDate: '',
  weeksToGenerate: 4,
  type: ''  // 标签/分类字段
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
      showToast(result.message || '添加失败');
    }
  } catch (error) {
    console.error('添加标签失败', error);
    showToast('添加失败');
  }
};

// 删除标签
const removeTag = async (tagName) => {
  if (!showConfirm(`确定要删除标签「${tagName}」吗？`)) return;
  
  try {
    const result = await deleteTag(tagName);
    if (result.code === 200) {
      tags.value = tags.value.filter(t => t.tagName !== tagName);
      // 如果当前分类是删除的标签，清空
      if (form.type === tagName) {
        form.type = '';
      }
    } else {
      showToast(result.message || '删除失败');
    }
  } catch (error) {
    console.error('删除标签失败', error);
    showToast('删除失败');
  }
};

// 计算某月的最后一周
const isLastWeek = (date, weekOfMonth) => {
  const lastDay = new Date(date.getFullYear(), date.getMonth() + 1, 0);
  const lastWeek = Math.ceil((lastDay.getDate() + new Date(date.getFullYear(), date.getMonth(), 1).getDay()) / 7);
  return weekOfMonth === lastWeek;
};

// 计算指定日期是当月的第几周
const getWeekOfMonth = (date) => {
  const firstDay = new Date(date.getFullYear(), date.getMonth(), 1);
  const firstDayWeek = firstDay.getDay();
  const dayOfMonth = date.getDate();
  let week = Math.ceil((dayOfMonth + firstDayWeek) / 7);
  
  const lastDay = new Date(date.getFullYear(), date.getMonth() + 1, 0);
  const lastWeek = Math.ceil((lastDay.getDate() + firstDayWeek) / 7);
  if (week === lastWeek) week = 5;
  
  return week;
};

// 生成周期任务
const generateRecurringTasks = () => {
  const tasks = [];
  const endDate = new Date(form.endDate);
  const startTime = form.startTime;
  const endTime = form.endTime;
  const maxWeeks = form.weeksToGenerate;
  
  let currentDate = new Date();
  currentDate.setHours(0, 0, 0, 0);
  let weeksGenerated = 0;
  
  while (currentDate <= endDate && weeksGenerated < maxWeeks) {
    let shouldAdd = false;
    
    if (form.recurringType === 'weekly') {
      const currentWeekday = currentDate.getDay() === 0 ? 7 : currentDate.getDay();
      shouldAdd = form.recurringDays.includes(currentWeekday);
    } else if (form.recurringType === 'monthly') {
      const weekOfMonth = getWeekOfMonth(currentDate);
      const targetWeek = form.recurringWeek === 5 ? 'last' : form.recurringWeek;
      const currentWeekday = currentDate.getDay() === 0 ? 7 : currentDate.getDay();
      
      if (targetWeek === 'last') {
        shouldAdd = isLastWeek(currentDate, weekOfMonth) && currentWeekday === form.recurringDay;
      } else {
        shouldAdd = (weekOfMonth === targetWeek && currentWeekday === form.recurringDay);
      }
    } else if (form.recurringType === 'daily') {
      shouldAdd = true;
    }
    
    if (shouldAdd) {
      const startDateTime = new Date(currentDate);
      const [startHour, startMinute] = startTime.split(':');
      startDateTime.setHours(parseInt(startHour), parseInt(startMinute), 0);
      
      const endDateTime = new Date(currentDate);
      const [endHour, endMinute] = endTime.split(':');
      endDateTime.setHours(parseInt(endHour), parseInt(endMinute), 0);
      
      tasks.push({
        title: form.title,
        description: form.description,
        priority: parseInt(form.priority),
        start: startDateTime.toISOString().replace('T', ' ').slice(0, 19),
        end: endDateTime.toISOString().replace('T', ' ').slice(0, 19),
        type: form.type || ''  // 添加标签
      });
    }
    
    currentDate.setDate(currentDate.getDate() + 1);
    
    if (currentDate.getDay() === 1 && form.recurringType === 'weekly') {
      weeksGenerated++;
    }
  }
  
  return tasks;
};

const submitRecurringTask = async () => {
  const token = localStorage.getItem('token');
  if (!token) {
    router.push('/login');
    return;
  }
  
  if (!form.title || !form.endDate) {
    showToast('请填写完整信息');
    return;
  }
  
  if (!form.startTime || !form.endTime) {
    showToast('请填写开始时间和结束时间');
    return;
  }
  
  if (form.recurringType === 'weekly' && form.recurringDays.length === 0) {
    showToast('请至少选择一个重复的星期');
    return;
  }
  
  const tasks = generateRecurringTasks();
  
  if (tasks.length === 0) {
    showToast('没有生成任何任务，请检查结束日期和周期设置');
    return;
  }
  
  let successCount = 0;
  let failCount = 0;
  
  for (const task of tasks) {
    try {
      const result = await addTask(task);
      if (result.code === 200) {
        successCount++;
      } else {
        failCount++;
      }
    } catch (error) {
      console.error('添加任务失败', error);
      failCount++;
    }
  }
  
  showToast(`周期任务创建完成！\n成功：${successCount} 个\n失败：${failCount} 个`);
  
  if (successCount > 0) {
    form.title = '';
    form.description = '';
    form.endDate = '';
    form.type = '';
  }
};

onMounted(() => {
  fetchTags();
});
</script>

<style>
/* 黑夜模式 - RecurringTaskForm */
body.dark-mode .content-wrapper {
  background: #1a1a2e;
}

body.dark-mode .task-form {
  background: transparent;
}

body.dark-mode .inputBar span {
  color: #e0e0e0;
}

body.dark-mode .inputBar input,
body.dark-mode .inputBar select {
  background: #2c2c3e;
  border-color: #3a3a4e;
  color: #e0e0e0;
}

body.dark-mode .inputBar input:focus,
body.dark-mode .inputBar select:focus {
  border-color: #5c83d8;
  outline: none;
}

body.dark-mode .priority {
  background-color: #2a2a3e;
}

body.dark-mode .weekday-label span {
  color: #e0e0e0;
}

body.dark-mode .weekday-label input {
  accent-color: #5c83d8;
}

body.dark-mode .monthly-select {
  background: #2a2a3e;
  border-color: #3a3a4e;
  color: #e0e0e0;
}

body.dark-mode .tag-list {
  background: #0f0f1a;
}

body.dark-mode .tag-item {
  background-color: #4a6fb8 !important;
  color: #ffffff !important;
}

body.dark-mode .tag-delete {
  background: rgba(255, 255, 255, 0.2);
  color: #ff9999;
}

body.dark-mode .tag-add input {
  background: #2c2c3e;
  border-color: #3a3a4e;
  color: #e0e0e0;
}

body.dark-mode .add-tag-btn {
  background: #4a6fb8;
}

body.dark-mode .add-tag-btn:hover {
  background: #5c83d8;
}

body.dark-mode .field-hint {
  color: #888888;
}

body.dark-mode .weeks-unit {
  color: #aaaaaa;
}

body.dark-mode .addBtn {
  background: #4a6fb8;
}

body.dark-mode .addBtn:active {
  background: #3a5a9a;
}

body.dark-mode input[type="date"],
body.dark-mode input[type="time"] {
  background: #2c2c3e;
  border-color: #3a3a4e;
  color: #e0e0e0;
}
</style>

<style scoped>
/* 原有样式保持不变，添加标签栏样式 */

.content-wrapper {
  background: rgb(255, 255, 255);
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 3% 1% 3%;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

.task-form {
  width: 100%;
  max-width: 600px;
}

.form-title {
  text-align: center;
  color: #2c4c96;
  margin-bottom: 30px;
  font-size: 24px;
  font-weight: 600;
}

.inputBar span {
  display: block;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 3%;
  border: transparent;
  font-weight: bold;
  font-size: 15px;
}

.inputBar input,
.inputBar select {
  margin-top: 3px;
  border: solid gray;
  background: transparent;
  padding: 15px 5px;
  font-size: 14px;
  color: #222e41;
  transition: all 0.2s;
  border-radius: 8px;
  width: 100%;
  height: auto;
  border: 1px solid #ddd;
  box-sizing: border-box;
}

.priority {
  display: block;
  width: 100%;
  height: 5vh;
  margin-top: 15px;
  background-color: rgb(240, 242, 249);
  border-radius: 0px 15px 15px 15px;
  text-align-last: center;
}

/* 标签栏样式 */
.tag-bar {
  margin-top: 8px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 8px;
  padding: 10px;
  background: #f5f5f5;
  border-radius: 12px;
  min-height: 55px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 8px 5px 12px;
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
}

.tag-delete:hover {
  background: rgba(0,0,0,0.4);
}

.tag-add {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 5px;
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

/* 时间组 */
.time-group {
  display: flex;
  gap: 20px;
  margin-top: 3%;
}

.inputBar.half {
  flex: 1;
}

.inputBar.half span {
  margin-top: 0;
}

/* 星期选择器 */
.weekday-group {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.weekday-label {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  font-size: 16px;
}

.weekday-label input {
  width: auto;
  margin: 0;
  padding: 0;
  transform: scale(1.2);
}

.weekday-label span {
  margin: 0;
  font-weight: normal;
  font-size: 16px;
}

/* 每月选择器 */
.monthly-group {
  display: flex;
  gap: 15px;
  margin-top: 10px;
}

.monthly-select {
  flex: 1;
  margin-top: 3px;
  border: solid gray;
  background: rgb(240, 242, 249);
  padding: 12px 5px;
  font-size: 16px;
  border-radius: 0px 15px 15px 15px;
  border-color: rgb(114, 130, 156);
}

/* 周数输入 */
.weeks-group {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 3px;
}

.weeks-input {
  width: 100px !important;
  text-align: center;
}

.weeks-unit {
  font-size: 16px;
  color: #666;
}

.field-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #888;
}

/* 提交按钮 */
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

.addBtn:hover {
  background-color: #5c83d8;
  color: white;
  border-color: #5c83d8;
}

input[type="date"],
input[type="time"] {
  font-size: 100%;
  font-weight: bold;
  font-family: inherit;
}

/* 响应式 */
@media (max-width: 600px) {
  .time-group {
    flex-direction: column;
    gap: 0;
  }
  
  .weekday-group {
    gap: 10px;
  }
  
  .monthly-group {
    flex-direction: column;
  }
}
</style>