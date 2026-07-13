<template>
  <!-- 修改根容器为flex布局，容纳侧边栏和主内容 -->
  <div class="main-container">
    <!-- 添加左侧边栏 -->
    <LeftSidebar />
    
    <!-- 主内容区域 -->
    <div class="model-config-container">
      <!-- 全局loading组件 -->
      <div v-if="loading" class="global-loading">
        <ASpin tip="加载中..." size="large" />
      </div>
      
      <div v-else>
        <div class="model-config-header">
          <!-- 添加返回按钮 -->
          <div class="header-left">
            <h2>大模型配置</h2>
          </div>
        </div>
        
        <!-- 配置内容区域 - 只显示列表和新增按钮 -->
        <div class="model-config-content">
          <div class="model-list">
            <div class="list-header">
              <h3>已配置大模型</h3>
              <a-button type="primary" @click="showModal">新增</a-button>
            </div>
            <a-table :columns="columns" :data-source="models" row-key="id">
              <template #action="{ record }">
                <a-space size="small">
                  <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
                  <a-button type="link" size="small" @click="handleTestConnection(record.id)">测试连接</a-button>
                  <a-button type="link" danger size="small" @click="handleDelete(record.id)">删除</a-button>
                </a-space>
              </template>
            </a-table>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 新增/编辑模型配置弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      title="模型配置"
      :footer="null"
      width="600px"
    >
      <a-form
        ref="formRef"
        :model="formData"
        layout="vertical"
        :label-col="{ span: 24 }"
        :wrapper-col="{ span: 24 }"
      >
        <a-form-item
          label="模型名称"
          name="model"
          :rules="[{ required: true, message: '请输入模型名称' }]"
        >
          <a-input v-model:value="formData.model" placeholder="请输入模型名称" />
        </a-form-item>

        <a-form-item
          label="API协议类型"
          name="type"
          :rules="[{ required: true, message: '请选择模型类型' }]"
        >
          <a-select v-model:value="formData.type" placeholder="请选择模型类型">
            <a-select-option value="openai">OpenAI API</a-select-option>
            <a-select-option value="ollama">Ollama API</a-select-option>
          </a-select>
        </a-form-item>

        <!-- 新增功能类型字段 -->
        <a-form-item
          label="功能类型"
          name="functionType"
          :rules="[{ required: true, message: '请选择功能类型' }]"
        >
          <a-select v-model:value="formData.functionType" placeholder="请选择功能类型">
            <a-select-option value="chat">chat</a-select-option>
            <a-select-option value="embedding">embedding</a-select-option>
          </a-select>
        </a-form-item>

        <!-- 新增平台字段 -->
        <a-form-item
          label="平台"
          name="platform"
          :rules="[{ required: true, message: '请选择平台' }]"
        >
          <a-select v-model:value="formData.platform" placeholder="请选择平台">
            <a-select-option value="openai">OpenAI</a-select-option>
            <a-select-option value="ollama">Ollama</a-select-option>
            <a-select-option value="siliconflow">硅基流动</a-select-option>
            <a-select-option value="dmx">DMX</a-select-option>
            <a-select-option value="qwen">通义千问</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item
          label="API Key"
          name="apiKey"
          :rules="[{ required: false, message: '请输入API Key' }]"
        >
          <a-input-password v-model:value="formData.apiKey" placeholder="请输入API Key" />
        </a-form-item>

        <a-form-item
          label="API 地址"
          name="apiBase"
          :rules="[{ required: true, message: '请输入API 地址' }]"
        >
          <a-input v-model:value="formData.apiBase" placeholder="请输入API地址" />
        </a-form-item>

        <!-- 维度输入框，仅在功能类型为embedding时显示 -->
        <a-form-item
          v-if="formData.functionType === 'embedding'"
          label="嵌入维度"
          name="dimension"
          :rules="[{ required: true, message: '请输入嵌入维度' }]"
        >
          <a-input-number
            v-model:value="formData.dimension"
            placeholder="请输入嵌入维度"
            :min="1"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button type="primary" :loading="modalLoading" @click="handleSubmit">
              保存配置
            </a-button>
            <a-button @click="handleCancel">
              取消
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
// 导入路由和图标
import { ref, reactive, onMounted } from 'vue';
import type { FormInstance } from 'ant-design-vue';
import { message } from 'ant-design-vue'; 
import { Modal } from 'ant-design-vue';
import { Spin as ASpin } from 'ant-design-vue';

// 导入侧边栏组件
import LeftSidebar from '../components/LeftSidebar.vue';

// 导入模型相关API和类型
import {
  getModelList,
  createModel,
  updateModel,
  deleteModel,
  testModelConnection,
  type Model as ModelApi,
  type ModelFormData
} from '../api/model';

// 定义表单数据类型
interface FormData {
  model: string;
  type: string;
  functionType: string; // 新增功能类型字段
  platform: string;
  apiKey: string;
  apiBase: string;
  dimension?: number; // 新增维度字段，用于embedding模型
  id?: string;
  createTime?: string; // 添加创建时间字段
}

// 表单引用
const formRef = ref<FormInstance>();

// 弹窗显示状态
const modalVisible = ref(false);

// 添加loading状态变量（用于全局loading）
const loading = ref(false);
// 添加弹窗内loading状态
const modalLoading = ref(false);

// 表单数据
const formData = reactive<FormData>({
  model: '',
  type: '',
  functionType: '', // 初始化功能类型字段
  platform: '',
  apiKey: '',
  apiBase: '',
  dimension: undefined // 初始化维度字段
});

// 表格列配置
const columns = [
  {
    title: '模型名称',
    dataIndex: 'model',
    key: 'model'
  },
  {
    title: '类型',
    dataIndex: 'type',
    key: 'type'
  },
  {
    title: '功能',
    dataIndex: 'functionType',
    key: 'functionType'
  },
  {
    title: '平台',
    dataIndex: 'platform',
    key: 'platform'
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

// 模型列表 - 修改为FormData类型
const models = ref<FormData[]>([]);

// 获取模型列表
const fetchModels = async () => {
  try {
    // 设置全局loading为true
    loading.value = true;
    const data = await getModelList();
    // 转换API返回的数据格式以适应表格显示
    models.value = data.map((model: ModelApi) => ({
      id: model.id,
      model: model.model,
      type: model.type, // 将平台类型映射到API协议类型
      functionType: model.functionType,
      platform: model.platform, // 平台字段
      apiKey: model.apiKey || '', // 保存API Key以便编辑时回显
      apiBase: model.apiBase || '', // 保存API地址以便编辑时回显
      dimension: model.dimension, // 保存维度字段
      createTime: model.createTime
    }));
  } finally {
    // 无论成功失败，都在最后设置全局loading为false
    loading.value = false;
  }
};

// 组件挂载时获取数据
onMounted(() => {
  fetchModels();
});

// 显示弹窗
const showModal = () => {
  handleReset(); // 重置表单
  modalVisible.value = true;
};

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value?.validate();
    
    const formDataToSubmit: ModelFormData = {
      model: formData.model,
      platform: formData.platform,
      type: formData.type,
      functionType: formData.functionType, // 添加功能类型到提交数据中
      apiKey: formData.apiKey,
      apiBase: formData.apiBase,
      dimension: formData.dimension // 添加维度字段到提交数据中
    };
    
    // 设置弹窗内loading为true
    modalLoading.value = true;
    
    if (formData.id) {
      // 编辑模式 - 将 id 添加到提交的数据中
      (formDataToSubmit as any).id = formData.id;
      await updateModel(formDataToSubmit); // 注意这里只传递一个参数
      message.success('大模型配置更新成功');
    } else {
      // 新增模式
      await createModel(formDataToSubmit);
      message.success('大模型配置添加成功');
    }
    
    modalVisible.value = false;
    handleReset();
    // 重新获取模型列表
    await fetchModels();
  } finally {
    // 无论成功失败，都在最后设置弹窗内loading为false
    modalLoading.value = false;
  }
};

// 重置表单
const handleReset = () => {
  // 清空表单数据
  formData.model = '';
  formData.type = '';
  formData.functionType = '';
  formData.platform = '';
  formData.apiKey = '';
  formData.apiBase = '';
  formData.dimension = undefined;
  formData.id = undefined;
  
  // 重置表单验证状态
  formRef.value?.resetFields();
};

// 取消操作
const handleCancel = () => {
  modalVisible.value = false;
  handleReset();
};

// 测试连接
const handleTestConnection = async (id: string) => {
  const loadingInstance = message.loading('正在测试连接...', 0);
  try {
    await testModelConnection(id);
    loadingInstance();
    message.success('连接测试成功');
  } catch (error: any) {
    loadingInstance();
    message.error(error.message || '连接测试失败');
    console.error('测试连接失败:', error);
  }
};

// 编辑模型配置
const handleEdit = (record: FormData) => {
  // 查找完整的模型数据
  const modelData = models.value.find(item => item.id === record.id);
  
  if (modelData) {
    // 填充完整的表单数据进行回显
    formData.id = modelData.id;
    formData.model = modelData.model;
    formData.type = modelData.type;
    formData.functionType = modelData.functionType;
    formData.platform = modelData.platform;
    formData.apiKey = modelData.apiKey || '';
    formData.apiBase = modelData.apiBase || '';
    formData.dimension = modelData.dimension;
  } else {
    // 如果没有找到完整数据，至少填充基本字段
    formData.id = record.id;
    formData.model = record.model;
    formData.type = record.type;
    formData.functionType = record.functionType || '';
    formData.platform = record.platform;
    formData.apiKey = '';
    formData.apiBase = '';
    formData.dimension = undefined;
  }
  
  // 显示弹窗
  modalVisible.value = true;
};

// 删除模型配置
const handleDelete = async (id: string) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个大模型配置吗？',
    okText: '确定',
    cancelText: '取消',
    async onOk() {
      try {
        // 设置全局loading为true
        loading.value = true;
        // 调用后端API删除模型
        await deleteModel(id);
        // 删除成功后更新列表
        models.value = models.value.filter(item => item.id !== id);
        message.success('大模型配置已删除');
      } catch (error) {
        console.error('删除模型失败:', error);
        message.error('删除失败，请重试');
      } finally {
        // 无论成功失败，都在最后设置全局loading为false
        loading.value = false;
      }
    }
  });
};
</script>

<style lang="scss" scoped>
/* 修改样式以适应新的布局 */
.main-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.model-config-container {
  flex: 1;
  background-color: #f0f2f5;
  padding: 24px;
  box-sizing: border-box;
  overflow-y: auto;
  margin-left: 0; /* 移除之前可能存在的margin */
  position: relative; /* 添加相对定位，用于loading绝对定位 */
}

/* 全局loading样式 - 与DatasetConfig.vue保持一致 */
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

/* 修改头部样式以适应返回按钮 */
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-button {
  color: #1890ff;
}

/* 以下样式保持不变 */
.model-config-header {
  margin-bottom: 24px;
}

.model-config-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.model-config-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.model-list {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
</style>