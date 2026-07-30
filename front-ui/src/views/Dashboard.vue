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
            <span class="header-tip">拖拽标题栏移动图表，拖拽右下角调整大小，双击标题重命名</span>
          </div>
          <div class="header-actions">
            <a-button @click="openBgColorPanel">
              <template #icon><BgColorsOutlined /></template>
              背景色
            </a-button>
            <a-button @click="openShareModal">
              <template #icon><ShareAltOutlined /></template>
              分享
            </a-button>
            <a-button @click="openFullscreenPreview">
              <template #icon><FullscreenOutlined /></template>
              预览
            </a-button>
            <a-dropdown trigger="click">
              <a-button type="primary">
                <template #icon><PlusOutlined /></template>
                添加
              </a-button>
              <template #overlay>
                <a-menu @click="handleAddItem">
                  <a-menu-item key="chart">
                    <BarChartOutlined /> 添加图表
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="carousel">
                    <PictureOutlined /> 轮播组件
                  </a-menu-item>
                  <a-menu-item key="text">
                    <FontSizeOutlined /> 文本标注
                  </a-menu-item>
                  <a-menu-item key="timer">
                    <FieldTimeOutlined /> 计时器
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="marquee">
                    <SoundOutlined /> 滚动字幕
                  </a-menu-item>
                  <a-menu-item key="numberFlip">
                    <NumberOutlined /> 数字翻牌器
                  </a-menu-item>
                  <a-menu-item key="progressRing">
                    <PieChartOutlined /> 进度环
                  </a-menu-item>
                  <a-menu-item key="pulse">
                    <AimOutlined /> 脉冲指示器
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>

        <!-- 图表网格区域 -->
        <div ref="dashboardContainer" class="dashboard-grid" :style="gridBgStyle">
          <DashboardChart
            v-for="chart in chartList"
            :key="'c_' + chart.id"
            :chart-data="chart"
            :container-width="containerWidth"
            :container-height="containerHeight"
            :card-color="getCardColor(chart.id!)"
            :content-only="getContentOnly(chart.id!)"
            @delete="handleDeleteChart"
            @layout-change="handleLayoutChange"
            @rename="handleRenameChart"
            @color-change="handleCardColorChange"
            @content-only-change="handleContentOnlyChange"
          />

          <WidgetWrapper
            v-for="widget in widgetList"
            :key="'w_' + widget.id"
            :widget="widget"
            :container-width="containerWidth"
            :container-height="containerHeight"
            :content-only="getWidgetContentOnly(widget.id)"
            @delete="handleDeleteWidget"
            @layout-change="handleWidgetLayoutChange"
            @rename="handleRenameWidget"
            @color-change="handleWidgetColorChange"
            @content-only-change="handleWidgetContentOnlyChange"
          >
            <WidgetCarousel
              v-if="widget.type === 'carousel'"
              :config="widget.config as any"
              @config-change="(c:any) => handleWidgetConfigChange(widget.id, c)"
            />
            <WidgetText
              v-else-if="widget.type === 'text'"
              :config="widget.config as any"
              @config-change="(c:any) => handleWidgetConfigChange(widget.id, c)"
            />
            <WidgetTimer
              v-else-if="widget.type === 'timer'"
              :config="widget.config as any"
              @config-change="(c:any) => handleWidgetConfigChange(widget.id, c)"
            />
            <WidgetMarquee
              v-else-if="widget.type === 'marquee'"
              :config="widget.config as any"
              @config-change="(c:any) => handleWidgetConfigChange(widget.id, c)"
            />
            <WidgetNumberFlip
              v-else-if="widget.type === 'numberFlip'"
              :config="widget.config as any"
              @config-change="(c:any) => handleWidgetConfigChange(widget.id, c)"
            />
            <WidgetProgressRing
              v-else-if="widget.type === 'progressRing'"
              :config="widget.config as any"
              @config-change="(c:any) => handleWidgetConfigChange(widget.id, c)"
            />
            <WidgetPulse
              v-else-if="widget.type === 'pulse'"
              :config="widget.config as any"
              @config-change="(c:any) => handleWidgetConfigChange(widget.id, c)"
            />
          </WidgetWrapper>

          <div v-if="!chartLoading && !widgetLoading && chartList.length === 0 && widgetList.length === 0" class="empty-state">
            <div class="empty-text">暂无内容</div>
            <div class="empty-subtext">点击右上角「添加」按钮，添加图表或功能组件</div>
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

    <!-- 免密分享弹窗 -->
    <a-modal
      v-model:open="shareModalVisible"
      title="免密分享仪表盘"
      width="560px"
      :footer="null"
      @cancel="shareModalVisible = false"
    >
      <div v-if="shareLoading" class="modal-loading">
        <a-spin />
      </div>
      <div v-else class="share-content">
        <template v-if="activeShare">
          <a-alert type="info" show-icon class="share-alert">
            <template #message>
              获得链接的任何人无需登录即可查看该仪表盘（只读），有效期至 {{ activeShare.expireTime?.substring(0, 16) }}
            </template>
          </a-alert>
          <div class="share-link-row">
            <a-input :value="shareUrl" readonly />
            <a-button type="primary" @click="copyShareUrl">复制链接</a-button>
          </div>
          <div class="share-actions">
            <a-button danger @click="handleRevokeShare">撤销链接</a-button>
          </div>
        </template>
        <template v-else>
          <div class="share-expire-row">
            <span class="expire-label">链接有效期：</span>
            <a-radio-group v-model:value="shareExpireDays">
              <a-radio-button :value="1">1 天</a-radio-button>
              <a-radio-button :value="7">7 天</a-radio-button>
              <a-radio-button :value="30">30 天</a-radio-button>
            </a-radio-group>
          </div>
          <div class="share-actions">
            <a-button type="primary" :loading="shareCreating" @click="handleCreateShare">生成免密链接</a-button>
          </div>
          <div class="share-tip">生成后链接内含随机令牌，仅展示图表名称与数据，不暴露 SQL 及提问记录；重新生成或撤销后旧链接立即失效。</div>
        </template>
      </div>
    </a-modal>

    <!-- 背景色设置弹窗 -->
    <a-modal
      v-model:open="bgColorModalVisible"
      title="设置画布背景色"
      width="320px"
      :footer="null"
      @cancel="bgColorModalVisible = false"
    >
      <div class="bg-color-panel">
        <div class="bg-preset-colors">
          <span
            v-for="c in bgPresetColors"
            :key="c"
            class="bg-color-swatch"
            :class="{ active: currentBgColor === c }"
            :style="{ background: c }"
            @click="applyBgColor(c)"
          >
            <CheckOutlined v-if="currentBgColor === c" class="check-icon" />
          </span>
        </div>
        <div class="bg-custom-row">
          <input type="color" :value="currentBgColor || '#f5f7fa'" @input="applyBgColor(($event.target as HTMLInputElement).value)" />
          <span class="bg-custom-label">自定义颜色</span>
          <a-button size="small" @click="applyBgColor('')">重置默认</a-button>
        </div>
      </div>
    </a-modal>

    <!-- 全屏预览 -->
    <Teleport to="body">
      <div v-if="fullscreenPreview" class="fullscreen-preview">
        <div class="preview-header">
          <span class="preview-title">{{ currentDashboard?.name }}</span>
          <a-button type="text" class="preview-close" @click="closeFullscreenPreview">
            <template #icon><CloseOutlined /></template>
            退出预览 (Esc)
          </a-button>
        </div>
        <div class="preview-canvas" :style="previewBgStyle">
          <div class="preview-canvas-inner" :style="previewCanvasStyle">
            <DashboardChart
              v-for="chart in chartList"
              :key="'c_' + chart.id"
              :chart-data="chart"
              :container-width="containerWidth"
              :container-height="containerHeight"
              :card-color="getCardColor(chart.id!)"
              readonly
            />
            <WidgetWrapper
              v-for="widget in widgetList"
              :key="'w_' + widget.id"
              :widget="widget"
              :container-width="containerWidth"
              :container-height="containerHeight"
              readonly
            >
              <WidgetCarousel v-if="widget.type === 'carousel'" :config="widget.config as any" readonly />
              <WidgetText v-else-if="widget.type === 'text'" :config="widget.config as any" readonly />
              <WidgetTimer v-else-if="widget.type === 'timer'" :config="widget.config as any" readonly />
              <WidgetMarquee v-else-if="widget.type === 'marquee'" :config="widget.config as any" readonly />
              <WidgetNumberFlip v-else-if="widget.type === 'numberFlip'" :config="widget.config as any" readonly />
              <WidgetProgressRing v-else-if="widget.type === 'progressRing'" :config="widget.config as any" readonly />
              <WidgetPulse v-else-if="widget.type === 'pulse'" :config="widget.config as any" readonly />
            </WidgetWrapper>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, nextTick, computed, watch } from 'vue';
import { message } from 'ant-design-vue';
import {
  PlusOutlined, MoreOutlined, EditOutlined, DeleteOutlined,
  FullscreenOutlined, CloseOutlined, ShareAltOutlined, BgColorsOutlined,
  BarChartOutlined, PictureOutlined, FontSizeOutlined, FieldTimeOutlined,
  SoundOutlined, NumberOutlined, PieChartOutlined, AimOutlined,
  CheckOutlined,
} from '@ant-design/icons-vue';
import DashboardChart from '@/components/DashboardChart.vue';
import LeftSidebar from '@/components/LeftSidebar.vue';
import WidgetWrapper from '@/components/widgets/WidgetWrapper.vue';
import WidgetCarousel from '@/components/widgets/WidgetCarousel.vue';
import WidgetText from '@/components/widgets/WidgetText.vue';
import WidgetTimer from '@/components/widgets/WidgetTimer.vue';
import WidgetMarquee from '@/components/widgets/WidgetMarquee.vue';
import WidgetNumberFlip from '@/components/widgets/WidgetNumberFlip.vue';
import WidgetProgressRing from '@/components/widgets/WidgetProgressRing.vue';
import WidgetPulse from '@/components/widgets/WidgetPulse.vue';
import {
  loadWidgets, saveWidgets, loadDashboardBg, saveDashboardBg,
  loadCardColor, saveCardColor, generateWidgetId,
  type DashboardWidget, type CarouselConfig, type TextConfig, type TimerConfig,
  type MarqueeConfig, type NumberFlipConfig, type ProgressRingConfig, type PulseConfig,
} from '@/types/dashboardWidget';
import {
  getDashboardList,
  createDashboard,
  renameDashboard,
  deleteDashboard,
  getDashboardChartList,
  updateDashboardLayout,
  updateDashboardChart,
  saveDashboardChart,
  deleteDashboardChart,
  getQuestionsWithChart,
  createDashboardShare,
  getDashboardShare,
  revokeDashboardShare,
  type DashboardShareItem,
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
      loadWidgetsForDashboard(firstId);
      loadBgColor(firstId);
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
  loadWidgetsForDashboard(id);
  loadBgColor(id);
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

const handleRenameChart = async (id: number, name: string) => {
  const chart = chartList.value.find(c => c.id === id);
  if (!chart) {return;}
  const oldName = chart.chartName;
  chart.chartName = name;
  try {
    await updateDashboardChart({ id, chartName: name });
  } catch {
    chart.chartName = oldName;
    message.error('重命名失败');
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

// 画布容器随 activeDashboardId 条件渲染，ref 出现/变化时重新监听尺寸
watch(dashboardContainer, el => {
  resizeObserver?.disconnect();
  if (el) {
    updateContainerSize();
    resizeObserver = new ResizeObserver(() => updateContainerSize());
    resizeObserver.observe(el);
  }
});

// ==================== 功能组件 ====================
const widgetList = ref<DashboardWidget[]>([]);
const widgetLoading = ref(false);

const loadWidgetsForDashboard = (dashboardId: number) => {
  widgetLoading.value = true;
  try {
    widgetList.value = loadWidgets(dashboardId);
  } finally {
    widgetLoading.value = false;
  }
};

const persistWidgets = () => {
  if (activeDashboardId.value) {
    saveWidgets(activeDashboardId.value, widgetList.value);
  }
};

const handleDeleteWidget = (id: string) => {
  widgetList.value = widgetList.value.filter(w => w.id !== id);
  persistWidgets();
  message.success('已删除');
};

const handleWidgetLayoutChange = (id: string, x: number, y: number, w: number, h: number) => {
  const widget = widgetList.value.find(item => item.id === id);
  if (widget) {
    widget.layoutX = x;
    widget.layoutY = y;
    widget.layoutW = w;
    widget.layoutH = h;
    persistWidgets();
  }
};

const handleRenameWidget = (id: string, name: string) => {
  const widget = widgetList.value.find(item => item.id === id);
  if (widget) {
    widget.title = name;
    persistWidgets();
  }
};

const handleWidgetColorChange = (id: string, color: string) => {
  const widget = widgetList.value.find(item => item.id === id);
  if (widget) {
    widget.bgColor = color;
    persistWidgets();
  }
};

const handleWidgetConfigChange = (id: string, config: Record<string, any>) => {
  const widget = widgetList.value.find(item => item.id === id);
  if (widget) {
    widget.config = config;
    persistWidgets();
  }
};

const addWidget = (type: 'carousel' | 'text' | 'timer' | 'marquee' | 'numberFlip' | 'progressRing' | 'pulse') => {
  const defaults: Record<string, { title: string; config: Record<string, any>; w: number; h: number }> = {
    carousel: {
      title: '轮播组件',
      config: { images: [], interval: 5000, transition: 'fade' } as CarouselConfig,
      w: 400, h: 300,
    },
    text: {
      title: '文本标注',
      config: { content: '在此输入文本内容...', fontSize: 14, fontColor: '#333333', textAlign: 'left' } as TextConfig,
      w: 300, h: 200,
    },
    timer: {
      title: '计时器',
      config: { format: '24h', fontSize: 32, fontColor: '#333333', showDate: true } as TimerConfig,
      w: 300, h: 180,
    },
    marquee: {
      title: '滚动字幕',
      config: { text: '欢迎使用数据大屏滚动字幕组件', speed: 60, direction: 'left', fontSize: 16, fontColor: '#333333' } as MarqueeConfig,
      w: 400, h: 80,
    },
    numberFlip: {
      title: '数字翻牌器',
      config: { value: 12345, prefix: '¥', suffix: '', fontSize: 36, fontColor: '#1890ff', duration: 1000, decimals: 0 } as NumberFlipConfig,
      w: 300, h: 160,
    },
    progressRing: {
      title: '进度环',
      config: { value: 75, size: 120, strokeWidth: 10, color: '#1890ff', trackColor: '#f0f0f0', showLabel: true, fontSize: 20 } as ProgressRingConfig,
      w: 240, h: 240,
    },
    pulse: {
      title: '脉冲指示器',
      config: { text: '在线', color: '#52c41a', fontSize: 14, pulseSize: 'medium' } as PulseConfig,
      w: 200, h: 100,
    },
  };
  const d = defaults[type];
  const widget: DashboardWidget = {
    id: generateWidgetId(),
    type,
    title: d.title,
    layoutX: 0,
    layoutY: 0,
    layoutW: d.w,
    layoutH: d.h,
    bgColor: '#ffffff',
    config: d.config,
  };
  widgetList.value.push(widget);
  persistWidgets();
  message.success(`已添加${d.title}`);
};

const handleAddItem = ({ key }: { key: string }) => {
  if (key === 'chart') {
    openAddModal();
  } else if (['carousel', 'text', 'timer', 'marquee', 'numberFlip', 'progressRing', 'pulse'].includes(key)) {
    addWidget(key as any);
  }
};

// ==================== 背景色 ====================
const bgColorModalVisible = ref(false);
const currentBgColor = ref('');

const bgPresetColors = [
  '#f5f7fa', '#ffffff', '#e8ecf1',
  '#1a1a2e', '#16213e', '#0f3460', '#2d3436', '#000000',
];

const gridBgStyle = computed(() => {
  return currentBgColor.value ? { background: currentBgColor.value } : {};
});

const previewBgStyle = computed(() => {
  return currentBgColor.value ? { background: currentBgColor.value } : {};
});

const loadBgColor = (dashboardId: number) => {
  currentBgColor.value = loadDashboardBg(dashboardId);
};

const openBgColorPanel = () => {
  bgColorModalVisible.value = true;
};

const applyBgColor = (color: string) => {
  currentBgColor.value = color;
  if (activeDashboardId.value) {
    saveDashboardBg(activeDashboardId.value, color);
  }
};

// ==================== 卡片颜色 ====================
const cardColorMap = ref<Record<number, string>>({});

const getCardColor = (cardId: number): string => {
  return cardColorMap.value[cardId] || '';
};

const handleCardColorChange = (cardId: number, color: string) => {
  cardColorMap.value[cardId] = color;
  saveCardColor(cardId, color);
};

const loadCardColors = () => {
  const map: Record<number, string> = {};
  chartList.value.forEach(chart => {
    if (chart.id) {
      const c = loadCardColor(chart.id);
      if (c) {map[chart.id] = c;}
    }
  });
  cardColorMap.value = map;
};

watch(chartList, () => {
  loadCardColors();
}, { deep: false });

// ==================== 仅显示内容模式 ====================
const chartContentOnlyMap = ref<Record<number, boolean>>({});
const widgetContentOnlyMap = ref<Record<string, boolean>>({});

const CONTENT_ONLY_PREFIX = 'content_only_';

const getContentOnly = (cardId: number): boolean => {
  return chartContentOnlyMap.value[cardId] || false;
};

const getWidgetContentOnly = (widgetId: string): boolean => {
  return widgetContentOnlyMap.value[widgetId] || false;
};

const handleContentOnlyChange = (cardId: number, value: boolean) => {
  chartContentOnlyMap.value[cardId] = value;
  localStorage.setItem(`${CONTENT_ONLY_PREFIX}chart_${cardId}`, value ? '1' : '0');
};

const handleWidgetContentOnlyChange = (widgetId: string, value: boolean) => {
  widgetContentOnlyMap.value[widgetId] = value;
  localStorage.setItem(`${CONTENT_ONLY_PREFIX}widget_${widgetId}`, value ? '1' : '0');
};

const loadContentOnlyStates = () => {
  const chartMap: Record<number, boolean> = {};
  chartList.value.forEach(chart => {
    if (chart.id) {
      const v = localStorage.getItem(`${CONTENT_ONLY_PREFIX}chart_${chart.id}`);
      if (v === '1') {chartMap[chart.id] = true;}
    }
  });
  chartContentOnlyMap.value = chartMap;

  const widgetMap: Record<string, boolean> = {};
  widgetList.value.forEach(w => {
    const v = localStorage.getItem(`${CONTENT_ONLY_PREFIX}widget_${w.id}`);
    if (v === '1') {widgetMap[w.id] = true;}
  });
  widgetContentOnlyMap.value = widgetMap;
};

watch([chartList, widgetList], () => {
  loadContentOnlyStates();
}, { deep: false });

onMounted(() => {
  loadDashboards();
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  layoutTimers.forEach(t => clearTimeout(t));
  document.removeEventListener('keydown', onPreviewKeydown);
});

// ==================== 全屏预览 ====================
const fullscreenPreview = ref(false);

// 预览画布尺寸按图表布局边界计算，保证超出视口时可滚动
const previewCanvasStyle = computed(() => {
  let maxX = 0;
  let maxY = 0;
  chartList.value.forEach(c => {
    maxX = Math.max(maxX, (c.layoutX || 0) + (c.layoutW || 0));
    maxY = Math.max(maxY, (c.layoutY || 0) + (c.layoutH || 0));
  });
  widgetList.value.forEach(w => {
    maxX = Math.max(maxX, (w.layoutX || 0) + (w.layoutW || 0));
    maxY = Math.max(maxY, (w.layoutY || 0) + (w.layoutH || 0));
  });
  return {
    width: `${maxX + 32}px`,
    height: `${maxY + 32}px`,
  };
});

const onPreviewKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {closeFullscreenPreview();}
};

const openFullscreenPreview = () => {
  if (chartList.value.length === 0 && widgetList.value.length === 0) {
    message.info('当前仪表盘暂无内容');
    return;
  }
  fullscreenPreview.value = true;
  document.addEventListener('keydown', onPreviewKeydown);
};

const closeFullscreenPreview = () => {
  fullscreenPreview.value = false;
  document.removeEventListener('keydown', onPreviewKeydown);
};

// ==================== 免密分享 ====================
const shareModalVisible = ref(false);
const shareLoading = ref(false);
const shareCreating = ref(false);
const shareExpireDays = ref(7);
const activeShare = ref<DashboardShareItem | null>(null);

const shareUrl = computed(() => {
  if (!activeShare.value) {return '';}
  return `${window.location.origin}/share/dashboard/${activeShare.value.token}`;
});

const openShareModal = async () => {
  if (!activeDashboardId.value) {return;}
  shareModalVisible.value = true;
  shareLoading.value = true;
  try {
    activeShare.value = await getDashboardShare(activeDashboardId.value);
  } catch {
    activeShare.value = null;
  } finally {
    shareLoading.value = false;
  }
};

const handleCreateShare = async () => {
  if (!activeDashboardId.value) {return;}
  shareCreating.value = true;
  try {
    activeShare.value = await createDashboardShare(activeDashboardId.value, shareExpireDays.value);
    message.success('免密链接已生成');
  } catch {
    message.error('生成失败');
  } finally {
    shareCreating.value = false;
  }
};

const handleRevokeShare = async () => {
  if (!activeDashboardId.value) {return;}
  try {
    await revokeDashboardShare(activeDashboardId.value);
    activeShare.value = null;
    message.success('链接已撤销，立即失效');
  } catch {
    message.error('撤销失败');
  }
};

const copyShareUrl = async () => {
  try {
    await navigator.clipboard.writeText(shareUrl.value);
    message.success('链接已复制');
  } catch {
    // 非安全上下文（http）降级处理
    const input = document.createElement('textarea');
    input.value = shareUrl.value;
    document.body.appendChild(input);
    input.select();
    document.execCommand('copy');
    document.body.removeChild(input);
    message.success('链接已复制');
  }
};

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

  .header-actions {
    display: flex;
    gap: 8px;
  }
}

// ==================== 全屏预览 ====================
.fullscreen-preview {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;

  .preview-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 24px;
    background: #fff;
    border-bottom: 1px solid #e8e8e8;
    flex-shrink: 0;

    .preview-title {
      font-size: 16px;
      font-weight: 600;
      color: #1a1a1a;
    }

    .preview-close {
      color: #666;
    }
  }

  .preview-canvas {
    flex: 1;
    overflow: auto;
    padding: 16px;
  }

  .preview-canvas-inner {
    position: relative;
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

// ==================== 免密分享弹窗 ====================
.share-content {
  .share-alert {
    margin-bottom: 16px;
  }

  .share-link-row {
    display: flex;
    gap: 8px;
    margin-bottom: 16px;
  }

  .share-expire-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 16px;

    .expire-label {
      font-size: 14px;
      font-weight: 500;
    }
  }

  .share-actions {
    margin-bottom: 12px;
  }

  .share-tip {
    font-size: 12px;
    color: #999;
    line-height: 1.6;
  }
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

// ==================== 背景色设置面板 ====================
.bg-color-panel {
  .bg-preset-colors {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 16px;
  }

  .bg-color-swatch {
    width: 36px;
    height: 36px;
    border-radius: 6px;
    cursor: pointer;
    border: 2px solid #d9d9d9;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;

    &:hover {
      border-color: #1890ff;
      transform: scale(1.1);
    }

    &.active {
      border-color: #1890ff;
      box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
    }

    .check-icon {
      font-size: 16px;
      color: #1890ff;
    }
  }

  .bg-custom-row {
    display: flex;
    align-items: center;
    gap: 10px;

    input[type="color"] {
      width: 32px;
      height: 32px;
      border: none;
      cursor: pointer;
      padding: 0;
      border-radius: 4px;
    }

    .bg-custom-label {
      font-size: 13px;
      color: #666;
      flex: 1;
    }
  }
}
</style>
