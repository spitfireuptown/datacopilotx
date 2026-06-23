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
          <a-button type="primary" :loading="saving || isLoading" @click="handleSubmit">
            {{ editMode ? '保存修改' : '创建连接' }}
          </a-button>
        </div>
      </div>
      
      <!-- 内容区域分为左侧数据库连接和右侧表字段信息 -->
      <div class="database-connection-content">
        <!-- 左侧数据库连接信息 -->
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
                label="数据表"
                name="table"
              >
                <a-input v-model:value="formData.table" placeholder="请输入Table名称（可选）" />
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

              <!-- 添加数据集描述字段 -->
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
                  <a-button type="primary" :loading="testing || isLoading" @click="handleTestConnection">
                    获取schema信息
                  </a-button>
                  <a-button @click="handleReset">
                    重置
                  </a-button>
                </a-space>
              </a-form-item>
            </a-form>
          </a-card>
        </div>

        <!-- 右侧表字段信息 -->
        <div class="table-fields-container">
          <a-card v-if="selectedTable">
            <!-- 添加提示信息 -->
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
              <h3>{{ selectedTable }} 表结构</h3>
            </div>
            
            <div class="fields-container">
              <a-table :columns="columns" :data-source="tableFields" row-key="fieldName" table-layout="fixed">
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
          <!-- 在表结构卡片后添加prompt注入卡片 -->
          </a-card>
          
          <!-- 添加prompt注入卡片 -->
          <a-card v-if="selectedTable" class="mt-4">
            <div class="prompt-injection-header">
              <h3>prompt注入</h3>
            </div>
            <a-form-item>
              <a-textarea 
                v-model:value="formData.prompt" 
                placeholder="请输入提示词，帮助模型更好地理解如何查询数据"
                :rows="4"
              />
            </a-form-item>
          </a-card>

          <!-- 添加联表关系配置卡片 -->
          <a-card v-if="selectedTable" class="mt-4">
            <div class="relations-header">
              <h3>联表关系配置</h3>
              <a-button type="primary" size="small" @click="handleAddRelation">
                添加关联
              </a-button>
            </div>
            <a-alert 
              message="提示" 
              description="配置多表关联关系后，支持跨表查询。例如：orders.user_id = users.id" 
              type="info" 
              show-icon 
              :closable="false" 
              class="mb-4"
            />
            <div v-if="tableRelations.length === 0" class="no-relations">
              暂无关联关系配置
            </div>
            <div v-else class="relations-list">
              <div v-for="(relation, index) in tableRelations" :key="index" class="relation-item">
                <a-space wrap>
                  <a-input v-model:value="relation.fromTable" placeholder="从表名称" style="width: 150px" />
                  <a-select v-model:value="relation.fromField" placeholder="从表字段" style="width: 150px">
                    <a-select-option v-for="fld in tableFields.map(f => f.fieldName)" :key="fld" :value="fld">{{ fld }}</a-select-option>
                  </a-select>
                  <span>=</span>
                  <a-input v-model:value="relation.toTable" placeholder="关联表名称" style="width: 150px" />
                  <a-input v-model:value="relation.toField" placeholder="关联表字段" style="width: 150px" />
                  <a-select v-model:value="relation.relationType" placeholder="关联类型" style="width: 120px">
                    <a-select-option value="INNER JOIN">INNER JOIN</a-select-option>
                    <a-select-option value="LEFT JOIN">LEFT JOIN</a-select-option>
                    <a-select-option value="RIGHT JOIN">RIGHT JOIN</a-select-option>
                  </a-select>
                </a-space>
                <a-button type="text" danger size="small" @click="handleRemoveRelation(index)">
                  删除
                </a-button>
              </div>
            </div>
          </a-card>
          
          <div v-else class="no-table-selected">
            请先连接数据库并选择数据表
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 导入路由和图标
import { ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import type { FormInstance } from 'ant-design-vue';
import { message } from 'ant-design-vue';

// 导入侧边栏组件
import LeftSidebar from '../components/LeftSidebar.vue';

// 导入数据集API
import { createDataset, updateDataset, getDatasetDetail, testDatabaseConnection } from '../api/dataset.js';

// 创建路由实例
const router = useRouter();
const route = useRoute();

// 定义表单数据类型
interface FormData {
  name: string;
  type: string;
  host: string;
  port: number | null;
  database: string;
  table: string;
  username: string;
  password: string;
  description: string; // 添加description字段
  prompt: string; // 添加prompt字段
}

// 定义表字段类型
interface TableField {
  fieldName: string;
  fieldType: string;
  description: string;
}

// 定义联表关系类型
interface TableRelation {
  fromTable: string;
  fromField: string;
  toTable: string;
  toField: string;
  relationType: string;
}

// 表单引用
const formRef = ref<FormInstance>();

// 是否处于编辑模式
const editMode = ref(false);

// 编辑的连接ID
const editConnectionId = ref('');

// 表单数据
const formData = reactive<FormData>({
  name: '',
  type: '',
  host: '',
  port: null,
  database: '',
  table: '',
  username: '',
  password: '',
  description: '', // 初始化description字段
  prompt: '' // 初始化prompt字段
});

// 表格列表
const tables = ref<string[]>([]);

// 选中的数据表
const selectedTable = ref<string>('');

// 表字段信息
const tableFields = ref<TableField[]>([]);

// 正在编辑的字段
const editingField = ref<string>('');

// 编辑的描述值
const editDescriptionValue = ref<string>('');

// 用于标记哪些字段描述为空（用于置红提醒）
const emptyDescriptionFields = ref<string[]>([]);

// 联表关系配置
const tableRelations = ref<TableRelation[]>([]);

// 保存状态
const saving = ref(false);

// 测试连接状态
const testing = ref(false);

// 添加全局loading状态
const isLoading = ref(false);

// 表格列配置
const columns = [
  {
    title: '字段名称',
    dataIndex: 'fieldName',
    key: 'fieldName',
    ellipsis: true // 添加省略显示，防止内容过长
  },
  {
    title: '字段类型',
    dataIndex: 'fieldType',
    key: 'fieldType',
    ellipsis: true // 添加省略显示
  },
  {
    title: '字段描述',
    dataIndex: 'description',
    key: 'description',
    slots: { customRender: 'description' },
    ellipsis: true // 添加省略显示
  }
];

// 加载编辑数据
const loadEditData = async () => {
  const id = route.query.id as string;
  if (id) {
    editMode.value = true;
    editConnectionId.value = id;
    
    try {
      isLoading.value = true;
      message.loading('正在加载数据...');
      
      // 使用API获取数据集详情
      const connectionData = await getDatasetDetail(editConnectionId.value);
      
      testing.value = false;
    message.destroy();
      
      // 填充表单数据
      formData.name = connectionData.name;
      formData.type = connectionData.type;
      formData.host = connectionData.host;
      formData.port = connectionData.port;
      formData.database = connectionData.database;
      formData.table = connectionData.table;
      formData.username = connectionData.username;
      formData.password = connectionData.password;
      
      // 加载数据集描述
      if (connectionData.description) {
        formData.description = connectionData.description;
      }
      
      // 加载prompt
      if (connectionData.prompt) {
        formData.prompt = connectionData.prompt;
      }
      
      // 如果有表结构信息，加载表结构
      if (connectionData.fields && connectionData.fields.length > 0) {
        tableFields.value = connectionData.fields.map(field => ({
          fieldName: field.fieldName,
          fieldType: field.fieldType,
          description: field.description || ''
        }));
        selectedTable.value = connectionData.table;
      } else if (connectionData.table) {
        // 否则调用获取表结构的API
        onTableChange(connectionData.table);
      }

      // 加载联表关系
      if (connectionData.relations && connectionData.relations.length > 0) {
        tableRelations.value = connectionData.relations;
      }
    } catch (error) {
      message.destroy();
      console.error('加载数据失败:', error);
      message.error('加载数据失败，请重试');
    } finally {
      isLoading.value = false;
    }
  }
};

// 组件挂载时加载数据
onMounted(() => {
  loadEditData();
});

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value?.validate();
    
    // 检查字段描述是否为空
    const emptyFields = tableFields.value.filter(field => !field.description?.trim());
    if (emptyFields.length > 0) {
      // 更新空描述字段列表用于置红提醒
      emptyDescriptionFields.value = emptyFields.map(field => field.fieldName);
      
      message.warning(`有${emptyFields.length}个字段描述为空，请填写描述或忽略继续`);
      // 滚动到右侧表格区域
      document.querySelector('.table-fields-container')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
    
    saving.value = true;
    isLoading.value = true;
    
    // 准备提交数据
    const submitData = {
      name: formData.name,
      type: formData.type,
      host: formData.host,
      port: formData.port || 0,
      database: formData.database,
      table: formData.table,
      username: formData.username,
      password: formData.password,
      description: formData.description, // 添加description参数
      prompt: formData.prompt, // 添加prompt参数
      // 添加表结构字段描述信息
      fields: tableFields.value.map(field => ({
        fieldName: field.fieldName,
        fieldType: field.fieldType,
        description: field.description
      })),
      // 添加联表关系配置
      relations: tableRelations.value
    };
    
    if (editMode.value) { 
        // 更新现有数据集
        submitData.id = editConnectionId.value;
        await updateDataset(submitData);
        message.destroy();
        message.success('数据集更新成功');
      } else {
        // 创建新数据集
        await createDataset(submitData);
        message.destroy();
        message.success('数据集创建成功');
      }
      
      saving.value = false;
      
      // 返回数据集配置页面
      router.push('/dataset-config');
  } catch (error) {
      saving.value = false;
      message.destroy();
      console.error('表单提交失败:', error);
      message.error('表单提交失败，请重试');
    } finally {
      isLoading.value = false;
    }
  };

// 重置表单
const handleReset = () => {
  formRef.value?.resetFields();
  tables.value = [];
  selectedTable.value = '';
  tableFields.value = [];
};

// 测试连接
const handleTestConnection = async () => {
  try {
    await formRef.value?.validateFields(['host', 'port', 'database', 'username']);
    
    testing.value = true;
    isLoading.value = true;
    message.loading('正在测试连接...');
    
    // 准备连接测试数据
    const connectionData = {
      type: formData.type,
      host: formData.host,
      port: formData.port || 0,
      username: formData.username,
      password: formData.password,
      database: formData.database,
      table: formData.table,
      name: formData.name
    };
    
    // 调用API测试连接并获取字段信息
    const result = await testDatabaseConnection(connectionData);
    
    // 更新表字段数据
    tableFields.value = result.map(field => ({
      fieldName: field.fieldName,
      fieldType: field.fieldType,
      description: field.description || ''
    }));
    
    // 设置选中的表名，这是关键！
    selectedTable.value = formData.table || '未知表';
    
    // 重置测试状态和显示成功消息
    testing.value = false;
    message.destroy();
    message.success('表结构加载成功');
  } catch (error) {
    testing.value = false;
    message.destroy();
    console.error('测试连接失败:', error);
    message.error('数据库连接失败，请检查配置');
  } finally {
    isLoading.value = false;
  }
};

// 表选择变化
const onTableChange = async (tableName: string) => {
  if (!tableName) {return;}
  
  formData.table = tableName;
  selectedTable.value = tableName;
  
  message.loading('正在加载表结构...');
  
  try {
    isLoading.value = true;
    // 准备获取表结构的数据
    const structureData = {
      type: formData.type,
      host: formData.host,
      port: formData.port || 0,
      username: formData.username,
      password: formData.password,
      database: formData.database,
      table: tableName,
      name: formData.name
    };
    
    // 调用API获取表结构
    const result = await testDatabaseConnection(structureData);
    
    message.destroy();
    
    // 更新表结构字段
    tableFields.value = result.map(field => ({
      fieldName: field.fieldName,
      fieldType: field.fieldType,
      description: field.description || ''
    }));
    
    message.success('表结构加载成功');
  } catch (error) {
    message.destroy();
    console.error('加载表结构失败:', error);
    message.error('加载表结构失败，请重试');
  } finally {
    isLoading.value = false;
  }
};

// 开始编辑字段描述
const startEditDescription = (record: TableField) => {
  editingField.value = record.fieldName;
  editDescriptionValue.value = record.description;
};

// 保存字段描述
const saveDescription = (record: TableField) => {
  const field = tableFields.value.find(f => f.fieldName === record.fieldName);
  if (field) {
    field.description = editDescriptionValue.value;
  }
  editingField.value = '';
  message.success('描述保存成功');
};

// 取消编辑字段描述
const cancelEditDescription = () => {
  editingField.value = '';
};

// 添加联表关系
const handleAddRelation = () => {
  tableRelations.value.push({
    fromTable: '',
    fromField: '',
    toTable: '',
    toField: '',
    relationType: 'INNER JOIN'
  });
};

// 移除联表关系
const handleRemoveRelation = (index: number) => {
  tableRelations.value.splice(index, 1);
};

// 移除数据集描述相关的编辑方法

// 在脚本部分添加返回方法
const handleBack = () => {
  router.push('/dataset-config');
};
</script>

<style lang="scss" scoped>
/* 修改样式以适应新的布局 */
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

/* 头部样式 */
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

/* 内容区域样式 */
.database-connection-content {
  display: flex;
  gap: 24px;
}

/* 左侧数据库连接样式 */
.connection-sidebar {
  width: 400px;
  flex-shrink: 0;
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

/* 右侧表字段信息样式 */
.table-fields-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px; /* 添加卡片之间的间距 */
  max-height: calc(100% - 40px); /* 限制最大高度，留出一些空间 */
  overflow: hidden;
}
.prompt-injection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.mt-4 {
  margin-top: 16px;
}
// .
.prompt-injection-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.table-info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-info-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.fields-container {
  flex: 1;
  overflow: auto;
  max-height: 500px; /* 设置最大高度限制，防止表结构过高 */
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
  height: 200px;
  color: #999;
}

/* 联表关系配置样式 */
.relations-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.no-relations {
  color: #999;
  text-align: center;
  padding: 20px;
}

.relations-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.relation-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  background: #fafafa;
  border-radius: 4px;
}

/* 表格样式 */
:deep(.ant-table) {
  margin-bottom: 0;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .database-connection-content {
    flex-direction: column;
  }

  .connection-sidebar {
    width: 100%;
  }
}

/* 提示信息样式 */
.description-tip {
  margin-bottom: 16px;
  .ant-alert {
    margin-bottom: 0;
  }
}

/* 修改数据集描述样式 - 移除hover效果，适应直接文本框 */
.dataset-description-section {
  margin-bottom: 20px;
}

.dataset-description-section h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
}

.dataset-description {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background-color: #fafafa;
  min-height: 60px;
  cursor: pointer;
  transition: all 0.3s;
  white-space: pre-wrap;
  word-break: break-word;
}

.dataset-description:hover {
  border-color: #40a9ff;
  background-color: #fff;
}

/* 确保数据集描述文本框在左侧表单中有适当的样式 */
:deep(.ant-input-textarea.ant-input-textarea-autosize) {
  resize: vertical;
}
</style>