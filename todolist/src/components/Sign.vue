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
import { getSignStatus, signIn } from '@/api/sign';
import { useRouter } from 'vue-router';

const router = useRouter();
const totalDays = ref(0);
const isSignedToday = ref(false);
const isLoading = ref(false);

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

// 获取签到状态
const fetchSignStatus = async () => {
  // 检查是否已登录
  const token = localStorage.getItem('token');
  if (!token) {
    router.push('/login');
    return;
  }
  
  try {
    const result = await getSignStatus();
    if (result.code === 200) {
      totalDays.value = result.data.totalDays;
      isSignedToday.value = result.data.signedToday;
    }
  } catch (err) {
    console.error('获取签到状态失败', err);
  }
};

// 执行签到
const handleSign = async () => {
  if (isSignedToday.value) return;
  
  isLoading.value = true;
  
  try {
    const result = await signIn();
    if (result.code === 200) {
      totalDays.value = result.data.totalDays;
      isSignedToday.value = true;
      showMessage('签到成功！');
    } else {
      showMessage(result.message || '签到失败', true);
    }
  } catch (err) {
    console.error('签到失败', err);
    showMessage('签到失败，请稍后重试', true);
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  fetchSignStatus();
});
</script>

<style>
/* 黑夜模式 - Sign */
body.dark-mode .sign-wrapper {
  background: #1a1a2e;
}

body.dark-mode .sign-card {
  background: #16213e;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

body.dark-mode .days-count .number {
  color: #8ab3ff;
}

body.dark-mode .days-count .label {
  color: #aaaaaa;
}

body.dark-mode .status-badge {
  background: #2a2a3e;
  color: #aaaaaa;
}

body.dark-mode .status-badge.signed {
  background: #0a2e1a;
  color: #6fbf6f;
}

body.dark-mode .sign-btn {
  background: #4a6fb8;
}

body.dark-mode .sign-btn:hover:not(:disabled) {
  background: #5c83d8;
}

body.dark-mode .sign-btn:disabled {
  background: #2a2a3e;
  color: #888888;
}
</style>

<style scoped>
/* 样式保持不变 */
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

@keyframes fadeOut {
  0% { opacity: 1; }
  70% { opacity: 1; }
  100% { opacity: 0; visibility: hidden; }
}
</style>