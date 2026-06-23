<template>
  <div class="main-container">
    <LeftSidebar />
    <div class="user-management-container">
      <div class="page-header">
        <h2>用户管理</h2>
      </div>
      <div class="page-content">
        <a-table :columns="columns" :data-source="users" row-key="userId">
          <template #name="{ record }">
            {{ record.nickname || record.username }}
          </template>
          <template #role="{ record }">
            {{ roleMap[record.role] || '未知' }}
          </template>
          <template #status="{ record }">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template #action="{ record }">
            <a-space size="small">
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" size="small" @click="handleResetPassword(record)">重置密码</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </a-table>
      </div>
    </div>

    <a-modal
      v-model:open="showEditModal"
      :title="editMode === 'add' ? '新增用户' : '编辑用户'"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="用户名">
          <a-input v-model:value="form.username" :disabled="editMode === 'edit'" />
        </a-form-item>
        <a-form-item label="昵称">
          <a-input v-model:value="form.nickname" />
        </a-form-item>
        <a-form-item label="邮箱">
          <a-input v-model:value="form.email" />
        </a-form-item>
        <a-form-item label="手机号">
          <a-input v-model:value="form.phone" />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="form.role">
            <a-select-option :value="0">超级管理员</a-select-option>
            <a-select-option :value="1">管理员</a-select-option>
            <a-select-option :value="2">普通用户</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="form.status">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="editMode === 'add'" label="密码">
          <a-input-password v-model:value="form.password" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { message } from 'ant-design-vue';
import LeftSidebar from '@/components/LeftSidebar.vue';

const roleMap: Record<number, string> = { 0: '超级管理员', 1: '管理员', 2: '普通用户' };

const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '昵称', dataIndex: 'name', key: 'name', slots: { customRender: 'name' } },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '角色', dataIndex: 'role', key: 'role', slots: { customRender: 'role' } },
  { title: '状态', dataIndex: 'status', key: 'status', slots: { customRender: 'status' } },
  { title: '操作', key: 'action', slots: { customRender: 'action' } },
];

const users = ref<any[]>([]);
const showEditModal = ref(false);
const editMode = ref<'add' | 'edit'>('add');
const saving = ref(false);
const form = reactive({
  userId: '',
  username: '',
  nickname: '',
  email: '',
  phone: '',
  role: 2,
  status: 1,
  password: '',
});

const handleEdit = (record: any) => {
  editMode.value = 'edit';
  form.userId = record.userId;
  form.username = record.username;
  form.nickname = record.nickname;
  form.email = record.email;
  form.phone = record.phone;
  form.role = record.role;
  form.status = record.status;
  showEditModal.value = true;
};

const handleSave = async () => {
  saving.value = true;
  try {
    message.success('保存成功');
    showEditModal.value = false;
  } catch {
    message.error('保存失败');
  } finally {
    saving.value = false;
  }
};

const handleResetPassword = (record: any) => {
  message.info(`重置 ${record.nickname || record.username} 的密码`);
};

const handleDelete = (record: any) => {
  message.info(`删除 ${record.nickname || record.username}`);
};
</script>

<style lang="scss" scoped>
.main-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.user-management-container {
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
</style>