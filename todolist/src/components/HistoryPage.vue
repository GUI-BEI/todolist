<template>
    <div class="content-wrapper">
        <div class="chart-container">
            <div ref="chartRef" class="chart-box"></div>
            <div class="chart-note">本周任务统计</div>
        </div>

        <!-- 筛选栏 -->
        <div class="filter-bar">
            <select v-model="dateFilter" @change="filterTasks" class="filter-select">
                <option value="all">全部任务</option>
                <option value="week">本周</option>
                <option value="month">本月</option>
                <option value="year">今年</option>
            </select>
            <input type="text" v-model="searchKeyword" placeholder="搜索任务..." class="search-input" @input="filterTasks">
        </div>

        <div class="task-list">
            <h3>已完成任务列表 <span class="task-count">(共 {{ filteredCompletedTasks.length }} 项)</span></h3>
            
            <div v-if="filteredCompletedTasks.length === 0" class="empty-tip">
                暂无已完成任务
            </div>
            
            <div v-for="task in filteredCompletedTasks" :key="task.id" class="Card">
                <div class="card-header">
                    <h4 class="taskTitle">{{ task.title }}</h4>
                    <span class="priority-badge" :class="priorityClass(task.priority)">
                        {{ priorityMap[task.priority] }}
                    </span>
                </div>
                
                <p class="taskDescription">{{ task.description || '无描述' }}</p>
                
                <div class="task-detail-row">
                    <span class="detail-label">分类：</span>
                    <span class="detail-value">{{ task.type || '未分类' }}</span>
                </div>
                
                <div class="taskTime">
                    <div class="time-item">
                        <span class="time-label">开始</span>
                        <span class="time-value">{{ formatDateTime(task.start) }}</span>
                    </div>
                    <div class="time-arrow">→</div>
                    <div class="time-item">
                        <span class="time-label">结束</span>
                        <span class="time-value">{{ formatDateTime(task.end) }}</span>
                    </div>
                </div>
                
                <div class="task-stats">
                    <div class="stat-item">
                        <span class="stat-label">耗时</span>
                        <span class="stat-value">
                            {{ calculateDuration(task.start, task.completedAt, task.end) }}
                        </span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">完成于</span>
                        <span class="stat-value">
                            <template v-if="task.completedAt">
                                {{ formatDate(task.completedAt) }}
                            </template>
                            <template v-else-if="task.end">
                                {{ formatDate(task.end) }}
                                <span style="color:#999; font-size:11px;">(计划)</span>
                            </template>
                            <template v-else>
                                未知
                            </template>
                        </span>
                    </div>
                </div>
                
                <!-- 附件预览 -->
                <div v-if="task.attachments && task.attachments.length > 0" class="attachments-preview">
                    <span class="attachment-label">📎 附件 ({{ task.attachments.length }})</span>
                    <div class="attachment-thumbnails">
                        <div v-for="att in task.attachments.slice(0, 3)" :key="att.id" class="attachment-thumb">
                            <span class="thumb-icon">📄</span>
                            <span class="thumb-name">{{ att.fileName }}</span>
                        </div>
                        <span v-if="task.attachments.length > 3" class="more-attach">
                            +{{ task.attachments.length - 3 }}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import * as echarts from 'echarts';
import { getTasks, getAttachments } from '@/api/task';
import { useRouter } from 'vue-router';

const router = useRouter();
const allTasks = ref([]);
const chartRef = ref(null);
const dateFilter = ref('all');
const searchKeyword = ref('');
const attachmentsCache = ref(new Map());

const priorityMap = { 1: '🟢 低', 2: '🟡 中', 3: '🔴 高' };

const priorityClass = (priority) => {
    return {
        1: 'priority-low-badge',
        2: 'priority-medium-badge',
        3: 'priority-high-badge'
    }[priority];
};

// 格式化日期时间（完整显示）
const formatDateTime = (dateStr) => {
    if (!dateStr) return '未设置';
    return dateStr.replace('T', ' ').slice(0, 19);
};

// 格式化日期（仅日期）
const formatDate = (dateStr) => {
    if (!dateStr) return '';
    return dateStr.replace('T', ' ').slice(0, 10);
};

// 计算任务耗时
const calculateDuration = (start, completedAt, end) => {
    if (!start) return '未知';
    
    // 获取有效的结束时间
    let endTime = null;
    
    // 对于已完成的任务，优先使用实际完成时间
    if (completedAt) {
        endTime = completedAt;
        console.log('使用实际完成时间:', endTime);
    } else if (end) {
        // 降级使用计划结束时间（兼容旧数据）
        endTime = end;
        console.log('使用计划结束时间:', endTime);
    } else {
        return '未知';
    }
    
    const startDate = new Date(start.replace(' ', 'T'));
    const endDate = new Date(endTime.replace(' ', 'T'));
    const diffMs = endDate - startDate;
    
    // 如果实际完成时间早于开始时间（数据异常）
    if (diffMs < 0) {
        return '数据异常';
    }
    
    if (diffMs === 0) return '0分钟';
    
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffMinutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    
    if (diffHours > 0) {
        return `${diffHours}小时${diffMinutes > 0 ? diffMinutes + '分钟' : ''}`;
    }
    return `${diffMinutes}分钟`;
};

// 获取本周起止日期
const getThisWeekRange = () => {
    const today = new Date();
    const dayOfWeek = today.getDay();
    const monday = new Date(today);
    const diff = today.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1);
    monday.setDate(diff);
    monday.setHours(0, 0, 0, 0);
    
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    sunday.setHours(23, 59, 59, 999);
    
    return { start: monday, end: sunday };
};

// 获取本月起止日期
const getThisMonthRange = () => {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1);
    const end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59);
    return { start, end };
};

// 获取今年起止日期
const getThisYearRange = () => {
    const now = new Date();
    const start = new Date(now.getFullYear(), 0, 1);
    const end = new Date(now.getFullYear(), 11, 31, 23, 59, 59);
    return { start, end };
};

// 筛选任务
const filterTasks = () => {
    let filtered = allTasks.value.filter(task => task.completed === true);
    
    // 日期筛选
    const now = new Date();
    if (dateFilter.value === 'week') {
        const weekRange = getThisWeekRange();
        filtered = filtered.filter(task => {
            const endDate = new Date(task.end.replace(' ', 'T'));
            return endDate >= weekRange.start && endDate <= weekRange.end;
        });
    } else if (dateFilter.value === 'month') {
        const monthRange = getThisMonthRange();
        filtered = filtered.filter(task => {
            const endDate = new Date(task.end.replace(' ', 'T'));
            return endDate >= monthRange.start && endDate <= monthRange.end;
        });
    } else if (dateFilter.value === 'year') {
        const yearRange = getThisYearRange();
        filtered = filtered.filter(task => {
            const endDate = new Date(task.end.replace(' ', 'T'));
            return endDate >= yearRange.start && endDate <= yearRange.end;
        });
    }
    
    // 关键词搜索
    if (searchKeyword.value.trim()) {
        const keyword = searchKeyword.value.trim().toLowerCase();
        filtered = filtered.filter(task => 
            task.title.toLowerCase().includes(keyword) ||
            (task.description && task.description.toLowerCase().includes(keyword)) ||
            (task.type && task.type.toLowerCase().includes(keyword))
        );
    }
    
    // 按完成时间倒序排序（最新完成的在前）
    filtered.sort((a, b) => {
        const dateA = a.completedAt ? new Date(a.completedAt) : new Date(a.end);
        const dateB = b.completedAt ? new Date(b.completedAt) : new Date(b.end);
        return dateB - dateA;
    });
    
    return filtered;
};

const filteredCompletedTasks = computed(() => filterTasks());

// 本周任务统计
const thisWeekTasks = computed(() => {
    const weekRange = getThisWeekRange();
    return allTasks.value.filter(task => {
        const taskStart = new Date(task.start);
        return taskStart >= weekRange.start && taskStart <= weekRange.end;
    });
});

const thisWeekStats = computed(() => {
    const total = thisWeekTasks.value.length;
    const finished = thisWeekTasks.value.filter(t => t.completed === true).length;
    return { finished, unfinished: total - finished, total };
});

// 初始化图表
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
        series: [{
            name: '本周任务统计',
            type: 'pie',
            radius: '55%',
            center: ['50%', '50%'],
            data: [
                { value: finished, name: '已完成', itemStyle: { color: '#00bcd4' } },
                { value: unfinished, name: '未完成', itemStyle: { color: '#2c4c96' } }
            ],
            label: { show: true, formatter: '{b}: {d}%' }
        }]
    };
    
    if (total === 0) {
        option = {
            title: { text: '本周暂无任务', left: 'center', top: 'center', textStyle: { color: '#999' } },
            series: []
        };
    }
    
    chart.setOption(option);
    window.addEventListener('resize', () => chart.resize());
};

// 加载附件信息
const loadAttachments = async (taskId) => {
    if (attachmentsCache.value.has(taskId)) {
        return attachmentsCache.value.get(taskId);
    }
    
    try {
        const result = await getAttachments(taskId);
        if (result.code === 200) {
            attachmentsCache.value.set(taskId, result.data);
            return result.data;
        }
    } catch (err) {
        console.error('加载附件失败', err);
    }
    return [];
};

// 获取所有任务并补充附件信息
const fetchAllTasks = async () => {
    const token = localStorage.getItem('token');
    if (!token) {
        router.push('/login');
        return;
    }
    
    try {
        const result = await getTasks();
        if (result.code === 200) {
            const tasks = result.data;
            
            // 为每个任务加载附件
            for (const task of tasks) {
                task.attachments = await loadAttachments(task.id);
            }
            
            // 详细打印每个任务的完成信息
            tasks.forEach(task => {
                console.log(`任务: ${task.title}`);
                console.log(`  completed: ${task.completed}`);
                console.log(`  completedAt: ${task.completedAt}`);
                console.log(`  end: ${task.end}`);
                console.log('---');
            });
            
            allTasks.value = tasks;
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

<style>
/* 黑夜模式 - HistoryPage */
body.dark-mode .content-wrapper {
  background: #1a1a2e;
}

body.dark-mode .chart-container {
  background: #16213e;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

body.dark-mode .filter-bar {
  background: #0f0f1a;
}

body.dark-mode .filter-select,
body.dark-mode .search-input {
  background: #2c2c3e;
  border-color: #3a3a4e;
  color: #e0e0e0;
}

body.dark-mode .task-list h3 {
  color: #8ab3ff;
}

body.dark-mode .empty-tip {
  background: #16213e;
  color: #888888;
}

body.dark-mode .Card {
  background: #16213e;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

body.dark-mode .taskTitle {
  color: #e0e0e0;
}

body.dark-mode .taskDescription {
  color: #aaaaaa;
}

body.dark-mode .detail-label {
  color: #888888;
}

body.dark-mode .detail-value {
  color: #cccccc;
}

body.dark-mode .time-label {
  color: #888888;
}

body.dark-mode .time-value {
  color: #e0e0e0;
}

body.dark-mode .stat-label {
  color: #888888;
}

body.dark-mode .stat-value {
  color: #cccccc;
}

body.dark-mode .attachments-preview {
  background: #0f0f1a;
  border-color: #2c2c3e;
}

body.dark-mode .attachment-thumb {
  background: #1a1a2e;
}

body.dark-mode .thumb-name {
  color: #8ab3ff;
}
</style>

<style scoped>
.chart-box {
    height: 300px;
    width: 100%;
}

.chart-container {
    background: white;
    border-radius: 20px;
    padding: 20px;
    margin-bottom: 20px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.chart-note {
    text-align: center;
    color: #666;
    font-size: 14px;
    margin-top: 10px;
}

/* 筛选栏 */
.filter-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
    padding: 12px 16px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.filter-select {
    padding: 8px 16px;
    border: 1px solid #ddd;
    border-radius: 20px;
    font-size: 14px;
    background: white;
    cursor: pointer;
}

.search-input {
    flex: 1;
    padding: 8px 16px;
    border: 1px solid #ddd;
    border-radius: 20px;
    font-size: 14px;
    outline: none;
}

.search-input:focus {
    border-color: #5c83d8;
}

.content-wrapper {
    background: rgb(245, 247, 254);
    min-height: 100%;
    padding: 20px 5%;
    font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.task-list h3 {
    margin-bottom: 16px;
    color: #2c4c96;
    font-size: 20px;
    font-weight: 600;
}

.task-count {
    font-size: 14px;
    color: #888;
    font-weight: normal;
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
    border-radius: 16px;
    padding: 20px 24px;
    margin-top: 12px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    transition: all 0.2s;
}

.Card:hover {
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
}

.taskTitle {
    font-size: 18px;
    font-weight: 600;
    color: #333;
    margin: 0;
}

.priority-badge {
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;
}

.priority-low-badge {
    background: #e8f5e9;
    color: #4caf50;
}

.priority-medium-badge {
    background: #fff3e0;
    color: #ff9800;
}

.priority-high-badge {
    background: #ffebee;
    color: #f44336;
}

.taskDescription {
    color: #666;
    margin-bottom: 12px;
    font-size: 14px;
    line-height: 1.4;
}

.task-detail-row {
    margin-bottom: 12px;
    font-size: 13px;
}

.detail-label {
    color: #999;
}

.detail-value {
    color: #555;
}

.taskTime {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #f8f9fa;
    padding: 12px 16px;
    border-radius: 12px;
    margin: 12px 0;
}

.time-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
}

.time-label {
    font-size: 11px;
    color: #999;
    margin-bottom: 4px;
}

.time-value {
    font-size: 13px;
    font-family: monospace;
    color: #333;
    font-weight: 500;
}

.time-arrow {
    color: #5c83d8;
    font-size: 18px;
    padding: 0 12px;
}

.task-stats {
    display: flex;
    gap: 24px;
    padding-top: 12px;
    border-top: 1px solid #eee;
}

.stat-item {
    display: flex;
    gap: 8px;
    align-items: baseline;
}

.stat-label {
    font-size: 12px;
    color: #999;
}

.stat-value {
    font-size: 14px;
    font-weight: 500;
    color: #5c83d8;
}

/* 附件预览 */
.attachments-preview {
    margin-top: 12px;
    padding: 10px 12px;
    background: #f5f5f5;
    border-radius: 10px;
}

.attachment-label {
    font-size: 12px;
    color: #666;
    display: block;
    margin-bottom: 8px;
}

.attachment-thumbnails {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    align-items: center;
}

.attachment-thumb {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    background: white;
    border-radius: 16px;
    font-size: 12px;
}

.thumb-icon {
    font-size: 12px;
}

.thumb-name {
    color: #2c4c96;
    max-width: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.more-attach {
    font-size: 12px;
    color: #999;
}
</style>