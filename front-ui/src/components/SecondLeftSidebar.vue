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
      <a-button type="primary" block @click="newChat">
        <template #icon>
          <PlusOutlined />
        </template>
        新建对话
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { PlusOutlined, EllipsisOutlined } from '@ant-design/icons-vue';
import { defineEmits, onMounted, ref } from 'vue';
import { getChatHistory, getChatHistoryDetail, deleteChatHistory, type ChatHistoryItem } from '@/api/chat';
import { message } from 'ant-design-vue';

// 添加新的emit事件类型
const emit = defineEmits(['newChat', 'chatSelect']);

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

// 处理删除聊天记录
const handleDelete = async (id: string) => {
  try {
    await deleteChatHistory(id);
    console.log(historyList)
    historyList.value = historyList.value.filter(item => item.session_id !== id);
    // 如果删除后列表为空，重新加载数据
    if (historyList.value.length === 0) {
      currentPage.value = 1;
      hasMore.value = true;
      loadData();
    }
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
  width: 280px;
  height: 100vh;
  background-color: #fff;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  padding: 16px;
  box-sizing: border-box;
}

.search-container {
  margin-bottom: 24px;
}

.search-input {
  width: 100%;
}

.menu-list {
  margin-bottom: 24px;
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 10px 16px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: all 0.3s;
}

.menu-item:hover {
  background-color: #f0f7ff;
}

.menu-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
}

.menu-icon {
  margin-right: 12px;
  font-size: 18px;
}

.menu-text {
  font-size: 14px;
}

.history-container {
  flex: 1;
  // 改为scroll，确保始终显示滚动条
  overflow-y: scroll;
  // 为Firefox设置滚动条样式
  scrollbar-width: thin;
  scrollbar-color: #999 #f0f0f0;
  // 确保容器高度正确设置
  min-height: 0;
  // 强制显示滚动条容器，防止内容区域宽度变化
  scrollbar-gutter: stable;
}

/* Chrome, Edge, Safari 滚动条样式 */
.history-container::-webkit-scrollbar {
  width: 10px; /* 增加滚动条宽度，使其更明显 */
}

.history-container::-webkit-scrollbar-track {
  background: #f0f0f0; /* 设置滚动条轨道背景色 */
}

.history-container::-webkit-scrollbar-thumb {
  background-color: #999; /* 加深滚动条滑块颜色，使其更明显 */
  border-radius: 4px; /* 添加圆角 */
  border: 2px solid #f0f0f0; /* 添加边框，增加对比度 */
}

.history-container::-webkit-scrollbar-thumb:hover {
  background-color: #666; /* 悬停时颜色更深 */
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.history-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.history-more {
  color: #999;
  cursor: pointer;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  padding: 13px 10px;
  background-color: #fafafa;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.history-item:hover {
  background-color: #f0f0f0;
}

.history-content {
  flex: 1;
  margin-right: 20px;
}

.history-text {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.history-time {
  font-size: 12px;
  color: #999;
}

.action-button {
  margin-top: 16px;
}

.no-history {
  text-align: center;
  color: #999;
  padding: 20px 0;
  font-size: 14px;
}

.loading {
  text-align: center;
  color: #999;
  padding: 10px 0;
  font-size: 14px;
  // 添加加载动画
  .loading-spinner {
    display: inline-block;
    width: 16px;
    height: 16px;
    border: 2px solid #f3f3f3;
    border-top: 2px solid #3498db;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin-right: 8px;
    vertical-align: middle;
  }
  
  @keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }
}

.no-more {
  text-align: center;
  color: #999;
  padding: 10px 0;
  font-size: 14px;
}

.delete-dropdown {
  opacity: 0;
  transition: opacity 0.3s;
}

.history-item:hover .delete-dropdown {
  opacity: 1;
}

.delete-icon {
  color: #999;
  padding: 4px;
  display: inline-block;
  border-radius: 4px;
}

.delete-icon:hover {
  background-color: rgba(0, 0, 0, 0.05);
  color: #333;
}

</style>