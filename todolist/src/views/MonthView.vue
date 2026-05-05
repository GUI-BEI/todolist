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

      <button class="nav-btn" @click="exportToLongImage">导出 PNG</button>
      <button class="nav-btn" @click="exportToExcel">导出 Excel</button>

      <div class="import-export-group">
        <label class="nav-btn import-csv-btn">
          导入 CSV
          <input type="file" accept=".csv" @change="importFromCSV" style="display: none;">
        </label>
        <button class="nav-btn template-btn" @click="downloadCSVTemplate">下载模板</button>
        <button class="nav-btn export-csv-btn" @click="exportToCSV">导出 CSV</button>
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
            <input type="datetime-local" v-model="editingTask.end">
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

          <div class="inputBar">
            <label>附件</label>
            <div class="attachment-area">
              <div class="attachment-list">
                <div v-for="att in attachments" :key="att.id" class="attachment-item">
                  <span class="attachment-icon">📎</span>
                  <span class="attachment-name" @click="previewAttachment(att)">
                    {{ att.fileName }}
                  </span>
                  <span class="attachment-size">{{ formatFileSize(att.fileSize) }}</span>
                  <button class="attachment-delete" @click="deleteAttachmentFile(att.id)">×</button>
                </div>
                <div v-if="attachments.length === 0" class="attachment-empty">
                  暂无附件
                </div>
              </div>
              <div class="attachment-upload">
                <label class="upload-btn">
                  选择文件
                  <input type="file" @change="uploadNewAttachment" style="display: none;">
                </label>
                <span class="upload-hint">支持图片、文档等，最大50MB</span>
              </div>
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

import { 
  getFilteredTasks, 
  getTasks, 
  updateTaskTime, 
  updateTask, 
  deleteTask as deleteTaskApi,
  uploadAttachment,
  getAttachments,
  deleteAttachment,
  addTask
} from '@/api/task';

import { useRouter } from 'vue-router';
import html2canvas from 'html2canvas';
import * as XLSX from 'xlsx';
import { getFullAttachmentUrl } from '@/utils/urlHelper';
import { showToast, showConfirm } from '@/utils/message';

const router = useRouter();

// 筛选配置
const filters = [
  { label: '所有', value: 'all' },
  { label: '高', value: '3' },
  { label: '中', value: '2' },
  { label: '低', value: '1' }
];

// 附件相关
const attachments = ref([]);
const uploading = ref(false);

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

// 优先级配置（仅用于背景色，彩条使用CSS类）
const priorityConfig = {
  3: { name: '高', bgColor: '#ffebee' },
  2: { name: '中', bgColor: '#fff3e0' },
  1: { name: '低', bgColor: '#e8f5e9' }
};

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
    
    fetchTasks();
  }
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
  loadAttachments(task.id);
};

// 删除任务
const deleteTask = async () => {
  if (!editingTask.value.id) return;
  
  const confirmed = await showConfirm(`确定要删除任务「${editingTask.value.title}」吗？`);
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
  attachments.value = [];
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
    const startDateTime = editingTask.value.start 
      ? editingTask.value.start.replace('T', ' ') + ':00' 
      : '';
    const endDateTime = editingTask.value.end 
      ? editingTask.value.end.replace('T', ' ') + ':00' 
      : '';

    // 时间校验
    if (startDateTime && endDateTime) {
      const start = new Date(startDateTime.replace(' ', 'T'));
      const end = new Date(endDateTime.replace(' ', 'T'));
      
      if (end <= start) {
        showMessage('结束时间必须晚于开始时间', true);
        return;
      }
    }

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

// Scheduler 配置
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
    const completed = args.data.completed;
    const type = args.data.type;

    // 计算距离截止时间还有多少小时
    let hoursRemaining = 25;
    const endStr = args.data.end;
    if (endStr) {
      let endDate = new Date(endStr);
      const now = new Date();
      const diffMs = endDate - now;
      hoursRemaining = diffMs / (1000 * 60 * 60);
    }

    // 设置背景色
    args.data.barHidden = true;
    const config = priorityConfig[priority] || priorityConfig[1];
    args.data.backColor = config.bgColor;
    args.data.fontColor = "#333333";

    // 根据完成状态调整样式
    if (completed) {
      args.data.backColor = "#e0e0e0";
      args.data.fontColor = "#888888";
    }

    // 设置 cssClass - 优先级上边框
    // 紧急任务使用闪烁样式，优先级效果会被父容器的优先级样式覆盖
    const isUrgent = (hoursRemaining < 24 && hoursRemaining > -12 && !completed);
    
    if (isUrgent) {
      // 紧急任务：根据优先级使用不同的闪烁样式
      if (priority === 3) {
        args.data.cssClass = "flash-high";
      } else if (priority === 2) {
        args.data.cssClass = "flash-medium";
      } else {
        args.data.cssClass = "flash-low";
      }
    } else {
      // 正常任务：只显示优先级上边框
      if (priority === 3) {
        args.data.cssClass = "priority-high";
      } else if (priority === 2) {
        args.data.cssClass = "priority-medium";
      } else if (priority === 1) {
        args.data.cssClass = "priority-low";
      }
    }
    
    // 设置显示文本：如果有分类，显示 [分类] 标题
    if (type && type.trim()) {
      args.data.text = `[${type}] ${args.data.text}`;
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

// 导出为长图 PNG
const exportToLongImage = async () => {
  if (!schedulerRef.value) {
    showMessage('调度器未就绪', true);
    return;
  }
  
  showMessage('正在生成长图，请稍候...');
  
  try {
    const container = document.querySelector('.scheduler_default_main');
    if (!container) {
      showMessage('未找到调度器元素', true);
      return;
    }
    
    const scrollWidth = container.scrollWidth;
    const scrollHeight = container.scrollHeight;
    const originalWidth = container.style.width;
    const originalHeight = container.style.height;
    const originalOverflow = container.style.overflow;
    
    container.style.width = `${scrollWidth}px`;
    container.style.height = `${scrollHeight}px`;
    container.style.overflow = 'visible';
    
    const canvas = await html2canvas(container, {
      scale: 2,
      backgroundColor: '#ffffff',
      logging: false,
      useCORS: true,
      windowWidth: scrollWidth,
      windowHeight: scrollHeight
    });
    
    container.style.width = originalWidth;
    container.style.height = originalHeight;
    container.style.overflow = originalOverflow;
    
    const link = document.createElement('a');
    link.download = `month_schedule_${currentYear.value}_${currentMonth.value}.png`;
    link.href = canvas.toDataURL();
    link.click();
    
    showMessage('导出成功');
  } catch (error) {
    console.error('导出失败:', error);
    showMessage('导出失败: ' + error.message, true);
    const container = document.querySelector('.scheduler_default_main');
    if (container) {
      container.style.overflow = '';
    }
  }
};

// 导出为 Excel
const exportToExcel = () => {
  if (!currentTasks.value || currentTasks.value.length === 0) {
    showMessage('没有可导出的数据', true);
    return;
  }
  
  const data = currentTasks.value.map(task => ({
    '标题': task.title,
    '描述': task.description || '',
    '优先级': task.priority === 3 ? '高' : (task.priority === 2 ? '中' : '低'),
    '开始时间': task.start,
    '结束时间': task.end,
    '分类': task.type || '',
    '状态': task.completed ? '已完成' : '未完成'
  }));
  
  const ws = XLSX.utils.json_to_sheet(data);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, '任务列表');
  
  ws['!cols'] = [
    { wch: 20 }, { wch: 30 }, { wch: 8 }, { wch: 18 }, { wch: 18 }, { wch: 12 }, { wch: 8 }
  ];
  
  XLSX.writeFile(wb, `tasks_${new Date().toISOString().slice(0, 10)}.xlsx`);
  showMessage('导出成功');
};

// 导入 CSV 文件
const importFromCSV = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  if (!file.name.endsWith('.csv')) {
    showMessage('请选择 CSV 文件', true);
    return;
  }
  
  const reader = new FileReader();
  reader.onload = async (e) => {
    const content = e.target.result;
    const rows = parseCSV(content);
    
    if (rows.length === 0) {
      showMessage('CSV 文件为空或格式错误', true);
      return;
    }
    
    const headers = rows[0];
    const requiredHeaders = ['标题', '开始时间', '结束时间'];
    const missingHeaders = requiredHeaders.filter(h => !headers.includes(h));
    
    if (missingHeaders.length > 0) {
      showMessage(`CSV 缺少必要列: ${missingHeaders.join(', ')}`, true);
      return;
    }
    
    const confirmed = await showConfirm(`共发现 ${rows.length - 1} 条任务，确定要导入吗？`);
    if (!confirmed) return;
    
    let successCount = 0;
    let failCount = 0;
    const errors = [];
    
    for (let i = 1; i < rows.length; i++) {
      const row = rows[i];
      const taskData = {};
      
      headers.forEach((header, index) => {
        const value = row[index] ? row[index].trim() : '';
        if (header === '标题') taskData.title = value;
        else if (header === '描述') taskData.description = value;
        else if (header === '优先级') taskData.priority = parsePriority(value);
        else if (header === '开始时间') taskData.start = value;
        else if (header === '结束时间') taskData.end = value;
        else if (header === '分类') taskData.type = value;
      });
      
      if (!taskData.title || !taskData.start || !taskData.end) {
        failCount++;
        errors.push(`第 ${i + 1} 行: 缺少必填字段`);
        continue;
      }
      
      taskData.start = formatImportDateTime(taskData.start);
      taskData.end = formatImportDateTime(taskData.end);
      
      try {
        const result = await addTask(taskData);
        if (result.code === 200) {
          successCount++;
        } else {
          failCount++;
          errors.push(`第 ${i + 1} 行: ${result.message || '添加失败'}`);
        }
      } catch (err) {
        failCount++;
        errors.push(`第 ${i + 1} 行: 网络错误`);
      }
      
      if ((successCount + failCount) % 10 === 0) {
        showMessage(`正在导入... 成功: ${successCount}, 失败: ${failCount}`);
      }
    }
    
    let message = `导入完成！\n成功: ${successCount}\n失败: ${failCount}`;
    if (errors.length > 0 && errors.length <= 5) {
      message += '\n\n错误详情:\n' + errors.join('\n');
    } else if (errors.length > 5) {
      message += `\n\n共有 ${errors.length} 条错误，请检查数据格式`;
    }
    showToast(message);
    
    if (successCount > 0) {
      await fetchTasks();
    }
    
    event.target.value = '';
  };
  
  reader.readAsText(file, 'UTF-8');
};

const parseCSV = (content) => {
  const rows = [];
  const lines = content.split(/\r?\n/);
  
  for (const line of lines) {
    if (line.trim() === '') continue;
    
    const row = [];
    let inQuotes = false;
    let currentCell = '';
    
    for (let i = 0; i < line.length; i++) {
      const char = line[i];
      
      if (char === '"') {
        inQuotes = !inQuotes;
      } else if (char === ',' && !inQuotes) {
        row.push(currentCell);
        currentCell = '';
      } else {
        currentCell += char;
      }
    }
    row.push(currentCell);
    
    rows.push(row);
  }
  
  return rows;
};

const parsePriority = (priorityStr) => {
  if (!priorityStr) return 2;
  const str = priorityStr.toString().toLowerCase();
  if (str === '高' || str === 'high' || str === '3') return 3;
  if (str === '低' || str === 'low' || str === '1') return 1;
  return 2;
};

const formatImportDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '';
  
  if (/^\d{4}-\d{2}-\d{2}$/.test(dateTimeStr)) {
    return dateTimeStr + ' 00:00:00';
  }
  
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/.test(dateTimeStr)) {
    return dateTimeStr + ':00';
  }
  
  if (dateTimeStr.includes('T')) {
    return dateTimeStr.replace('T', ' ') + ':00';
  }
  
  return dateTimeStr;
};

const downloadCSVTemplate = () => {
  const headers = ['标题', '描述', '优先级', '开始时间', '结束时间', '分类', '状态'];
  const exampleRow = ['示例任务', '这是示例描述', '中', '2026-05-02 09:00', '2026-05-02 17:00', '工作', '未完成'];
  
  const csvContent = [headers.join(','), exampleRow.join(',')].join('\n');
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  link.href = url;
  link.download = 'task_import_template.csv';
  link.click();
  URL.revokeObjectURL(url);
  
  showMessage('模板下载成功');
};

const exportToCSV = () => {
  if (!currentTasks.value || currentTasks.value.length === 0) {
    showMessage('没有可导出的数据', true);
    return;
  }
  
  const headers = ['标题', '描述', '优先级', '开始时间', '结束时间', '分类'];
  
  const rows = currentTasks.value.map(task => [
    task.title,
    task.description || '',
    task.priority === 3 ? '高' : (task.priority === 2 ? '中' : '低'),
    task.start ? task.start.replace('T', ' ').slice(0, 16) : '',
    task.end ? task.end.replace('T', ' ').slice(0, 16) : '',
    task.type || ''
  ]);
  
  let csvContent = headers.join(',') + '\n';
  
  rows.forEach(row => {
    const escapedRow = row.map(cell => {
      const value = cell || '';
      if (value.includes(',') || value.includes('\n') || value.includes('"')) {
        return `"${value.replace(/"/g, '""')}"`;
      }
      return value;
    });
    csvContent += escapedRow.join(',') + '\n';
  });
  
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  link.href = url;
  link.download = `tasks_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.csv`;
  link.click();
  URL.revokeObjectURL(url);
  
  showMessage(`导出成功，共 ${currentTasks.value.length} 条任务`);
};

const formatFileSize = (bytes) => {
  if (!bytes) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
};

const loadAttachments = async (taskId) => {
  try {
    const result = await getAttachments(taskId);
    if (result.code === 200) {
      attachments.value = result.data;
    }
  } catch (err) {
    console.error('加载附件失败', err);
  }
};

// 上传附件
const uploadNewAttachment = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  // 前端大小校验
  if (file.size > 50 * 1024 * 1024) {
    showMessage('文件大小不能超过50MB', true);
    event.target.value = '';
    return;
  }
  
  if (!editingTask.value.id) {
    showMessage('请先保存任务后再上传附件', true);
    return;
  }
  
  uploading.value = true;
  
  try {
    const result = await uploadAttachment(editingTask.value.id, file);
    
    if (result.code === 200) {
      attachments.value.push(result.data);
      showMessage('附件上传成功');
    } else {
      throw new Error(result.message || '上传失败');
    }
  } catch (err) {
    console.error('上传失败:', err);
    showMessage('上传失败: ' + (err.message || '未知错误'), true);
  } finally {
    uploading.value = false;
    event.target.value = '';
  }
};

const deleteAttachmentFile = async (attachmentId) => {
  if (!await showConfirm('确定要删除此附件吗？')) return;
  
  try {
    const result = await deleteAttachment(attachmentId);
    if (result.code === 200) {
      attachments.value = attachments.value.filter(a => a.id !== attachmentId);
      showMessage('删除成功');
    } else {
      throw new Error(result.message || '删除失败');
    }
  } catch (err) {
    console.error('删除失败', err);
    showMessage('删除失败', true);
  }
};

const previewImageUrl = ref(null);

const previewAttachment = async (attachment) => {
  if (attachment.fileType?.startsWith('image/')) {
    previewImageUrl.value = getFullAttachmentUrl(attachment.downloadUrl);
  } else {
    window.open(getFullAttachmentUrl(attachment.downloadUrl), '_blank');
  }
};

const closePreview = () => {
  previewImageUrl.value = null;
};

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
      const firstDay = getFirstDayOfMonth(currentYear.value, currentMonth.value);
      const lastDay = new Date(currentYear.value, currentMonth.value, 0);
      params.startDate = firstDay.toISOString().split('T')[0] + 'T00:00:00';
      params.endDate = lastDay.toISOString().split('T')[0] + 'T23:59:59';
      
      result = await getFilteredTasks(params);
    } else {
      const firstDay = getFirstDayOfMonth(currentYear.value, currentMonth.value);
      const lastDay = new Date(currentYear.value, currentMonth.value, 0);
      const params = {
        startDate: firstDay.toISOString().split('T')[0] + 'T00:00:00',
        endDate: lastDay.toISOString().split('T')[0] + 'T23:59:59'
      };
      result = await getTasks(params);
    }
    
    if (result.code === 200) {
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

const handleFilterChange = (value) => {
  currentFilter.value = value;
  fetchTasks();
};

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

  const firstDay = getFirstDayOfMonth(currentYear.value, currentMonth.value);
  const daysInMonth = getDaysInMonth(currentYear.value, currentMonth.value);
  
  schedulerConfig.value.startDate = new DayPilot.Date(firstDay, true);
  schedulerConfig.value.days = daysInMonth;
  
  setTimeout(() => {
    if (schedulerRef.value) {
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
/* 优先级上边框 - 与 MainInterface 保持一致 */
.priority-high {
  border-top: 5px solid #e05656 !important;
}
.priority-medium {
  border-top: 5px solid #ee9445 !important;
}
.priority-low {
  border-top: 5px solid #82d36e !important;
}

:deep(.scheduler_default_event_inner) {
  padding-top: 8px !important;
}

/* 高优先级闪烁 - 红色 */
@keyframes flashHigh {
  0% { border: 2px solid #ee8a8a; border-top: 5px solid #f19595; box-shadow: 0 0 0 0 rgba(247, 152, 152, 0.5); }
  50% { border: 2px solid #eb5c5c; border-top: 5px solid #ea5555; box-shadow: 0 0 8px 2px rgba(230, 107, 107, 0.8); }
  100% { border: 2px solid #e05656; border-top: 5px solid #e05656; box-shadow: 0 0 0 0 rgba(224, 80, 80, 0.942); }
}
.flash-high {
  animation: flashHigh 1.5s ease-out infinite !important;
  border-radius: 8px !important;
}

/* 中优先级闪烁 - 橙色 */
@keyframes flashMedium {
  0% { border: 2px solid #f3e1ab; border-top: 5px solid #edd898; box-shadow: 0 0 0 0 rgba(233, 213, 154, 0.886); }
  50% { border: 2px solid #eac26c; border-top: 5px solid #e6bc5f; box-shadow: 0 0 8px 2px rgba(240, 198, 72, 0.8); }
  100% { border: 2px solid #fdd251; border-top: 5px solid #f8c93d; box-shadow: 0 0 0 0 rgba(255, 197, 23, 0.889); }
}
.flash-medium {
  animation: flashMedium 1.5s ease-out infinite !important;
  border-radius: 8px !important;
}

/* 低优先级闪烁 - 黄色/橙色 */
@keyframes flashLow {
  0% { border: 2px solid #c9f7ae; border-top: 5px solid #c9f7ae; box-shadow: 0 0 0 0 rgba(210, 243, 160, 0.5); }
  50% { border: 2px solid #b3e794; border-top: 5px solid #b9ed9b; box-shadow: 0 0 8px 2px rgba(189, 228, 132, 0.8); }
  100% { border: 2px solid #7ec059; border-top: 5px solid #96d373; box-shadow: 0 0 0 0 rgba(168, 225, 83, 0.5); }
}
.flash-low {
  animation: flashLow 1.5s ease-out infinite !important;
  border-radius: 8px !important;
}

/* DayPilot 事件内部样式 */
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

/* 黑夜模式覆盖 */
body.dark-mode .month-view {
  background: #1a1a2e;
}

body.dark-mode .toolbar {
  background: #0f0f1a;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

body.dark-mode .current-month {
  color: #8ab3ff;
}

body.dark-mode .nav-btn {
  background: #1a1a2e;
  border-color: #2c2c3e;
  color: #e0e0e0;
}

body.dark-mode .nav-btn:hover {
  background: #5c83d8;
  color: white;
}

body.dark-mode .search-box input {
  background: #1a1a2e;
  border-color: #2c2c3e;
  color: #e0e0e0;
}

body.dark-mode .search-box input:focus {
  border-color: #5c83d8;
}

body.dark-mode .search-btn {
  background: #2a2a3e;
  color: #8ab3ff;
}

body.dark-mode .filter-btn {
  background: #1a1a2e;
  border-color: #2c2c3e;
  color: #e0e0e0;
}

body.dark-mode .filter-btn.active {
  background: #5c83d8;
  color: white;
}

/* DayPilot 黑夜模式 */
body.dark-mode .scheduler_default_main,
body.dark-mode .scheduler_default_cell {
  background-color: #16213e !important;
  color: #e0e0e0 !important;
}

body.dark-mode .scheduler_default_timeheader_cell {
  background-color: #0f0f1a !important;
  border-bottom: 2px solid #2c2c3e !important;
  color: #8ab3ff !important;
}

body.dark-mode .scheduler_default_timeheader_cell_inner {
  color: #8ab3ff !important;
}

body.dark-mode .scheduler_default_event_inner {
  background-color: #0f3460 !important;
  color: #e0e0e0 !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.3) !important;
}

/* 弹窗黑夜模式 */
body.dark-mode .modal-overlay {
  background-color: rgba(0, 0, 0, 0.7);
}

body.dark-mode .modal-content {
  background: #16213e;
  color: #e0e0e0;
}

body.dark-mode .modal-header {
  border-bottom-color: #2c2c3e;
}

body.dark-mode .modal-header h3 {
  color: #8ab3ff;
}

body.dark-mode .inputBar label {
  color: #e0e0e0;
}

body.dark-mode .inputBar input,
body.dark-mode .inputBar textarea,
body.dark-mode .inputBar select {
  background: #2c2c3e;
  border-color: #3a3a4e;
  color: #e0e0e0;
}

body.dark-mode .save-btn {
  background: #4a6fb8;
}

body.dark-mode .cancel-btn {
  background: #2a2a3e;
  color: #e0e0e0;
}

body.dark-mode .delete-btn {
  background: #c62828;
}

/* 附件区域黑夜模式 */
body.dark-mode .attachment-area {
  background: #0f0f1a;
  border-color: #2c2c3e;
}

body.dark-mode .attachment-item {
  background: #1a1a2e;
  border-color: #2c2c3e;
}

body.dark-mode .attachment-name {
  color: #8ab3ff;
}

body.dark-mode .upload-btn {
  background: #4a6fb8;
}
</style>

<style scoped>
.month-view {
  width: 100%;
  overflow-x: auto;
  background: rgb(255, 255, 255);
  min-height: 100%;
}

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

.scheduler-wrapper {
  width: 99%;
  overflow-x: auto;
  border-radius: 12px;
  border: 1px solid #eef2f6;
  background: white;
}

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

/* 附件区域样式 */
.attachment-area {
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
  background: #fafafa;
}

.attachment-list {
  max-height: 150px;
  overflow-y: auto;
  margin-bottom: 12px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: white;
  border-radius: 8px;
  margin-bottom: 6px;
  border: 1px solid #eee;
}

.attachment-icon {
  font-size: 16px;
}

.attachment-name {
  flex: 1;
  color: #2c4c96;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  text-decoration: none;
}

.attachment-name:hover {
  text-decoration: underline;
  color: #5c83d8;
}

.attachment-size {
  font-size: 11px;
  color: #999;
}

.attachment-delete {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background: #ffebee;
  color: #f44336;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.attachment-delete:hover {
  background: #f44336;
  color: white;
}

.attachment-empty {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
}

.attachment-upload {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.upload-btn {
  padding: 6px 16px;
  background-color: #5c83d8;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.upload-btn:hover {
  background-color: #456f9d;
}

.upload-hint {
  font-size: 11px;
  color: #999;
}

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
</style>