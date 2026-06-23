<template>
  <div class="main-container">
    <LeftSidebar />
    <div class="auth-config-container">
      <div class="page-header">
        <h2>权限配置</h2>
      </div>
      <div class="page-content">
        <a-table :columns="columns" :data-source="permissions" row-key="id">
          <template #action="{ record }">
            <a-space size="small">
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" danger size="small" @click="handleDelete(record)">删除</a-button>
            </a-space>
          </template>
        </a-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { message } from 'ant-design-vue';
import LeftSidebar from '@/components/LeftSidebar.vue';

const columns = [
  { title: '权限名称', dataIndex: 'name', key: 'name' },
  { title: '权限标识', dataIndex: 'code', key: 'code' },
  { title: '所属模块', dataIndex: 'module', key: 'module' },
  { title: '操作', key: 'action', slots: { customRender: 'action' } },
];

const permissions = ref<any[]>([]);

const handleEdit = (record: any) => {
  message.info(`编辑权限: ${record.name}`);
};

const handleDelete = (record: any) => {
  message.info(`删除权限: ${record.name}`);
};
</script>

<style lang="scss" scoped>
.main-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.auth-config-container {
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