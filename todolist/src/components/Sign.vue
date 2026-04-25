<template>
  <div class="sign-wrapper">
    <div class="sign-card">
      <div class="stats-section">
        <div class="days-count">
          <span class="number">{{ totalDays }}</span>
          <span class="label">累计签到天数</span>
        </div>
        <div class="today-status">
          <span class="status-badge" :class="{ signed: isSignedToday }">
            {{ isSignedToday ? '今日已签到' : '今日未签到' }}
          </span>
        </div>
      </div>

      <button 
        class="sign-btn" 
        :disabled="isSignedToday || isLoading"
        @click="handleSign"
      >
        {{ isLoading ? '签到中...' : (isSignedToday ? '今日已签到' : '签到') }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';

// 状态
const totalDays = ref(0);      // 累计签到天数
const isSignedToday = ref(false); // 今日是否已签到
const isLoading = ref(false);   // 加载状态

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
if (!document.querySelector('#sign-animation')) {
  style.id = 'sign-animation';
  style.textContent = `
    @keyframes fadeOut {
      0% { opacity: 1; }
      70% { opacity: 1; }
      100% { opacity: 0; visibility: hidden; }
    }
  `;
  document.head.appendChild(style);
}

// 获取签到状态
const fetchSignStatus = async () => {
  try {
    const res = await fetch('http://localhost:8080/api/sign/status', {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' }
    });
    const result = await res.json();
    
    if (result.code === 200) {
      totalDays.value = result.data.totalDays;
      isSignedToday.value = result.data.signedToday;
    } else {
      throw new Error(result.message || '获取签到状态失败');
    }
  } catch (err) {
    console.error('获取签到状态失败', err);
    // 测试数据
    totalDays.value = 0;
    isSignedToday.value = false;
  }
};

// 执行签到
const handleSign = async () => {
  if (isSignedToday.value) return;
  
  isLoading.value = true;
  
  try {
    const res = await fetch('http://localhost:8080/api/sign', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    });
    const result = await res.json();
    
    if (result.code === 200) {
      totalDays.value = result.data.totalDays;
      isSignedToday.value = true;
      showMessage('签到成功！');
    } else {
      throw new Error(result.message || '签到失败');
    }
  } catch (err) {
    console.error('签到失败', err);
    showMessage('签到失败，请稍后重试', true);
    // 测试模式：模拟签到成功
    if (err.message === 'Failed to fetch') {
      totalDays.value += 1;
      isSignedToday.value = true;
      showMessage('签到成功！(测试模式)');
    }
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchSignStatus();
});
</script>

<style scoped>
.sign-wrapper {
  background: rgb(255, 255, 255);
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.sign-card {
  background: white;
  border-radius: 24px;
  padding: 40px 32px;
  text-align: center;
  box-shadow: 0 10px 30px rgba(12, 39, 239, 0.1);
  min-width: 300px;
  transition: all 0.3s ease;
}

.stats-section {
  margin-bottom: 40px;
}

.days-count {
  margin-bottom: 30px;
}

.days-count .number {
  display: block;
  font-size: 72px;
  font-weight: 700;
  color: #5c83d8;
  line-height: 1.2;
}

.days-count .label {
  display: block;
  font-size: 16px;
  color: #666;
  margin-top: 8px;
}

.today-status {
  margin: 20px 0;
}

.status-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 30px;
  font-size: 14px;
  font-weight: 500;
  background-color: #f0f0f0;
  color: #888;
}

.status-badge.signed {
  background-color: #e8f5e9;
  color: #4caf50;
}

.sign-btn {
  width: 100%;
  padding: 14px 24px;
  background-color: #5c83d8;
  color: white;
  border: none;
  border-radius: 40px;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.sign-btn:hover:not(:disabled) {
  background-color: #456f9d;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(92, 131, 216, 0.3);
}

.sign-btn:active:not(:disabled) {
  transform: translateY(0);
}

.sign-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}
</style>