<template>
  <div class="chat-bubble-container">
    <!-- 使用fixed定位确保始终固定在顶部 -->
    <div class="header-bar">
      <a-button type="primary" class="new-chat-btn" @click="newChat">
        <template #icon>
          <CommentOutlined />
        </template>
        新对话
      </a-button>
      
      <!-- 对话名称居中显示 -->
      <div class="chat-title-container">
        <h2 v-if="chatTitle" class="chat-title">{{ chatTitle }}</h2>
        <h2 v-else class="chat-title">新对话</h2>
      </div>
      
      <!-- 占位元素，确保标题完美居中 -->
      <div class="header-placeholder"></div>
    </div>
    
    <div ref="bubbleListContentRef" class="bubble-list">
      <div v-for="item in messageItemList" :key="item.key" class="bubble-item-wrapper">
        <BubbleList :roles="roles" :items="[item]" />

        <!-- 用户发送的气泡下面显示重新生成按钮 -->
        <div v-if="item.role === 'local'" class="regenerate-btn-container">
          <a-button
            type="text"
            class="regenerate-btn"
            @click="handleRegenerate(item.content)"
          >
            <template #icon>
              <UndoOutlined />
            </template>
            重新生成
          </a-button>
        </div>

        <!-- AI回答的气泡下面显示归因分析按钮 -->
        <div v-if="item.role === 'ai'" class="attribution-btn-container">
          <a-button
            type="text"
            class="attribution-btn"
            @click="handleAttribution(item)"
          >
            <template #icon>
              <BarChartOutlined />
            </template>
            归因分析
          </a-button>
          <!-- 归因分析报告气泡：显示下载按钮 -->
          <a-button
            v-if="String(item.key).startsWith('attr_') && item.content"
            type="text"
            class="attribution-btn"
            @click="handleDownloadReport(item)"
          >
            <template #icon>
              <DownloadOutlined />
            </template>
            下载报告
          </a-button>
        </div>
      </div>

      <Bubble
        v-if="loading"
        :loading="loading"
        :avatar="roles.ai.avatar"
        :placement="roles.ai.placement"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { Bubble, BubbleList } from 'ant-design-x-vue';
import { Avatar } from 'ant-design-vue';
import { UndoOutlined, BarChartOutlined, DownloadOutlined } from '@ant-design/icons-vue';
import MdPreview from '@/components/MdPreview.vue';
import { MessageItem } from '@/dataTypes/chatType';
import { message as Message } from 'ant-design-vue';
import { saveDashboardChart, getDashboardList, createDashboard } from '@/api/dashboard';

const props = defineProps({
  messages: {
    type: Array as PropType<MessageItem[]>,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  // 添加对话标题prop
  chatTitle: {
    type: String,
    default: ''
  }
});

const emit = defineEmits(['newChat', 'regenerate', 'attribution']);

// 点击归因分析按钮 —— 在对话框内渲染归因分析报告
const handleAttribution = (item: any) => {
  const questionId = String(item.key || '').replace('_answer', '');
  const questionText = item.questionText || '';
  emit('attribution', { questionId, question: questionText });
};

// 下载归因分析报告
const handleDownloadReport = (item: any) => {
  if (!item.content) {
    Message.warning('暂无可下载的报告内容');
    return;
  }
  try {
    const questionText = (item.questionText || '归因分析').substring(0, 20).replace(/[\\/:*?"<>|]/g, '_');
    const pad = (n: number) => String(n).padStart(2, '0');
    const now = new Date();
    const ts = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}${pad(now.getHours())}${pad(now.getMinutes())}`;
    const fileName = `归因分析报告_${questionText}_${ts}.md`;
    const blob = new Blob([item.content], { type: 'text/markdown;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    Message.success('报告已下载');
  } catch (error) {
    Message.error('下载失败');
  }
};

// 暴露滚动方法供父组件调用
defineExpose({
  scrollToBottom
});

// 新对话
const newChat = () => {
  messageItemList.value = [];
  emit('newChat');
};

// 重新生成
const handleRegenerate = (content: string) => {
  emit('regenerate', content);
};

// 渲染对话
const roles: (typeof Bubble.List)['roles'] = {
  ai: {
    placement: 'start',
    avatar: {
      icon: h(
        Avatar,
        {
          src: '/images/avatar.png',
          style: {
            width: '30px',
            height: '30px'
          }
        }
      ),
      style: {
        background: 'unset'
      }
    }
  },
  local: {
    placement: 'end',
    avatar: {
      icon: h(
        Avatar,
        {
          src: '/images/user.png',
          style: {
            width: '30px',
            height: '30px'
          }
        },
        () => 'user'
      ),
      style: {
        background: 'unset'
      }
    }
  }
};

const messageItemList = ref<(typeof Bubble.List)['items']>([]);

watch(
  () => props.messages,
  (newMessage) => {
    // 更新为最新的消息列表
    messageItemList.value = newMessage?.map(({ id, message, status, jsonData }, index) => {
      // 处理message为null的情况
      const currentMessage = message || '';
      // 处理jsonData为null的情况
      let processedJsonData = jsonData;
      
      // 提取SQL文本（在"问数结果:"之前的部分中查找SQL）
      let extractedSql = '';
      const resultIndex = currentMessage.lastIndexOf('问数结果:');
      if (resultIndex !== -1) {
        const beforeResult = currentMessage.substring(0, resultIndex);
        const sqlMatch = beforeResult.match(/SQL[查询：:]\s*([\s\S]*?)(?=问数结果:|$)/i);
        if (sqlMatch) {
          extractedSql = sqlMatch[1].trim();
        }
      }
      
      // 找到最后一个"问数结果:"（处理重试导致的重复内容，最后一次执行通常是成功的）
      const lastIndex = currentMessage.lastIndexOf('问数结果:');
      if (lastIndex !== -1) {
        // 提取最后一个'问数结果:'后面的内容
        const afterLabel = currentMessage.substring(lastIndex + 5);
        // 只取第一个完整的JSON对象
        const jsonStart = afterLabel.indexOf('{');
        if (jsonStart !== -1) {
          // 找到匹配的结束花括号（处理嵌套JSON）
          let braceCount = 0;
          let jsonEnd = jsonStart;
          for (let i = jsonStart; i < afterLabel.length; i++) {
            if (afterLabel[i] === '{') {braceCount++;}
            if (afterLabel[i] === '}') {braceCount--;}
            if (braceCount === 0) {
              jsonEnd = i + 1;
              break;
            }
          }
          
          console.log('afterLabel', afterLabel)
          if (jsonEnd > jsonStart) {
            const extractedData = afterLabel.substring(jsonStart, jsonEnd);
            console.log('extractedData', extractedData)
            try {
              processedJsonData = JSON.parse(extractedData);
            } catch (error) {
              console.error('解析响应失败:', error);
              Message.error('问数结果解析失败，请检查返回数据格式');
            }
          }
        }
      }

      // 提取问题ID和问题文本（从前一条本地消息获取）
      const questionId = id?.replace('_answer', '') || '';
      let questionText = '';
      if (status !== 'local' && index > 0) {
        const prevMsg = newMessage[index - 1];
        if (prevMsg && prevMsg.status === 'local') {
          questionText = prevMsg.message || '';
        }
      }

      console.log('message', currentMessage)
      console.log('processedJsonData', processedJsonData)
      return {
        key: id,
        role: status === 'local' ? 'local' : 'ai',
        content: lastIndex > -1 ? currentMessage.substring(0, lastIndex + 5) : currentMessage,
        questionText,
        // @ts-expect-error-next-line 暂时忽略content类型
        messageRender: (content) =>
          h(MdPreview, {
            text: content,
            needScroll: false,
            jsonData: processedJsonData,
            questionId,
            questionText,
            sqlText: extractedSql,
            onAddToDashboard: (data: any) => {
              handleAddToDashboard(data);
            },
          })
      };
    }) || [];

    scrollToBottom();
  },
  { immediate: true }
);

/** message变化时自动滚动到底部 */
const bubbleListContentRef = ref<HTMLElement | null>(null);

/**
 * 将图表添加到仪表盘
 */
const handleAddToDashboard = async (data: { chartType: string; chartData: any; questionId: string; questionText: string; sqlText: string }) => {
  try {
    // 获取或创建仪表盘
    let dashboardId: number;
    try {
      const dashboards = await getDashboardList();
      if (dashboards.length > 0) {
        dashboardId = dashboards[0].id!;
      } else {
        const newDash = await createDashboard('默认仪表盘');
        dashboardId = newDash.id!;
      }
    } catch {
      const newDash = await createDashboard('默认仪表盘');
      dashboardId = newDash.id!;
    }

    await saveDashboardChart({
      dashboardId,
      chartName: (data.questionText || '未命名').substring(0, 50),
      chartType: data.chartType,
      chartData: typeof data.chartData === 'string' ? data.chartData : JSON.stringify(data.chartData),
      sqlText: data.sqlText || '',
      question: data.questionText || '',
      questionId: data.questionId || '',
      layoutX: 0,
      layoutY: 0,
      layoutW: 500,
      layoutH: 350,
    });
    Message.success('已添加到仪表盘');
  } catch (error) {
    console.error('添加到仪表盘失败:', error);
    Message.error('添加失败，请稍后重试');
  }
};

function scrollToBottom() {
  const doScroll = () => {
    const lastBubble = bubbleListContentRef.value?.querySelector('.bubble-item-wrapper:last-child');
    if (lastBubble) {
      lastBubble.scrollIntoView({ behavior: 'smooth', block: 'end' });
      return;
    }
    
    const contentWrap = document.querySelector('.content-wrap');
    if (contentWrap) {
      contentWrap.scrollTop = contentWrap.scrollHeight;
      return;
    }
    
    if (bubbleListContentRef.value) {
      bubbleListContentRef.value.scrollTop = bubbleListContentRef.value.scrollHeight;
    }
  };
  
  nextTick(() => {
    setTimeout(doScroll, 300);
  });
}
</script>

<style lang="scss" scoped>
.chat-bubble-container {
  position: relative;
}

.bubble-list {
  padding: 80px 30px 350px 30px;
  overflow: visible;
}

.active-template {
  border-color: #1677ff;
}

/* 气泡项包装器 */
.bubble-item-wrapper {
  margin-bottom: 8px;
}

/* 重新生成按钮容器 */
.regenerate-btn-container {
  display: flex;
  justify-content: flex-end;
  padding-right: 40px;
  margin-top: 4px;
}

/* 重新生成按钮样式 */
.regenerate-btn {
  color: #666;
  font-size: 12px;
  padding: 4px 12px;

  &:hover {
    color: #1890ff;
    background-color: #e6f7ff;
  }
}

/* 归因分析按钮容器 */
.attribution-btn-container {
  display: flex;
  justify-content: flex-start;
  padding-left: 40px;
  margin-top: 4px;
}

/* 归因分析按钮样式 */
.attribution-btn {
  color: #666;
  font-size: 12px;
  padding: 4px 12px;

  &:hover {
    color: #1890ff;
    background-color: #e6f7ff;
  }
}

:deep(.ant-bubble-content),
:deep(.ant-bubble-content-filled) {
  height: auto !important;
  min-height: auto !important;
  max-height: 600px !important;
  overflow-y: auto;
}

/* 固定在顶部的标题栏 */
.header-bar {
  position: fixed;
  top: 0px; /* 从60px改为40px，使标题栏向上移动 */
  left: 360px;
  right: 0;
  z-index: 100;
  background-color: white;
  padding: 10px 30px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 60px;
  box-sizing: border-box;
}

/* 新对话按钮样式 */
.new-chat-btn {
  height: 36px;
}

/* 对话标题容器，实现居中 */
.chat-title-container {
  flex: 1;
  text-align: center;
}

/* 对话标题样式 */
.chat-title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 400px;
  margin: 0 auto;
}

/* 占位元素，确保标题完美居中 */
.header-placeholder {
  width: 90px; /* 与新对话按钮宽度相当 */
}
</style>
