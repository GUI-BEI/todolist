<template>
    <div class="content-wrapper">
        <div class="chart-container">
            <div ref="chartRef" class="chart-box"></div>
            <div class="chart-note">本周任务统计</div>
        </div>

        <div class="task-list">
            <h3>已完成任务列表</h3>
            <div v-if="completedTasks.length === 0" class="empty-tip">
                暂无已完成任务
            </div>
            <div v-for="task in completedTasks" :key="task.id" class="Card">
                <h4 class="taskTitle">{{ task.title }}</h4>
                <p class="taskDescription">{{ task.description || '无描述' }}</p>
                <p class="taskPriority">优先级: {{ priorityMap[task.priority] }}</p>
                <div class="taskTime">
                    <span class="taskStart">{{ formatDate(task.start) }}</span>
                    <span>-</span>
                    <span class="taskEnd">{{ formatDate(task.end) }}</span>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import * as echarts from 'echarts';
import { getTasks } from '@/api/task';
import { useRouter } from 'vue-router';

const router = useRouter();
const allTasks = ref([]);
const chartRef = ref(null);
const priorityMap = { 1: '🟢 低', 2: '🟡 中', 3: '🔴 高' };

// 所有已完成任务
const completedTasks = computed(() => {
    return allTasks.value.filter(task => task.completed === true);
});

// 获取本周的任务（用于饼图）
const thisWeekTasks = computed(() => {
    const thisWeekRange = getThisWeekRange();
    return allTasks.value.filter(task => {
        const taskStart = new Date(task.start);
        return taskStart >= thisWeekRange.start && taskStart <= thisWeekRange.end;
    });
});

// 本周统计数据
const thisWeekStats = computed(() => {
    const total = thisWeekTasks.value.length;
    const finished = thisWeekTasks.value.filter(t => t.completed === true).length;
    return { finished, unfinished: total - finished, total };
});

// 获取本周的起止日期（周一到周日）
const getThisWeekRange = () => {
    const today = new Date();
    const dayOfWeek = today.getDay();
    // 计算本周一（周一为第一天）
    const monday = new Date(today);
    const diff = today.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1);
    monday.setDate(diff);
    monday.setHours(0, 0, 0, 0);
    
    // 计算本周日
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    sunday.setHours(23, 59, 59, 999);
    
    return { start: monday, end: sunday };
};

// 格式化日期显示
const formatDate = (dateStr) => {
    if (!dateStr) return '';
    // 处理 "2026-04-30T00:00:00" 或 "2026-04-30 00:00:00" 格式
    return dateStr.replace('T', ' ').slice(0, 16);
};

// 初始化饼图
const initChart = () => {
    if (!chartRef.value) return;
    
    const chart = echarts.init(chartRef.value);
    const { finished, unfinished, total } = thisWeekStats.value;
    
    let option = {
        tooltip: {
            trigger: 'item',
            formatter: '{b}: {d}% ({c}个)'
        },
        legend: {
            orient: 'vertical',
            left: 'left',
            data: [
                { name: '已完成', itemStyle: { color: '#00bcd4' } },
                { name: '未完成', itemStyle: { color: '#2c4c96' } }
            ]
        },
        series: [
            {
                name: '本周任务统计',
                type: 'pie',
                radius: '55%',
                center: ['50%', '50%'],
                data: [
                    { value: finished, name: '已完成', itemStyle: { color: '#00bcd4' } },
                    { value: unfinished, name: '未完成', itemStyle: { color: '#2c4c96' } }
                ],
                emphasis: {
                    scale: true
                },
                label: {
                    show: true,
                    formatter: '{b}: {d}%'
                }
            }
        ]
    };
    
    // 如果本周没有任务，显示提示
    if (total === 0) {
        option = {
            title: {
                text: '本周暂无任务',
                left: 'center',
                top: 'center',
                textStyle: {
                    color: '#999',
                    fontSize: 14
                }
            },
            series: []
        };
    }
    
    chart.setOption(option);
    
    // 响应窗口大小变化
    window.addEventListener('resize', () => {
        chart.resize();
    });
};

// 获取所有任务
const fetchAllTasks = async () => {
    const token = localStorage.getItem('token');
    if (!token) {
        router.push('/login');
        return;
    }
    
    try {
        const result = await getTasks();
        if (result.code === 200) {
            allTasks.value = result.data;
            initChart();
        } else {
            throw new Error(result.message || '获取任务失败');
        }
    } catch (err) {
        console.error('获取任务失败', err);
        allTasks.value = [];
        initChart();
    }
};

onMounted(() => {
    fetchAllTasks();
});
</script>

<style scoped>
.chart-box {
    height: 300px;
    width: 100%;
}

.chart-container {
    background: white;
    border-radius: 20px;
    padding: 20px;
    margin-bottom: 30px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.chart-note {
    text-align: center;
    color: #666;
    font-size: 14px;
    margin-top: 10px;
}

.content-wrapper {
    background: rgb(245, 247, 254);
    min-height: 100%;
    display: flex;
    flex-direction: column;
    padding: 5%;
    font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

.task-list h3 {
    margin-bottom: 16px;
    color: #2c4c96;
    font-size: 20px;
    font-weight: 600;
}

.empty-tip {
    text-align: center;
    color: #999;
    padding: 40px;
    background: white;
    border-radius: 15px;
    font-size: 16px;
}

.Card {
    background-color: white;
    border-radius: 15px;
    padding: 16px 24px;
    margin-top: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    transition: all 0.2s;
}

.Card:hover {
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
}

.taskTitle {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 8px;
    color: #333;
}

.taskDescription {
    color: #666;
    margin-bottom: 8px;
    font-size: 14px;
    line-height: 1.4;
}

.taskPriority {
    padding: 4px 0;
    border-radius: 15px;
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 8px;
}

.taskTime {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #888;
    font-size: 14px;
    margin-bottom: 8px;
}

.taskStart, .taskEnd {
    font-family: monospace;
}
</style>