<template>
  <div class="main-container">
    <LeftSidebar />
    
    <div class="dataset-table-config-container">
      <div class="dataset-table-config-header">
        <div class="header-left">
          <a-button type="text" class="back-button" @click="handleBack">
            <template #icon>
              <ArrowLeftOutlined />
            </template>
          </a-button>
          <h2>数据集表配置</h2>
        </div>
        <div class="header-right">
          <a-button type="primary" :loading="saving || isLoading" @click="handleSubmit">
            创建连接
          </a-button>
        </div>
      </div>
      
      <div class="dataset-table-config-content">
        <div class="table-list-sidebar">
          <a-card title="数据表列表">
            <div class="table-list">
              <div 
                v-for="table in selectedTables" 
                :key="table"
                :class="['table-item', { 'active': activeTable === table }]"
                @click="handleTableChange(table)"
              >
                {{ table }}
              </div>
            </div>
          </a-card>
        </div>
        
        <div class="table-config-content">
          <a-card v-if="activeTable">
            <div class="description-tip">
              <a-alert 
                message="提示" 
                description="字段描述对模型问答效果有极大的影响，建议为每个字段都补充详细描述" 
                type="info" 
                show-icon 
                :closable="false" 
              />
            </div>
            
            <div class="table-info-header">
              <h3>{{ activeTable }} 表结构</h3>
            </div>
            
            <div class="fields-container">
              <a-table :columns="columns" :data-source="currentTableFields" row-key="fieldName" table-layout="fixed">
                <template #description="{ text, record }">
                  <a-tooltip :title="editingField === record.fieldName ? '按Enter保存，ESC取消' : '点击编辑'">
                    <div 
                      v-if="editingField !== record.fieldName"
                      :class="['field-description', { 'empty-description': emptyDescriptionFields.includes(record.fieldName) }]"
                      @click="startEditDescription(record)"
                    >
                      {{ text || '无描述' }}
                    </div>
                    <a-input 
                      v-else
                      v-model:value="editDescriptionValue"
                      :auto-focus="true"
                      @blur="saveDescription(record)"
                      @keyup.enter="saveDescription(record)"
                      @keyup.esc="cancelEditDescription"
                    />
                  </a-tooltip>
                </template>
              </a-table>
            </div>
            
            <a-card class="prompt-card">
              <div class="prompt-injection-header">
                <h3>prompt注入</h3>
              </div>
              <a-form-item>
                <a-textarea 
                  v-model:value="tablePrompts[activeTable]" 
                  :placeholder="`请输入${activeTable}表的提示词，帮助模型更好地理解如何查询数据`"
                  :rows="4"
                />
              </a-form-item>
            </a-card>
          </a-card>
          
          <div v-else class="no-table-selected">
            请从左侧选择数据表进行配置
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';

import LeftSidebar from '../components/LeftSidebar.vue';
import { createDatasetWithTables, updateDatasetWithTables, testDatabaseConnection, getDatasetDetail } from '../api/dataset';

interface FormData {
  id: number | undefined;
  name: string;
  type: string;
  host: string;
  port: number | null;
  database: string;
  username: string;
  password: string;
  description: string;
}

interface TableField {
  fieldName: string;
  fieldType: string;
  description: string;
}

const router = useRouter();

const formData = reactive<FormData>({
  id: undefined,
  name: '',
  type: '',
  host: '',
  port: null,
  database: '',
  username: '',
  password: '',
  description: ''
});

const selectedTables = ref<string[]>([]);
const activeTable = ref<string>('');

const tableFieldsMap = ref<Map<string, TableField[]>>(new Map());
const tablePrompts = reactive<Record<string, string>>({});

const editingField = ref<string>('');
const editDescriptionValue = ref<string>('');
const emptyDescriptionFields = ref<string[]>([]);

const saving = ref(false);
const isLoading = ref(false);

const columns = [
  {
    title: '字段名称',
    dataIndex: 'fieldName',
    key: 'fieldName',
    ellipsis: true
  },
  {
    title: '字段类型',
    dataIndex: 'fieldType',
    key: 'fieldType',
    ellipsis: true
  },
  {
    title: '字段描述',
    dataIndex: 'description',
    key: 'description',
    slots: { customRender: 'description' },
    ellipsis: true
  }
];

const currentTableFields = computed(() => {
  return tableFieldsMap.value.get(activeTable.value) || [];
});

const loadDataFromStorage = () => {
  const storedData = sessionStorage.getItem('datasetTableConfig');
  if (!storedData) {
    message.error('未获取到连接信息，请返回重新选择');
    router.push('/database-connection-form');
    return false;
  }
  
  try {
    const data = JSON.parse(storedData);
    formData.id = data.id;
    formData.name = data.name;
    formData.type = data.type;
    formData.host = data.host;
    formData.port = data.port;
    formData.database = data.database;
    formData.username = data.username;
    formData.password = data.password;
    formData.description = data.description;
    selectedTables.value = data.selectedTables;
    
    if (selectedTables.value.length > 0) {
      activeTable.value = selectedTables.value[0];
    }
    
    return true;
  } catch (error) {
    message.error('解析连接信息失败');
    router.push('/database-connection-form');
    return false;
  }
};

const loadTableSchema = async (tableName: string) => {
  if (tableFieldsMap.value.has(tableName)) {
    return;
  }
  
  isLoading.value = true;
  
  try {
    const structureData = {
      type: formData.type,
      host: formData.host,
      port: formData.port || 0,
      username: formData.username,
      password: formData.password,
      database: formData.database,
      table: tableName
    };
    
    const result = await testDatabaseConnection(structureData);
    
    tableFieldsMap.value.set(tableName, result.map(field => ({
      fieldName: field.fieldName,
      fieldType: field.fieldType,
      description: field.description || ''
    })));
    
    if (formData.id) {
      const detail = await getDatasetDetail(formData.id.toString());
      if (detail.tables) {
        const tableConfig = detail.tables.find(t => t.table === tableName);
        if (tableConfig && tableConfig.prompt) {
          tablePrompts[tableName] = tableConfig.prompt;
          return;
        }
      }
    }
    
    if (!tablePrompts[tableName]) {
      tablePrompts[tableName] = '';
    }
  } catch (error) {
    console.error('加载表结构失败:', error);
    message.error(`加载 ${tableName} 表结构失败`);
  } finally {
    isLoading.value = false;
  }
};

onMounted(async () => {
  const success = loadDataFromStorage();
  if (success && activeTable.value) {
    await loadTableSchema(activeTable.value);
  }
});

const handleTableChange = async (tableName: string) => {
  activeTable.value = tableName;
  await loadTableSchema(tableName);
};

const handleSubmit = async () => {
  if (selectedTables.value.length === 0) {
    message.warning('请至少选择一张表');
    return;
  }
  
  let hasEmptyDescription = false;
  selectedTables.value.forEach(table => {
    const fields = tableFieldsMap.value.get(table) || [];
    const emptyFields = fields.filter(field => !field.description?.trim());
    if (emptyFields.length > 0) {
      hasEmptyDescription = true;
    }
  });
  
  if (hasEmptyDescription) {
    message.warning('部分字段描述为空，请填写描述或忽略继续');
  }
  
  saving.value = true;
  isLoading.value = true;
  
  try {
    const tablesData = selectedTables.value.map(tableName => {
      const fields = tableFieldsMap.value.get(tableName) || [];
      return {
        table: tableName,
        prompt: tablePrompts[tableName] || '',
        fields: fields.map(field => ({
          fieldName: field.fieldName,
          fieldType: field.fieldType,
          description: field.description
        }))
      };
    });
    
    const submitData = {
      name: formData.name,
      type: formData.type,
      host: formData.host,
      port: formData.port || 0,
      database: formData.database,
      username: formData.username,
      password: formData.password,
      description: formData.description,
      tables: tablesData
    };
    
    if (formData.id) {
      await updateDatasetWithTables({ ...submitData, id: formData.id });
      message.success('成功更新数据集');
    } else {
      await createDatasetWithTables(submitData);
      message.success('成功创建数据集');
    }
    
    saving.value = false;
    isLoading.value = false;
    
    sessionStorage.removeItem('datasetTableConfig');
    
    router.push('/dataset-config');
  } catch (error) {
    saving.value = false;
    isLoading.value = false;
    console.error('表单提交失败:', error);
    message.error('表单提交失败，请重试');
  }
};

const handleBack = () => {
  sessionStorage.removeItem('datasetTableConfig');
  router.push('/database-connection-form');
};

const startEditDescription = (record: TableField) => {
  editingField.value = record.fieldName;
  editDescriptionValue.value = record.description;
};

const saveDescription = (record: TableField) => {
  const fields = tableFieldsMap.value.get(activeTable.value);
  if (fields) {
    const field = fields.find(f => f.fieldName === record.fieldName);
    if (field) {
      field.description = editDescriptionValue.value;
    }
  }
  editingField.value = '';
  message.success('描述保存成功');
};

const cancelEditDescription = () => {
  editingField.value = '';
};
</script>

<style lang="scss" scoped>
.main-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.dataset-table-config-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
  padding: 24px;
  box-sizing: border-box;
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

.dataset-table-config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.dataset-table-config-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.dataset-table-config-content {
  flex: 1;
  display: flex;
  gap: 24px;
  margin-top: 24px;
  min-height: 0;
}

.table-list-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.table-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.table-item {
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.table-item:hover {
  background: #f5f5f5;
}

.table-item.active {
  background: #e6f7ff;
  border-color: #1890ff;
  color: #1890ff;
}

.table-config-content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

.description-tip {
  .ant-alert {
    margin-bottom: 0;
  }
}

.table-info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}

.table-info-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.fields-container {
  margin-top: 16px;
}

.field-description {
  display: inline-block;
  padding: 2px 4px;
  border-radius: 4px;
  transition: all 0.3s;
  cursor: pointer;
}

.field-description:hover {
  background-color: #f5f5f5;
}

.no-table-selected {
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
  color: #999;
}

.prompt-card {
  margin-top: 16px;
  flex-shrink: 0;
}

.prompt-injection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.prompt-injection-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

:deep(.ant-table) {
  margin-bottom: 0;
}

:deep(.ant-input-textarea.ant-input-textarea-autosize) {
  resize: vertical;
}

@media (max-width: 1000px) {
  .dataset-table-config-content {
    flex-direction: column;
  }
  
  .table-list-sidebar {
    width: 100%;
  }
}
</style>