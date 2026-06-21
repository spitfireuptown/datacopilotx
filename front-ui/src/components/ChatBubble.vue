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
      <BubbleList :roles="roles" :items="messageItemList" />

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
import MdPreview from '@/components/MdPreview.vue';
import { MessageItem } from '@/dataTypes/chatType';
import SimpleBar from 'simplebar';
import { message as Message } from 'ant-design-vue';

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

const emit = defineEmits(['newChat']);

// 新对话
const newChat = () => {
  messageItemList.value = [];
  emit('newChat');
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
    messageItemList.value = newMessage?.map(({ id, message, status, jsonData }) => {
      // 处理jsonData为null的情况
      let processedJsonData = jsonData;
      const startIndex = message.indexOf('问数结果:');
      if (startIndex !== -1) {
        // 提取'问数结果:'到最后的内容
        const extractedData = message.substring(startIndex + 5);
        console.log('extractedData', extractedData)
        // 检查extractedData是否为空，避免解析空字符串
        if (extractedData && extractedData.trim()) {
          try {
            processedJsonData = JSON.parse(extractedData);
          } catch (error) {
            console.error('解析响应失败:', error);
            Message.error('问数结果解析失败，请检查返回数据格式');
          }
        }
      }

      console.log('message', message)
      console.log('processedJsonData', processedJsonData)
      return {
        key: id,
        role: status === 'local' ? 'local' : 'ai',
        content: startIndex > -1 ? message.substring(0, startIndex + 5) : message,
        // @ts-expect-error-next-line 暂时忽略content类型
        messageRender: (content) =>
          h(MdPreview, {
            text: content,
            needScroll: false,
            jsonData: processedJsonData
          })
      };
    }) || [];

    scrollToBottom();
  },
  { immediate: true }
);

/** message变化时自动滚动到底部 */
let simpleBarInstance: SimpleBar | null = null;
const bubbleListContentRef = ref<HTMLElement | null>(null);

function scrollToBottom() {
  nextTick(() => {
    const scrollElement = simpleBarInstance?.getScrollElement();
    if (scrollElement) {
      scrollElement.scrollTo({
        top: scrollElement.scrollHeight,
        behavior: 'smooth'
      });
    }
  });
}

onMounted(() => {
  if (bubbleListContentRef.value) {
    simpleBarInstance = new SimpleBar(bubbleListContentRef.value);
  }
});

onUnmounted(() => {
  if (simpleBarInstance) {
    simpleBarInstance.unMount();
    simpleBarInstance = null;
  }
});
</script>

<style lang="scss" scoped>
.chat-bubble-container {
  position: relative;
  height: 100%;
}

.bubble-list {
  height: 100%;
  padding: 80px 30px 20px 30px; /* 为固定的header-bar留出空间 */
}

.active-template {
  border-color: #1677ff;
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
