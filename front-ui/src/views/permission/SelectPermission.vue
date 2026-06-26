<template>
  <div class="select-user_permission">
    <a-card class="user-select-card">
      <div class="card-header">
        <span class="section-title">选择受限用户</span>
      </div>
      
      <div :loading="loading" class="user-select-body">
        <div class="user-list-panel">
          <div class="panel-header">
            <a-input
              v-model:value="search"
              placeholder="搜索用户"
              allow-clear
              size="large"
            >
              <template #prefix>
                <SearchOutlined />
              </template>
            </a-input>
          </div>
          
          <div class="panel-body">
            <div class="select-all-row">
              <a-checkbox
                v-model:checked="checkAll"
                :indeterminate="isIndeterminate"
                @change="handleCheckAllChange"
              >
                全选
              </a-checkbox>
              <span class="user-count">{{ usersWithKeywords.length }} 人</span>
            </div>
            
            <div class="user-list">
              <a-checkbox-group
                v-model:value="checkedWorkspace"
                class="checkbox-group"
                @change="handleCheckedWorkspaceChange"
              >
                <a-checkbox
                  v-for="user in usersWithKeywords"
                  :key="user.userId"
                  :value="user"
                  class="user-item"
                >
                  <a-space>
                    <UserOutlined :style="{ fontSize: '20px', color: '#8f959e' }" />
                    <span class="user-name" :title="user.nickname || user.username">
                      {{ user.nickname || user.username }}
                    </span>
                    <span class="user-account" :title="user.username">
                      ({{ user.username }})
                    </span>
                  </a-space>
                </a-checkbox>
              </a-checkbox-group>
            </div>
          </div>
        </div>
        
        <div class="divider"></div>
        
        <div class="selected-panel">
          <div class="panel-header">
            <span class="selected-count">已选择 {{ checkTableList.length }} 人</span>
            <a-button size="small" @click="clearWorkspaceAll">
              清空
            </a-button>
          </div>
          
          <div class="panel-body">
            <div v-if="checkTableList.length === 0" class="empty-selected">
              <a-empty description="未选择用户" />
            </div>
            
            <div v-else class="selected-list">
              <div
                v-for="user in checkTableList"
                :key="user.userId"
                class="selected-item"
              >
                <a-space>
                  <UserOutlined :style="{ fontSize: '18px', color: '#8f959e' }" />
                  <span class="user-name" :title="user.nickname || user.username">
                    {{ user.nickname || user.username }}
                  </span>
                  <span class="user-account" :title="user.username">
                    ({{ user.username }})
                  </span>
                </a-space>
                <a-button type="link" size="small" @click="clearWorkspace(user)">
                  <CloseOutlined />
                </a-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { SearchOutlined, CloseOutlined, UserOutlined } from '@ant-design/icons-vue'
import { getUserList } from '@/api/user'

const checkAll = ref(false)
const isIndeterminate = ref(false)
const checkedWorkspace = ref<any[]>([])
const users = ref<any[]>([])
const search = ref('')
const loading = ref(false)
const checkTableList = ref([] as any[])

const usersWithKeywords = computed(() => {
  return users.value.filter((user: any) => 
    (user.nickname || user.username).includes(search.value)
  )
})

watch(search, () => {
  const userNameArr = usersWithKeywords.value.map((user: any) => user.nickname || user.username)
  checkedWorkspace.value = checkTableList.value.filter((user: any) =>
    userNameArr.includes(user.nickname || user.username)
  )
  const checkedCount = checkedWorkspace.value.length
  checkAll.value = checkedCount === usersWithKeywords.value.length
  isIndeterminate.value = checkedCount > 0 && checkedCount < usersWithKeywords.value.length
})

const handleCheckAllChange = (val: boolean) => {
  const userNameArr = usersWithKeywords.value.map((user: any) => user.nickname || user.username)
  checkedWorkspace.value = val
    ? [
        ...new Set([
          ...usersWithKeywords.value,
          ...checkedWorkspace.value.filter((user: any) => !userNameArr.includes(user.nickname || user.username)),
        ]),
      ]
    : []
  isIndeterminate.value = false
  checkTableList.value = val
    ? [
        ...new Set([
          ...usersWithKeywords.value,
          ...checkTableList.value.filter((user: any) => !userNameArr.includes(user.nickname || user.username)),
        ]),
      ]
    : checkTableList.value.filter((user: any) => !userNameArr.includes(user.nickname || user.username))
}

const handleCheckedWorkspaceChange = (value: any[]) => {
  const checkedCount = value.length
  checkAll.value = checkedCount === usersWithKeywords.value.length
  isIndeterminate.value = checkedCount > 0 && checkedCount < usersWithKeywords.value.length
  const userNameArr = usersWithKeywords.value.map((user: any) => user.nickname || user.username)
  checkTableList.value = [
    ...new Set([
      ...checkTableList.value.filter((user: any) => !userNameArr.includes(user.nickname || user.username)),
      ...value,
    ]),
  ]
}

const open = async (userIds: string[] = []) => {
  loading.value = true
  search.value = ''
  checkedWorkspace.value = []
  checkAll.value = false
  checkTableList.value = []
  isIndeterminate.value = false
  
  const result = await getUserList({ page: 1, size: 1000 })
  users.value = result.list || []
  
  if (userIds?.length) {
    checkedWorkspace.value = users.value.filter((user: any) => userIds.includes(user.userId))
    checkTableList.value = [...checkedWorkspace.value]
    handleCheckedWorkspaceChange(checkedWorkspace.value)
  }
  loading.value = false
}

const clearWorkspace = (val: any) => {
  checkedWorkspace.value = checkedWorkspace.value.filter((user: any) => user.userId !== val.userId)
  checkTableList.value = checkTableList.value.filter((user: any) => user.userId !== val.userId)
  handleCheckedWorkspaceChange(checkedWorkspace.value)
}

const clearWorkspaceAll = () => {
  checkedWorkspace.value = []
  handleCheckedWorkspaceChange([])
}

defineExpose({
  open,
  checkTableList,
})
</script>

<style lang="scss" scoped>
.select-user_permission {
  padding: 24px 0;
}

.user-select-card {
  height: 500px;
  display: flex;
  flex-direction: column;
}

.card-header {
  margin-bottom: 16px;

  .section-title {
    font-size: 16px;
    font-weight: 500;
    color: #1f2329;
  }
}

.user-select-body {
  flex: 1;
  display: flex;
  gap: 0;
  overflow: hidden;
}

.user-list-panel,
.selected-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.divider {
  width: 1px;
  background: #e5e6eb;
}

.panel-header {
  padding: 0 0 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .selected-count {
    font-size: 14px;
    color: #646a73;
  }
}

.panel-body {
  flex: 1;
  overflow-y: auto;
}

.select-all-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;

  .user-count {
    font-size: 12px;
    color: #8f959e;
  }
}

.user-list {
  padding-right: 8px;
}

.user-item {
  display: block;
  padding: 8px 0;
  
  &:hover {
    background: rgba(31, 35, 41, 0.04);
    border-radius: 4px;
    padding: 8px;
    margin: 0 -8px;
  }

  .user-name {
    font-size: 14px;
    color: #1f2329;
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .user-account {
    font-size: 12px;
    color: #8f959e;
    max-width: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.selected-list {
  padding-right: 8px;
}

.selected-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  margin-bottom: 4px;
  background: #f7f8fa;
  border-radius: 6px;
  
  &:hover {
    background: #eef0f4;
  }

  .user-name {
    font-size: 14px;
    color: #1f2329;
    max-width: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .user-account {
    font-size: 12px;
    color: #8f959e;
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.empty-selected {
  padding: 60px 0;
}
</style>
