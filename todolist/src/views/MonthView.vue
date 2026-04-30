<template>
  <div class="month-view">
    <!-- 功能栏 - 单行布局 -->
    <div class="toolbar">
      <button class="nav-btn" @click="goToPreviousMonth">< 上月</button>

      <div class="current-month">{{ currentYear }}年 {{ currentMonth }}月</div>

      <div class="search-box">
        <input 
          type="text" 
          v-model="searchKeyword" 
          placeholder="搜索任务..."
          @input="handleSearch"
          @keyup.enter="fetchTasks"
        />
        <button class="search-btn" @click="fetchTasks">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="10" cy="10" r="7"/>
            <line x1="15" y1="15" x2="21" y2="21"/>
          </svg>
        </button>
      </div>

      <div class="filter-buttons">
        <button 
          v-for="filter in filters" 
          :key="filter.value"
          :class="['filter-btn', { active: currentFilter === filter.value }]"
          @click="handleFilterChange(filter.value)"
        >
          {{ filter.label }}
        </button>
      </div>

      <button class="nav-btn" @click="goToNextMonth">下月 ></button>
    </div>

    <!-- 甘特图容器 -->
    <div class="scheduler-wrapper">
      <DayPilotScheduler 
        :config="schedulerConfig" 
        ref="schedulerRef" 
      />
    </div>

    <!-- 弹窗遮罩层 -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>编辑任务</h3>
          <button class="close-btn" @click="closeModal">×</button>
        </div>

        <div class="modal-body">
          <div class="inputBar">
            <label>标题</label>
            <input type="text" v-model="editingTask.title" placeholder="请输入标题">
          </div>

          <div class="inputBar">
            <label>描述</label>
            <textarea v-model="editingTask.description" placeholder="请输入描述" rows="3"></textarea>
          </div>

          <div class="inputBar">
            <label>优先级</label>
            <select v-model="editingTask.priority">
              <option :value="3">🔴 高</option>
              <option :value="2">🟡 中</option>
              <option :value="1">🟢 低</option>
            </select>
          </div>

          <div class="inputBar">
            <label>开始日期</label>
            <input type="datetime-local" v-model="editingTask.start">
          </div>

          <div class="inputBar">
            <label>结束日期</label>
            <input type="datetime-local" v-model="editingTask.start">
          </div>

          <div class="inputBar">
            <label>分类</label>
            <input type="text" v-model="editingTask.type" placeholder="请输入分类">
          </div>

          <div class="inputBar">
            <label>状态</label>
            <div class="checkbox-group">
              <input type="checkbox" v-model="editingTask.completed">
              <span>已完成</span>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="delete-btn" @click="deleteTask">删除</button>
          <div class="footer-right">
            <button class="cancel-btn" @click="closeModal">取消</button>
            <button class="save-btn" @click="saveTaskChanges">保存修改</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { DayPilot, DayPilotScheduler } from "@daypilot/daypilot-lite-vue";
import { getFilteredTasks, getTasks, updateTaskTime, updateTask, deleteTask as deleteTaskApi } from '@/api/task';
import { useRouter } from 'vue-router';

const router = useRouter();

// 筛选配置
const filters = [
  { label: '所有', value: 'all' },
  { label: '高', value: '3' },
  { label: '中', value: '2' },
  { label: '低', value: '1' }
];

// 状态
const currentFilter = ref('all');
const searchKeyword = ref('');
const currentTasks = ref([]);
const schedulerRef = ref(null);
const currentYear = ref(new Date().getFullYear());
const currentMonth = ref(new Date().getMonth() + 1);
let searchDebounceTimer = null;

// 弹窗相关状态
const showModal = ref(false);
const editingTask = ref({
  id: null,
  title: '',
  description: '',
  priority: 3,
  start: '',
  end: '',
  type: '',
  completed: false
});

// 获取当前月份的天数
const getDaysInMonth = (year, month) => {
  return new Date(year, month, 0).getDate();
};

// 获取当前月份的第一天
const getFirstDayOfMonth = (year, month) => {
  return new Date(year, month - 1, 1);
};

// 切换到上月
const goToPreviousMonth = () => {
  if (currentMonth.value === 1) {
    currentMonth.value = 12;
    currentYear.value--;
  } else {
    currentMonth.value--;
  }
  updateSchedulerStartDate();
};

// 切换到下月
const goToNextMonth = () => {
  if (currentMonth.value === 12) {
    currentMonth.value = 1;
    currentYear.value++;
  } else {
    currentMonth.value++;
  }
  updateSchedulerStartDate();
};

// 更新 scheduler 的起始日期和天数
const updateSchedulerStartDate = () => {
  if (schedulerRef.value && currentYear.value && currentMonth.value) {
    const firstDay = getFirstDayOfMonth(currentYear.value, currentMonth.value);
    const daysInMonth = getDaysInMonth(currentYear.value, currentMonth.value);
    
    schedulerRef.value.control.update({
      startDate: new DayPilot.Date(firstDay, true),
      days: daysInMonth
    });
    
    // 更新后重新获取数据
    fetchTasks();
  }
};

// 优先级配置
const priorityConfig = {
  3: { name: '高', color: '#ee3f3f', bgColor: '#ffebee', barColor: '#ee3f3fb8' },
  2: { name: '中', color: '#f55515', bgColor: '#fff3e0', barColor: '#f55515eb' },
  1: { name: '低', color: '#4caf50', bgColor: '#e8f5e9', barColor: '#6fbe90' }
};

// 显示消息
const showMessage = (text, isError = false) => {
  const msg = document.createElement('div');
  msg.textContent = text;
  msg.style.cssText = `
    position: fixed;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
    background-color: ${isError ? '#f44336' : '#4caf50'};
    color: white;
    padding: 10px 20px;
    border-radius: 8px;
    z-index: 10000;
    font-size: 14px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.2);
    animation: fadeOut 2s ease forwards;
  `;
  document.body.appendChild(msg);
  setTimeout(() => msg.remove(), 2000);
};

// 添加动画样式
const style = document.createElement('style');
style.textContent = `
  @keyframes fadeOut {
    0% { opacity: 1; }
    70% { opacity: 1; }
    100% { opacity: 0; visibility: hidden; }
  }
`;
document.head.appendChild(style);

// 打开编辑弹窗
const openEditModal = (task) => {
  // 将 "2026-04-30T14:30:00" 转换为 "2026-04-30T14:30" 供 datetime-local 使用
  const startValue = task.start ? task.start.replace(' ', 'T').slice(0, 16) : '';
  const endValue = task.end ? task.end.replace(' ', 'T').slice(0, 16) : '';
  
  editingTask.value = {
    id: task.id,
    title: task.title,
    description: task.description || '',
    priority: task.priority,
    start: startValue,
    end: endValue,
    type: task.type || '',
    completed: task.completed || false
  };
  showModal.value = true;
};

// 删除任务
const deleteTask = async () => {
  if (!editingTask.value.id) return;
  
  const confirmed = confirm(`确定要删除任务「${editingTask.value.title}」吗？`);
  if (!confirmed) return;
  
  try {
    const result = await deleteTaskApi(editingTask.value.id);
    
    if (result.code === 200) {
      showMessage("删除成功");
      closeModal();
      await fetchTasks();
    } else {
      throw new Error(result.message || "删除失败");
    }
  } catch (err) {
    console.error("删除失败", err);
    showMessage(err.message || "删除失败", true);
  }
};

// 关闭弹窗
const closeModal = () => {
  showModal.value = false;
  editingTask.value = {
    id: null,
    title: '',
    description: '',
    priority: 3,
    start: '',
    end: '',
    completed: false
  };
};

// 保存修改
const saveTaskChanges = async () => {
  try {
    // datetime-local 的值是 "2026-04-30T14:30"，需要转换为 "2026-04-30 14:30:00"
    const startDateTime = editingTask.value.start ? editingTask.value.start.replace('T', ' ') + ':00' : '';
    const endDateTime = editingTask.value.end ? editingTask.value.end.replace('T', ' ') + ':00' : '';
    
    const updatedTask = {
      title: editingTask.value.title,
      description: editingTask.value.description,
      priority: parseInt(editingTask.value.priority),
      start: startDateTime,
      end: endDateTime,
      type: editingTask.value.type || '',
      completed: editingTask.value.completed
    };

    const result = await updateTask(editingTask.value.id, updatedTask);
    
    if (result.code !== 200) {
      throw new Error(result.message || "保存失败");
    }
    
    showMessage("保存成功");
    closeModal();
    await fetchTasks();
    
  } catch (err) {
    console.error("保存失败", err);
    showMessage(err.message || "保存失败", true);
  }
};

// 渲染 Scheduler
const renderScheduler = (tasks) => {
  if (!schedulerRef.value) return;

  const resources = tasks.map(task => ({
    name: task.title,
    id: `row_${task.id}`,
    description: task.description,
    completed: task.completed,
    priority: task.priority,
    type: task.type
  }));

  const events = tasks.map(task => {
    let startDate = task.start;
    let endDate = task.end;
    
    // 如果日期包含空格，转换为 T（保留时间）
    if (startDate && startDate.includes(' ')) {
      startDate = startDate.replace(' ', 'T');
    }
    if (endDate && endDate.includes(' ')) {
      endDate = endDate.replace(' ', 'T');
    }
    
    return {
      id: task.id,
      resource: `row_${task.id}`,
      text: task.title,
      start: startDate,
      end: endDate,
      priority: task.priority,
      description: task.description,
      type: task.type,
      completed: task.completed
    };
  });

  schedulerRef.value.control.update({
    resources: resources,
    events: events
  });
};

// Scheduler 配置 - 完全参照周视图，只改 days 为当月天数
const schedulerConfig = ref({
  viewType: "Days",
  scale: "Day",
  cellWidth: 120,
  rowHeaderWidth: 0,
  eventHeight: 80,
  headerHeight: 40,
  height: "auto",
  eventMoveHandling: "Update",

  onBeforeTimeHeaderRender: (args) => {
    const headerDate = args.header.start.getDatePart();
    const today = new DayPilot.Date().getDatePart();
    if (headerDate === today) {
      args.header.backColor = "rgb(230, 234, 249)";
    }
  },

  onBeforeEventRender: (args) => {
    const priority = args.data.priority;
    const config = priorityConfig[priority] || priorityConfig[1];
    const completed = args.data.completed;
    const type = args.data.type;  // 获取分类

    // 计算距离截止时间还有多少小时
    let hoursRemaining = 25; // 默认大于24，不触发

    const end = args.data.end;
    const now = new DayPilot.Date();
    
    const endStr = args.data.end;
    if (endStr) {
      // 处理 "2026-04-30T18:17:00" 格式
      let endDate = new Date(endStr);
      const now = new Date();
      const diffMs = endDate - now;
      hoursRemaining = diffMs / (1000 * 60 * 60);
    }

    if (hoursRemaining < 24 && hoursRemaining > 0 && !completed) {
      args.data.cssClass = "border-flash";
    }

    
    // 设置样式
    args.data.barColor = config.barColor;
    args.data.backColor = config.bgColor;
    args.data.fontColor = "#333333";

    // 根据完成状态调整样式
    if (completed) {
      args.data.backColor = "#e0e0e0";
      args.data.fontColor = "#888888";
    }

    // 距离截止时间不足24小时且未完成
    if (hoursRemaining < 24 && hoursRemaining > 0 && !completed) {
      console.log('urgent');
      args.data.cssClass = "border-flash";
    }
    
    // 设置显示文本：如果有分类，显示 [分类] 标题
    if (type && type.trim()) {
      args.data.text = `[${type}] ${args.data.text}`;
    } else {
      args.data.text = args.data.text;
    }
  },

  onEventClick: (args) => {
    const task = {
      id: args.e.id(),
      title: args.e.text(),
      description: args.e.data.description,
      priority: args.e.data.priority,
      start: args.e.start().toString(),
      end: args.e.end().toString(),
      type: args.e.data.type || '',
      completed: args.e.data.completed
    };
    openEditModal(task);
  },

  // 禁止拖拽到其他行
  onEventMoving: (args) => {
    if (args.newResource !== args.e.resource()) {
      args.allowed = false;
      showMessage("不能将任务移动到其他行", true);
    }
  },

  onEventMoved: async (args) => {
    if (args.newResource !== args.e.resource()) {
      showMessage("不能将任务移动到其他行", true);
      await fetchTasks();
      return;
    }

    try {
      let newStart = args.newStart.toString().replace('T', ' ');
      let newEnd = args.newEnd.toString().replace('T', ' ');
      
      const result = await updateTaskTime(
        args.e.id(),
        newStart,
        newEnd
      );
      
      if (result.code !== 200) {
        throw new Error(result.message || "同步失败");
      }
      
      showMessage("同步成功");
      await fetchTasks();

    } catch (err) {
      console.error("更新失败", err);
      showMessage(err.message || "同步失败，已回滚", true);
      await fetchTasks();
    }
  }
});

// 获取任务列表
const fetchTasks = async () => {
  if (!schedulerRef.value) return;
  
  const token = localStorage.getItem('token');
  if (!token) {
    router.push('/login');
    return;
  }

  try {
    let result;
    
    const hasFilter = currentFilter.value !== 'all' || searchKeyword.value.trim();
    
    if (hasFilter) {
      const params = {};
      if (currentFilter.value !== 'all') {
        params.priority = currentFilter.value;
      }
      if (searchKeyword.value.trim()) {
        params.keyword = searchKeyword.value.trim();
      }
      // 月视图也需要传递日期范围，只获取当月的任务
      const firstDay = getFirstDayOfMonth(currentYear.value, currentMonth.value);
      const lastDay = new Date(currentYear.value, currentMonth.value, 0);
      params.startDate = firstDay.toISOString().split('T')[0] + 'T00:00:00';
      params.endDate = lastDay.toISOString().split('T')[0] + 'T23:59:59';
      
      result = await getFilteredTasks(params);
    } else {
      // 没有筛选时，也传递日期范围，只获取当月任务
      const firstDay = getFirstDayOfMonth(currentYear.value, currentMonth.value);
      const lastDay = new Date(currentYear.value, currentMonth.value, 0);
      const params = {
        startDate: firstDay.toISOString().split('T')[0] + 'T00:00:00',
        endDate: lastDay.toISOString().split('T')[0] + 'T23:59:59'
      };
      result = await getTasks(params);
    }
    
    if (result.code === 200) {
      // 转换日期格式：将 "2026-04-30 00:00:00" 转换为 "2026-04-30T00:00:00"
      currentTasks.value = result.data.map(task => ({
        ...task,
        start: task.start ? task.start.replace(' ', 'T') : null,
        end: task.end ? task.end.replace(' ', 'T') : null
      }));
      renderScheduler(currentTasks.value);
    } else {
      throw new Error(result.message || "获取任务失败");
    }
  } catch (err) {
    console.error('获取任务失败', err);
    currentTasks.value = [];
    renderScheduler(currentTasks.value);
  }
};

// 处理筛选变化
const handleFilterChange = (value) => {
  currentFilter.value = value;
  fetchTasks();
};

// 处理搜索
const handleSearch = () => {
  if (searchDebounceTimer) clearTimeout(searchDebounceTimer);
  searchDebounceTimer = setTimeout(() => {
    fetchTasks();
  }, 500);
};

onMounted(() => {
  const token = localStorage.getItem('token');
  if (!token) {
    router.push('/login');
    return;
  }

  // 设置当前月份的第一天作为起始日期
  const firstDay = getFirstDayOfMonth(currentYear.value, currentMonth.value);
  const daysInMonth = getDaysInMonth(currentYear.value, currentMonth.value);
  
  // 先设置 startDate 和 days
  schedulerConfig.value.startDate = new DayPilot.Date(firstDay, true);
  schedulerConfig.value.days = daysInMonth;  // 动态设置天数
  
  // 确保 scheduler 初始化
  setTimeout(() => {
    if (schedulerRef.value) {
      // 强制更新一次
      schedulerRef.value.control.update({
        startDate: new DayPilot.Date(firstDay, true),
        days: daysInMonth
      });
    }
    fetchTasks();
  }, 100);
});
</script>

<style>
/* box-shadow 光晕效果，不受 barColor 影响 */
@keyframes borderFlash {
  0% { box-shadow: 0 0 0 0 rgba(255, 42, 0, 0.7); }
  70% { box-shadow: 0 0 0 5px rgba(255, 152, 0, 0); }
  100% { box-shadow: 0 0 0 0 rgba(255, 152, 0, 0); }
}

.border-flash {
  animation: borderFlash 1.5s ease-out infinite !important;
  border-radius: 8px !important;
}
</style>

<style scoped>
.month-view {
  width: 100%;
  overflow-x: auto;
  background: rgb(255, 255, 255);
  min-height: 100%;
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 20px;
  background: white;
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  flex-wrap: nowrap;
  justify-content: space-between;
}

.nav-btn {
  padding: 6px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  white-space: nowrap;
  flex-shrink: 0;
}

.nav-btn:hover {
  background: #5c83d8;
  color: white;
  border-color: #5c83d8;
}

.current-month {
  font-size: 18px;
  font-weight: 600;
  color: #2c4c96;
  min-width: 150px;
  text-align: center;
}

.search-box {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.search-box input {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 20px;
  width: 220px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.search-box input:focus {
  border-color: #5c83d8;
  box-shadow: 0 0 0 2px rgba(92, 131, 216, 0.2);
}

.search-btn {
  padding: 6px 12px;
  background: #bbc9e9;
  color: rgb(59, 85, 140);
  border: none;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.search-btn:hover {
  background: #456f9d;
  color: white;
}

.filter-buttons {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.filter-btn {
  padding: 6px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
  min-width: 52px;
  white-space: nowrap;
}

.filter-btn:hover {
  background: #f0f0f0;
}

.filter-btn.active {
  background: #5c83d8;
  color: white;
  border-color: #5c83d8;
}

/* 甘特图包装器 - 横向滚动 */
.scheduler-wrapper {
  width: 99%;
  overflow-x: auto;
  border-radius: 12px;
  border: 1px solid #eef2f6;
  background: white;
}

/* DayPilot 样式 - 与周视图一致 */
:deep(.scheduler_default_event_inner) {
  border-radius: 0 0 12px 12px !important;
  padding: 6px 10px !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  font-weight: 500;
}

:deep(.scheduler_default_timeheader_cell) {
  background-color: #f8f9fa !important;
  border-bottom: 2px solid #dee2e6 !important;
}

:deep(.scheduler_default_timeheader_cell_inner) {
  font-weight: 600 !important;
  color: rgb(44, 76, 150) !important;
  font-size: 14px;
}

/* 滚动条 */
.scheduler-wrapper::-webkit-scrollbar {
  height: 8px;
}

.scheduler-wrapper::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.scheduler-wrapper::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 20px;
  width: 90%;
  max-width: 500px;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 20px 35px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #2c4c96;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #999;
  transition: all 0.2s;
}

.close-btn:hover {
  color: #333;
}

.modal-body {
  padding: 24px;
}

.inputBar {
  margin-bottom: 20px;
}

.inputBar label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.inputBar input,
.inputBar textarea,
.inputBar select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 10px;
  font-size: 14px;
  font-family: inherit;
  box-sizing: border-box;
  transition: all 0.2s;
}

.inputBar input:focus,
.inputBar textarea:focus,
.inputBar select:focus {
  outline: none;
  border-color: #5c83d8;
  box-shadow: 0 0 0 2px rgba(92, 131, 216, 0.1);
}

.checkbox-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.checkbox-group input {
  width: auto;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-top: 1px solid #eee;
}

.footer-right {
  display: flex;
  gap: 12px;
}

.delete-btn {
  padding: 10px 20px;
  background: #f04a3e;
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.delete-btn:hover {
  background: #d43a2e;
}

.cancel-btn {
  padding: 10px 20px;
  background: #f0f0f0;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: #e0e0e0;
}

.save-btn {
  padding: 10px 20px;
  background: #5c83d8;
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.save-btn:hover {
  background: #456f9d;
}

/* 响应式 */
@media (max-width: 800px) {
  .toolbar {
    flex-wrap: wrap;
    gap: 12px;
  }
  
  .search-box {
    order: 1;
  }
  
  .filter-buttons {
    order: 2;
  }
  
  .nav-btn:first-child {
    order: 0;
  }
  
  .nav-btn:last-child {
    order: 3;
  }
}

/* 温和的边框闪烁动画 */
@keyframes borderSoftFlash {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 152, 0, 0.4);
    border: 1px solid #ff9800;
  }
  70% {
    box-shadow: 0 0 0 4px rgba(255, 152, 0, 0);
    border: 1px solid #ffcc80;
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 152, 0, 0);
    border: 1px solid #ff9800;
  }
}

:deep(.border-flash) {
  animation: borderSoftFlash 1.5s ease-out infinite;
  border-radius: 8px !important;
}

</style>