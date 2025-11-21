<template>
  <div class="main-container">
    <!-- 添加左侧边栏 -->
    <LeftSidebar />
    
    <!-- 主内容区域 -->
    <div class="data-source-select-container">
      <!-- 新建数据源标题 - 居中显示 -->
      <div class="data-source-select-header">
        <h2>新建数据源</h2>
      </div>
      
      <div class="data-source-select-content">
        <!-- 数据源类型列表 -->
        <div class="data-source-type-list">
          <div 
            v-for="source in filteredDataSources" 
            :key="source.type"
            class="data-source-type-item" 
            :class="{ 'selected': selectedType === source.type }"
            @click="selectDataSource(source.type)"
          >
            <div class="data-source-icon" :class="`${source.type}-icon`">
              <img :src="`/images/${source.icon}`" :alt="source.name" :title="source.name" />
            </div>
            <div class="data-source-info">
              <h3>{{ source.name }}</h3>
            </div>
          </div>
        </div>
        
        <div class="action-buttons">
          <a-button @click="goBack">取消</a-button>
          <a-button type="primary" :disabled="!selectedType" @click="confirmSelection">确认</a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import LeftSidebar from '../components/LeftSidebar.vue';

const router = useRouter();
const selectedType = ref<string>('');
const searchKeyword = ref<string>('');

// 数据源类型列表 - 使用图片文件名替换emoji
const dataSources = ref([
  { type: 'excel', name: '本地Excel/CSV', icon: 'excel.png', category: 'file' },
  { type: 'mysql', name: 'MySQL', icon: 'mysql.png', category: 'database' },
  { type: 'clickhouse', name: 'ClickHouse', icon: 'ck.png', category: 'database' }
]);

// 过滤数据源
const filteredDataSources = computed(() => {
  if (!searchKeyword.value) {return dataSources.value;}
  
  const keyword = searchKeyword.value.toLowerCase();
  return dataSources.value.filter(source => 
    source.name.toLowerCase().includes(keyword) ||
    source.type.toLowerCase().includes(keyword)
  );
});

// 选择数据源类型
const selectDataSource = (type: string) => {
  selectedType.value = type;
};

// 确认选择
const confirmSelection = () => {
  if (!selectedType.value) {return;}
  
  if (selectedType.value === 'excel') {
    // 跳转到Excel上传页面
    router.push('/excel-upload-form');
  } else {
    // 跳转到数据库连接表单
    router.push({
      path: '/database-connection-form',
      query: { type: selectedType.value }
    });
  }
};

// 返回上一页
const goBack = () => {
  router.back();
};
</script>

<style lang="scss" scoped>
.main-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.data-source-select-container {
  flex: 1;
  background-color: #fff;
  padding: 24px;
  box-sizing: border-box;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  align-items: center; /* 主容器居中 */
}

.data-source-select-header {
  margin-bottom: 24px;
  width: 100%;
  text-align: center; /* 标题文本居中 */
}

.data-source-select-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.data-source-select-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 800px;
  width: 100%;
  align-items: center; /* 内容区域居中 */
}

.data-source-type-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-bottom: 40px;
  width: 100%;
  justify-items: center; /* 网格项居中 */
}

.data-source-type-item {
  background-color: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 80px;
  box-sizing: border-box;
  width: 260px; /* 固定宽度确保对齐 */
  
  &:hover {
    border-color: #1890ff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  }
  
  &.selected {
    border-color: #1890ff;
    background-color: #e6f7ff;
  }
}

.data-source-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  border-radius: 4px;
  
  img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
  }
}

.data-source-info h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.action-buttons {
  display: flex;
  gap: 16px;
  justify-content: center; /* 按钮居中显示 */
  margin-top: auto;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
  width: 100%;
}

// 数据源特定图标背景色
:deep(.excel-icon) {
  background-color: #e6f7ff;
}

:deep(.mysql-icon) {
  background-color: #f6ffed;
}

:deep(.clickhouse-icon) {
  background-color: #f0f5ff;
}
</style>