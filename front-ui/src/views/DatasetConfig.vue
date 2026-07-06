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
              <div class="header-actions">
                <a-button 
                  v-if="selectedRowKeys.length > 0" 
                  danger 
                  @click="handleBatchDelete"
                >
                  批量删除 ({{ selectedRowKeys.length }})
                </a-button>
                <a-button type="primary" @click="showForm">
                  新建
                </a-button>
              </div>
            </div>
            <a-table :columns="columns" :data-source="datasets" row-key="id" :row-selection="rowSelection">
              <template #action="{ record }">
                <a-space size="small">
                  <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
                  <a-button type="link" size="small" @click="handleRelation(record)">关联</a-button>
                  <a-button type="link" danger size="small" @click="handleDelete(record.id)">删除</a-button>
                </a-space>
              </template>
            </a-table>
          </div>
        </div>
      </div>
    </div>

    <!-- 关联管理模态框 -->
    <a-modal
      v-model:open="showRelationModal"
      :title="`数据集关联 - ${selectedDataset?.name}`"
      :footer="null"
      width="700px"
    >
      <div v-if="selectedDataset">
        <!-- 已关联的数据集列表 -->
        <div class="related-datasets">
          <h4>已关联的数据集</h4>
          <div v-if="currentRelations.length === 0" class="empty-relations">
            暂无关联数据集
          </div>
          <a-list v-else :data-source="currentRelations" :locale="{ emptyText: '' }">
            <a-list-item v-for="(relation, index) in currentRelations" :key="index">
                <a-list-item-meta :title="relation.datasetName">
                  <template #description>
                    {{ relation.leftTable }}.{{ relation.leftField }} {{ relation.relationType }} {{ relation.rightTable }}.{{ relation.rightField }}
                  </template>
                </a-list-item-meta>
                <a-button type="text" danger size="small" @click="handleRemoveRelation(relation)">
                  删除关联
                </a-button>
              </a-list-item>
          </a-list>
        </div>

        <!-- 添加关联 -->
        <div class="add-relation">
          <h4>添加关联</h4>
          <a-form layout="vertical">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="当前数据集左表">
                  <a-select v-model:value="relationForm.leftTable" placeholder="请选择左表" @change="onLeftTableChange">
                    <a-select-option 
                      v-for="table in currentDatasetTables" 
                      :key="table.table" 
                      :value="table.table"
                    >
                      {{ table.table }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="当前数据集右表">
                  <a-select v-model:value="relationForm.rightTable" placeholder="请选择右表" @change="onRightTableChange">
                    <a-select-option 
                      v-for="table in currentDatasetTables" 
                      :key="table.table" 
                      :value="table.table"
                      :disabled="relationForm.leftTable === table.table"
                    >
                      {{ table.table }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="当前数据集左表字段">
                  <a-select v-model:value="relationForm.leftField" placeholder="请选择左表字段">
                    <a-select-option 
                      v-for="field in leftTableFields" 
                      :key="field.fieldName" 
                      :value="field.fieldName"
                    >
                      {{ field.fieldName }} ({{ field.fieldType }})
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="当前数据集右表字段">
                  <a-select v-model:value="relationForm.rightField" placeholder="请选择右表字段">
                    <a-select-option 
                      v-for="field in rightTableFields" 
                      :key="field.fieldName" 
                      :value="field.fieldName"
                    >
                      {{ field.fieldName }} ({{ field.fieldType }})
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="关联类型">
              <a-select v-model:value="relationForm.relationType" placeholder="请选择关联类型">
                <a-select-option value="INNER JOIN">INNER JOIN（内连接）</a-select-option>
                <a-select-option value="LEFT JOIN">LEFT JOIN（左连接）</a-select-option>
                <a-select-option value="RIGHT JOIN">RIGHT JOIN（右连接）</a-select-option>
              </a-select>
            </a-form-item>
            <div class="relation-form-footer">
              <a-button @click="resetRelationForm">重置</a-button>
              <a-button type="primary" @click="handleAddRelation">添加关联</a-button>
            </div>
          </a-form>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
// 导入路由
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
// 导入Spin组件用于loading效果
import { Spin as ASpin } from 'ant-design-vue';

// 导入侧边栏组件
import LeftSidebar from '../components/LeftSidebar.vue';

// 导入数据集API
import { getDatasetList, deleteDataset, getDatasetDetail, addDatasetRelation, deleteDatasetRelation, getDatasetRelations, getTableFields } from '../api/dataset';

// 创建路由实例
const router = useRouter();

interface TableInfo {
  table: string;
  prompt: string;
  fields?: Array<{
    fieldName: string;
    fieldType: string;
    description: string;
  }>;
}

interface Dataset {
  id: number;
  name: string;
  type: string;
  host: string;
  port: number;
  database: string;
  table: string;
  username: string;
  createTime: string;
  creatorName: string;
  tables?: TableInfo[];
}

interface Relation {
  id: number;
  datasetId: number;
  datasetName: string;
  leftTable: string;
  leftField: string;
  rightTable: string;
  rightField: string;
  relationType: string;
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
    title: '创建者',
    dataIndex: 'creatorName',
    key: 'creatorName'
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
// 选中的行key
const selectedRowKeys = ref<number[]>([]);

// 表格行选择配置
const rowSelection = {
  selectedRowKeys,
  onChange: (keys: number[]) => {
    selectedRowKeys.value = keys;
  }
};

// 关联模态框状态
const showRelationModal = ref(false);
// 当前选中的数据集
const selectedDataset = ref<Dataset | null>(null);
// 关联关系列表
const relations = ref<Relation[]>([]);

const relationForm = ref<{
  leftTable: string;
  rightTable: string;
  leftField: string;
  rightField: string;
  relationType: string;
}>({
  leftTable: '',
  rightTable: '',
  leftField: '',
  rightField: '',
  relationType: 'INNER JOIN'
});

// 获取同类型的数据集（排除当前数据集）
const sameTypeDatasets = computed(() => {
  if (!selectedDataset.value) {return [];}
  return datasets.value.filter(d => 
    d.type === selectedDataset.value?.type && 
    d.id !== selectedDataset.value?.id
  );
});

const currentDatasetTables = computed(() => {
  return selectedDataset.value?.tables || [];
});

const leftTableFields = computed(() => {
  if (!relationForm.value.leftTable) {return [];}
  const table = selectedDataset.value?.tables?.find(t => t.table === relationForm.value.leftTable);
  return table?.fields || [];
});

const rightTableFields = computed(() => {
  if (!relationForm.value.rightTable) {return [];}
  const table = selectedDataset.value?.tables?.find(t => t.table === relationForm.value.rightTable);
  return table?.fields || [];
});

// 当前数据集的关联关系
const currentRelations = computed(() => {
  if (!selectedDataset.value) {return [];}
  return relations.value.filter(r => r.datasetId === selectedDataset.value!.id);
});

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

// 加载关联关系
const loadRelations = async (datasetId: number | string) => {
  if (datasetId) {
    try {
      const result = await getDatasetRelations(String(datasetId));
      relations.value = result || [];
    } catch (error) {
      console.error('获取关联关系失败:', error);
      relations.value = [];
    }
  } else {
    relations.value = [];
  }
};

// 跳转到表单页面
const showForm = () => {
  router.push('/data-source-type-select');
};

// 编辑数据集
const handleEdit = (record: Dataset) => {
  router.push({ path: '/database-connection-form', query: { id: record.id } });
};

// 关联管理
const handleRelation = async (record: Dataset) => {
  try {
    const detail = await getDatasetDetail(String(record.id));
    console.log('Dataset detail:', JSON.stringify(detail));
    console.log('Tables:', JSON.stringify(detail.tables?.map(t => ({table: t.table, fieldCount: t.fields?.length}))));
    selectedDataset.value = detail;
    await loadRelations(record.id);
    resetRelationForm();
    showRelationModal.value = true;
  } catch (error) {
    console.error('获取数据集详情失败:', error);
    message.error('获取数据集详情失败');
  }
};

// 删除数据集
const handleDelete = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个数据集吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteDataset(String(id));
        await loadDatasets();
        selectedRowKeys.value = [];
        message.success('删除成功');
      } catch (error) {
        console.error('删除数据集失败:', error);
        message.error('删除数据集失败');
      }
    }
  });
};

// 批量删除数据集
const handleBatchDelete = () => {
  Modal.confirm({
    title: '确认批量删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个数据集吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        for (const id of selectedRowKeys.value) {
          await deleteDataset(String(id));
        }
        await loadDatasets();
        selectedRowKeys.value = [];
        message.success('批量删除成功');
      } catch (error) {
        console.error('批量删除数据集失败:', error);
        message.error('批量删除数据集失败');
      }
    }
  });
};

const onLeftTableChange = () => {
  relationForm.value.leftField = '';
  if (relationForm.value.rightTable === relationForm.value.leftTable) {
    relationForm.value.rightTable = '';
    relationForm.value.rightField = '';
  }
};

const onRightTableChange = async () => {
  relationForm.value.rightField = '';
  
  if (!relationForm.value.rightTable || !selectedDataset.value) {
    return;
  }
  
  const tableInfo = selectedDataset.value.tables?.find(t => t.table === relationForm.value.rightTable);
  if (tableInfo && tableInfo.fields && tableInfo.fields.length > 0) {
    return;
  }
  
  try {
    const fields = await getTableFields({
      type: selectedDataset.value.type,
      host: selectedDataset.value.host,
      port: selectedDataset.value.port,
      username: selectedDataset.value.username,
      password: selectedDataset.value.password,
      database: selectedDataset.value.database,
      table: relationForm.value.rightTable
    });
    
    if (selectedDataset.value.tables) {
      const tableIndex = selectedDataset.value.tables.findIndex(t => t.table === relationForm.value.rightTable);
      if (tableIndex !== -1) {
        selectedDataset.value.tables[tableIndex].fields = fields;
      }
    }
  } catch (error) {
    console.error('获取表字段失败:', error);
  }
};

const resetRelationForm = () => {
  relationForm.value = {
    leftTable: '',
    rightTable: '',
    leftField: '',
    rightField: '',
    relationType: 'INNER JOIN'
  };
};

const handleAddRelation = async () => {
  if (!selectedDataset.value || 
      !relationForm.value.leftTable || !relationForm.value.rightTable ||
      !relationForm.value.leftField || !relationForm.value.rightField) {
    message.error('请填写完整的关联信息');
    return;
  }

  if (relationForm.value.leftTable === relationForm.value.rightTable) {
    message.error('左表和右表不能相同');
    return;
  }

  const newRelation = {
    datasetId: selectedDataset.value.id,
    leftTable: relationForm.value.leftTable,
    leftField: relationForm.value.leftField,
    rightTable: relationForm.value.rightTable,
    rightField: relationForm.value.rightField,
    relationType: relationForm.value.relationType
  };

  const exists = relations.value.some(r => 
    r.datasetId === newRelation.datasetId &&
    r.leftTable === newRelation.leftTable &&
    r.leftField === newRelation.leftField &&
    r.rightTable === newRelation.rightTable &&
    r.rightField === newRelation.rightField
  );

  if (exists) {
    message.warning('该关联已存在');
    return;
  }

  try {
    await addDatasetRelation(newRelation);
    await loadRelations(selectedDataset.value.id);
    resetRelationForm();
    message.success('关联添加成功');
  } catch (error) {
    console.error('添加关联失败:', error);
    message.error('添加关联失败');
  }
};

// 删除关联
const handleRemoveRelation = (relation: Relation) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这个关联吗？',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await deleteDatasetRelation(String(relation.id));
        // 重新加载关联关系
        if (selectedDataset.value) {
          await loadRelations(selectedDataset.value.id);
        }
        message.success('关联删除成功');
      } catch (error) {
        console.error('删除关联失败:', error);
        message.error('删除关联失败');
      }
    }
  });
};

// 组件挂载时加载数据
onMounted(() => {
  loadDatasets();
});
</script>

<style lang="scss" scoped>
.main-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.dataset-config-container {
  flex: 1;
  padding: 24px;
  background: #f5f5f5;
  overflow-y: auto;
}

.global-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}

.dataset-config-header {
  margin-bottom: 24px;
}

.dataset-config-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.dataset-config-content {
  background: white;
  border-radius: 8px;
  padding: 24px;
}

.dataset-list {
  width: 100%;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

/* 关联模态框样式 */
.current-dataset {
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 24px;
}

.current-dataset h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 500;
}

.current-dataset p {
  margin: 4px 0;
  font-size: 13px;
}

.related-datasets {
  margin-bottom: 24px;
}

.related-datasets h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 500;
}

.empty-relations {
  color: #999;
  padding: 16px;
  text-align: center;
  background: #fafafa;
  border-radius: 4px;
}

.add-relation h4 {
  margin: 0 0 16px 0;
  font-size: 14px;
  font-weight: 500;
}

.relation-form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}
</style>