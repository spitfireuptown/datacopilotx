<template>
  <div class="md-editor w-full h-full">
    <div
      class="markdown-content h-full"
      v-html="renderedContent"
    />
    <div v-if="hasTableData" style="text-align: right">
      <div class="chart-toolbar">
        <!-- 添加到仪表盘按钮：置于左侧 -->
        <a-tooltip placement="topLeft" title="添加到仪表盘">
          <PlusSquareOutlined
            class="add-dashboard-btn"
            @click="handleAddToDashboard" />
        </a-tooltip>
        <!-- 图表类型切换按钮：置于右侧，与添加按钮同行 -->
        <div v-show="tableColumns.length>1" class="type-list">
          <div
            v-for="item in typeList" :key="item.name" class="type-item" :class="{active:item.active}"
            @click="handleTypeChange(item)">
            <a-tooltip v-if="item.name==='PieChart'" placement="topLeft" title="饼图">
              <PieChartOutlined
                :style="{fontSize: '16px', color: item.active?'#34c2a0':''}" />
            </a-tooltip>
            <a-tooltip v-if="item.name==='LineChart'" placement="topLeft" title="折线图">
              <LineChartOutlined
                :style="{fontSize: '16px', color: item.active?'#34c2a0':''}" />
            </a-tooltip>
            <a-tooltip v-if="item.name==='BarChart'" placement="topLeft" title="柱状图">
              <BarChartOutlined
                :style="{fontSize: '16px', color: item.active?'#34c2a0':''}" />
            </a-tooltip>
            <a-tooltip v-if="item.name==='Table'" placement="topLeft" title="表格">
              <TableOutlined :style="{fontSize: '16px', color: item.active?'#34c2a0':''}" />
            </a-tooltip>
          </div>
        </div>
      </div>
      <a-table
        v-show="chartType=='Table'"
        :columns="tableColumns"
        :data-source="tableData"
        :pagination="false"
        :scroll="{ x: 'max-content', y: 200 }"
        class="mt-4"
      />
      <div>
        <div v-show="chartType!='Table'" ref="chartRef" class="mt-4 chart"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { marked } from 'marked';
import hljs from 'highlight.js';
import 'highlight.js/styles/github-dark.css';
import { computed, ref } from 'vue';
import * as echarts from 'echarts';
import { message } from 'ant-design-vue';

// 定义props
const props = defineProps({
  text: {
    type: String,
    default: ''
  },
  jsonData: {
    type: [Object, Array],
    default: null
  },
  needScroll: {
    type: Boolean,
    default: false
  },
  questionId: {
    type: String,
    default: ''
  },
  questionText: {
    type: String,
    default: ''
  },
  sqlText: {
    type: String,
    default: ''
  }
});

const emit = defineEmits<{
  addToDashboard: [data: { chartType: string; chartData: any; questionId: string; questionText: string; sqlText: string }];
}>();

// 配置marked使用highlight.js进行代码高亮
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value;
    }
    return hljs.highlightAuto(code).value;
  },
  breaks: true,
  gfm: true,
  headerIds: true
});

// 计算渲染内容
const renderedContent = computed(() => {
  let content = '';
  // 渲染text内容
  if (props.text) {
    content += marked.parse(props.text);
  }

  return content;
});

// 判断是否有表格数据
const hasTableData = computed(() => {
  if (props.jsonData) {
    return true;
  }
  return false;
});

// 处理表格列数据
const tableColumns = computed(() => {
  if (!props.jsonData || typeof props.jsonData !== 'object') {
    return [];
  }

  // 处理标准表格格式
  if ('columns' in props.jsonData && 'data' in props.jsonData &&
    Array.isArray(props.jsonData.columns) && Array.isArray(props.jsonData.data)) {
    return props.jsonData.columns.map(col => ({
      title: col.name,
      dataIndex: col.name,
      key: col.name
    }));
  }

  // 处理数组对象格式
  if (Array.isArray(props.jsonData) && props.jsonData.length > 0 && typeof props.jsonData[0] === 'object') {
    const firstItem = props.jsonData[0];
    const keys = Object.keys(firstItem);
    return keys.map(key => ({
      title: key,
      dataIndex: key,
      key: key
    }));
  }

  return [];
});

// 处理表格数据
const tableData = computed(() => {
  if (!props.jsonData || typeof props.jsonData !== 'object') {
    return [];
  }

  // 处理标准表格格式
  if ('columns' in props.jsonData && 'data' in props.jsonData &&
    Array.isArray(props.jsonData.columns) && Array.isArray(props.jsonData.data)) {
    return props.jsonData.data;
  }

  // 处理数组对象格式
  if (Array.isArray(props.jsonData) && props.jsonData.length > 0 && typeof props.jsonData[0] === 'object') {
    return props.jsonData;
  }

  return [];
});

// 处理图表数据
const chartXData = computed(() => {
  if (!props.jsonData || typeof props.jsonData !== 'object') {
    return [];
  }
  if ('columns' in props.jsonData && 'data' in props.jsonData &&
    Array.isArray(props.jsonData.columns) && Array.isArray(props.jsonData.data)) {
    return props.jsonData.data.map(col => {
      return col[props.jsonData.columns[0].name]
    });
  }
  return [];
});
const chartYData = computed(() => {
  if (!props.jsonData || typeof props.jsonData !== 'object') {
    return [];
  }

  if ('columns' in props.jsonData && 'data' in props.jsonData &&
    Array.isArray(props.jsonData.columns) && Array.isArray(props.jsonData.data)) {
    return props.jsonData.data.map(col => {
      // TODO: 此处需要根据实际数据结构调整，这里仅为示例（多条）
      return col[props.jsonData.columns[1].name]
    });
  }
  return [];
});

/**
 * 图表初始化
 */
const chartRef = ref();
const chartContainer = ref();

/**
 * 图表初始化函数，根据数据类型选择不同的图表配置并渲染。
 */
const echartsInit = () => {
  if (!chartRef.value) {
    return;
  }
  chartContainer.value = echarts.init(chartRef.value);
  const pieChartData = chartXData.value.map((item,index) => {
   return {
       value: chartYData.value[index],
       name: item
   }
  });
  const typeOptions:any ={
    'PieChart': {
      tooltip: {
        trigger: 'item'
      },
      legend: {
        top: '5%',
        left: 'center'
      },
      series: [
        {
          name: '问数结果',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          labelLine: {
            show: false
          },
          data: pieChartData
        }
      ]
    },
    'LineChart': {
      xAxis: {
        type: 'category',
        data: chartXData.value
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          data: chartYData.value,
          type: 'line',
          smooth: true
        }
      ]
    }
      /*{
      tooltip: {
        trigger: 'axis'
      },
      xAxis: [{
        type: 'category',
        data: chartXData.value,
        axisLine: {
          lineStyle: {
            color: '#999'
          }
        },
      }],
      yAxis: [{
        type: 'value',
        splitNumber: 4,
        splitLine: {
          lineStyle: {
            type: 'dashed',
            color: '#DDD'
          }
        },
        axisLine: {
          show: true,
          lineStyle: {
            color: '#333'
          },
        },
        nameTextStyle: {
          color: '#999'
        },
        splitArea: {
          show: false
        }
      }],
      series: [{
        name: props.jsonData.columns[1].name,
        type: 'line',
        data: chartYData.value,
        lineStyle: {
          normal: {
            width: 8,
            color: {
              type: 'linear',

              colorStops: [{
                offset: 0,
                color: '#A9F387' // 0% 处的颜色
              }, {
                offset: 1,
                color: '#48D8BF' // 100% 处的颜色
              }],
              globalCoord: false // 缺省为 false
            },
            shadowColor: 'rgba(72,216,191, 0.3)',
            shadowBlur: 10,
            shadowOffsetY: 20
          }
        },
        itemStyle: {
          normal: {
            color: '#fff',
            borderWidth: 10,
            shadowColor: 'rgba(72,216,191, 0.3)',
            shadowBlur: 100,
            borderColor: '#A9F387'
          }
        },
        smooth: true
      }]
    }*/,
    'BarChart':  {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          data: chartXData.value,
          axisTick: {
            alignWithLabel: true
          }
        }
      ],
      yAxis: [
        {
          type: 'value'
        }
      ],
      series: [
        {
          name: 'Direct',
          type: 'bar',
          barWidth: '60%',
          data: chartYData.value
        }
      ]
    }
    /*{
      backgroundColor:"rgba(17,28,78,0)",
      color: ['#3398DB'],
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'line',
          lineStyle: {
            opacity: 0
          }
        },
        formatter: function(prams) {
          if (prams[0].data === 0) {
            return "0"
          } else {
            return "" + prams[0].data
          }
        }
      },
      legend: {
        data: [],
        show: false
      },
      grid: {
        left: '0%',
        right: '0%',
        bottom: '5%',
        top: '7%',
        height: '85%',
        containLabel: true,
        z: 22
      },
      xAxis: [{
        type: 'category',
        gridIndex: 0,
        data: chartXData.value,
        axisTick: {
          alignWithLabel: true
        },
        axisLine: {
          lineStyle: {
            color: '#0c3b71'
          }
        },
        axisLabel: {
          show: true,
          color: 'rgb(90,90,90)',
          fontSize:16
        }
      }],
      yAxis: [{
        type: 'value',
        gridIndex: 0,
        splitLine: {
          show: false
        },
        axisTick: {
          show: false
        },
        axisLine: {
          lineStyle: {
            color: '#0c3b71'
          }
        },
        axisLabel: {
          color: 'rgb(90,90,90)',
          formatter: '{value}'
        }
      },
        {
          type: 'value',
          gridIndex: 0,
          splitNumber: 12,
          splitLine: {
            show: false
          },
          axisLine: {
            show: false
          },
          axisTick: {
            show: false
          },
          axisLabel: {
            show: false
          },
          splitArea: {
            show: true,
            areaStyle: {
              color: ['rgba(250,250,250,0.0)', 'rgba(250,250,250,0.05)']
            }
          }
        }
      ],
      series: [{
        name: '合格率',
        type: 'bar',
        barWidth: '30%',
        xAxisIndex: 0,
        yAxisIndex: 0,
        itemStyle: {
          normal: {
            barBorderRadius: 30,
            color: new echarts.graphic.LinearGradient(
              0, 0, 0, 1, [{
                offset: 0,
                color: '#00feff'
              },
                {
                  offset: 0.5,
                  color: '#027eff'
                },
                {
                  offset: 1,
                  color: '#0286ff'
                }
              ]
            )
          }
        },
        data: chartYData.value,
        zlevel: 11

      },
        {
          name: '背景',
          type: 'bar',
          barWidth: '50%',
          xAxisIndex: 0,
          yAxisIndex: 1,
          barGap: '-135%',
          data: [100, 100, 100, 100, 100, 100, 100],
          itemStyle: {
            normal: {
              color: 'rgba(255,255,255,0.1)'
            }
          },
          zlevel: 9
        },

      ]
    }*/,
  }

  chartContainer.value.setOption(typeOptions[chartType.value]);
};
const chartType = ref('Table');
const typeList = ref([
  {
    name: 'PieChart',
    active: false
  },
  {
    name: 'BarChart',
    active: false
  },
  {
    name: 'LineChart',
    active: false
  },
  {
    name: 'Table',
    active: true
  }
]);

/**
 * 图表类型切换
 * @param item
 */
const handleTypeChange = (item) => {
  if(chartContainer.value) {chartContainer.value.clear()}
  item.active = true;
  typeList.value.forEach(i => {
    if (i.name !== item.name) {
      i.active = false;
    }
  });
  chartType.value = item.name;
  if(item.name === 'Table'){
    return;
  }
  echartsInit();
};

/**
 * 添加到仪表盘
 */
const handleAddToDashboard = () => {
  emit('addToDashboard', {
    chartType: chartType.value,
    chartData: props.jsonData,
    questionId: props.questionId || '',
    questionText: props.questionText || '',
    sqlText: props.sqlText || '',
  });
};
</script>

<style lang="scss">
.markdown-content {
  height: auto;
  min-height: 25px;
  max-height: 600px;
  overflow-y: auto;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-1);
  word-break: break-word;
  background: unset;

  pre {
    padding: 12px;
    margin: 8px 0;
    overflow-x: auto;
    color: #fff;
    background-color: #24292e;
    border-radius: 6px;
  }

  code {
    padding: 0.2em 0.4em;
    font-size: 85%;
    color: var(--text-1);
    background-color: var(--bg-subtle);
    border-radius: 3px;
  }

  pre code {
    padding: 0;
    background-color: transparent;
  }

  h1, h2, h3, h4, h5, h6 {
    margin-top: 16px;
    margin-bottom: 8px;
    font-weight: 600;
    line-height: 1.25;
    color: var(--text-1);
  }

  ul, ol {
    padding-left: 2em;
    margin-bottom: 12px;
  }

  a {
    color: var(--brand-primary);
    text-decoration: none;
  }

  a:hover {
    color: var(--brand-primary-hover);
    text-decoration: underline;
  }

  blockquote {
    color: var(--text-2);
    border-left: 3px solid var(--brand-primary);
  }

  table {
    border-collapse: collapse;

    th,
    td {
      padding: 6px 10px;
      border: 1px solid var(--border-color);
    }

    th {
      background: var(--bg-subtle);
    }
  }
}

.md-editor {
  height: auto;
  min-height: 25px;
  max-height: 600px;
}

:deep(.ant-bubble-content.ant-bubble-content-filled) {
  height: auto;
  min-height: 32px;
  max-height: 600px;
}

// Ant Design表格样式
:deep(.ant-table) {
  margin-top: 16px;
  overflow: hidden;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
}

:deep(.ant-table-thead > tr > th) {
  font-weight: 600;
  color: var(--text-1);
  background-color: var(--bg-subtle);
  border-bottom: 1px solid var(--border-color);
}

:deep(.ant-table-tbody > tr) {
  transition: all 0.3s;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background-color: var(--bg-hover);
}

:deep(.ant-table-tbody > tr.ant-table-row-even > td) {
  background-color: var(--bg-subtle);
}

/* 工具栏：左侧添加到仪表盘按钮 + 右侧图表类型切换按钮，同一行 */
.chart-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px;
}

.type-list {
  display: flex;
  flex-direction: row-reverse;

  .type-item {
    padding: 2px 5px;
    margin: 0 5px;
    cursor: pointer;
    border-radius: 5px;

    &:hover {
      background-color: var(--bg-hover);
    }
  }

  .active {
    background-color: var(--bg-hover);
  }
}

.add-dashboard-btn {
  padding: 2px 5px;
  font-size: 16px;
  color: var(--text-3);
  cursor: pointer;
  border-radius: 5px;
  transition: all 0.2s;

  &:hover {
    color: var(--brand-primary);
    background-color: var(--bg-hover);
  }
}

.chart{
  width: 73vw;
  max-width: 100%;
  height: 350px;
  margin: 0 auto;
}
</style>