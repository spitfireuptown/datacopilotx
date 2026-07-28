<template>
  <div class="dashboard-page">
    <LeftSidebar />

    <!-- 左侧仪表盘列表 -->
    <div class="dashboard-sidebar">
      <div class="sidebar-header">
        <span class="sidebar-title">仪表盘</span>
        <a-tooltip title="新建仪表盘">
          <a-button type="text" size="small" @click="createNewDashboard">
            <template #icon><PlusOutlined /></template>
          </a-button>
        </a-tooltip>
      </div>

      <div v-if="dashboardLoading" class="sidebar-loading">
        <a-spin size="small" />
      </div>

      <div v-else class="dashboard-list">
        <div
          v-for="dash in dashboardList"
          :key="dash.id"
          class="dashboard-item"
          :class="{ active: dash.id === activeDashboardId }"
          @click="switchDashboard(dash.id!)"
        >
          <span class="item-icon">📊</span>
          <div class="item-content">
            <template v-if="editingId === dash.id">
              <a-input
                ref="renameInputRef"
                v-model:value="editingName"
                size="small"
                @blur="confirmRename(dash.id!)"
                @keydown.enter="confirmRename(dash.id!)"
                @keydown.esc="cancelRename"
              />
            </template>
            <template v-else>
              <span class="item-name">{{ dash.name }}</span>
              <span class="item-meta">{{ dash.ctime?.substring(0, 10) }}</span>
            </template>
          </div>

          <a-dropdown trigger="click" :get-popup-container="(node:any) => node.parentNode">
            <a-button type="text" size="small" class="item-more" @click.stop>
              <MoreOutlined />
            </a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item key="rename" @click="startRename(dash.id!, dash.name)">
                  <EditOutlined /> 重命名
                </a-menu-item>
                <a-menu-item key="delete" danger @click="handleDeleteDashboard(dash.id!)">
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>

        <div v-if="dashboardList.length === 0" class="sidebar-empty">
          <span class="empty-hint">暂无仪表盘，点击 + 新建</span>
        </div>
      </div>
    </div>

    <!-- 右侧画布 -->
    <div class="dashboard-main">
      <!-- 空选择状态 -->
      <div v-if="!activeDashboardId" class="no-selection">
        <div class="no-selection-icon">📋</div>
        <div class="no-selection-text">选择或创建一个仪表盘</div>
        <a-button type="primary" @click="createNewDashboard">
          <template #icon><PlusOutlined /></template>
          新建仪表盘
        </a-button>
      </div>

      <template v-else>
        <!-- 顶部工具栏 -->
        <div class="dashboard-header">
          <div class="header-left">
            <h2 class="page-title">{{ currentDashboard?.name }}</h2>
            <span class="header-tip">拖拽标题栏移动图表，拖拽右下角调整大小</span>
          </div>
          <a-button type="primary" @click="openAddModal">
            <template #icon><PlusOutlined /></template>
            添加图表
          </a-button>
        </div>

        <!-- 图表网格区域 -->
        <div ref="dashboardContainer" class="dashboard-grid">
          <DashboardChart
            v-for="chart in chartList"
            :key="chart.id"
            :chart-data="chart"
            :container-width="containerWidth"
            :container-height="containerHeight"
            @delete="handleDeleteChart"
            @layout-change="handleLayoutChange"
          />

          <div v-if="!chartLoading && chartList.length === 0" class="empty-state">
            <div class="empty-icon">📊</div>
            <div class="empty-text">暂无图表</div>
            <div class="empty-subtext">点击「添加图表」从对话记录中选择，或在问数对话中点击「添加到仪表盘」</div>
          </div>

          <div v-if="chartLoading" class="loading-state">
            <a-spin size="large" tip="加载中..." />
          </div>
        </div>
      </template>
    </div>

    <!-- 添加图表弹窗 -->
    <a-modal
      v-model:open="addModalVisible"
      title="从对话记录中添加图表"
      width="700px"
      :footer="null"
      @cancel="addModalVisible = false"
    >
      <div v-if="questionLoading" class="modal-loading">
        <a-spin tip="加载对话记录..." />
      </div>
      <div v-else-if="questionsWithChart.length === 0" class="modal-empty">
        <a-empty description="暂无含图表数据的对话记录" />
      </div>
      <div v-else class="question-list">
        <div
          v-for="item in questionsWithChart"
          :key="item.id"
          class="question-item"
          @click="selectQuestion(item)"
        >
          <div class="question-text">{{ item.question }}</div>
          <div class="question-time">{{ item.ctime }}</div>
        </div>
      </div>
    </a-modal>

    <!-- 图表预览弹窗 -->
    <a-modal
      v-model:open="previewVisible"
      title="选择图表类型添加"
      width="800px"
      :confirm-loading="addLoading"
      @ok="confirmAddChart"
      @cancel="previewVisible = false"
    >
      <div v-if="selectedQuestion" class="preview-content">
        <div class="preview-question">
          <strong>问数：</strong>{{ selectedQuestion.question }}
        </div>
        <div class="preview-chart-type">
          <span class="type-label">图表类型：</span>
          <a-radio-group v-model:value="selectedChartType">
            <a-radio-button value="Table">表格</a-radio-button>
            <a-radio-button value="BarChart">柱状图</a-radio-button>
            <a-radio-button value="LineChart">折线图</a-radio-button>
            <a-radio-button value="PieChart">饼图</a-radio-button>
          </a-radio-group>
        </div>
        <div class="preview-table">
          <a-table
            v-if="previewColumns.length > 0"
            :columns="previewColumns"
            :data-source="previewData"
            :pagination="false"
            size="small"
            :scroll="{ y: 200 }"
          />
          <a-empty v-else description="无法解析图表数据" />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, nextTick, computed } from 'vue';
import { message } from 'ant-design-vue';
import { PlusOutlined, MoreOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue';
import DashboardChart from '@/components/DashboardChart.vue';
import LeftSidebar from '@/components/LeftSidebar.vue';
import {
  getDashboardList,
  createDashboard,
  renameDashboard,
  deleteDashboard,
  getDashboardChartList,
  updateDashboardLayout,
  saveDashboardChart,
  deleteDashboardChart,
  getQuestionsWithChart,
  type DashboardItem,
  type DashboardChartItem,
  type QuestionWithChartItem,
} from '@/api/dashboard';

// ==================== 仪表盘列表 ====================
const dashboardList = ref<DashboardItem[]>([]);
const dashboardLoading = ref(false);
const activeDashboardId = ref<number | null>(null);
const editingId = ref<number | null>(null);
const editingName = ref('');
const renameInputRef = ref<any>(null);

const currentDashboard = computed(() => {
  return dashboardList.value.find(d => d.id === activeDashboardId.value) || null;
});

// Auto-select first dashboard on load
const loadDashboards = async (autoSelect?: boolean) => {
  dashboardLoading.value = true;
  try {
    dashboardList.value = await getDashboardList();
    if (autoSelect !== false && dashboardList.value.length > 0 && !activeDashboardId.value) {
      const firstId = dashboardList.value[0].id!;
      activeDashboardId.value = firstId;
      await loadCharts(firstId);
    }
  } catch {
    message.error('加载仪表盘列表失败');
  } finally {
    dashboardLoading.value = false;
  }
};

const createNewDashboard = async () => {
  try {
    const dash = await createDashboard('未命名仪表盘');
    await loadDashboards(false);
    activeDashboardId.value = dash.id!;
    chartList.value = [];
    // Prompt rename
    nextTick(() => startRename(dash.id!, '未命名仪表盘'));
  } catch {
    message.error('创建失败');
  }
};

const switchDashboard = async (id: number) => {
  if (id === activeDashboardId.value) {return;}
  activeDashboardId.value = id;
  await loadCharts(id);
};

const startRename = (id: number, name: string) => {
  editingId.value = id;
  editingName.value = name;
  nextTick(() => {
    renameInputRef.value?.focus?.();
    renameInputRef.value?.select?.();
  });
};

const confirmRename = async (id: number) => {
  if (!editingName.value.trim()) {
    editingName.value = currentDashboard.value?.name || '未命名仪表盘';
    editingId.value = null;
    return;
  }
  try {
    await renameDashboard(id, editingName.value.trim());
    const dash = dashboardList.value.find(d => d.id === id);
    if (dash) {dash.name = editingName.value.trim();}
    editingId.value = null;
  } catch {
    message.error('重命名失败');
  }
};

const cancelRename = () => {
  editingId.value = null;
  editingName.value = '';
};

const handleDeleteDashboard = async (id: number) => {
  if (dashboardList.value.length <= 1) {
    message.warning('至少保留一个仪表盘');
    return;
  }
  try {
    await deleteDashboard(id);
    dashboardList.value = dashboardList.value.filter(d => d.id !== id);
    if (activeDashboardId.value === id) {
      const first = dashboardList.value[0];
      activeDashboardId.value = first?.id ?? null;
      if (first) {await loadCharts(first.id!);} else {chartList.value = [];}
    }
  } catch {
    message.error('删除失败');
  }
};

// ==================== 图表网格 ====================
const chartList = ref<DashboardChartItem[]>([]);
const chartLoading = ref(false);
const dashboardContainer = ref<HTMLDivElement>();
const containerWidth = ref(1200);
const containerHeight = ref(800);

const layoutTimers = new Map<number, ReturnType<typeof setTimeout>>();

const loadCharts = async (dashboardId: number) => {
  chartLoading.value = true;
  try {
    chartList.value = await getDashboardChartList(dashboardId);
    chartList.value.forEach((chart, index) => {
      if (chart.layoutX == null || (chart.layoutX === 0 && chart.layoutY === 0)) {
        const col = index % 3;
        const row = Math.floor(index / 3);
        chart.layoutX = col * 420;
        chart.layoutY = row * 320;
      }
      if (!chart.layoutW) {chart.layoutW = 400;}
      if (!chart.layoutH) {chart.layoutH = 300;}
    });
  } catch {
    message.error('加载图表失败');
  } finally {
    chartLoading.value = false;
  }
};

const handleDeleteChart = async (id: number) => {
  try {
    await deleteDashboardChart(id);
    chartList.value = chartList.value.filter(c => c.id !== id);
    message.success('已删除');
  } catch {
    message.error('删除失败');
  }
};

const handleLayoutChange = (id: number, x: number, y: number, w: number, h: number) => {
  const chart = chartList.value.find(c => c.id === id);
  if (chart) {
    chart.layoutX = x;
    chart.layoutY = y;
    chart.layoutW = w;
    chart.layoutH = h;
    if (layoutTimers.has(id)) {clearTimeout(layoutTimers.get(id));}
    layoutTimers.set(id, setTimeout(() => {
      updateDashboardLayout({ id, layoutX: x, layoutY: y, layoutW: w, layoutH: h }).catch(() => {});
    }, 500));
  }
};

const updateContainerSize = () => {
  if (dashboardContainer.value) {
    containerWidth.value = dashboardContainer.value.clientWidth;
    containerHeight.value = Math.max(800, window.innerHeight - 160);
  }
};

let resizeObserver: ResizeObserver | null = null;

onMounted(() => {
  loadDashboards();
  nextTick(() => {
    updateContainerSize();
    if (dashboardContainer.value) {
      resizeObserver = new ResizeObserver(() => updateContainerSize());
      resizeObserver.observe(dashboardContainer.value);
    }
  });
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  layoutTimers.forEach(t => clearTimeout(t));
});

// ==================== 添加图表弹窗 ====================
const addModalVisible = ref(false);
const previewVisible = ref(false);
const questionLoading = ref(false);
const addLoading = ref(false);
const questionsWithChart = ref<QuestionWithChartItem[]>([]);
const selectedQuestion = ref<QuestionWithChartItem | null>(null);
const selectedChartType = ref('Table');

const openAddModal = async () => {
  if (!activeDashboardId.value) {
    message.warning('请先选择一个仪表盘');
    return;
  }
  addModalVisible.value = true;
  questionLoading.value = true;
  try {
    questionsWithChart.value = await getQuestionsWithChart();
  } catch {
    message.error('加载对话记录失败');
  } finally {
    questionLoading.value = false;
  }
};

const selectQuestion = (item: QuestionWithChartItem) => {
  selectedQuestion.value = item;
  selectedChartType.value = 'Table';
  previewVisible.value = true;
};

const parsedResult = computed(() => {
  if (!selectedQuestion.value?.result) {return null;}
  try {
    return JSON.parse(selectedQuestion.value.result);
  } catch {
    return null;
  }
});

const previewColumns = computed(() => {
  if (!parsedResult.value) {return [];}
  if (parsedResult.value.columns && Array.isArray(parsedResult.value.columns)) {
    return parsedResult.value.columns.map((col: any) => ({
      title: col.name,
      dataIndex: col.name,
      key: col.name,
    }));
  }
  if (Array.isArray(parsedResult.value) && parsedResult.value.length > 0) {
    return Object.keys(parsedResult.value[0]).map(k => ({ title: k, dataIndex: k, key: k }));
  }
  return [];
});

const previewData = computed(() => {
  if (!parsedResult.value) {return [];}
  if (parsedResult.value.data) {return parsedResult.value.data;}
  if (Array.isArray(parsedResult.value)) {return parsedResult.value;}
  return [];
});

const confirmAddChart = async () => {
  if (!selectedQuestion.value || !activeDashboardId.value) {return;}
  addLoading.value = true;
  try {
    await saveDashboardChart({
      dashboardId: activeDashboardId.value,
      chartName: (selectedQuestion.value.question || '未命名').substring(0, 50),
      chartType: selectedChartType.value,
      chartData: selectedQuestion.value.result,
      sqlText: selectedQuestion.value.sql || '',
      question: selectedQuestion.value.question || '',
      questionId: selectedQuestion.value.questionId || '',
      sessionId: selectedQuestion.value.sessionId || '',
      layoutX: 0,
      layoutY: 0,
      layoutW: 500,
      layoutH: 350,
    });
    message.success('已添加到仪表盘');
    previewVisible.value = false;
    await loadCharts(activeDashboardId.value);
  } catch {
    message.error('添加失败');
  } finally {
    addLoading.value = false;
  }
};
</script>

<style lang="scss" scoped>
.dashboard-page {
  width: 100%;
  height: 100vh;
  display: flex;
  background: #f5f7fa;
  box-sizing: border-box;
  overflow: hidden;
}

// ==================== 左侧仪表盘列表面板 ====================
.dashboard-sidebar {
  width: 220px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;

  .sidebar-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 12px 12px 16px;
    border-bottom: 1px solid #f0f0f0;

    .sidebar-title {
      font-size: 14px;
      font-weight: 600;
      color: #1a1a1a;
    }
  }

  .sidebar-loading {
    display: flex;
    justify-content: center;
    padding: 24px 0;
  }

  .dashboard-list {
    flex: 1;
    overflow-y: auto;
    padding: 8px 0;
  }

  .dashboard-item {
    display: flex;
    align-items: center;
    padding: 10px 12px 10px 16px;
    cursor: pointer;
    transition: background 0.15s;
    gap: 8px;

    &:hover {
      background: #f5f7fa;
    }

    &.active {
      background: #e6f4ff;
    }

    .item-icon {
      font-size: 16px;
      flex-shrink: 0;
    }

    .item-content {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .item-name {
      font-size: 13px;
      color: #333;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-weight: 500;
    }

    .item-meta {
      font-size: 11px;
      color: #999;
    }

    .item-more {
      opacity: 0;
      flex-shrink: 0;
    }

    &:hover .item-more {
      opacity: 1;
    }
  }

  .sidebar-empty {
    padding: 24px 16px;
    text-align: center;

    .empty-hint {
      font-size: 12px;
      color: #999;
    }
  }
}

// ==================== 画布主区域 ====================
.dashboard-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.no-selection {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: #fff;

  .no-selection-icon {
    font-size: 56px;
  }

  .no-selection-text {
    font-size: 15px;
    color: #999;
  }
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: baseline;
    gap: 16px;
  }

  .page-title {
    font-size: 18px;
    font-weight: 600;
    margin: 0;
    color: #1a1a1a;
  }

  .header-tip {
    font-size: 12px;
    color: #999;
  }
}

.dashboard-grid {
  flex: 1;
  position: relative;
  overflow: auto;
  padding: 16px;
  min-height: 0;
}

.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 12px;
  }

  .empty-text {
    font-size: 16px;
    color: #666;
    margin-bottom: 4px;
  }

  .empty-subtext {
    font-size: 13px;
    color: #999;
  }
}

.loading-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

// ==================== 弹窗样式 ====================
.modal-loading {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.modal-empty {
  padding: 20px 0;
}

.question-list {
  max-height: 400px;
  overflow-y: auto;

  .question-item {
    padding: 12px 16px;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: #f0f7ff;
    }

    & + .question-item {
      border-top: 1px solid #f0f0f0;
    }

    .question-text {
      font-size: 14px;
      color: #333;
      margin-bottom: 4px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .question-time {
      font-size: 12px;
      color: #999;
    }
  }
}

.preview-content {
  .preview-question {
    margin-bottom: 16px;
    font-size: 14px;
    color: #333;
    padding: 8px 12px;
    background: #fafafa;
    border-radius: 6px;
  }

  .preview-chart-type {
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 8px;

    .type-label {
      font-size: 14px;
      font-weight: 500;
    }
  }

  .preview-table {
    margin-bottom: 8px;
  }
}
</style>
