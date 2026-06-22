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
import { UndoOutlined } from '@ant-design/icons-vue';
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

const emit = defineEmits(['newChat', 'regenerate']);

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
    messageItemList.value = newMessage?.map(({ id, message, status, jsonData }) => {
      // 处理message为null的情况
      const currentMessage = message || '';
      // 处理jsonData为null的情况
      let processedJsonData = jsonData;
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

      console.log('message', currentMessage)
      console.log('processedJsonData', processedJsonData)
      return {
        key: id,
        role: status === 'local' ? 'local' : 'ai',
        content: lastIndex > -1 ? currentMessage.substring(0, lastIndex + 5) : currentMessage,
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
  console.log('scrollToBottom 被调用');
  
  const doScroll = () => {
    console.log('开始执行滚动');
    
    // 方式1：找到最后一个气泡元素并滚动到视图
    const lastBubble = bubbleListContentRef.value?.querySelector('.bubble-item-wrapper:last-child');
    console.log('lastBubble:', lastBubble);
    
    if (lastBubble) {
      lastBubble.scrollIntoView({ behavior: 'smooth', block: 'end' });
      console.log('使用 scrollIntoView 滚动');
      return;
    }
    
    // 方式2：尝试找到外层的 .content-wrap 滚动容器
    const contentWrap = document.querySelector('.content-wrap');
    console.log('contentWrap:', contentWrap, contentWrap?.scrollHeight);
    
    if (contentWrap) {
      contentWrap.scrollTop = contentWrap.scrollHeight;
      console.log('content-wrap 滚动:', contentWrap.scrollTop, '/', contentWrap.scrollHeight);
      return;
    }
    
    // 方式3：尝试操作 SimpleBar 容器
    const scrollContainer = bubbleListContentRef.value?.querySelector('.simplebar-scroll-content');
    console.log('scrollContainer:', scrollContainer, scrollContainer?.scrollHeight);
    
    if (scrollContainer) {
      scrollContainer.scrollTop = scrollContainer.scrollHeight;
      console.log('SimpleBar 滚动:', scrollContainer.scrollTop, '/', scrollContainer.scrollHeight);
      return;
    }
    
    // 方式4：直接操作容器
    if (bubbleListContentRef.value) {
      bubbleListContentRef.value.scrollTop = bubbleListContentRef.value.scrollHeight;
      console.log('直接滚动:', bubbleListContentRef.value.scrollTop, '/', bubbleListContentRef.value.scrollHeight);
    }
  };
  
  // 增加延迟时间，确保DOM完全渲染
  nextTick(() => {
    setTimeout(doScroll, 500);
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
