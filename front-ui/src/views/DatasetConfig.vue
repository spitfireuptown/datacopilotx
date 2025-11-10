<template>
  <!-- 修改根容器为flex布局，容纳侧边栏和主内容 -->
  <div class="main-container">
    <!-- 添加左侧边栏 -->
    <LeftSidebar />
    
    <!-- 移除第二侧边栏 -->
    
    <!-- 主内容区域 -->
    <div class="dataset-config-container">
      <!-- 全局loading组件 -->
      <div v-if="loading" class="global-loading">
        <ASpin tip="加载中..." size="large" />
      </div>
      
      <div v-else>
        <div class="dataset-config-header">
          <h2>数据源配置</h2>
        </div>
        
        <div class="dataset-config-content">
          <div class="dataset-list">
            <div class="list-header">
              <h3>已配置数据源</h3>
              <a-button type="primary" @click="showForm">
                新建
              </a-button>
            </div>
            <a-table :columns="columns" :data-source="datasets" row-key="id">
              <template #action="{ record }">
                <a-space size="small">
                  <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
                  <a-button type="link" danger size="small" @click="handleDelete(record.id)">删除</a-button>
                </a-space>
              </template>
            </a-table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 导入路由
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
// 导入Spin组件用于loading效果
import { Spin as ASpin } from 'ant-design-vue';

// 导入侧边栏组件
import LeftSidebar from '../components/LeftSidebar.vue';

// 导入数据集API
import { getDatasetList, deleteDataset } from '../api/dataset';

// 创建路由实例
const router = useRouter();

// 定义数据集类型
interface Dataset {
  id: string;
  name: string;
  type: string;
  host: string;
  port: number;
  database: string;
  username: string;
  createTime: string;
}

// 表格列配置
const columns = [
  {
    title: '数据源名称',
    dataIndex: 'name',
    key: 'name'
  },
  {
    title: '类型',
    dataIndex: 'type',
    key: 'type'
  },
  {
    title: '数据表',
    dataIndex: 'table',
    key: 'table'
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime'
  },
  {
    title: '操作',
    key: 'action',
    slots: { customRender: 'action' }
  }
];

// 数据集列表
const datasets = ref<Dataset[]>([]);
// 添加loading状态
const loading = ref(false);

// 加载数据集列表
const loadDatasets = async () => {
  // 开始加载时设置loading为true
  loading.value = true;
  try {
    const data = await getDatasetList();
    datasets.value = data;
  } catch (error) {
    console.error('获取数据集列表失败:', error);
    message.error('获取数据集列表失败');
  } finally {
    // 无论成功失败，结束加载时设置loading为false
    loading.value = false;
  }
};

// 在组件挂载时加载数据
onMounted(() => {
  loadDatasets();
});

// 显示新建表单
const showForm = () => {
  router.push('/database-connection-form');
};

// 编辑数据集
const handleEdit = (record: Dataset) => {
  // 跳转到编辑页面，传递记录ID
  router.push({
    path: '/database-connection-form',
    query: { id: record.id }
  });
};

// 删除数据集
const handleDelete = async (id: string) => {
  try {
    // 删除操作也显示loading
    loading.value = true;
    await deleteDataset(id);
    message.success('数据源已删除');
    // 重新加载数据集列表
    await loadDatasets();
  } catch (error) {
    console.error('删除数据集失败:', error);
    message.error('删除失败');
  } finally {
    loading.value = false;
  }
};
</script>

<style lang="scss" scoped>
/* 修改样式以适应新的布局 */
.main-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.dataset-config-container {
  flex: 1;
  background-color: #f0f2f5;
  padding: 24px;
  box-sizing: border-box;
  overflow-y: auto;
  margin-left: 0; /* 移除之前可能存在的margin */
  position: relative;
}

/* 全局loading样式 */
.global-loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.7);
  z-index: 1000;
}

/* 头部样式 */
.dataset-config-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dataset-config-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

/* 以下样式保持不变 */
.dataset-config-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.dataset-list {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
}

.dataset-list h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}
/* 列表头部样式 */
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>