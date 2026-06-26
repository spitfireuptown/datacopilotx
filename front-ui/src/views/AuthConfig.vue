<template>
  <div class="main-container">
    <LeftSidebar />
    <div class="auth-container">
      <div class="page-header">
        <h2>数据权限配置</h2>
      </div>

      <a-tabs v-model:activeKey="activeTab" class="auth-tabs">
        <a-tab-pane key="permission" tab="权限规则">
          <div class="tab-content">
            <div class="toolbar">
              <a-select v-model:value="permissionType" placeholder="权限类型">
                <a-select-option value="all">全部</a-select-option>
                <a-select-option value="row">行权限</a-select-option>
                <a-select-option value="column">列权限</a-select-option>
              </a-select>
              <a-select v-model:value="selectedDsId" placeholder="选择数据集" style="width: 200px">
                <a-select-option value="0">全部数据集</a-select-option>
                <a-select-option v-for="ds in datasets" :key="ds.id" :value="ds.id">
                  {{ ds.dsName }}
                </a-select-option>
              </a-select>
              <a-button type="primary" @click="handleAddPermission">
                <PlusOutlined />
                添加权限规则
              </a-button>
            </div>

            <a-table :columns="permissionColumns" :data-source="permissions" :loading="loading">
              <template #type="{ record }">
                <a-tag :color="record.type === 'row' ? 'blue' : 'green'">
                  {{ record.type === 'row' ? '行权限' : '列权限' }}
                </a-tag>
              </template>
              <template #status="{ record }">
                <a-tag :color="record.enable === 1 ? 'green' : 'red'">
                  {{ record.enable === 1 ? '启用' : '禁用' }}
                </a-tag>
              </template>
              <template #action="{ record }">
                <a-space>
                  <a-button type="link" size="small" @click="handleEditPermission(record)">编辑</a-button>
                  <a-popconfirm title="确定删除?" @confirm="handleDeletePermission(record)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </a-table>
          </div>
        </a-tab-pane>

        <a-tab-pane key="rule" tab="规则组">
          <div class="tab-content">
            <div class="toolbar">
              <a-button type="primary" @click="handleAddRule">
                <PlusOutlined />
                添加规则组
              </a-button>
            </div>

            <a-table :columns="ruleColumns" :data-source="rules" :loading="loading">
              <template #status="{ record }">
                <a-tag :color="record.enable === 1 ? 'green' : 'red'">
                  {{ record.enable === 1 ? '启用' : '禁用' }}
                </a-tag>
              </template>
              <template #permissions="{ record }">
                {{ record.permissionList?.length || 0 }} 个权限
              </template>
              <template #users="{ record }">
                {{ record.userList?.length || 0 }} 个用户
              </template>
              <template #action="{ record }">
                <a-space>
                  <a-button type="link" size="small" @click="handleEditRule(record)">编辑</a-button>
                  <a-popconfirm title="确定删除?" @confirm="handleDeleteRule(record)">
                    <a-button type="link" danger size="small">删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </a-table>
          </div>
        </a-tab-pane>
      </a-tabs>
    </div>

    <a-modal
      v-model:open="showPermissionModal"
      :title="permissionEditMode === 'add' ? '添加权限规则' : '编辑权限规则'"
      :confirm-loading="saving"
      width="700px"
      @ok="handleSavePermission"
      @cancel="showPermissionModal = false"
    >
      <a-form :model="permissionForm" layout="vertical">
        <a-form-item label="规则名称" :rules="[{ required: true }]">
          <a-input v-model:value="permissionForm.name" placeholder="请输入规则名称" />
        </a-form-item>
        <a-form-item label="权限类型" :rules="[{ required: true }]">
          <a-select v-model:value="permissionForm.type">
            <a-select-option value="row">行权限</a-select-option>
            <a-select-option value="column">列权限</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="所属数据集" :rules="[{ required: true }]">
          <a-select v-model:value="permissionForm.dsId" placeholder="请选择数据集">
            <a-select-option v-for="ds in datasets" :key="ds.id" :value="ds.id">
              {{ ds.dsName }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <template v-if="permissionForm.type === 'row'">
          <a-form-item label="行权限表达式">
            <a-input-textarea
              v-model:value="expressionTreeJson"
              :rows="8"
              placeholder='{"logic":"and","items":[{"type":"item","fieldName":"department","term":"eq","value":"销售部"}]}'
            />
          </a-form-item>
        </template>

        <template v-if="permissionForm.type === 'column'">
          <a-form-item label="列权限设置">
            <a-checkbox-group v-model:value="selectedColumns">
              <a-space direction="vertical">
                <a-checkbox
                  v-for="field in currentDsFields"
                  :key="field.fieldName"
                  :value="field.fieldName"
                >
                  {{ field.fieldName }} ({{ field.fieldType }})
                </a-checkbox>
              </a-space>
            </a-checkbox-group>
            <div style="color: #999; font-size: 12px; margin-top: 8px">
              勾选表示允许访问该字段，未勾选表示禁止访问
            </div>
          </a-form-item>
        </template>

        <a-form-item label="状态">
          <a-select v-model:value="permissionForm.enable">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="showRuleModal"
      :title="ruleEditMode === 'add' ? '添加规则组' : '编辑规则组'"
      :confirm-loading="saving"
      width="700px"
      @ok="handleSaveRule"
      @cancel="showRuleModal = false"
    >
      <a-form :model="ruleForm" layout="vertical">
        <a-form-item label="规则组名称" :rules="[{ required: true }]">
          <a-input v-model:value="ruleForm.name" placeholder="请输入规则组名称" />
        </a-form-item>
        <a-form-item label="关联权限规则" :rules="[{ required: true }]">
          <a-select v-model:value="ruleForm.permissionList" mode="multiple" placeholder="请选择权限规则">
            <a-select-option v-for="p in allPermissions" :key="p.id" :value="p.id">
              {{ p.name }} ({{ p.type === 'row' ? '行权限' : '列权限' }})
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="关联用户" :rules="[{ required: true }]">
          <a-select v-model:value="ruleForm.userList" mode="multiple" placeholder="请选择用户">
            <a-select-option v-for="user in users" :key="user.userId" :value="user.userId">
              {{ user.nickname || user.username }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="ruleForm.enable">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch, computed } from 'vue';
import { message } from 'ant-design-vue';
import { PlusOutlined } from '@ant-design/icons-vue';
import LeftSidebar from '@/components/LeftSidebar.vue';
import { getDatasetList } from '@/api/dataset';
import { getUserList } from '@/api/user';
import {
  createPermission,
  updatePermission,
  deletePermission,
  getPermissionsByDsId,
  createRule,
  updateRule,
  deleteRule,
  getAllRules,
  type Permission,
  type Rule
} from '@/api/permission';

const activeTab = ref('permission');
const loading = ref(false);
const saving = ref(false);

const permissionType = ref('all');
const selectedDsId = ref(0);

const datasets = ref<any[]>([]);
const users = ref<any[]>([]);
const permissions = ref<Permission[]>([]);
const rules = ref<Rule[]>([]);
const allPermissions = ref<Permission[]>([]);

const showPermissionModal = ref(false);
const permissionEditMode = ref<'add' | 'edit'>('add');
const expressionTreeJson = ref('');
const selectedColumns = ref<string[]>([]);

const permissionForm = reactive<Permission>({
  id: undefined,
  enable: 1,
  type: 'row',
  dsId: 0,
  name: '',
  expressionTree: undefined,
  permissions: undefined
});

const showRuleModal = ref(false);
const ruleEditMode = ref<'add' | 'edit'>('add');

const ruleForm = reactive<Rule>({
  id: undefined,
  enable: 1,
  name: '',
  permissionList: [],
  userList: []
});

const permissionColumns = [
  { title: '规则名称', dataIndex: 'name', key: 'name' },
  { title: '权限类型', dataIndex: 'type', key: 'type', slots: { customRender: 'type' } },
  { title: '所属数据集', dataIndex: 'dsId', key: 'dsId', render: (id: number) => datasets.value.find(d => d.id === id)?.dsName || id },
  { title: '状态', dataIndex: 'enable', key: 'enable', slots: { customRender: 'status' } },
  { title: '操作', key: 'action', slots: { customRender: 'action' } }
];

const ruleColumns = [
  { title: '规则组名称', dataIndex: 'name', key: 'name' },
  { title: '关联权限', key: 'permissions', slots: { customRender: 'permissions' } },
  { title: '关联用户', key: 'users', slots: { customRender: 'users' } },
  { title: '状态', dataIndex: 'enable', key: 'enable', slots: { customRender: 'status' } },
  { title: '操作', key: 'action', slots: { customRender: 'action' } }
];

const currentDsFields = computed(() => {
  const ds = datasets.value.find(d => d.id === permissionForm.dsId);
  return ds?.fields ? JSON.parse(ds.fields) : [];
});

const loadDatasets = async () => {
  try {
    datasets.value = await getDatasetList();
  } catch (e) {
    console.error('加载数据集失败', e);
  }
};

const loadUsers = async () => {
  try {
    const result = await getUserList({ page: 1, size: 100 });
    users.value = result.list || [];
  } catch (e) {
    console.error('加载用户失败', e);
  }
};

const loadPermissions = async () => {
  loading.value = true;
  try {
    if (selectedDsId.value > 0) {
      permissions.value = await getPermissionsByDsId(selectedDsId.value);
    } else {
      permissions.value = [];
      for (const ds of datasets.value) {
        const ps = await getPermissionsByDsId(ds.id);
        permissions.value.push(...ps);
      }
    }
    allPermissions.value = permissions.value;
    if (permissionType.value !== 'all') {
      permissions.value = permissions.value.filter(p => p.type === permissionType.value);
    }
  } catch (e) {
    console.error('加载权限失败', e);
  } finally {
    loading.value = false;
  }
};

const loadRules = async () => {
  loading.value = true;
  try {
    rules.value = await getAllRules();
  } catch (e) {
    console.error('加载规则组失败', e);
  } finally {
    loading.value = false;
  }
};

const handleAddPermission = () => {
  permissionEditMode.value = 'add';
  Object.assign(permissionForm, {
    id: undefined,
    enable: 1,
    type: 'row',
    dsId: 0,
    name: '',
    expressionTree: undefined,
    permissions: undefined
  });
  expressionTreeJson.value = '';
  selectedColumns.value = [];
  showPermissionModal.value = true;
};

const handleEditPermission = (record: Permission) => {
  permissionEditMode.value = 'edit';
  Object.assign(permissionForm, record);
  if (record.type === 'row' && record.expressionTree) {
    expressionTreeJson.value = JSON.stringify(record.expressionTree, null, 2);
  } else {
    expressionTreeJson.value = '';
  }
  if (record.type === 'column' && record.permissions) {
    selectedColumns.value = record.permissions.filter(p => p.enable).map(p => p.fieldName);
  } else {
    selectedColumns.value = [];
  }
  showPermissionModal.value = true;
};

const handleSavePermission = async () => {
  if (!permissionForm.name || !permissionForm.dsId) {
    message.warning('请填写完整信息');
    return;
  }

  if (permissionForm.type === 'row') {
    try {
      permissionForm.expressionTree = JSON.parse(expressionTreeJson.value);
    } catch (e) {
      message.warning('表达式格式错误');
      return;
    }
  }

  if (permissionForm.type === 'column') {
    const fields = currentDsFields.value;
    permissionForm.permissions = fields.map(f => ({
      fieldId: 0,
      fieldName: f.fieldName,
      fieldComment: f.description || '',
      enable: selectedColumns.value.includes(f.fieldName)
    }));
  }

  saving.value = true;
  try {
    if (permissionEditMode.value === 'add') {
      await createPermission(permissionForm);
      message.success('添加成功');
    } else {
      await updatePermission(permissionForm);
      message.success('更新成功');
    }
    showPermissionModal.value = false;
    loadPermissions();
  } catch (e) {
    console.error('保存失败', e);
    message.error('保存失败');
  } finally {
    saving.value = false;
  }
};

const handleDeletePermission = async (record: Permission) => {
  try {
    await deletePermission(record.id!);
    message.success('删除成功');
    loadPermissions();
  } catch (e) {
    console.error('删除失败', e);
    message.error('删除失败');
  }
};

const handleAddRule = () => {
  ruleEditMode.value = 'add';
  Object.assign(ruleForm, {
    id: undefined,
    enable: 1,
    name: '',
    permissionList: [],
    userList: []
  });
  showRuleModal.value = true;
};

const handleEditRule = (record: Rule) => {
  ruleEditMode.value = 'edit';
  Object.assign(ruleForm, record);
  showRuleModal.value = true;
};

const handleSaveRule = async () => {
  if (!ruleForm.name || ruleForm.permissionList.length === 0 || ruleForm.userList.length === 0) {
    message.warning('请填写完整信息');
    return;
  }

  saving.value = true;
  try {
    if (ruleEditMode.value === 'add') {
      await createRule(ruleForm);
      message.success('添加成功');
    } else {
      await updateRule(ruleForm);
      message.success('更新成功');
    }
    showRuleModal.value = false;
    loadRules();
  } catch (e) {
    console.error('保存失败', e);
    message.error('保存失败');
  } finally {
    saving.value = false;
  }
};

const handleDeleteRule = async (record: Rule) => {
  try {
    await deleteRule(record.id!);
    message.success('删除成功');
    loadRules();
  } catch (e) {
    console.error('删除失败', e);
    message.error('删除失败');
  }
};

watch([permissionType, selectedDsId], () => {
  loadPermissions();
});

watch(() => activeTab.value, (tab) => {
  if (tab === 'permission') {
    loadPermissions();
  } else {
    loadRules();
  }
});

onMounted(() => {
  loadDatasets();
  loadUsers();
  loadPermissions();
});
</script>

<style lang="scss" scoped>
.main-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.auth-container {
  flex: 1;
  padding: 24px;
  background: #f5f5f5;
  overflow-y: auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
}

.auth-tabs {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.tab-content {
  margin-top: 16px;
}

.toolbar {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  align-items: center;
}
</style>