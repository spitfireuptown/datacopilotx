<template>
  <div class="main-container">
    <LeftSidebar />
    <div class="user-management-container">
      <div class="page-header">
        <h2>用户管理</h2>
      </div>
      <div class="page-content">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <a-input
            v-model:value="searchForm.username"
            placeholder="搜索用户名或昵称"
            class="search-input"
            @press-enter="handleSearch"
          >
            <template #suffix>
              <SearchOutlined class="search-icon" @click="handleSearch" />
            </template>
          </a-input>
          <a-button type="primary" @click="handleAdd">
            <PlusOutlined />
            添加用户
          </a-button>
        </div>

        <!-- 用户列表 -->
        <a-table
          :columns="columns"
          :data-source="users"
          :pagination="pagination"
          :loading="loading"
          row-key="userId"
          @change="handleTableChange"
        >
          <template #name="{ record }">
            {{ record.nickname || record.username }}
          </template>
          <template #role="{ record }">
            <a-tag :color="getRoleColor(record.role)">
              {{ record.roleDesc || '未知' }}
            </a-tag>
          </template>
          <template #status="{ record }">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </a-tag>
          </template>
          <template #createdAt="{ record }">
            {{ formatDate(record.createdAt) }}
          </template>
          <template #action="{ record }">
            <a-space size="small">
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" size="small" @click="handleStatusChange(record)">
                {{ record.status === 1 ? '禁用' : '启用' }}
              </a-button>
              <a-popconfirm
                title="确定要重置该用户密码吗?"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleResetPassword(record)"
              >
                <a-button type="link" size="small">重置密码</a-button>
              </a-popconfirm>
              <a-popconfirm
                title="确定要删除该用户吗?"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" danger size="small">删除</a-button>
              </a-popconfirm>
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
      @cancel="handleModalCancel"
    >
      <a-form ref="formRef" :model="form" layout="vertical">
        <a-form-item label="用户名" name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="form.username" :disabled="editMode === 'edit'" placeholder="请输入用户名" />
        </a-form-item>
        <a-form-item label="昵称" name="nickname" :rules="[{ required: true, message: '请输入昵称' }]">
          <a-input v-model:value="form.nickname" placeholder="请输入昵称" />
        </a-form-item>
        <a-form-item label="邮箱" name="email" :rules="[{ type: 'email', message: '邮箱格式不正确' }]">
          <a-input v-model:value="form.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="手机号" name="phone" :rules="[{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确' }]">
          <a-input v-model:value="form.phone" placeholder="请输入手机号" />
        </a-form-item>
        <a-form-item label="角色" name="role" :rules="[{ required: true, message: '请选择角色' }]">
          <a-select v-model:value="form.role" placeholder="请选择角色">
            <a-select-option :value="1">管理员</a-select-option>
            <a-select-option :value="2">普通用户</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="editMode === 'edit'" label="状态" name="status" :rules="[{ required: true, message: '请选择状态' }]">
          <a-select v-model:value="form.status" placeholder="请选择状态">
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">禁用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { SearchOutlined, PlusOutlined } from '@ant-design/icons-vue';
import LeftSidebar from '@/components/LeftSidebar.vue';
import {
  getUserList,
  addUser,
  updateUser,
  deleteUser,
  changeUserStatus,
  resetPassword,
  type UserInfo,
  type UserForm
} from '@/api/user';

// 角色颜色映射
const getRoleColor = (role: number) => {
  const colorMap: Record<number, string> = {
    0: 'blue',
    1: 'purple',
    2: 'default'
  };
  return colorMap[role] || 'default';
};

// 日期格式化
const formatDate = (timestamp: number) => {
  if (!timestamp) {return '-';}
  const date = new Date(timestamp);
  return date.toLocaleString('zh-CN');
};

// 表格列定义
const columns = [
  { title: '用户名', dataIndex: 'username', key: 'username' },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
  { title: '邮箱', dataIndex: 'email', key: 'email' },
  { title: '手机号', dataIndex: 'phone', key: 'phone' },
  { title: '角色', dataIndex: 'role', key: 'role', slots: { customRender: 'role' } },
  { title: '状态', dataIndex: 'status', key: 'status', slots: { customRender: 'status' } },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', slots: { customRender: 'createdAt' } },
  { title: '操作', key: 'action', slots: { customRender: 'action' } },
];

// 数据状态
const users = ref<UserInfo[]>([]);
const loading = ref(false);
const showEditModal = ref(false);
const editMode = ref<'add' | 'edit'>('add');
const saving = ref(false);
const formRef = ref();

// 搜索表单
const searchForm = reactive({
  username: ''
});

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
});

// 编辑表单
const form = reactive<UserForm>({
  userId: '',
  username: '',
  nickname: '',
  email: '',
  phone: '',
  role: 2,
  status: 1,
  password: ''
});

// 加载用户列表
const loadUserList = async () => {
  loading.value = true;
  try {
    const response = await getUserList({
      username: searchForm.username,
      page: pagination.current,
      size: pagination.pageSize
    });
    users.value = response.list;
    pagination.total = response.total;
  } catch (error) {
    console.error('获取用户列表失败:', error);
  } finally {
    loading.value = false;
  }
};

// 搜索
const handleSearch = () => {
  pagination.current = 1;
  loadUserList();
};

// 表格变化
const handleTableChange = (pag: any) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadUserList();
};

// 添加用户
const handleAdd = () => {
  editMode.value = 'add';
  Object.assign(form, {
    userId: '',
    username: '',
    nickname: '',
    email: '',
    phone: '',
    role: 2,
    status: 1,
    password: ''
  });
  showEditModal.value = true;
};

// 编辑用户
const handleEdit = (record: UserInfo) => {
  editMode.value = 'edit';
  Object.assign(form, {
    userId: record.userId,
    username: record.username,
    nickname: record.nickname,
    email: record.email,
    phone: record.phone,
    role: record.role,
    status: record.status,
    password: ''
  });
  showEditModal.value = true;
};

// 保存用户
const handleSave = async () => {
  try {
    await formRef.value.validate();
    saving.value = true;
    
    if (editMode.value === 'add') {
      // 添加用户时设置默认状态为启用，密码为datacopilotx
      const addData = {
        ...form,
        status: 1,
        password: 'datacopilotx'
      };
      await addUser(addData);
      message.success('添加用户成功，默认密码为 datacopilotx');
    } else {
      await updateUser(form);
      message.success('更新用户成功');
    }
    
    showEditModal.value = false;
    loadUserList();
  } catch (error: any) {
    // 只处理表单验证错误，其他HTTP错误由request.ts统一处理
    if (error.errorFields) {
      message.warning('请填写完整表单信息');
    }
    // HTTP错误（如400参数校验失败）已在request.ts中统一提示，此处不再重复
  } finally {
    saving.value = false;
  }
};

// 取消编辑
const handleModalCancel = () => {
  showEditModal.value = false;
  formRef.value?.resetFields();
};

// 修改用户状态
const handleStatusChange = async (record: UserInfo) => {
  const newStatus = record.status === 1 ? 0 : 1;
  try {
    await changeUserStatus(record.userId, newStatus);
    message.success(newStatus === 1 ? '已启用' : '已禁用');
    loadUserList();
  } catch (error) {
    console.error('修改状态失败:', error);
    message.error('修改状态失败');
  }
};

// 重置密码
const handleResetPassword = async (record: UserInfo) => {
  try {
    await resetPassword(record.userId);
    message.success('密码已重置为默认密码');
  } catch (error) {
    console.error('重置密码失败:', error);
    message.error('重置密码失败');
  }
};

// 删除用户
const handleDelete = async (record: UserInfo) => {
  try {
    await deleteUser(record.userId);
    message.success('删除成功');
    loadUserList();
  } catch (error) {
    console.error('删除失败:', error);
    message.error('删除失败');
  }
};

// 页面加载时获取用户列表
onMounted(() => {
  loadUserList();
});
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

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .search-input {
    width: 300px;
  }

  .search-icon {
    cursor: pointer;
    color: rgba(0, 0, 0, 0.45);

    &:hover {
      color: rgba(0, 0, 0, 0.75);
    }
  }
}
</style>