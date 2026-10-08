<template>
  <div class="sidebar">
    <div class="search-container">
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="搜索"
        size="small"
        class="search-input"
        @search="handleSearch"
      />
    </div>
    
    <div 
      ref="historyContainer" 
      class="history-container"
      @scroll="handleScroll"
    >
      <div class="history-header">
        <span class="history-title">{{ isSearching ? '搜索结果' : '历史记录' }}</span>
      </div>
      
      <div class="history-list">
        <div 
          v-for="item in historyList" 
          :key="item.id" 
          class="history-item"
          @click="handleChatClick(item)"
        >
          <div class="history-content">
            <div class="history-text">{{ item.question }}</div>
            <div class="history-time">{{ item.ctime }}</div>
          </div>
          <a-dropdown 
            :trigger="['click']" 
            class="delete-dropdown"
            @click.stop
          >
            <a class="delete-icon">
              <EllipsisOutlined />
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="handleDelete(item.session_id)">删除</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <!-- 加载状态 -->
        <div v-if="loading" class="loading">
          加载中...
        </div>
        <!-- 没有更多数据提示 -->
        <div v-if="!hasMore && historyList.length > 0" class="no-more">
          {{ isSearching ? '没有更多搜索结果了' : '没有更多历史记录了' }}
        </div>
        <!-- 如果没有历史记录，显示提示 -->
        <div v-if="historyList.length === 0 && !loading" class="no-history">
          {{ isSearching ? '暂无搜索结果' : '暂无历史记录' }}
        </div>
      </div>
    </div>
    
    <div class="action-button">
      <button type="button" class="new-chat-btn" @click="newChat">
        <PlusOutlined class="new-chat-icon" />
        <span>新建对话</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { PlusOutlined, EllipsisOutlined } from '@ant-design/icons-vue';
import { defineEmits, onMounted, ref } from 'vue';
import { getChatHistory, getChatHistoryDetail, deleteChatHistory, type ChatHistoryItem } from '@/api/chat';
import { message } from 'ant-design-vue';

const emit = defineEmits(['newChat', 'chatSelect', 'loadingChange', 'deleteChat']);

// 历史记录容器引用
const historyContainer = ref<HTMLElement>();
// 历史记录列表
const historyList = ref<ChatHistoryItem[]>([]);
// 当前页码（从1开始）
const currentPage = ref(1);
// 每页数量
const pageSize = ref(10);
// 总记录数
const totalCount = ref(0);
// 总页数
const totalPages = ref(0);
// 是否还有更多数据
const hasMore = ref(true);
// 是否正在加载
const loading = ref(false);
// 搜索关键词
const searchKeyword = ref('');
// 是否处于搜索状态
const isSearching = ref(false);

// 获取历史记录（支持搜索）
const loadData = async (isLoadMore: boolean = false) => {
  console.log("loaddata")
  // 如果已经没有更多数据或正在加载中，则不再请求
  if (!hasMore.value || loading.value) {
    return;
  }
  
  loading.value = true;
  
  try {
    const pageToLoad = isLoadMore ? currentPage.value : 1;
    // 调用统一的getChatHistory方法，传入搜索关键词（如果有）
    const data = await getChatHistory(
      pageToLoad, 
      pageSize.value, 
      isSearching.value ? searchKeyword.value : ''
    );
    
    if (isLoadMore) {
      // 加载更多时，追加数据
      historyList.value = [...historyList.value, ...data.data];
    } else {
      // 首次加载时，替换数据
      historyList.value = data.data;
    }
    
    totalCount.value = data.total;
    totalPages.value = data.totalPage;
    // 计算下一页页码
    currentPage.value = data.pageNo + 1;
    
    // 判断是否还有更多数据
    hasMore.value = data.pageNo < data.totalPage;
    
    // 添加调试信息，检查加载情况
    console.log(`Loaded page ${data.pageNo}, total items: ${data.total}, hasMore: ${hasMore.value}`);
  } catch (error) {
    console.error('加载数据失败:', error);
  } finally {
    loading.value = false;
  }
};

// 处理搜索
const handleSearch = async () => {
  isSearching.value = !!searchKeyword.value.trim();
  currentPage.value = 1;
  hasMore.value = true;
  loadData();
};

// 处理滚动事件，实现懒加载
const handleScroll = () => {
  if (!historyContainer.value || loading.value || !hasMore.value) {
    return;
  }
  
  const container = historyContainer.value;
  // 当滚动到距离底部100px时就开始加载数据，提前触发加载，提升用户体验
  if (container.scrollTop + container.clientHeight >= container.scrollHeight - 100) {
    console.log('触发加载更多数据');
    loadData(true);
  }
};

const newChat = () => {
  emit('newChat');
};

const handleDelete = async (id: string) => {
  try {
    await deleteChatHistory(id);
    historyList.value = historyList.value.filter(item => item.session_id !== id);
    if (historyList.value.length === 0) {
      currentPage.value = 1;
      hasMore.value = true;
      loadData();
    }
    emit('deleteChat', id);
    message.success('删除成功');
  } catch (error) {
    console.error('删除聊天记录时出错:', error);
    message.error('删除失败');
  }
};

// 组件挂载时加载历史记录
onMounted(() => {
  loadData();
});

// 添加点击历史对话项的处理函数
const handleChatClick = async (item: ChatHistoryItem) => {
  try {
    // 发送加载开始事件
    emit('loadingChange', true);
    
    const chatDetail = await getChatHistoryDetail(item.session_id);
    
    // 发送事件给父组件AIChat.vue
    emit('chatSelect', chatDetail);
  } catch (error) {
    console.error('获取聊天详情失败:', error);
  } finally {
    // 发送加载结束事件
    emit('loadingChange', false);
  }
};
</script>

<style lang="scss" scoped>
.sidebar {
  z-index: 15;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  width: 280px;
  height: 100%;
  padding: 16px;
  background-color: var(--bg-sidebar);
  border-right: 1px solid var(--border-color-light);
}

.search-container {
  margin-bottom: 20px;

  :deep(.ant-input-wrapper) {
    .ant-input-affix-wrapper,
    .ant-input {
      border-radius: 10px;
    }
  }
}

.search-input {
  width: 100%;
}

.history-container {
  flex: 1;
  min-height: 0;
  padding-right: 4px;
  overflow-y: scroll;
  scrollbar-color: var(--scrollbar-thumb) var(--scrollbar-track);
  scrollbar-gutter: stable;
  scrollbar-width: thin;
}

/* 细窄半透明滚动条 */
.history-container::-webkit-scrollbar {
  width: 5px;
}

.history-container::-webkit-scrollbar-track {
  background: var(--scrollbar-track);
}

.history-container::-webkit-scrollbar-thumb {
  background-color: var(--scrollbar-thumb);
  border-radius: 4px;
}

.history-container::-webkit-scrollbar-thumb:hover {
  background-color: var(--scrollbar-thumb-hover);
}

.history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.history-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
  letter-spacing: 0.02em;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* 历史项卡片 */
.history-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 12px;
  cursor: pointer;
  background-color: var(--bg-subtle);
  border: 1px solid transparent;
  border-radius: 12px;
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.history-item:hover {
  background-color: var(--bg-elevated);
  border-color: var(--brand-primary);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.history-content {
  flex: 1;
  margin-right: 20px;
}

.history-text {
  display: -webkit-box;
  margin-bottom: 4px;
  overflow: hidden;
  font-size: 13px;
  line-height: 1.5;
  color: var(--text-1);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.history-time {
  font-size: 11px;
  color: var(--text-3);
}

.action-button {
  margin-top: 16px;
}

/* 新建对话：品牌渐变按钮 */
.new-chat-btn {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 40px;
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  letter-spacing: 0.02em;
  cursor: pointer;
  background: var(--brand-gradient);
  border: none;
  border-radius: 12px;
  box-shadow: var(--brand-glow);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    filter 0.2s ease;

  .new-chat-icon {
    font-size: 14px;
  }

  &:hover {
    filter: brightness(1.08);
    box-shadow: 0 6px 24px rgb(99 102 241 / 45%);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
}

.no-history {
  padding: 20px 0;
  font-size: 13px;
  color: var(--text-3);
  text-align: center;
}

.loading {
  padding: 10px 0;
  font-size: 13px;
  color: var(--text-3);
  text-align: center;
}

.no-more {
  padding: 10px 0;
  font-size: 12px;
  color: var(--text-3);
  text-align: center;
}

.delete-dropdown {
  opacity: 0;
  transition: opacity 0.2s;
}

.history-item:hover .delete-dropdown {
  opacity: 1;
}

.delete-icon {
  display: inline-block;
  padding: 4px;
  color: var(--text-3);
  border-radius: 6px;
  transition:
    color 0.2s ease,
    background-color 0.2s ease;
}

.delete-icon:hover {
  color: var(--text-1);
  background-color: var(--bg-hover);
}
</style>