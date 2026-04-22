<template>
    <div class="content-wrapper">
        <div ref="chartRef" class="chart-box"></div>

        <div class="task-list">
            <h3>已完成任务列表</h3>

            <div v-for="task in completedTasks" :key="task.id" class="Card">
                <h4 class="taskTitle">{{ task.title }}</h4>
                <p class="taskDescription">{{ task.description }}</p>
                <p class="taskPriority">优先级: {{ priorityMap[task.priority] }}</p>
                <div class="taskTime">
                    <span class="taskStart">{{ task.start }}</span>
                    <span>-</span>
                    <span class="taskEnd">{{ task.end }}</span>
                </div>
                <p class="taskAction">{{ task.completed ? '已完成' : '进行中' }}</p>
            </div>

        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, watch} from 'vue';
import * as echarts from 'echarts';

let allTasks = ref([]); // 存储从后端拿到的所有任务
let chartRef = ref(null);
const priorityMap = { 1: '🟢 低', 2: '🟡 中', 3: '🔴 高' };

// 【核心】：利用计算属性自动过滤已完成任务
const completedTasks = computed(() => {
  return allTasks.value.filter(t => {return t.completed === true;});
});

// 计算统计数据用于饼图
const stats = computed(() => {
  const total = allTasks.value.length;
  const finished = completedTasks.value.length;
  return { finished, unfinished: total - finished };
});

// 筛选出本周的任务
const isWithinThisWeek = (dateStr) => {
    const taskDate = new Date(dateStr);
    const today = new Date();
    
    // 获取今天是周几（0是周日，1是周一...6是周六）
    const dayOfWeek = today.getDay();
    
    // 计算本周一的日期
    const monday = new Date(today);
    const diff = today.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1); 
    monday.setDate(diff);
    monday.setHours(0, 0, 0, 0); // 时间归零，方便比较

    // 比较：任务日期必须大于等于本周一，且小于等于今天
    return taskDate >= monday && taskDate <= today;
};

const fetchAllTasks = async () => {
  try {
    const res = await fetch('http://localhost:8080/api/getTasks');
    const data = await res.json();
    if (data.code === 200) {
      allTasks.value = data.data; // 假设后端返回 {code: 200, data: [...]}
      initChart();
    }
  } catch (err) {
    console.error('获取任务失败', err);
    allTasks.value= [
        { id: 1, title: '完成项目报告', description: '数据分析', priority: 3, start: '2026-04-08', end: '2026-04-10', completed: true },
        { id: 2, title: '拼豆', description: '拼2778个豆', priority: 2, start: '2026-04-12', end: '2026-04-15', completed: true },
        { id: 3, title: '拼拼豆', description: '拼2778个豆', priority: 1, start: '2026-04-12', end: '2026-04-14', completed: false }
    ];
    initChart();
  }
};

const initChart = () => {
  const chart = echarts.init(chartRef.value);
  chart.setOption({
    title: { text: '本周任务完成占比', left: 'center' },
    series: [{
      type: 'pie',
      data: [
        { value: stats.value.finished, name: '已完成 ' + stats.value.finished },
        { value: stats.value.unfinished, name: '未完成 ' + stats.value.unfinished }
      ]
    }]
  });
};

onMounted(fetchAllTasks);

</script>

<style scoped>
.chart-box { height: 300px; width: 100%; }
.task-item { border-bottom: 1px solid #ccc; padding: 10px; }

.content-wrapper{
    /* 背景颜色 */
    background: rgb(230, 234, 249);
    min-height: 100%;
    /* flex布局 */
    display: flex;
    flex-direction: column;
    /* 左对齐 */
    justify-content: left;
    /* 内边距 */
    padding: 5%;
    /* 字体 默认值 */
    font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

.Card {
    background-color: white;
    border-radius: 15px;
    padding: 1% 3% 1% 3%;
    margin-top: 10px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    transition: all 0.2s;
}

.Card:hover {
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
}

.taskTitle {
    font-size: 20px;
    font-weight: 600;
    margin-bottom: 5px;
}

.taskDescription {
    color: rgb(69, 111, 157);
    margin-bottom: 5px;
    line-height: 1.0;
}

.taskPriority {
    padding: 3px;
    border-radius: 15px;
    font-size: 15px;
    font-weight: 550;
    line-height: 1.0;
}

.taskTime {
    display: flex;
    align-items: center;
    gap: 3%;
    line-height: 1.5;
}

.taskAction {
    font-weight: 600;
    line-height: 1.0;
}

</style>

