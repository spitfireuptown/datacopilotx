<template>
  <div ref="scholarAiRef" class="chat-comp">
    <LeftSidebar />
    <SecondLeftSidebar 
      @new-chat="resetChat" 
      @chat-select="handleChatSelect"
      @loading-change="handleHistoryLoading"
      @delete-chat="handleDeleteChat"
    />
    <div class="content-wrap">
      <!-- 历史记录加载遮罩 -->
      <div v-if="historyLoading" class="history-loading-overlay">
        <div class="history-loading-spinner">
          <a-spin size="large" tip="加载对话中..." />
        </div>
      </div>
      
      <WelcomeIndex v-if="!messages.length && !historyLoading" class="mb-4" />
      <ChatBubble
        v-else-if="!historyLoading"
        ref="chatBubbleRef"
        class="bubble-list-wrap"
        :messages="messages"
        :loading="waitResponse"
        :loading-tip="loadingTip"
        :chat-title="currentChatTitle"
        @new-chat="resetChat"
        @regenerate="handleRegenerate"
        @attribution="handleAttribution"
        @report="handleReport"
      />
    </div>
    <div class="sender-wrap">
      <SenderInput
        ref="senderInputRef"
        :external-session-id="currentSessionId"
        @messages-change="handleMessagesChange"
        @loading-change="handleLoading"
        @attribution-progress="handleAttributionProgress"
        @report-loading-change="handleReportLoadingChange"
        @report-progress="handleReportProgress"
        @report-data="handleReportData"
        @report-error="handleReportError"
      />
    </div>

    <!-- 数据报告后台任务浮窗：生成中显示进度（不阻塞页面），完成后作为查看入口 -->
    <div v-if="reportGenerating" class="report-task-float">
      <a-spin size="small" />
      <div class="report-task-info">
        <div class="report-task-title">数据报告生成中</div>
        <div class="report-task-progress" :title="reportProgress">{{ reportProgress }}</div>
      </div>
      <a-button size="small" type="text" danger @click="cancelReport">取消</a-button>
    </div>
    <div
      v-else-if="reportData"
      class="report-task-float report-task-entry"
      title="查看已生成的数据报告"
      @click="openReportDrawer"
    >
      <FileTextOutlined class="report-task-icon" />
      <span>查看数据报告</span>
    </div>

    <!-- 数据报告抽屉：仅用户主动查看时打开 -->
    <DataReportDrawer
      :open="reportDrawerOpen"
      :report="reportData"
      :loading="reportGenerating"
      :progress="reportProgress"
      :error="reportError"
      @close="handleReportDrawerClose"
    />
  </div>
</template>

<script setup lang="ts">
// 监听消息变化
const messages = ref<any[]>([]);
const handleMessagesChange = (newMessages: any[]) => {
  messages.value = newMessages;
};

const waitResponse = ref<boolean>(false);
const handleLoading = (loading: boolean) => {
  waitResponse.value = loading;
  if (!loading) {
    // loading 结束时清空进度提示，避免下次 loading 气泡残留旧文本
    loadingTip.value = '';
  }
};

// loading 气泡的进度提示文本（归因分析各阶段进度等；空则显示默认 loading 动画）
const loadingTip = ref<string>('');
const handleAttributionProgress = (progress: string) => {
  loadingTip.value = progress;
};

// 历史记录加载状态
const historyLoading = ref<boolean>(false);
const handleHistoryLoading = (loading: boolean) => {
  historyLoading.value = loading;
};

const senderInputRef = ref();
const chatBubbleRef = ref();
const resetChat = () => {
  waitResponse.value = false;
  senderInputRef.value?.newChat();
  currentChatTitle.value = '';
  currentSessionId.value = undefined;
  dialogueStore.setSessionId('');
  // 清理后台报告任务状态（newChat 内部已中止请求）
  notification.close(REPORT_READY_NOTIFY_KEY);
  reportData.value = null;
  reportError.value = '';
  reportProgress.value = '';
};

const handleRegenerate = (content: string) => {
  senderInputRef.value?.setQuestion(content);
};

// 归因分析：委托给 SenderInput 执行（复用其 datasetId/modelId/sessionId 状态）
const handleAttribution = ({ questionId, question }: { questionId: string; question: string }) => {
  senderInputRef.value?.triggerAttribution(questionId, question);
};

// ===== 数据报告（后台任务） =====
// 抽屉开关、报告数据、生成状态、进度与错误
// 报告在后台生成，不自动打开全屏抽屉；完成后通过通知提示，用户主动点击才查看
const reportDrawerOpen = ref<boolean>(false);
const reportData = ref<DataReport | null>(null);
const reportGenerating = ref<boolean>(false);
const reportProgress = ref<string>('');
const reportError = ref<string>('');

const REPORT_READY_NOTIFY_KEY = 'report-ready';

// 数据报告：委托给 SenderInput 执行（复用其 datasetId/modelId/sessionId 状态）
const handleReport = ({ questionId, question }: { questionId: string; question: string }) => {
  // 后台启动生成，仅重置状态，不打开抽屉、不阻塞当前页面
  notification.close(REPORT_READY_NOTIFY_KEY);
  reportData.value = null;
  reportError.value = '';
  reportProgress.value = '正在准备生成数据报告...';
  reportGenerating.value = true;
  senderInputRef.value?.triggerReport(questionId, question);
};

const handleReportLoadingChange = (loading: boolean) => {
  reportGenerating.value = loading;
};

const handleReportProgress = (progress: string) => {
  reportProgress.value = progress;
};

const handleReportData = (report: DataReport) => {
  reportData.value = report;
  reportError.value = '';
  // 后台生成完成：通知提示，用户主动点击才打开报告抽屉
  notification.open({
    key: REPORT_READY_NOTIFY_KEY,
    message: '数据报告已生成',
    description: '报告已在后台完成，点击按钮即可查看，不影响当前对话。',
    duration: 0,
    btn: () =>
      h(
        Button,
        {
          type: 'primary',
          size: 'small',
          onClick: () => {
            notification.close(REPORT_READY_NOTIFY_KEY);
            openReportDrawer();
          }
        },
        () => '查看报告'
      )
  });
};

const handleReportError = (errorMsg: string) => {
  reportError.value = errorMsg;
  if (errorMsg.includes('取消')) {
    notification.warning({ message: '数据报告', description: errorMsg });
  } else {
    notification.error({ message: '数据报告生成失败', description: errorMsg });
  }
};

const handleReportDrawerClose = () => {
  reportDrawerOpen.value = false;
};

const openReportDrawer = () => {
  if (reportData.value) {
    reportDrawerOpen.value = true;
  }
};

const cancelReport = () => {
  senderInputRef.value?.cancelReport();
};

const handleDeleteChat = (sessionId: string) => {
  if (currentSessionId.value === sessionId) {
    resetChat();
  }
};

// Import necessary dependencies
import { h, onMounted, onUnmounted, ref } from 'vue';
import { Button, notification } from 'ant-design-vue';
import { FileTextOutlined } from '@ant-design/icons-vue';
import { useDialogueStore } from '@/stores/modules/dialogues';
import DataReportDrawer from '@/components/DataReportDrawer.vue';
import type { DataReport } from '@/api/chat.ts';

// Create store instance
const dialogueStore = useDialogueStore();
// Define currentSessionId
const currentSessionId = ref<string | undefined>(undefined);
// 添加当前对话标题
const currentChatTitle = ref<string>('');

// 监听创建新对话事件
onMounted(() => {
  const handleCreateNewChat = () => {
    resetChat();
  };
  
  window.addEventListener('createNewChat', handleCreateNewChat);

  // 组件卸载时移除事件监听
  onUnmounted(() => {
    window.removeEventListener('createNewChat', handleCreateNewChat);
  });
});

// 添加处理聊天选择的函数
const handleChatSelect = (chatDetail: any[]) => {
  // 确保chatDetail数组有数据
  if (chatDetail && chatDetail.length > 0) {
    // 获取历史对话的sessionId
    currentSessionId.value = chatDetail[0].sessionId;
    console.log('选择的历史对话sessionId:', currentSessionId.value);
    // 将sessionId保存到store中
    dialogueStore.setSessionId(currentSessionId.value);
    
    // 提取对话标题（使用第一条问题的前30个字符作为标题）
    const firstQuestion = chatDetail[0].question || '历史对话';
    currentChatTitle.value = firstQuestion.length > 30 
      ? firstQuestion.substring(0, 30) + '...' 
      : firstQuestion;
      
    // 回显数据集和模型
    const firstItem = chatDetail[0];
    senderInputRef.value?.setDatasetAndModel(firstItem.datasetId, firstItem.modelId, firstItem.dsName, firstItem.modelName);
  }
  
  // 将获取到的聊天详情转换为MessageItem格式并设置到messages中
  const convertedMessages = chatDetail.map((item: any) => [
    {
      id: item.questionId,
      message: String(item.question), // 确保是字符串类型
      status: 'local'
    },
    {
      id: `${item.questionId}_answer`,
      message: item.answer ? String(item.answer) : '暂无回答', // 处理answer为null的情况
      status: 'ai',
      // 尝试解析answer字段中的JSON数据
      jsonData: parseJsonData(item.result)
    }
  ]).flat();
  
  // 更新消息列表
  messages.value = convertedMessages;
  
  // 同步到 SenderInput 内部 useXChat 状态：
  // 否则后续在历史对话中触发归因分析等 setMessages(prev => ...) 操作时，
  // 会基于过期的内部列表重建消息，导致历史消息丢失
  senderInputRef.value?.syncMessages(convertedMessages);
  
  // 消息更新后，使用 requestAnimationFrame 确保滚动时机正确
  nextTick(() => {
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        // 尝试找到所有可能的滚动容器
        const containers = [
          document.querySelector('.content-wrap'),
          document.querySelector('.bubble-list-wrap'),
          document.querySelector('.chat-comp')
        ];
        
        for (const container of containers) {
          if (container) {
            console.log('尝试滚动:', container.className, container.scrollHeight);
            container.scrollTop = container.scrollHeight;
          }
        }
        
        // 额外检查：尝试滚动到最后一个气泡元素
        const lastBubble = document.querySelector('.bubble-item-wrapper:last-child');
        if (lastBubble) {
          console.log('使用 scrollIntoView');
          lastBubble.scrollIntoView({ behavior: 'auto', block: 'end' });
        }
      });
    });
  });
};

// 辅助函数：尝试解析JSON数据
const parseJsonData = (data: string) => {
  try {
    // 尝试将字符串解析为JSON对象
    const parsedData = JSON.parse(data);
    // 验证是否是对象或数组
    if (typeof parsedData === 'object' && parsedData !== null) {
      return parsedData;
    }
    // 如果解析后不是对象或数组，返回null
    return null;
  } catch (error) {
    // 如果解析失败，返回null
    console.log(error);
    return null;
  }
};
</script>

<style lang="scss" scoped>
.chat-comp {
  position: relative;
  width: 100%;
  height: 100vh;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  display: flex;
  overflow: hidden;
  left: 0;
  right: 0;
  .content-wrap {
    flex: 1;
    margin-right: 0;
    overflow-y: auto;
    overflow-x: hidden;
    box-sizing: border-box;
    min-height: 0;
    .bubble-list-wrap {
      margin-left: 0 !important;
      padding-left: 20px;
    }
    .welcome-wrap.mb-4 {
      margin-left: -360px; // 让welcome-wrap靠到左侧
      padding-left: 360px; // 为内容添加内边距，避免被侧边栏遮挡
      width: 100%;
      box-sizing: border-box;
      > * {
        margin-left: 0 !important; // 确保内部所有元素都顶到左侧
        width: 100%;
        box-sizing: border-box;
      }
    }
    
    /* 历史记录加载遮罩 */
    .history-loading-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(255, 255, 255, 0.8);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 100;
    }
    
    .history-loading-spinner {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
    }
  }

  .sender-wrap {
    position: absolute;
    bottom: 0;
    left: 360px; // 与两个侧边栏对齐
    right: 0; // 右侧完全顶边
    z-index: 10;
    width: calc(100% - 360px); // 减去两个侧边栏的宽度
    background: white;
  }

  /* 数据报告后台任务浮窗：不遮挡主体内容 */
  .report-task-float {
    position: fixed;
    right: 24px;
    bottom: 140px; // 高于输入框，避免遮挡
    z-index: 1000;
    display: flex;
    align-items: center;
    gap: 10px;
    max-width: 380px;
    padding: 10px 14px;
    background: #fff;
    border: 1px solid #e5e6eb;
    border-radius: 10px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);

    .report-task-info {
      flex: 1;
      min-width: 0;

      .report-task-title {
        font-size: 13px;
        font-weight: 600;
        color: #1f2329;
      }

      .report-task-progress {
        margin-top: 2px;
        font-size: 12px;
        color: #86909c;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }

  .report-task-entry {
    cursor: pointer;
    font-size: 13px;
    font-weight: 600;
    color: #1677ff;

    .report-task-icon {
      font-size: 16px;
    }

    &:hover {
      box-shadow: 0 4px 16px rgba(22, 119, 255, 0.25);
      border-color: #91caff;
    }
  }
}
</style>
