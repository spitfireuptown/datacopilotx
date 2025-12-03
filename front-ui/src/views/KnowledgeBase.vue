<template>
  <!-- 使用flex布局，容纳侧边栏和主内容 -->
  <div class="main-container">
    <!-- 添加左侧边栏 -->
    <LeftSidebar />
    
    <!-- 主内容区域 -->
    <div class="knowledge-base-container">
      <!-- 全局loading组件 -->
      <div v-if="loading" class="global-loading">
        <ASpin tip="加载中..." size="large" />
      </div>
      
      <div v-else>
        <!-- 工具栏 -->
        <div class="toolbar">
          <div class="search-filter">
            <a-checkbox v-model:checked="allKnowledge" class="mr-4">所有知识库</a-checkbox>
          </div>
          <a-input-search placeholder="搜索" :style="{ width: '200px' }" />
        </div>
        
        <!-- 主要内容区域 -->
        <div class="knowledge-content">
          <!-- 左侧创建区域 -->
          <div class="create-section">
            <div class="create-card" @click="handleCreate">
              <div class="create-icon">+</div>
              <h3>创建知识库</h3>
            </div>
          </div>
          
          <!-- 右侧卡片网格 -->
          <div class="knowledge-grid">
            <div 
              v-for="item in knowledgeItemsWithMockData" 
              :key="item.id" 
              class="knowledge-card"
              @click="handleCardClick(item.id)"
            >
              <div class="card-header">
                <div class="file-icon">📁</div>
                <h4>{{ item.name }}</h4>
                <a-dropdown @click.stop>
                  <a-button type="text" size="small">
                    <EllipsisOutlined />
                  </a-button>
                  <template #overlay>
                    <a-menu>
                      <a-menu-item @click="handleEdit(item)">编辑</a-menu-item>
                      <a-menu-item danger :loading="deleteLoading" @click="handleDelete(item.id)">删除</a-menu-item>
                    </a-menu>
                  </template>
                </a-dropdown>
              </div>

              <div class="card-description">
                <p>{{ item.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      okText="保存"
      cancelText="取消"
      @ok="handleModalSubmit"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        layout="vertical"
      >
        <!-- 知识库名称输入框 -->
        <a-form-item
          label="知识库名称"
          name="name"
          :rules="[{ required: true, message: '请输入知识库名称' }]"
        >
          <a-input
            v-model:value="formData.name"
            placeholder="请输入知识库名称"
            style="width: 100%"
          />
        </a-form-item>
        
        <!-- 模型选择 -->
        <a-form-item
          label="选择嵌入模型"
          name="modelId"
          :rules="[{ required: true, message: '请选择模型' }]"
        >
          <a-select
            v-model:value="formData.modelId"
            placeholder="请选择模型"
            style="width: 100%"
          >
            <a-select-option v-for="model in models" :key="model.id" :value="model.id">
              {{ model.platform + '/' + model.model }}
            </a-select-option>
          </a-select>
        </a-form-item>
        
        <!-- 数据集选择 -->
        <a-form-item
          label="选择数据集"
          name="datasetId"
          :rules="[{ required: true, message: '请选择数据集' }]"
        >
          <a-select
            v-model:value="formData.datasetId"
            placeholder="请选择数据集"
            style="width: 100%"
          >
            <a-select-option v-for="dataset in datasets" :key="dataset.id" :value="dataset.id">
              {{ dataset.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        
        <!-- 知识库描述输入框 -->
        <a-form-item
          label="知识库描述"
          name="description"
          :rules="[{ required: true, message: '请输入知识库描述' }]"
        >
          <a-textarea
            v-model:value="formData.description"
            placeholder="请输入知识库描述"
            style="width: 100%"
            rows={4}
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
// 导入必要的依赖
import { ref, computed, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { Spin as ASpin, FormInstance } from 'ant-design-vue';
import { EllipsisOutlined } from '@ant-design/icons-vue';

// 导入侧边栏组件
import LeftSidebar from '../components/LeftSidebar.vue';

// 导入API
import { getKnowledgeList, createKnowledge, deleteKnowledge, updateKnowledge, type KnowledgeListItem } from '../api/knowledge';
import { getModelList, type Model } from '../api/model';
import { getDatasetList, type Dataset } from '../api/dataset';

// 定义表单数据类型
interface FormData {
  name: string;
  modelId: string;
  datasetId: string;
  description: string;
  id?: string;
}

// 定义知识库列表数据
const knowledgeItems = ref<KnowledgeListItem[]>([]);
// 添加loading状态
const loading = ref(false);

// 全选状态
const allKnowledge = ref(true);

// 弹窗相关状态
const modalVisible = ref(false);
const modalTitle = ref('新建知识库条目');
const modalLoading = ref(false);
const formRef = ref<FormInstance>();
const isEdit = ref(false);
// 添加删除操作的loading状态
const deleteLoading = ref(false);

// 表单数据
const formData = ref<FormData>({
  name: '',
  modelId: '',
  datasetId: '',
  description: '',
  id: undefined
});

// 模型和数据集列表
const models = ref<Model[]>([]);
const datasets = ref<Dataset[]>([]);

// 创建包含模拟数据的知识库列表
const knowledgeItemsWithMockData = computed(() => {
  // 始终使用实际数据，不再返回模拟数据
  return knowledgeItems.value.map(item => ({
    id: item.id, // 使用id作为docId
    name: item.name, // 直接使用接口返回的name
    description: item.description, // 直接使用接口返回的description
    datasetId: item.datasetId,
    modelId: item.modelId
  }));
});

// 加载知识库列表
const loadKnowledgeList = async () => {
  // 开始加载时设置loading为true
  loading.value = true;
  try {
    // 获取对象数组
    const data = await getKnowledgeList();
    knowledgeItems.value = data;
    console.log(knowledgeItems.value)
  } catch (error) {
    console.error('获取知识库列表失败:', error);
    message.error('获取知识库列表失败');
  } finally {
    // 无论成功失败，结束加载时设置loading为false
    loading.value = false;
  }
};

// 加载模型列表
const loadModels = async () => {
  try {
    const data = await getModelList({
      'type': 'embedding'
    });
    models.value = data.map(model => {
      model.id = String(model.id);
      return model;
    });
  } catch (error) {
    console.error('获取模型列表失败:', error);
    message.error('获取模型列表失败');
  }
};

// 加载数据集列表
const loadDatasets = async () => {
  try {
    const data = await getDatasetList();
    datasets.value = data.map(dataset => {
      dataset.id = String(dataset.id);
      return dataset;
    });
  } catch (error) {
    console.error('获取数据集列表失败:', error);
    message.error('获取数据集列表失败');
  }
};

// 导入useRouter
import { useRouter } from 'vue-router';

// 在setup函数中获取router实例
const router = useRouter();

// 处理卡片点击
const handleCardClick = (id: string) => {
  // 使用路由跳转到详情页
  router.push({ name: 'KnowledgeDetail', params: { id } });
};

// 在组件挂载时加载数据
onMounted(() => {
  loadKnowledgeList();
});

// 处理新建操作
const handleCreate = async () => {
  isEdit.value = false;
  modalTitle.value = '新建知识库条目';
  
  // 重置表单
  if (formRef.value) {
    await formRef.value.resetFields();
  }
  
  formData.value = {
    name: '',
    modelId: '',
    datasetId: '',
    description: '',
    id: undefined
  };
  
  // 加载模型和数据集
  await Promise.all([loadModels(), loadDatasets()]);
  
  // 显示弹窗
  modalVisible.value = true;
};

// 处理编辑操作
const handleEdit = async (record: any) => {
  isEdit.value = true;
  modalTitle.value = '编辑知识库条目';
  
  // 加载模型和数据集（先加载，确保数据可用）
  await Promise.all([loadModels(), loadDatasets()]);
  console.log(record)
  // 设置表单数据，回显模型和数据集
  formData.value = {
    name: record.name || '',
    modelId: String(record.modelId || ''), // 使用记录中的modelId并转换为string
    datasetId: String(record.datasetId || ''), // 使用记录中的datasetId并转换为string
    description: record.description || '',
    id: record.id
  };
  
  // 显示弹窗
  modalVisible.value = true;
};

// 处理删除操作
const handleDelete = async (id: string) => {
  try {
    // 开始删除时设置loading为true
    deleteLoading.value = true;
    // 调用deleteKnowledge方法
    await deleteKnowledge(id);
    message.success('删除知识库成功');
    // 删除成功后重新加载列表
    loadKnowledgeList();
  } catch (error) {
    console.error('删除知识库失败:', error);
    message.error('删除知识库失败');
  } finally {
    // 无论成功失败，结束删除时设置loading为false
    deleteLoading.value = false;
  }
};

// 处理弹窗提交
const handleModalSubmit = async () => {
  if (!formRef.value) {return;}
  
  try {
    await formRef.value.validateFields();
    modalLoading.value = true;
    
    const { name, modelId, datasetId, description, id } = formData.value;
    
    if (isEdit.value && id) {
      // 编辑模式且有id时调用updateKnowledge
      await updateKnowledge({
        id,
        modelId,
        datasetId,
        name,
        description
      });
    } else {
      // 创建模式调用createKnowledge
      await createKnowledge({
        modelId,
        datasetId,
        name,
        description
      });
    }
    
    message.success(isEdit.value ? '更新成功' : '创建成功');
    modalVisible.value = false;
    loadKnowledgeList();
  } catch (error) {
    console.error('提交失败:', error); // 查看具体错误信息
    message.error('提交失败，请检查表单数据');
  } finally {
    modalLoading.value = false;
  }
};

// 处理弹窗取消
const handleModalCancel = () => {
  modalVisible.value = false;
};
</script>

<style lang="scss" scoped>
/* 修改样式以适应新的布局 */
.main-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.knowledge-base-container {
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

/* 标签页样式 */
.tabs-container {
  margin-bottom: 16px;
  background-color: #fff;
  padding: 0 24px;
  border-radius: 8px 8px 0 0;
}

/* 工具栏样式 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.search-filter {
  display: flex;
  align-items: center;
}

/* 主要内容区域 */
.knowledge-content {
  display: flex;
  gap: 16px;
  padding: 16px 0;
}

/* 创建区域样式 */
.create-section {
  width: 280px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.create-card {
  background-color: #fff;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s;
  text-align: center;
}

.create-card:hover {
  border-color: #1890ff;
  background-color: #f0f8ff;
}

.create-icon {
  font-size: 32px;
  font-weight: bold;
  color: #1890ff;
  margin-bottom: 12px;
}

.create-card h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
}

.create-card p {
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

.import-section {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px 24px;
}

/* 知识库网格样式 */
.knowledge-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

/* 知识库卡片样式 */
.knowledge-card {
  background-color: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.knowledge-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
}

.file-icon {
  font-size: 24px;
  margin-right: 12px;
}

.card-header h4 {
  flex: 1;
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta {
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
}

.card-description {
  flex: 1;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 12px;
}

.card-footer {
  margin-top: auto;
}
</style>