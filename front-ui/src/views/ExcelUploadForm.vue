<template>
  <div class="main-container">
    <!-- 添加左侧边栏 -->
    <LeftSidebar />
    
    <!-- 主内容区域 -->
    <div class="excel-upload-container">
      <div class="excel-upload-header">
        <div class="header-content">
          <h2>本地 Excel/CSV</h2>
          <a-button type="primary" :disabled="saving" @click="saveDataset">
            <template #icon>
              <LoadingOutlined v-if="saving" />
            </template>
            {{ saving ? '保存中...' : '保存' }}
          </a-button>
        </div>
      </div>
      
      <!-- 左右分栏布局 -->
      <div class="main-content-wrapper">
        <!-- 左侧上传表单 -->
        <div class="left-section">
          <div class="excel-upload-content">
            <!-- 文件上传区域 -->
            <div class="form-section">
              <a-form layout="vertical">
                <!-- 文件上传字段 -->
                <a-form-item label="文件" :required="true">
                  <div v-if="selectedFile" class="file-preview-card">
                    <div class="file-icon">
                      <img src="/images/excel.png" alt="Excel文件" class="file-type-icon" />
                    </div>
                    <div class="file-info">
                      <div class="file-name">{{ selectedFile.name }}</div>
                      <div class="file-size">{{ formatFileSize(selectedFile.size) }}</div>
                    </div>
                    <a-button type="text" class="file-remove-btn" @click="handleRemoveFile">
                      <DeleteOutlined />
                    </a-button>
                  </div>
                  
                  <div v-else-if="editMode && editModeFileName" class="file-preview-card">
                    <div class="file-icon">
                      <img src="/images/excel.png" alt="Excel文件" class="file-type-icon" />
                    </div>
                    <div class="file-info">
                      <div class="file-name">{{ editModeFileName }}</div>
                      <div class="file-size">-</div>
                    </div>
                    <a-button type="text" class="file-remove-btn" @click="editModeFileName = ''">
                      <DeleteOutlined />
                    </a-button>
                  </div>
                  
                  <div v-else class="upload-container">
                    <a-upload
                      name="file"
                      :multiple="false"
                      :headers="{ Authorization: `Bearer ${token}` }"
                      :before-upload="beforeUpload"
                      :show-upload-list="false"
                      @change="handleChange"
                    >
                      <div class="upload-area">
                        <span class="text-5xl">📁</span>
                        <p>点击或拖拽文件到此处上传</p>
                        <p class="upload-hint">支持 .xlsx, .xls 格式文件</p>
                      </div>
                    </a-upload>
                  </div>
                </a-form-item>
                
                <!-- 名称字段（必填） -->
                <a-form-item
                  label="名称"
                  :required="true"
                  :validate-status="nameError ? 'error' : ''"
                  :help="nameError || ''"
                >
                  <a-input
                    v-model:value="name"
                    placeholder="请输入名称"
                    :maxlength="50"
                    show-count
                  />
                </a-form-item>
                
                <!-- 描述字段（不必填） -->
                <a-form-item label="描述">
                  <a-textarea
                    v-model:value="description"
                    placeholder="请输入描述"
                    :maxlength="200"
                    show-count
                    :rows="3"
                  />
                </a-form-item>
                
                <!-- 内置Prompt字段（不必填） -->
                <a-form-item label="内置Prompt">
                  <a-textarea
                    v-model:value="innerPrompt"
                    placeholder="请输入内置Prompt"
                    :maxlength="500"
                    show-count
                    :rows="4"
                  />
                </a-form-item>
              </a-form>
            </div>
            
            <div class="action-buttons">
              <a-button @click="goBack">取消</a-button>
              <a-button type="primary" @click="submitForm">
                <template #icon>
                  <LoadingOutlined v-if="uploading" />
                </template>
                {{ uploading ? '解析中...' : '解析文件' }}
              </a-button>
            </div>
          </div>
        </div>
        
        <!-- 右侧表结构预览 -->
        <div class="right-section">
          <div class="table-structure-preview">
            <div class="preview-header">
              <div class="preview-title">
                {{ selectedFile ? selectedFile.name : '表结构' }}
              </div>
              
              <div :class="['table-container', { 'table-error': showTableError }]">
                <a-table 
                  :columns="columns" 
                  :data-source="tableData" 
                  :pagination="false"
                  :row-key="row => row.fieldName"
                  size="small"
                >
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'fieldType'">
                      <a-select
                        v-model:value="record.fieldType"
                        size="small"
                        style="width: 100%"
                        :options="fieldTypeOptions"
                      />
                    </template>
                    <template v-else-if="column.key === 'comment'">
                      <a-input 
                        v-model:value="record.description" 
                        size="small" 
                        placeholder="字段备注"
                        :maxlength="100"
                      />
                    </template>
                    <template v-else-if="column.key === 'fieldName'">
                      {{ record.fieldName }}
                    </template>
                  </template>
                </a-table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import { LoadingOutlined, DeleteOutlined } from '@ant-design/icons-vue';
import LeftSidebar from '../components/LeftSidebar.vue';
import type { UploadProps, ColumnsType } from 'ant-design-vue';
// 导入上传文件接口
import { uploadFile, createDataset, updateDataset, getDatasetDetail } from '../api/dataset';

const router = useRouter();
const route = useRoute();
const selectedFile = ref<File | null>(null);
const uploading = ref(false);
const saving = ref(false);
const token = localStorage.getItem('token') || '';

// 新增表单字段
const name = ref('');
const description = ref('');
const nameError = ref('');

// 新增状态：表格错误显示
const showTableError = ref(false);

// 表结构数据 - 模拟数据
const tableData = ref([]);

// 字段类型选项
const fieldTypeOptions = [
  { label: 'String', value: 'String' },
  { label: 'Integer', value: 'Integer' },
  { label: 'Long', value: 'Long' },
  { label: 'Double', value: 'Double' },
  { label: 'Float', value: 'Float' },
  { label: 'Boolean', value: 'Boolean' },
  { label: 'Date', value: 'Date' },
  { label: 'DateTime', value: 'DateTime' },
  { label: 'Text', value: 'Text' }
];

// 表格列定义
const columns: ColumnsType<typeof tableData.value[0]> = [
  { title: '字段名称', dataIndex: 'fieldName', key: 'fieldName', width: 150 },
  { title: '字段类型', dataIndex: 'fieldType', key: 'fieldType', width: 120 },
  { title: '字段备注', dataIndex: 'description', key: 'comment', width: 200 },
];

// 是否处于编辑模式
const editMode = ref(false);

// 编辑模式下的文件名（用于显示）
const editModeFileName = ref('');

// 加载编辑数据
const loadEditData = async () => {
  const id = route.query.id as string;
  if (id) {
    try {
      editMode.value = true;
      message.loading('正在加载数据...');
      
      // 使用API获取数据集详情
      const datasetDetail = await getDatasetDetail(id);
      
      // 回显表单数据
      name.value = datasetDetail.name;
      description.value = datasetDetail.description || '';
      innerPrompt.value = datasetDetail.prompt || '';
      
      // 回显文件名到文件上传框（如果存在table字段）
      if (datasetDetail.table) {
        // 存储文件名用于显示
        editModeFileName.value = datasetDetail.table;
      }
      
      // 回显表结构数据
      if (datasetDetail.fields) {
        tableData.value = datasetDetail.fields.map(field => ({
          fieldName: field.fieldName,
          fieldType: field.fieldType,
          description: field.description || ''
        }));
      }
      
      message.destroy();
      message.success('数据加载成功');
    } catch (error) {
      console.error('加载数据失败:', error);
      message.destroy();
      message.error('加载数据失败，请重试');
    }
  }
};

// 组件挂载时加载数据
onMounted(() => {
  loadEditData();
});

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) {return '0 Bytes';}
  
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

// 验证名称
const validateName = (): boolean => {
  if (!name.value.trim()) {
    nameError.value = '请输入名称';
    return false;
  }
  nameError.value = '';
  return true;
};

// 上传前检查
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isExcel = file.type === 'application/vnd.ms-excel' || 
                 file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
  const isLt2M = file.size / 1024 / 1024 < 2;
  
  if (!isExcel) {
    message.error('请上传Excel文件!');
  }

  if (isExcel && isLt2M) {
    selectedFile.value = file;
    // 重置表格错误状态
    showTableError.value = false;
    
    // 移除文件选择后立即调用uploadFile接口的代码
    // 仅设置选中的文件，不再创建formData和调用接口
  }
  
  return false; // 阻止自动上传，我们将在点击提交按钮时手动上传
};

// 处理文件选择
const handleChange: UploadProps['onChange'] = (info) => {
  const { file } = info;
  if (file.status === 'done') {
    message.success(`${file.name} 文件上传成功`);
  } else if (file.status === 'error') {
    message.error(`${file.name} 文件上传失败`);
  } else if (file.status === 'uploading') {
    uploading.value = true;
  }
};

// 移除文件
const handleRemoveFile = () => {
  selectedFile.value = null;
  editModeFileName.value = '';
  tableData.value = [];
};

// 提交表单
const submitForm = async () => {
  if (!selectedFile.value) {
    message.error('请选择要上传的文件');
    // 设置表格错误状态
    showTableError.value = true;
    return;
  }
  
  uploading.value = true;
  try {
    const result = await uploadFile(selectedFile.value, name.value.trim(), description.value.trim());
    
    // 将返回的数据渲染到右侧表结构中
    tableData.value = result.map(item => ({
      fieldName: item.fieldName,
      fieldType: item.fieldType,
      description: item.description || ''
    }));
    
    message.success('Excel文件上传成功');
  } catch (error) {
    console.error('上传失败:', error);
    message.error('上传失败，请重试');
  } finally {
    uploading.value = false;
  }
};

// 内置Prompt变量
const innerPrompt = ref('');

// 保存数据集
const saveDataset = async () => {
  // 验证名称
  if (!validateName()) {
    return;
  }
  
  if (!selectedFile.value) {
    message.error('请选择要上传的文件');
    // 设置表格错误状态
    showTableError.value = true;
    return;
  }
  
  if (tableData.value.length === 0) {
    message.error('请先上传文件以获取表结构');
    return;
  }
  
  saving.value = true;

  try {
    // 构造数据集对象
    const dataset = {
      name: name.value.trim(),
      description: description.value.trim(),
      type: 'excel',
      table: selectedFile.value ? selectedFile.value.name : '', // 去除文件扩展名
      prompt: innerPrompt.value.trim(),
      fields: tableData.value.map(item => ({
        fieldName: item.fieldName,
        fieldType: item.fieldType,
        description: item.description || ''
      }))
    };

    if (editMode.value) {
      // 编辑模式下更新数据集
      const id = route.query.id as string;
      // 将ID添加到dataset对象中
      const datasetWithId = { ...dataset, id };
      await updateDataset(datasetWithId);
      message.success('数据集更新成功');
    } else {
      // 新建模式下调用创建数据集接口
      await createDataset(dataset);
      message.success('数据集保存成功');
    }
    
    router.push('/dataset-config');
  } catch (error) {
    console.error('保存失败:', error);
    message.error('保存失败，请重试');
  } finally {
    saving.value = false;
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

.excel-upload-container {
  flex: 1;
  background-color: #f0f2f5;
  padding: 24px;
  box-sizing: border-box;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.excel-upload-header {
  margin-bottom: 32px;
  width: 100%;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.header-content h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
}

// 主内容包装器 - 左右分栏
.main-content-wrapper {
  display: flex;
  gap: 24px;
  width: 100%;
  flex: 1;
}

// 左侧上传表单区域
.left-section {
  width: 45%;
  max-width: 550px;
}

.excel-upload-content {
  width: 100%;
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.form-section {
  margin-bottom: 24px;
}

.file-preview-card {
  display: flex;
  align-items: center;
  padding: 12px;
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 6px;
  margin-bottom: 12px;
}

.file-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.file-type-icon {
  width: 32px;
  height: 32px;
  object-fit: contain;
}

.file-info {
  flex: 1;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
  color: #333;
}

.file-size {
  font-size: 12px;
  color: #666;
}

.file-remove-btn {
  color: #ff4d4f;
  margin-left: 8px;
}

.upload-container {
  margin-bottom: 12px;
  width: 100%;
}

.upload-area {
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  padding: 40px 20px;
  text-align: center;
  transition: all 0.3s;
  cursor: pointer;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  
  &:hover {
    border-color: #1890ff;
  }
}

.upload-area span {
  margin-bottom: 16px;
  display: block;
}

.upload-area p {
  margin: 0 0 8px 0;
  font-size: 16px;
  text-align: center;
}

.upload-hint {
  color: #999;
  font-size: 14px !important;
  margin-bottom: 0 !important;
}

.action-buttons {
  display: flex;
  gap: 16px;
  padding-top: 24px;
  margin-top: 24px;
  border-top: 1px solid #f0f0f0;
}

// 右侧表结构预览区域
.right-section {
  width: 55%;
  min-width: 700px;
}

.table-structure-preview {
  background-color: #fff;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.preview-header {
  margin-bottom: 16px;
}

.preview-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

// 新增表格容器样式
.table-container {
  border-radius: 6px;
  overflow: hidden;
  transition: all 0.3s;
}

// 表格错误样式 - 边框置红
.table-error {
  border: 2px solid #ff4d4f !important;
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.2);
}

// 响应式布局
@media (max-width: 1200px) {
  .main-content-wrapper {
    flex-direction: column;
  }
  
  .left-section,
  .right-section {
    width: 100%;
    max-width: none;
    min-width: unset;
  }
}
</style>