<template>
  <div
    class="dashboard-chart-card"
    :style="cardStyle"
    @mousedown.stop
  >
    <!-- 标题栏（拖拽手柄） -->
    <div class="chart-header" @mousedown="startDrag">
      <div class="chart-title">{{ chartData.chartName || '未命名图表' }}</div>
      <div class="chart-actions">
        <a-tooltip title="删除">
          <DeleteOutlined class="action-icon delete-icon" @click.stop="handleDelete" />
        </a-tooltip>
      </div>
    </div>

    <!-- 图表内容 -->
    <div class="chart-body">
      <a-table
        v-if="chartData.chartType === 'Table'"
        :columns="tableColumns"
        :data-source="tableData"
        :pagination="false"
        :scroll="{ x: 'max-content', y: cardBodyHeight }"
        size="small"
      />
      <div
        v-else
        ref="chartRef"
        class="echarts-container"
      />
    </div>

    <!-- 右下角缩放手柄 -->
    <div class="resize-handle" @mousedown="startResize">
      <svg width="12" height="12" viewBox="0 0 12 12">
        <path d="M0 12 L12 0 M4 12 L12 4 M8 12 L12 8" stroke="#bbb" stroke-width="1.2" fill="none" />
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { DeleteOutlined } from '@ant-design/icons-vue';
import * as echarts from 'echarts';
import type { DashboardChartItem } from '@/api/dashboard';

const props = defineProps<{
  chartData: DashboardChartItem;
  containerWidth: number;
  containerHeight: number;
}>();

const emit = defineEmits<{
  delete: [id: number];
  layoutChange: [id: number, x: number, y: number, w: number, h: number];
}>();

// Grid snap size
const GRID = 20;

const cardStyle = computed(() => ({
  left: `${props.chartData.layoutX}px`,
  top: `${props.chartData.layoutY}px`,
  width: `${props.chartData.layoutW}px`,
  height: `${props.chartData.layoutH}px`,
}));

const cardBodyHeight = computed(() => Math.max(100, props.chartData.layoutH - 50));

// ---- Table data parsing ----
const tableColumns = computed(() => {
  try {
    const parsed = typeof props.chartData.chartData === 'string'
      ? JSON.parse(props.chartData.chartData)
      : props.chartData.chartData;
    if (parsed?.columns && Array.isArray(parsed.columns)) {
      return parsed.columns.map((col: any) => ({
        title: col.name,
        dataIndex: col.name,
        key: col.name,
        ellipsis: true,
      }));
    }
    if (Array.isArray(parsed) && parsed.length > 0 && typeof parsed[0] === 'object') {
      return Object.keys(parsed[0]).map(key => ({
        title: key,
        dataIndex: key,
        key,
        ellipsis: true,
      }));
    }
    return [];
  } catch {
    return [];
  }
});

const tableData = computed(() => {
  try {
    const parsed = typeof props.chartData.chartData === 'string'
      ? JSON.parse(props.chartData.chartData)
      : props.chartData.chartData;
    if (parsed?.data && Array.isArray(parsed.data)) {return parsed.data;}
    if (Array.isArray(parsed)) {return parsed;}
    return [];
  } catch {
    return [];
  }
});

// ---- ECharts ----
const chartRef = ref<HTMLDivElement>();
let chartInstance: echarts.ECharts | null = null;

const getChartData = () => {
  try {
    return typeof props.chartData.chartData === 'string'
      ? JSON.parse(props.chartData.chartData)
      : props.chartData.chartData;
  } catch {
    return null;
  }
};

const renderChart = () => {
  if (!chartRef.value || props.chartData.chartType === 'Table') {return;}
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
  }
  const data = getChartData();
  if (!data?.columns || !data?.data) {return;}

  const xData = data.data.map((row: any) => row[data.columns[0]?.name] ?? '');
  const yData = data.data.map((row: any) => row[data.columns[1]?.name] ?? 0);
  const pieData = xData.map((name: string, i: number) => ({ name, value: yData[i] }));

  let option: any;
  switch (props.chartData.chartType) {
    case 'PieChart':
      option = {
        tooltip: { trigger: 'item' },
        legend: { top: '5%', left: 'center', textStyle: { fontSize: 10 } },
        series: [{
          name: '数据',
          type: 'pie',
          radius: ['40%', '70%'],
          data: pieData,
          label: { fontSize: 10 },
        }],
      };
      break;
    case 'LineChart':
      option = {
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: xData, axisLabel: { fontSize: 10 } },
        yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
        series: [{ data: yData, type: 'line', smooth: true }],
      };
      break;
    case 'BarChart':
      option = {
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: xData, axisLabel: { fontSize: 10 } },
        yAxis: { type: 'value', axisLabel: { fontSize: 10 } },
        series: [{ data: yData, type: 'bar', barWidth: '50%' }],
      };
      break;
    default:
      return;
  }
  chartInstance.setOption(option, true);
};

watch(() => [props.chartData.chartType, props.chartData.chartData, props.chartData.layoutW, props.chartData.layoutH], () => {
  nextTick(() => {
    renderChart();
    chartInstance?.resize();
  });
}, { deep: true });

onMounted(() => {
  nextTick(() => renderChart());
});

onBeforeUnmount(() => {
  chartInstance?.dispose();
  chartInstance = null;
});

// ---- Drag ----
const isDragging = ref(false);
const dragStartX = ref(0);
const dragStartY = ref(0);
const dragOrigX = ref(0);
const dragOrigY = ref(0);

const startDrag = (e: MouseEvent) => {
  isDragging.value = true;
  dragStartX.value = e.clientX;
  dragStartY.value = e.clientY;
  dragOrigX.value = props.chartData.layoutX;
  dragOrigY.value = props.chartData.layoutY;
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
};

const onDrag = (e: MouseEvent) => {
  if (!isDragging.value) {return;}
  const dx = e.clientX - dragStartX.value;
  const dy = e.clientY - dragStartY.value;
  let nx = Math.round((dragOrigX.value + dx) / GRID) * GRID;
  let ny = Math.round((dragOrigY.value + dy) / GRID) * GRID;
  nx = Math.max(0, Math.min(nx, Math.max(0, props.containerWidth - props.chartData.layoutW)));
  ny = Math.max(0, Math.min(ny, Math.max(0, props.containerHeight - props.chartData.layoutH)));
  emit('layoutChange', props.chartData.id!, nx, ny, props.chartData.layoutW, props.chartData.layoutH);
};

const stopDrag = () => {
  isDragging.value = false;
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
};

// ---- Resize ----
const isResizing = ref(false);
const resizeStartX = ref(0);
const resizeStartY = ref(0);
const resizeOrigW = ref(0);
const resizeOrigH = ref(0);

const startResize = (e: MouseEvent) => {
  e.stopPropagation();
  e.preventDefault();
  isResizing.value = true;
  resizeStartX.value = e.clientX;
  resizeStartY.value = e.clientY;
  resizeOrigW.value = props.chartData.layoutW;
  resizeOrigH.value = props.chartData.layoutH;
  document.addEventListener('mousemove', onResize);
  document.addEventListener('mouseup', stopResize);
};

const onResize = (e: MouseEvent) => {
  if (!isResizing.value) {return;}
  const dx = e.clientX - resizeStartX.value;
  const dy = e.clientY - resizeStartY.value;
  let nw = Math.round((resizeOrigW.value + dx) / GRID) * GRID;
  let nh = Math.round((resizeOrigH.value + dy) / GRID) * GRID;
  nw = Math.max(200, nw);
  nh = Math.max(150, nh);
  emit('layoutChange', props.chartData.id!, props.chartData.layoutX, props.chartData.layoutY, nw, nh);
};

const stopResize = () => {
  isResizing.value = false;
  document.removeEventListener('mousemove', onResize);
  document.removeEventListener('mouseup', stopResize);
};

// ---- Delete ----
const handleDelete = () => {
  emit('delete', props.chartData.id!);
};
</script>

<style lang="scss" scoped>
.dashboard-chart-card {
  position: absolute;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 38px;
  padding: 0 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  cursor: move;
  user-select: none;
  flex-shrink: 0;

  .chart-title {
    font-size: 13px;
    font-weight: 500;
    color: #333;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    margin-right: 8px;
  }

  .chart-actions {
    display: flex;
    gap: 4px;
  }

  .action-icon {
    font-size: 14px;
    color: #999;
    cursor: pointer;
    padding: 2px;

    &:hover {
      color: #ff4d4f;
    }
  }
}

.chart-body {
  flex: 1;
  padding: 8px;
  overflow: auto;
  min-height: 0;
}

.echarts-container {
  width: 100%;
  height: 100%;
  min-height: 120px;
}

.resize-handle {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 20px;
  height: 20px;
  cursor: nwse-resize;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  padding: 2px;
  z-index: 10;

  &:hover svg path {
    stroke: #1890ff;
  }
}
</style>
