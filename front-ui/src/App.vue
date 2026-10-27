<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { theme as antdTheme } from 'ant-design-vue';
import { useAuthStore } from '@/stores/modules/auth';
import ReportTaskGlobal from '@/components/ReportTaskGlobal.vue';

const authStore = useAuthStore();

// antd 全局主题：统一品牌色与圆角
const themeConfig = {
  algorithm: antdTheme.defaultAlgorithm,
  token: {
    colorPrimary: '#6366f1',
    colorInfo: '#6366f1',
    borderRadius: 10,
    fontFamily:
      "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif"
  }
};

onMounted(() => {
  window.addEventListener('authExpired', handleAuthExpired);
});

const handleAuthExpired = () => {
  authStore.clearAuth();
};

onUnmounted(() => {
  window.removeEventListener('authExpired', handleAuthExpired);
});
</script>

<template>
  <a-config-provider :theme="themeConfig">
    <router-view></router-view>
    <!-- 数据报告后台任务浮窗与抽屉：全局挂载，切换页面不丢失 -->
    <ReportTaskGlobal />
  </a-config-provider>
</template>
