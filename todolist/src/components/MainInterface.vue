<template>
  <div class="main-interface">
    <div class="schedule-list">
      <div 
        v-for="task in tasks" 
        :key="task.id" 
        class="schedule-item"
      >
        <div 
          class="schedule-content" 
          :style="{ 
            width: getTaskWidth(task.start, task.end) + '%', 
            marginLeft: getTaskOffset(task.start) + '%' 
          }"
        >
          {{ formatDate(task.start) }}->{{ formatDate(task.end) }} {{ task.title }}:{{ task.description }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

const tasks = ref([]);
const today = new Date();

// --- 核心算法逻辑 (直接从你原来的脚本搬过来并适配) ---

// 1. 获取本周第一天
const getFirstWeekDate = (date) => {
  const firstDay = new Date(date);
  let diff = date.getDay() === 0 ? -6 : 1 - date.getDay();
  firstDay.setDate(date.getDate() + diff);
  return firstDay;
};

// 2. 计算宽度 (%)
const getTaskWidth = (startDate, endDate) => {
  const start = new Date(startDate);
  const end = new Date(endDate);
  const firstDay = getFirstWeekDate(today);
  const endDay = new Date(firstDay);
  endDay.setDate(firstDay.getDate() + 6);

  const actualStart = new Date(Math.max(start, firstDay));
  const actualEnd = new Date(Math.min(end, endDay));
  
  if (actualStart > actualEnd) return 0;

  const diffDays = Math.ceil((actualEnd - actualStart) / (1000 * 60 * 60 * 24)) + 1;
  return diffDays * 14; // 每个单位 14%
};

// 3. 计算左偏移 (%)
const getTaskOffset = (startDate) => {
  const start = new Date(startDate);
  const firstDay = getFirstWeekDate(today);
  
  // 如果任务开始时间早于本周第一天，偏移为 0
  if (start < firstDay) return 0;
  
  const dayIndex = (start.getDay() + 6) % 7;
  return dayIndex * 14;
};

// 4. 数据获取
const fetchTasks = async () => {
  try {
    const res = await fetch('http://localhost:8080/api/getTasks');
    const result = await res.json();
    if (result.code === 200) {
      tasks.value = result.data;
    }
  } catch (err) {
    console.error('获取任务失败', err);
  }
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const parts = dateStr.split('-');
  return `${parseInt(parts[1])}/${parseInt(parts[2])}`;
};

onMounted(() => {
  fetchTasks();
});

</script>

<style scoped>
/* 这里填入原来 .css 里的样式*/
</style>