<template>
  <div class="share-dashboard-page">
    <div v-if="loading" class="share-loading">
      <a-spin size="large" tip="加载中..." />
    </div>

    <div v-else-if="errorMsg" class="share-error">
      <a-result status="403" :title="errorMsg" sub-title="请联系分享者重新生成链接" />
    </div>

    <template v-else-if="dashboard">
      <div class="share-header">
        <span class="share-title">{{ dashboard.name }}</span>
        <span class="share-badge">只读预览</span>
      </div>
      <div class="share-canvas">
        <div class="share-canvas-inner" :style="canvasStyle">
          <DashboardChart
            v-for="chart in dashboard.charts"
            :key="chart.id"
            :chart-data="chart"
            :container-width="0"
            :container-height="0"
            readonly
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import DashboardChart from '@/components/DashboardChart.vue';
import { getSharedDashboard, type SharedDashboard } from '@/api/dashboard';

const route = useRoute();
const loading = ref(true);
const errorMsg = ref('');
const dashboard = ref<SharedDashboard | null>(null);

// 画布尺寸按图表布局边界计算，超出视口时可滚动
const canvasStyle = computed(() => {
  let maxX = 0;
  let maxY = 0;
  dashboard.value?.charts.forEach(c => {
    maxX = Math.max(maxX, (c.layoutX || 0) + (c.layoutW || 0));
    maxY = Math.max(maxY, (c.layoutY || 0) + (c.layoutH || 0));
  });
  return {
    width: `${maxX + 32}px`,
    height: `${maxY + 32}px`,
  };
});

onMounted(async () => {
  const token = route.params.token as string;
  try {
    dashboard.value = await getSharedDashboard(token);
    document.title = dashboard.value?.name || '仪表盘分享';
  } catch (e: any) {
    errorMsg.value = e?.message || '分享链接无效或已过期';
  } finally {
    loading.value = false;
  }
});
</script>

<style lang="scss" scoped>
.share-dashboard-page {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  overflow: hidden;
}

.share-loading,
.share-error {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.share-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;

  .share-title {
    font-size: 16px;
    font-weight: 600;
    color: #1a1a1a;
  }

  .share-badge {
    font-size: 12px;
    color: #1890ff;
    background: #e6f4ff;
    padding: 2px 8px;
    border-radius: 4px;
  }
}

.share-canvas {
  flex: 1;
  overflow: auto;
  padding: 16px;
}

.share-canvas-inner {
  position: relative;
}
</style>
