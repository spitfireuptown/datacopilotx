<template>
  <template v-if="!isPublicRoute">
    <!-- 数据报告后台任务浮窗：生成中显示进度（不阻塞页面），完成后作为查看入口 -->
    <div v-if="taskStore.generating" class="report-task-float">
      <a-spin size="small" />
      <div class="report-task-info">
        <div class="report-task-title">数据报告生成中</div>
        <div class="report-task-progress" :title="taskStore.progress">{{ taskStore.progress }}</div>
      </div>
      <a-button size="small" type="text" danger @click="taskStore.cancelTask()">取消</a-button>
    </div>
    <div
      v-else-if="taskStore.report"
      class="report-task-float report-task-entry"
      title="查看已生成的数据报告"
      @click="taskStore.openDrawer()"
    >
      <FileTextOutlined class="report-task-icon" />
      <span>查看数据报告</span>
    </div>

    <!-- 数据报告抽屉：仅用户主动查看时打开 -->
    <DataReportDrawer
      :open="taskStore.drawerOpen"
      :report="taskStore.report"
      :loading="taskStore.generating"
      :progress="taskStore.progress"
      :error="taskStore.error"
      @close="taskStore.closeDrawer()"
    />
  </template>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { FileTextOutlined } from '@ant-design/icons-vue';
import DataReportDrawer from '@/components/DataReportDrawer.vue';
import { useReportTaskStore } from '@/stores/modules/reportTask';

const taskStore = useReportTaskStore();
const route = useRoute();

// 登录页、分享页等公开页面不展示任务浮窗
const isPublicRoute = computed(() => route.meta.requiresAuth === false);
</script>

<style lang="scss" scoped>
/* 数据报告后台任务浮窗：玻璃卡片，不遮挡主体内容，全局固定于右下角 */
.report-task-float {
  position: fixed;
  right: 24px;
  bottom: 140px;
  z-index: 1000;
  display: flex;
  gap: 10px;
  align-items: center;
  max-width: 380px;
  padding: 10px 14px;
  background: var(--bg-glass);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  box-shadow: var(--shadow-lg);

  .report-task-info {
    flex: 1;
    min-width: 0;

    .report-task-title {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-1);
    }

    .report-task-progress {
      margin-top: 2px;
      overflow: hidden;
      font-size: 12px;
      color: var(--text-3);
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.report-task-entry {
  font-size: 13px;
  font-weight: 600;
  color: var(--brand-primary);
  cursor: pointer;

  .report-task-icon {
    font-size: 16px;
  }

  &:hover {
    border-color: var(--brand-primary);
    box-shadow: var(--brand-glow);
  }
}
</style>
