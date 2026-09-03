<template>
  <a-drawer
    :open="open"
    placement="right"
    width="100%"
    class="report-drawer"
    :closable="false"
    :keyboard="false"
    :body-style="{ padding: '0', background: '#f5f7fa' }"
    @close="handleClose"
  >
    <template #title>
      <div class="report-toolbar no-print">
        <div class="toolbar-left">
          <FileTextOutlined class="toolbar-icon" />
          <span class="toolbar-title">数据分析报告</span>
        </div>
        <div class="toolbar-right">
          <a-button
            v-if="report && !loading"
            type="primary"
            ghost
            @click="handleDownloadPdf"
          >
            <template #icon>
              <DownloadOutlined />
            </template>
            下载 PDF
          </a-button>
          <a-button type="text" @click="handleClose">
            <template #icon>
              <CloseOutlined />
            </template>
          </a-button>
        </div>
      </div>
    </template>

    <div class="report-body">
      <!-- 生成中：进度展示 -->
      <div v-if="loading" class="report-loading no-print">
        <div class="loading-card">
          <div class="loading-icon">
            <a-spin size="large" />
          </div>
          <div class="loading-title">正在生成数据报告</div>
          <div class="loading-desc">归因分析 · 数据预测 · 图表解释，预计需要 1-2 分钟</div>
          <div class="loading-progress">
            <a-progress
              :percent="progressPercent"
              :show-info="false"
              stroke-color="#1677ff"
            />
          </div>
          <div class="loading-message">{{ progress || '正在准备...' }}</div>
        </div>
      </div>

      <!-- 错误态 -->
      <div v-else-if="error && !report" class="report-error no-print">
        <div class="error-card">
          <CloseCircleFilled class="error-icon" />
          <div class="error-title">数据报告生成失败</div>
          <div class="error-desc">{{ error }}</div>
          <a-button type="primary" @click="handleClose">关闭</a-button>
        </div>
      </div>

      <!-- 报告内容 -->
      <div v-else-if="report" class="report-print-area">
        <!-- 报告头部 -->
        <div class="report-header">
          <div class="header-badge">DATA REPORT</div>
          <h1 class="header-title">{{ report.title || '数据分析报告' }}</h1>
          <div class="header-question">“{{ report.originalQuestion }}”</div>
          <div class="header-meta">
            <div class="meta-item">
              <FieldTimeOutlined class="meta-icon" />
              生成时间：{{ formatDateTime(report.createdAt) }}
            </div>
            <a-divider type="vertical" class="meta-divider" />
            <div class="meta-item">
              <ThunderboltOutlined class="meta-icon" />
              耗时：{{ formatDuration(report.totalExecutionTimeMs) }}
            </div>
            <a-divider type="vertical" class="meta-divider" />
            <div class="meta-item">
              <DatabaseOutlined class="meta-icon" />
              Token 消耗：{{ report.totalTokenUsage.toLocaleString() }}
            </div>
          </div>
        </div>

        <div class="report-content">
          <!-- 执行摘要 -->
          <div v-if="report.executiveSummary" class="report-section">
            <div class="section-title">
              <span class="section-bar"></span>
              执行摘要
            </div>
            <div class="summary-card">
              <div class="markdown-content" v-html="renderMarkdown(report.executiveSummary)" />
            </div>
          </div>

          <!-- 关键发现 -->
          <div v-if="report.keyFindings && report.keyFindings.length" class="report-section">
            <div class="section-title">
              <span class="section-bar"></span>
              关键发现
            </div>
            <div class="findings-grid">
              <div
                v-for="(finding, index) in report.keyFindings"
                :key="index"
                class="finding-card"
              >
                <div class="finding-index">{{ String(index + 1).padStart(2, '0') }}</div>
                <div class="finding-text">{{ finding }}</div>
              </div>
            </div>
          </div>

          <!-- 归因分析章节 -->
          <div v-if="report.sections && report.sections.length" class="report-section">
            <div class="section-title">
              <span class="section-bar"></span>
              归因分析
            </div>
            <div
              v-for="(section, index) in report.sections"
              :key="index"
              class="attribution-card"
            >
              <div class="attribution-card-header">
                <div class="attribution-card-title">{{ section.title }}</div>
                <a-tag v-if="section.attributionAngle" color="blue" class="angle-tag">
                  {{ angleLabel(section.attributionAngle) }}
                </a-tag>
              </div>
              <div class="markdown-content" v-html="renderMarkdown(section.content || '')" />
            </div>
          </div>

          <!-- 数据预测 -->
          <div v-if="report.prediction" class="report-section">
            <div class="section-title">
              <span class="section-bar"></span>
              数据预测
            </div>

            <!-- 预测失败降级占位 -->
            <div v-if="!report.prediction.success" class="prediction-fallback">
              <InfoCircleOutlined class="fallback-icon" />
              预测数据不足，本次未生成可靠的预测结果，建议补充时序数据后重试。
            </div>

            <template v-else>
              <!-- 趋势图：历史实线 + 预测虚线 -->
              <div
                v-if="report.prediction.trendPoints && report.prediction.trendPoints.length > 1"
                class="chart-card"
              >
                <div class="chart-card-title">指标趋势预测</div>
                <div ref="trendChartRef" class="chart-container" />
                <div class="chart-legend">
                  <span class="legend-item"><span class="legend-line legend-solid"></span>历史数据</span>
                  <span class="legend-item"><span class="legend-line legend-dashed"></span>预测数据</span>
                </div>
              </div>

              <!-- 指标预测卡片 -->
              <div v-if="report.prediction.metrics && report.prediction.metrics.length" class="metrics-grid">
                <div
                  v-for="(metric, index) in report.prediction.metrics"
                  :key="index"
                  class="metric-card"
                >
                  <div class="metric-name">{{ metric.name }}</div>
                  <div class="metric-values">
                    <span class="metric-current">{{ formatNumber(metric.currentValue) }}</span>
                    <ArrowRightOutlined class="metric-arrow" />
                    <span class="metric-forecast">{{ formatNumber(metric.forecastValue) }}</span>
                  </div>
                  <div class="metric-change" :class="changeClass(metric.changeRate)">
                    <CaretUpOutlined v-if="metric.changeRate > 0" />
                    <CaretDownOutlined v-else-if="metric.changeRate < 0" />
                    {{ formatPercent(metric.changeRate) }}
                  </div>
                </div>
              </div>

              <!-- 预测结论 -->
              <div v-if="report.prediction.forecastSummary" class="summary-card">
                <div class="markdown-content" v-html="renderMarkdown(report.prediction.forecastSummary)" />
              </div>

              <!-- 置信水平与风险 -->
              <div v-if="report.prediction.confidenceLevel || (report.prediction.risks && report.prediction.risks.length)" class="risk-row">
                <div v-if="report.prediction.confidenceLevel" class="confidence-card">
                  <div class="confidence-label">预测置信水平</div>
                  <div class="confidence-value" :class="confidenceClass(report.prediction.confidenceLevel)">
                    {{ report.prediction.confidenceLevel }}
                  </div>
                </div>
                <div
                  v-if="report.prediction.risks && report.prediction.risks.length"
                  class="risk-card"
                >
                  <div class="risk-title">
                    <WarningOutlined class="risk-icon" />
                    风险提示
                  </div>
                  <ul class="risk-list">
                    <li v-for="(risk, index) in report.prediction.risks" :key="index">{{ risk }}</li>
                  </ul>
                </div>
              </div>
            </template>
          </div>

          <!-- 图表解释 -->
          <div v-if="report.charts && report.charts.length" class="report-section">
            <div class="section-title">
              <span class="section-bar"></span>
              图表解释
            </div>
            <div
              v-for="(chart, index) in report.charts"
              :key="index"
              class="chart-card"
            >
              <div class="chart-card-title">{{ chart.title }}</div>
              <div
                :ref="(el: any) => setChartEl(`chart_${index}`, el)"
                class="chart-container"
              />
              <div class="chart-explanation">
                <BulbOutlined class="explanation-icon" />
                <span>{{ chart.explanation }}</span>
              </div>
            </div>
          </div>

          <!-- 建议与行动项 -->
          <div v-if="report.recommendations && report.recommendations.length" class="report-section">
            <div class="section-title">
              <span class="section-bar"></span>
              建议与行动项
            </div>
            <div class="recommendation-card">
              <div
                v-for="(item, index) in report.recommendations"
                :key="index"
                class="recommendation-item"
              >
                <div class="recommendation-index">{{ index + 1 }}</div>
                <div class="recommendation-text">{{ item }}</div>
              </div>
            </div>
          </div>

          <!-- 页脚 -->
          <div class="report-footer">
            <div class="footer-line"></div>
            <div class="footer-text">
              本报告由 DataCopilotX 基于 AI 归因分析与数据预测自动生成，仅供决策参考，不构成任何投资或经营建议。
            </div>
            <div class="footer-meta">
              报告编号：{{ report.reportId }} · 生成耗时 {{ formatDuration(report.totalExecutionTimeMs) }} · Token 消耗 {{ report.totalTokenUsage.toLocaleString() }}
            </div>
          </div>
        </div>
      </div>

      <!-- 空态 -->
      <div v-else class="report-error no-print">
        <div class="error-card">
          <FileTextOutlined class="error-icon" style="color: #1677ff" />
          <div class="error-title">暂无报告数据</div>
          <a-button type="primary" @click="handleClose">关闭</a-button>
        </div>
      </div>
    </div>
  </a-drawer>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue';
import * as echarts from 'echarts';
import { marked } from 'marked';
import { message } from 'ant-design-vue';
import {
  FileTextOutlined,
  DownloadOutlined,
  CloseOutlined,
  CloseCircleFilled,
  FieldTimeOutlined,
  ThunderboltOutlined,
  DatabaseOutlined,
  ArrowRightOutlined,
  CaretUpOutlined,
  CaretDownOutlined,
  WarningOutlined,
  BulbOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue';
import type { DataReport, ChartSpec, PredictionTrendPoint } from '@/api/chat.ts';

const props = defineProps<{
  open: boolean;
  report: DataReport | null;
  loading: boolean;
  progress: string;
  error: string;
}>();

const emit = defineEmits(['close']);

// ============ 加载进度 ============
// 从进度消息中解析 "Step x/y" 计算百分比
const progressPercent = computed(() => {
  const match = props.progress.match(/Step\s+(\d+)\s*\/\s*(\d+)/i);
  if (match) {
    const current = parseInt(match[1], 10);
    const total = parseInt(match[2], 10);
    if (total > 0) {
      return Math.min(99, Math.round((current / total) * 100));
    }
  }
  return props.loading ? 15 : 100;
});

// ============ 工具函数 ============
const renderMarkdown = (text: string) => marked.parse(text || '');

const formatDateTime = (ts: number) => {
  if (!ts) {return '-';}
  const d = new Date(ts);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const formatDuration = (ms: number) => {
  if (!ms || ms < 0) {return '-';}
  if (ms < 1000) {return `${ms}ms`;}
  if (ms < 60000) {return `${(ms / 1000).toFixed(1)}s`;}
  return `${Math.floor(ms / 60000)}分${Math.round((ms % 60000) / 1000)}秒`;
};

const formatNumber = (n: number) => {
  if (n === null || n === undefined || Number.isNaN(n)) {return '-';}
  return n.toLocaleString('zh-CN', { maximumFractionDigits: 2 });
};

const formatPercent = (rate: number) => {
  if (rate === null || rate === undefined || Number.isNaN(rate)) {return '-';}
  return `${rate >= 0 ? '+' : ''}${(rate * 100).toFixed(1)}%`;
};

const changeClass = (rate: number) => (rate > 0 ? 'change-up' : rate < 0 ? 'change-down' : '');

const confidenceClass = (level: string) => {
  if (level === '高') {return 'confidence-high';}
  if (level === '中') {return 'confidence-middle';}
  return 'confidence-low';
};

const angleLabel = (angle: string) => {
  const labels: Record<string, string> = {
    drill_down: '下钻分析',
    comparison: '对比分析',
    anomaly: '异常检测',
    contribution: '贡献度分析'
  };
  return labels[angle] || angle;
};

// ============ 图表渲染 ============
const trendChartRef = ref<HTMLElement | null>(null);
const chartEls = new Map<string, HTMLElement>();
const chartInstances = new Map<string, echarts.ECharts>();

const setChartEl = (key: string, el: any) => {
  if (el) {
    chartEls.set(key, el);
  } else {
    chartEls.delete(key);
  }
};

// 渲染所有图表（报告数据就绪后）
const renderCharts = () => {
  if (!props.report) {return;}

  // 趋势预测图
  if (trendChartRef.value && props.report.prediction?.trendPoints?.length) {
    renderTrendChart(props.report.prediction.trendPoints);
  }

  // 图表解释卡片
  props.report.charts?.forEach((chart, index) => {
    const el = chartEls.get(`chart_${index}`);
    if (el) {
      renderSpecChart(`chart_${index}`, chart);
    }
  });
};

const disposeCharts = () => {
  chartInstances.forEach((instance) => instance.dispose());
  chartInstances.clear();
};

const getOrCreateInstance = (key: string, el: HTMLElement) => {
  let instance = chartInstances.get(key);
  if (!instance || instance.isDisposed()) {
    instance = echarts.init(el);
    chartInstances.set(key, instance);
  }
  return instance;
};

/** 趋势预测图：历史数据实线 + 预测数据虚线 + 预测区间标注 */
const renderTrendChart = (trendPoints: PredictionTrendPoint[]) => {
  const instance = getOrCreateInstance('trend', trendChartRef.value!);

  const labels = trendPoints.map((p) => p.label);
  const firstForecastIdx = trendPoints.findIndex((p) => p.isForecast);
  const hasForecast = firstForecastIdx > -1;

  // 历史序列（预测位置为 null）
  const historyData = trendPoints.map((p) => (p.isForecast ? null : p.value));
  // 预测序列（从最后一个历史点开始连线，保证两段衔接）
  const forecastData = trendPoints.map((p, i) => {
    if (!p.isForecast) {
      return i === firstForecastIdx - 1 ? p.value : null;
    }
    return p.value;
  });

  const series: any[] = [
    {
      name: '历史数据',
      type: 'line',
      data: historyData,
      smooth: true,
      symbolSize: 7,
      lineStyle: { width: 3, color: '#1677ff' },
      itemStyle: { color: '#1677ff', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(22, 119, 255, 0.18)' },
          { offset: 1, color: 'rgba(22, 119, 255, 0.02)' }
        ])
      }
    }
  ];

  if (hasForecast) {
    series.push({
      name: '预测数据',
      type: 'line',
      data: forecastData,
      smooth: true,
      symbolSize: 7,
      lineStyle: { width: 3, type: 'dashed', color: '#fa8c16' },
      itemStyle: { color: '#fa8c16', borderColor: '#fff', borderWidth: 2 },
      markArea: {
        silent: true,
        itemStyle: { color: 'rgba(250, 140, 22, 0.06)' },
        label: {
          show: true,
          position: 'insideTopRight',
          color: '#fa8c16',
          fontSize: 12,
          formatter: '预测区间'
        },
        data: [[{ xAxis: labels[firstForecastIdx] }, { xAxis: labels[labels.length - 1] }]]
      }
    });
  }

  instance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '12%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: labels },
    yAxis: { type: 'value' },
    series
  });
};

/** 图表解释卡片：line / bar / pie */
const renderSpecChart = (key: string, chart: ChartSpec) => {
  const el = chartEls.get(key);
  if (!el) {return;}
  const instance = getOrCreateInstance(key, el);

  const labels = chart.data.map((p) => p.label);
  const values = chart.data.map((p) => p.value);

  let option: any = null;
  if (chart.chartType === 'pie') {
    option = {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { top: '2%', left: 'center' },
      series: [
        {
          name: chart.title || '占比',
          type: 'pie',
          radius: ['40%', '68%'],
          center: ['50%', '54%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          data: chart.data.map((p) => ({ name: p.label, value: p.value }))
        }
      ]
    };
  } else if (chart.chartType === 'bar') {
    option = {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: { type: 'category', data: labels, axisTick: { alignWithLabel: true } },
      yAxis: { type: 'value' },
      series: [
        {
          name: chart.yField || '',
          type: 'bar',
          barWidth: '55%',
          itemStyle: {
            borderRadius: [6, 6, 0, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#1677ff' },
              { offset: 1, color: '#69b1ff' }
            ])
          },
          data: values
        }
      ]
    };
  } else {
    // line
    option = {
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: labels },
      yAxis: { type: 'value' },
      series: [
        {
          name: chart.yField || '',
          type: 'line',
          smooth: true,
          symbolSize: 7,
          data: values,
          lineStyle: {
            width: 3,
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#1677ff' },
              { offset: 1, color: '#36cfc9' }
            ])
          },
          itemStyle: { color: '#1677ff', borderColor: '#fff', borderWidth: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(22, 119, 255, 0.15)' },
              { offset: 1, color: 'rgba(22, 119, 255, 0.01)' }
            ])
          }
        }
      ]
    };
  }

  instance.setOption(option);
};

// 报告数据变化或抽屉打开时渲染图表
watch(
  [() => props.report, () => props.open],
  () => {
    if (props.open && props.report && !props.loading) {
      nextTick(() => {
        // 等待 DOM 渲染完成后再初始化图表
        setTimeout(renderCharts, 100);
      });
    }
  }
);

// 抽屉关闭时释放图表实例
watch(
  () => props.open,
  (open) => {
    if (!open) {
      disposeCharts();
    }
  }
);

onUnmounted(() => {
  disposeCharts();
});

// ============ 交互 ============
const handleClose = () => {
  emit('close');
};

/** 下载 PDF：调整图表尺寸后调用浏览器打印 */
const handleDownloadPdf = () => {
  chartInstances.forEach((instance) => {
    if (!instance.isDisposed()) {
      instance.resize();
    }
  });
  message.info('正在打开打印预览，请在打印对话框中选择"另存为 PDF"');
  setTimeout(() => {
    window.print();
  }, 300);
};
</script>

<style lang="scss" scoped>
.report-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 8px;

    .toolbar-icon {
      font-size: 18px;
      color: #1677ff;
    }

    .toolbar-title {
      font-size: 16px;
      font-weight: 600;
      color: #1f2329;
    }
  }

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.report-body {
  height: 100%;
  overflow-y: auto;
  background: #f5f7fa;
}

/* ============ 加载态 ============ */
.report-loading {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;

  .loading-card {
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    padding: 56px 64px;
    text-align: center;
    max-width: 560px;
    width: 100%;

    .loading-title {
      margin-top: 24px;
      font-size: 20px;
      font-weight: 600;
      color: #1f2329;
    }

    .loading-desc {
      margin-top: 8px;
      font-size: 14px;
      color: #86909c;
    }

    .loading-progress {
      margin-top: 28px;
    }

    .loading-message {
      margin-top: 12px;
      font-size: 13px;
      color: #4e5969;
      min-height: 20px;
    }
  }
}

/* ============ 错误/空态 ============ */
.report-error {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;

  .error-card {
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
    padding: 56px 64px;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;

    .error-icon {
      font-size: 48px;
      color: #f53f3f;
    }

    .error-title {
      font-size: 18px;
      font-weight: 600;
      color: #1f2329;
    }

    .error-desc {
      font-size: 14px;
      color: #86909c;
      max-width: 420px;
      word-break: break-all;
    }
  }
}

/* ============ 报告头部 ============ */
.report-header {
  background: linear-gradient(135deg, #1677ff 0%, #0958d9 60%, #003eb3 100%);
  padding: 56px 48px 44px;
  color: #fff;
  position: relative;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    right: -80px;
    top: -80px;
    width: 320px;
    height: 320px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.08);
  }

  &::before {
    content: '';
    position: absolute;
    right: 60px;
    bottom: -120px;
    width: 240px;
    height: 240px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.05);
  }

  .header-badge {
    display: inline-block;
    font-size: 11px;
    letter-spacing: 4px;
    font-weight: 600;
    color: rgba(255, 255, 255, 0.85);
    border: 1px solid rgba(255, 255, 255, 0.4);
    border-radius: 999px;
    padding: 4px 14px;
    margin-bottom: 18px;
  }

  .header-title {
    margin: 0;
    font-size: 30px;
    font-weight: 700;
    line-height: 1.3;
    text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }

  .header-question {
    margin-top: 14px;
    font-size: 15px;
    color: rgba(255, 255, 255, 0.9);
    line-height: 1.6;
    max-width: 860px;
  }

  .header-meta {
    margin-top: 26px;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
    font-size: 13px;
    color: rgba(255, 255, 255, 0.85);

    .meta-item {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .meta-icon {
      font-size: 14px;
    }

    .meta-divider {
      background: rgba(255, 255, 255, 0.35);
    }
  }
}

/* ============ 报告主体 ============ */
.report-content {
  max-width: 1080px;
  margin: 0 auto;
  padding: 32px 48px 60px;
}

.report-section {
  margin-bottom: 40px;

  .section-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 19px;
    font-weight: 600;
    color: #1f2329;
    margin-bottom: 18px;

    .section-bar {
      display: inline-block;
      width: 4px;
      height: 20px;
      border-radius: 2px;
      background: linear-gradient(180deg, #1677ff, #69b1ff);
    }
  }
}

.summary-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  border-left: 4px solid #1677ff;
  padding: 24px 28px;
}

/* ============ 关键发现 ============ */
.findings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;

  .finding-card {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    padding: 20px 22px;
    display: flex;
    gap: 14px;
    align-items: flex-start;
    transition: box-shadow 0.2s;

    &:hover {
      box-shadow: 0 4px 16px rgba(22, 119, 255, 0.12);
    }

    .finding-index {
      flex-shrink: 0;
      font-size: 24px;
      font-weight: 700;
      line-height: 1;
      color: #1677ff;
      opacity: 0.35;
      font-family: 'DIN Alternate', 'Segoe UI', sans-serif;
    }

    .finding-text {
      font-size: 14px;
      line-height: 1.7;
      color: #4e5969;
    }
  }
}

/* ============ 归因分析章节 ============ */
.attribution-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 24px 28px;
  margin-bottom: 16px;

  .attribution-card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;
    flex-wrap: wrap;

    .attribution-card-title {
      font-size: 16px;
      font-weight: 600;
      color: #1f2329;
    }

    .angle-tag {
      border-radius: 999px;
    }
  }
}

/* ============ 数据预测 ============ */
.prediction-fallback {
  background: #fffbe6;
  border: 1px solid #ffe58f;
  border-radius: 12px;
  padding: 18px 22px;
  font-size: 14px;
  color: #874d00;
  display: flex;
  align-items: center;
  gap: 10px;

  .fallback-icon {
    font-size: 18px;
    color: #faad14;
  }
}

.chart-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 24px 28px;
  margin-bottom: 16px;

  .chart-card-title {
    font-size: 15px;
    font-weight: 600;
    color: #1f2329;
    margin-bottom: 16px;
  }

  .chart-container {
    width: 100%;
    height: 340px;
  }

  .chart-legend {
    display: flex;
    justify-content: center;
    gap: 24px;
    margin-top: 8px;

    .legend-item {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      color: #86909c;
    }

    .legend-line {
      display: inline-block;
      width: 22px;
      height: 0;
      border-top: 2px solid #1677ff;
    }

    .legend-dashed {
      border-top-style: dashed;
      border-top-color: #fa8c16;
    }
  }

  .chart-explanation {
    margin-top: 16px;
    background: #f0f7ff;
    border-radius: 8px;
    padding: 12px 16px;
    display: flex;
    align-items: flex-start;
    gap: 8px;
    font-size: 13px;
    line-height: 1.7;
    color: #4e5969;

    .explanation-icon {
      color: #1677ff;
      font-size: 15px;
      margin-top: 3px;
    }
  }
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
  margin-bottom: 16px;

  .metric-card {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    padding: 20px 22px;
    transition: box-shadow 0.2s;

    &:hover {
      box-shadow: 0 4px 16px rgba(22, 119, 255, 0.12);
    }

    .metric-name {
      font-size: 13px;
      color: #86909c;
      margin-bottom: 10px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .metric-values {
      display: flex;
      align-items: center;
      gap: 8px;

      .metric-current {
        font-size: 18px;
        font-weight: 600;
        color: #4e5969;
      }

      .metric-arrow {
        font-size: 12px;
        color: #c9cdd4;
      }

      .metric-forecast {
        font-size: 22px;
        font-weight: 700;
        color: #1677ff;
      }
    }

    .metric-change {
      margin-top: 8px;
      font-size: 13px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 4px;

      &.change-up {
        color: #f53f3f;
      }

      &.change-down {
        color: #00b42a;
      }
    }
  }
}

.risk-row {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
  margin-bottom: 16px;

  .confidence-card {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    padding: 22px;
    text-align: center;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 10px;

    .confidence-label {
      font-size: 13px;
      color: #86909c;
    }

    .confidence-value {
      font-size: 32px;
      font-weight: 700;

      &.confidence-high {
        color: #00b42a;
      }

      &.confidence-middle {
        color: #fa8c16;
      }

      &.confidence-low {
        color: #f53f3f;
      }
    }
  }

  .risk-card {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
    border-top: 3px solid #fa8c16;
    padding: 20px 24px;

    .risk-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 15px;
      font-weight: 600;
      color: #1f2329;
      margin-bottom: 12px;

      .risk-icon {
        color: #fa8c16;
      }
    }

    .risk-list {
      margin: 0;
      padding-left: 18px;

      li {
        font-size: 14px;
        line-height: 1.8;
        color: #4e5969;
        margin-bottom: 6px;
      }
    }
  }
}

/* ============ 建议与行动项 ============ */
.recommendation-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  padding: 8px 28px;

  .recommendation-item {
    display: flex;
    gap: 16px;
    align-items: flex-start;
    padding: 18px 0;
    border-bottom: 1px dashed #e5e6eb;

    &:last-child {
      border-bottom: none;
    }

    .recommendation-index {
      flex-shrink: 0;
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: linear-gradient(135deg, #1677ff, #69b1ff);
      color: #fff;
      font-size: 13px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .recommendation-text {
      font-size: 14px;
      line-height: 1.8;
      color: #4e5969;
      padding-top: 2px;
    }
  }
}

/* ============ 页脚 ============ */
.report-footer {
  margin-top: 48px;
  text-align: center;

  .footer-line {
    height: 1px;
    background: linear-gradient(90deg, transparent, #d5d9e0, transparent);
    margin-bottom: 20px;
  }

  .footer-text {
    font-size: 13px;
    color: #86909c;
    line-height: 1.7;
    max-width: 640px;
    margin: 0 auto 8px;
  }

  .footer-meta {
    font-size: 12px;
    color: #c9cdd4;
  }
}

/* ============ Markdown 内容渲染 ============ */
.markdown-content {
  font-size: 14px;
  line-height: 1.8;
  color: #4e5969;
  word-break: break-word;

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4) {
    margin: 14px 0 8px;
    font-weight: 600;
    color: #1f2329;
  }

  :deep(h2) {
    font-size: 17px;
    padding-bottom: 6px;
    border-bottom: 1px solid #f0f0f0;
  }

  :deep(h3) {
    font-size: 15px;
  }

  :deep(p) {
    margin: 0 0 10px;
  }

  :deep(ul),
  :deep(ol) {
    padding-left: 22px;
    margin-bottom: 10px;
  }

  :deep(li) {
    margin-bottom: 4px;
  }

  :deep(strong) {
    color: #1f2329;
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 12px 0;

    th,
    td {
      border: 1px solid #e5e6eb;
      padding: 8px 12px;
      font-size: 13px;
      text-align: left;
    }

    th {
      background: #f7f8fa;
      font-weight: 600;
      color: #1f2329;
    }
  }

  :deep(code) {
    background: rgba(27, 31, 35, 0.05);
    padding: 0.2em 0.4em;
    border-radius: 3px;
    font-size: 85%;
  }

  :deep(blockquote) {
    margin: 10px 0;
    padding: 8px 16px;
    border-left: 3px solid #1677ff;
    background: #f7faff;
    border-radius: 0 6px 6px 0;
    color: #4e5969;
  }
}
</style>

<style lang="scss">
/* ============ 打印样式：仅打印报告内容 ============ */
@media print {
  body * {
    visibility: hidden;
  }

  .report-print-area,
  .report-print-area * {
    visibility: visible;
  }

  .report-print-area {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
  }

  .no-print,
  .ant-drawer-header {
    display: none !important;
  }

  .ant-drawer-content-wrapper {
    width: 100% !important;
  }

  .ant-drawer-body,
  .report-body {
    height: auto !important;
    overflow: visible !important;
  }

  .report-header {
    padding: 32px 32px 28px !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .report-content {
    padding: 24px 32px 40px !important;
  }

  .chart-container {
    height: 300px !important;
  }

  .finding-card,
  .metric-card,
  .chart-card,
  .attribution-card,
  .summary-card,
  .recommendation-card,
  .risk-card,
  .confidence-card {
    box-shadow: none !important;
    border: 1px solid #e5e6eb;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>
