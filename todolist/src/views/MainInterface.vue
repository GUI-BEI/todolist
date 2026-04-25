<template>
  <div class="main-interface">
    <DayPilotScheduler 
      :config="schedulerConfig" 
      ref="schedulerRef" 
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { DayPilot, DayPilotScheduler } from "@daypilot/daypilot-lite-vue";

const getMondayOfCurrentWeek = () => {
  const today = new Date();
  // 获取本地时间的年、月、日
  const year = today.getFullYear();
  const month = today.getMonth();
  const date = today.getDate();
  
  // 创建本地日期对象（不涉及UTC转换）
  const localToday = new Date(year, month, date);
  const day = localToday.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  
  const monday = new Date(year, month, date + diff);
  monday.setHours(0, 0, 0, 0);
  return monday;
};

const schedulerRef = ref(null);

const schedulerConfig = ref({
  // 核心：使用 Days 视图，每一列代表一天
  viewType: "Days",
  days: 7, // 显示7天
  scale: "Day",

  cellWidth: 150,
  
  // 设置每一行的行高，方便横向排列
  rowHeight: 60,
  headerHeight: 30,
  
  // 启用事件拖拽或调整大小（可选）
  onEventClick: (args) => {
    console.log("点击任务: ", args.e.data());
  }
});

const fetchTasks = async () => {
  try {
    const res = await fetch('http://localhost:8080/api/getTasks');
    const result = await res.json();
    if (result.code === 200) {
      // 映射数据到 Scheduler
      // 注意：Scheduler 需要 resource 字段，如果不需要分组，可以统一设为默认资源
      const events = result.data.map(task => ({
        id: task.id,
        resource: "A", // Scheduler 必须有一个资源 ID
        text: task.title,
        start: task.start,
        end: task.end,
        backColor: "#5c83d8" 
      }));

      // 更新资源和事件
      schedulerRef.value.control.update({
        resources: [{ name: "我的任务", id: "A" }],
        events: events
      });
    }
  } catch (err) {
    console.error('获取任务失败', err);
  }
};

onMounted(() => {
  const monday = getMondayOfCurrentWeek();
  schedulerConfig.value.startDate = new DayPilot.Date(monday, true);
  if (schedulerRef.value) {
    schedulerRef.value.control.update();
  }

  fetchTasks();
});
</script>

<style scoped>
.main-interface {
  width: 100%;
  overflow-x: auto;
}
</style>