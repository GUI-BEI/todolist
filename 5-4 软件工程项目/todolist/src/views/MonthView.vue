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
                <span class="upload-hint">支持图片、文档等，最大10MB</span>
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
  deleteAttachment
} from '@/api/task';

import { useRouter } from 'vue-router';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import * as XLSX from 'xlsx';

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

  // 加载附件
  loadAttachments(task.id);
  
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
      closeEditModal();
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

// 导出为 Excel
const exportToExcel = () => {
  if (!currentTasks.value || currentTasks.value.length === 0) {
    showMessage('没有可导出的数据', true);
    return;
  }
  
  // 准备数据
  const data = currentTasks.value.map(task => ({
    '标题': task.title,
    '描述': task.description || '',
    '优先级': task.priority === 3 ? '高' : (task.priority === 2 ? '中' : '低'),
    '开始时间': task.start,
    '结束时间': task.end,
    '分类': task.type || '',
    '状态': task.completed ? '已完成' : '未完成'
  }));
  
  // 创建工作簿和工作表
  const ws = XLSX.utils.json_to_sheet(data);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, '任务列表');
  
  // 设置列宽
  ws['!cols'] = [
    { wch: 20 },  // 标题
    { wch: 30 },  // 描述
    { wch: 8 },   // 优先级
    { wch: 18 },  // 开始时间
    { wch: 18 },  // 结束时间
    { wch: 12 },  // 分类
    { wch: 8 }    // 状态
  ];
  
  // 导出文件
  XLSX.writeFile(wb, `tasks_${new Date().toISOString().slice(0, 10)}.xlsx`);
  showMessage('导出成功');
};

// 导入 CSV 文件
const importFromCSV = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  
  // 验证文件类型
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
    
    // 检查表头
    const headers = rows[0];
    const requiredHeaders = ['标题', '开始时间', '结束时间'];
    const missingHeaders = requiredHeaders.filter(h => !headers.includes(h));
    
    if (missingHeaders.length > 0) {
      showMessage(`CSV 缺少必要列: ${missingHeaders.join(', ')}`, true);
      return;
    }
    
    // 确认导入
    const confirmed = confirm(`共发现 ${rows.length - 1} 条任务，确定要导入吗？`);
    if (!confirmed) return;
    
    let successCount = 0;
    let failCount = 0;
    const errors = [];
    
    for (let i = 1; i < rows.length; i++) {
      const row = rows[i];
      const taskData = {};
      
      // 根据表头映射数据
      headers.forEach((header, index) => {
        const value = row[index] ? row[index].trim() : '';
        if (header === '标题') taskData.title = value;
        else if (header === '描述') taskData.description = value;
        else if (header === '优先级') taskData.priority = parsePriority(value);
        else if (header === '开始时间') taskData.start = value;
        else if (header === '结束时间') taskData.end = value;
        else if (header === '分类') taskData.type = value;
        else if (header === '状态') taskData.completed = value === '已完成' || value === 'true';
      });
      
      // 验证必填字段
      if (!taskData.title || !taskData.start || !taskData.end) {
        failCount++;
        errors.push(`第 ${i + 1} 行: 缺少必填字段`);
        continue;
      }
      
      // 格式化时间
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
      
      // 每10条显示一次进度
      if ((successCount + failCount) % 10 === 0) {
        showMessage(`正在导入... 成功: ${successCount}, 失败: ${failCount}`);
      }
    }
    
    // 显示导入结果
    let message = `导入完成！\n成功: ${successCount}\n失败: ${failCount}`;
    if (errors.length > 0 && errors.length <= 5) {
      message += '\n\n错误详情:\n' + errors.join('\n');
    } else if (errors.length > 5) {
      message += `\n\n共有 ${errors.length} 条错误，请检查数据格式`;
    }
    alert(message);
    
    if (successCount > 0) {
      await fetchTasks(); // 刷新视图
    }
    
    // 清空 input，允许重复导入同一文件
    event.target.value = '';
  };
  
  reader.readAsText(file, 'UTF-8');
};

// 解析 CSV
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

// 解析优先级
const parsePriority = (priorityStr) => {
  if (!priorityStr) return 2;
  const str = priorityStr.toString().toLowerCase();
  if (str === '高' || str === 'high' || str === '3') return 3;
  if (str === '低' || str === 'low' || str === '1') return 1;
  return 2; // 默认中优先级
};

// 格式化导入的日期时间
const formatImportDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '';
  
  // 处理 "2026-05-02" 格式（只有日期）
  if (/^\d{4}-\d{2}-\d{2}$/.test(dateTimeStr)) {
    return dateTimeStr + ' 00:00:00';
  }
  
  // 处理 "2026-05-02 14:30" 格式
  if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}$/.test(dateTimeStr)) {
    return dateTimeStr + ':00';
  }
  
  // 处理 "2026-05-02T14:30" 格式
  if (dateTimeStr.includes('T')) {
    return dateTimeStr.replace('T', ' ') + ':00';
  }
  
  // 已经是完整格式
  return dateTimeStr;
};

// 下载 CSV 模板
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

// 导出为 CSV
const exportToCSV = () => {
  if (!currentTasks.value || currentTasks.value.length === 0) {
    showMessage('没有可导出的数据', true);
    return;
  }
  
  // 定义表头
  const headers = ['标题', '描述', '优先级', '开始时间', '结束时间', '分类', '状态'];
  
  // 转换数据
  const rows = currentTasks.value.map(task => [
    task.title,
    task.description || '',
    task.priority === 3 ? '高' : (task.priority === 2 ? '中' : '低'),
    task.start || '',
    task.end || '',
    task.type || '',
    task.completed ? '已完成' : '未完成'
  ]);
  
  // 构建 CSV 内容
  let csvContent = headers.join(',') + '\n';
  
  rows.forEach(row => {
    const escapedRow = row.map(cell => {
      // 处理空值
      const value = cell || '';
      // 如果包含逗号、换行或引号，用双引号包裹
      if (value.includes(',') || value.includes('\n') || value.includes('"')) {
        return `"${value.replace(/"/g, '""')}"`;
      }
      return value;
    });
    csvContent += escapedRow.join(',') + '\n';
  });
  
  // 下载文件（添加 BOM 解决中文乱码）
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  const url = URL.createObjectURL(blob);
  link.href = url;
  link.download = `tasks_${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.csv`;
  link.click();
  URL.revokeObjectURL(url);
  
  showMessage(`导出成功，共 ${currentTasks.value.length} 条任务`);
};

// 获取完整附件URL
const getFullAttachmentUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http')) return url;
  return `http://localhost:8080${url}`;
};

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
};

// 加载附件列表
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
    console.error('上传失败', err);
    showMessage('上传失败', true);
  } finally {
    uploading.value = false;
    event.target.value = '';
  }
};

// 删除附件
const deleteAttachmentFile = async (attachmentId) => {
  if (!confirm('确定要删除此附件吗？')) return;
  
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
const previewImageName = ref('');

const previewAttachment = async (attachment) => {
  if (attachment.fileType?.startsWith('image/')) {
    // 图片预览
    previewImageUrl.value = getFullAttachmentUrl(attachment.downloadUrl);
    previewImageName.value = attachment.fileName;
  } else {
    // 非图片直接下载
    await downloadAttachment(attachment);
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

/* 图片预览弹窗 */
.preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.preview-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.preview-content img {
  max-width: 100%;
  max-height: 90vh;
  border-radius: 8px;
}

.preview-close {
  position: absolute;
  top: -40px;
  right: 0;
  background: white;
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  font-size: 20px;
  cursor: pointer;
}
</style>