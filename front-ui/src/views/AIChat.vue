<template>
  <div ref="scholarAiRef" class="chat-comp">
    <LeftSidebar />
    <SecondLeftSidebar @new-chat="resetChat" @chat-select="handleChatSelect" />
    <div class="content-wrap">
      <WelcomeIndex v-if="!messages.length" class="mb-4" />
      <ChatBubble
        v-else
        class="bubble-list-wrap mb-4"
        :messages="messages"
        :loading="waitResponse"
        :chat-title="currentChatTitle"
        @new-chat="resetChat"
      />
    </div>
    <div class="sender-wrap">
      <SenderInput
        ref="senderInputRef"
        :external-session-id="currentSessionId"
        @messages-change="handleMessagesChange"
        @loading-change="handleLoading"
      />
    </div>
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
};

// 重置对话
const senderInputRef = ref();
const resetChat = () => {
  waitResponse.value = false;
  senderInputRef.value?.newChat();
  currentChatTitle.value = '';
};

// Import necessary dependencies
import { onMounted, onUnmounted, ref } from 'vue';
import { useDialogueStore } from '@/stores/modules/dialogues';

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
      message: String(item.answer), // 确保是字符串类型
      status: 'ai',
      // 尝试解析answer字段中的JSON数据
      jsonData: parseJsonData(item.result)
    }
  ]).flat();
  
  // 更新消息列表
  messages.value = convertedMessages;
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
  height: calc(100vh - 60px);
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  display: flex;
  overflow: hidden;
  /* 确保左右两侧完全顶边 */
  left: 0;
  right: 0;
  .content-wrap {
    flex: 1;
    //margin-left: 360px; // 为两个侧边栏留出空间（80px + 280px）
    margin-right: 0; // 右侧不留空间
    padding-bottom: 56px; // 为固定定位的 SenderInput 预留空间
    overflow-y: auto;
    .bubble-list-wrap {
      height: 100%;
      margin-left: 0 !important; // 强制左边靠死，移除任何可能的左侧边距
      padding-left: 20px; // 可以根据需要添加一些内边距，让内容不紧贴侧边栏
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
}
</style>
