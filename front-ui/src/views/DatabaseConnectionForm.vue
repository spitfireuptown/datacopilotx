<template>
  <div class="main-container">
    <LeftSidebar />
    
    <div class="database-connection-container">
      <div class="database-connection-header">
        <div class="header-left">
          <a-button type="text" class="back-button" @click="handleBack">
            <template #icon>
              <ArrowLeftOutlined />
            </template>
          </a-button>
          <h2>{{ editMode ? '编辑数据库连接' : '新建数据库连接' }}</h2>
        </div>
        <div class="header-right">
        </div>
      </div>
      
      <!-- 步骤指示器 -->
      <div class="steps-container">
        <a-steps :current="currentStep" :items="steps" />
      </div>
      
      <!-- 内容区域 -->
      <div class="database-connection-content">
        <!-- 步骤1：连接信息 -->
        <div v-if="currentStep === 1" class="step-content">
          <div class="connection-sidebar">
            <a-card>
              <div class="connection-form-header">
                <h3>连接信息</h3>
              </div>
              <a-form
                ref="formRef"
                :model="formData"
                layout="vertical"
                :label-col="{ span: 24 }"
                :wrapper-col="{ span: 24 }"
              >
                <a-form-item
                  label="连接名称"
                  name="name"
                  :rules="[{ required: true, message: '请输入连接名称' }]"
                >
                  <a-input v-model:value="formData.name" placeholder="请输入连接名称" />
                </a-form-item>

                <a-form-item
                  label="数据库类型"
                  name="type"
                  :rules="[{ required: true, message: '请选择数据库类型' }]"
                >
                  <a-select v-model:value="formData.type" placeholder="请选择数据库类型">
                    <a-select-option value="mysql">MySQL</a-select-option>
                    <a-select-option value="clickhouse">ClickHouse</a-select-option>
                  </a-select>
                </a-form-item>

                <a-form-item
                  label="主机地址"
                  name="host"
                  :rules="[{ required: true, message: '请输入主机地址' }]"
                >
                  <a-input v-model:value="formData.host" placeholder="请输入主机地址" />
                </a-form-item>

                <a-form-item
                  label="端口号"
                  name="port"
                  :rules="[{ required: true, message: '请输入端口号' }]"
                >
                  <a-input-number v-model:value="formData.port" style="width: 100%" />
                </a-form-item>

                <a-form-item
                  label="数据库名称"
                  name="database"
                  :rules="[{ required: true, message: '请输入数据库名称' }]"
                >
                  <a-input v-model:value="formData.database" placeholder="请输入数据库名称" />
                </a-form-item>

                <a-form-item
                  label="用户名"
                  name="username"
                  :rules="[{ required: true, message: '请输入用户名' }]"
                >
                  <a-input v-model:value="formData.username" placeholder="请输入用户名" />
                </a-form-item>

                <a-form-item
                  label="密码"
                  name="password"
                >
                  <a-input-password v-model:value="formData.password" placeholder="请输入密码" />
                </a-form-item>

                <a-form-item
                  label="数据集描述"
                  name="description"
                  :rules="[{ required: true, message: '请输入数据集描述字段' }]"
                >
                  <a-textarea 
                    v-model:value="formData.description" 
                    placeholder="请输入数据集描述，帮助模型更好地理解数据"
                    :rows="3"
                  />
                </a-form-item>

                <a-form-item>
                  <a-space>
                    <a-button type="primary" :loading="testing || isLoading" @click="handleGetTables">
                      获取表列表
                    </a-button>
                    <a-button v-if="editMode" type="primary" @click="handleNextStep">
                      下一步
                    </a-button>
                    <a-button @click="handleReset">
                      重置
                    </a-button>
                  </a-space>
                </a-form-item>
              </a-form>
            </a-card>
          </div>
        </div>
        
        <!-- 步骤2：选择表 -->
        <div v-if="currentStep === 2" class="step-content">
          <div class="connection-sidebar">
            <a-card>
              <div class="table-selection-header">
                <h3>选择数据表</h3>
                <a-space>
                  <a-button @click="handlePrevStep">上一步</a-button>
                  <a-button type="primary" :disabled="selectedTables.length === 0" @click="handleNextStep">
                    下一步
                  </a-button>
                </a-space>
              </div>
              
              <a-input-search
                v-model:value="searchKeyword"
                placeholder="请输入表名搜索"
                allow-clear
                style="margin-bottom: 16px; width: 280px;"
              />
              
              <a-table 
                :columns="tableColumns" 
                :data-source="filteredTables" 
                :row-selection="rowSelection"
                row-key="key"
                :pagination="false"
              />
              
              <div v-if="tables.length === 0" class="no-tables">
                未获取到表，请检查连接信息是否正确
              </div>
              <div v-else-if="filteredTables.length === 0" class="no-tables">
                未找到匹配的表，请尝试其他关键词
              </div>
            </a-card>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import type { FormInstance } from 'ant-design-vue';
import { message } from 'ant-design-vue';

import LeftSidebar from '../components/LeftSidebar.vue';
import { getTables, getDatasetDetail } from '../api/dataset';

interface FormData {
  name: string;
  type: string;
  host: string;
  port: number | null;
  database: string;
  username: string;
  password: string;
  description: string;
}

const router = useRouter();
const route = useRoute();

const formRef = ref<FormInstance>();
const editMode = ref(false);

const formData = reactive<FormData>({
  name: '',
  type: '',
  host: '',
  port: null,
  database: '',
  username: '',
  password: '',
  description: ''
});

const currentStep = ref(1);
const steps = [
  { title: '连接信息' },
  { title: '选择表' }
];

const tables = ref<string[]>([]);
const selectedTables = ref<string[]>([]);
const searchKeyword = ref<string>('');

const filteredTables = computed(() => {
  if (!searchKeyword.value) {
    return tables.value.map(t => ({ key: t, name: t }));
  }
  const keyword = searchKeyword.value.toLowerCase();
  return tables.value
    .filter(t => t.toLowerCase().includes(keyword))
    .map(t => ({ key: t, name: t }));
});

const testing = ref(false);
const isLoading = ref(false);

const tableColumns = [
  {
    title: '表名',
    dataIndex: 'name',
    key: 'name'
  }
];

const rowSelection = {
  selectedRowKeys: selectedTables,
  onChange: (keys: string[]) => {
    selectedTables.value = keys;
  }
};

onMounted(async () => {
  const id = route.query.id;
  if (id) {
    editMode.value = true;
    try {
      isLoading.value = true;
      const detail = await getDatasetDetail(String(id));
      
      formData.name = detail.name || '';
      formData.type = detail.type || '';
      formData.host = detail.host || '';
      formData.port = detail.port || null;
      formData.database = detail.database || '';
      formData.username = detail.username || '';
      formData.password = detail.password || '';
      formData.description = detail.description || '';
      
      const savedTables = detail.tables ? detail.tables.map(t => t.table) : [];
      selectedTables.value = savedTables;
      
      const connectionData = {
        type: formData.type,
        host: formData.host,
        port: formData.port || 0,
        username: formData.username,
        password: formData.password,
        database: formData.database
      };
      
      const allTables = await getTables(connectionData);
      tables.value = allTables;
    } catch (error) {
      console.error('获取数据集详情失败:', error);
      message.error('获取数据集详情失败');
    } finally {
      isLoading.value = false;
    }
  }
});

const handleGetTables = async () => {
  try {
    await formRef.value?.validateFields(['host', 'port', 'database', 'username']);
    
    testing.value = true;
    isLoading.value = true;
    message.loading('正在获取表列表...');
    
    const connectionData = {
      type: formData.type,
      host: formData.host,
      port: formData.port || 0,
      username: formData.username,
      password: formData.password,
      database: formData.database
    };
    
    const result = await getTables(connectionData);
    
    tables.value = result;
    
    testing.value = false;
    isLoading.value = false;
    message.destroy();
    
    if (tables.value.length > 0) {
      message.success(`成功获取到 ${tables.value.length} 张表`);
      currentStep.value = 2;
    } else {
      message.warning('未获取到任何表，请检查数据库连接信息');
    }
  } catch (error) {
    testing.value = false;
    isLoading.value = false;
    message.destroy();
    console.error('获取表列表失败:', error);
    message.error('获取表列表失败，请检查配置');
  }
};

const handlePrevStep = () => {
  if (currentStep.value > 1) {
    currentStep.value--;
  }
};

const handleNextStep = () => {
  if (currentStep.value === 1) {
    currentStep.value = 2;
    return;
  }
  
  if (selectedTables.value.length === 0) {
    message.warning('请至少选择一张表');
    return;
  }
  
  const configData = {
    id: route.query.id ? Number(route.query.id) : undefined,
    name: formData.name,
    type: formData.type,
    host: formData.host,
    port: formData.port,
    database: formData.database,
    username: formData.username,
    password: formData.password,
    description: formData.description,
    selectedTables: selectedTables.value
  };
  
  sessionStorage.setItem('datasetTableConfig', JSON.stringify(configData));
  
  router.push('/dataset-table-config');
};

const handleReset = () => {
  formRef.value?.resetFields();
  tables.value = [];
  selectedTables.value = [];
  currentStep.value = 1;
};

const handleBack = () => {
  router.push('/dataset-config');
};
</script>

<style lang="scss" scoped>
.main-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.database-connection-container {
  flex: 1;
  background-color: #f0f2f5;
  padding: 24px;
  box-sizing: border-box;
  overflow-y: auto;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
}

.back-button {
  color: #1890ff;
  margin-right: 8px;
}

.database-connection-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.database-connection-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.steps-container {
  margin-bottom: 24px;
  display: flex;
  justify-content: center;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
  
  :deep(.ant-steps) {
    width: 100%;
    
    .ant-steps-item-title {
      font-size: 14px;
    }
  }
}

.step-content {
  display: flex;
  justify-content: center;
}

.connection-sidebar {
  width: 100%;
  max-width: 1200px;
}

.connection-form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.connection-form-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.table-selection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-selection-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.no-tables {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: #999;
}

:deep(.ant-table) {
  margin-bottom: 0;
}
</style>