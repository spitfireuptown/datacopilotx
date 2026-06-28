<template>
  <div class="main-container">
    <LeftSidebar />
    <div class="permission-container">
      <div class="page-header">
        <h2>权限配置</h2>
      </div>
      <div class="page-content">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <a-input
            v-model:value="keywords"
            placeholder="搜索规则组"
            class="search-input"
            allow-clear
          >
            <template #suffix>
              <SearchOutlined class="search-icon" />
            </template>
          </a-input>
          <a-button type="primary" @click="addHandler()">
            <PlusOutlined />
            添加规则组
          </a-button>
        </div>

        <!-- 权限列表 -->
        <a-table
          :columns="columns"
          :data-source="ruleListWithSearch"
          :pagination="{ pageSize: 10 }"
          :loading="searchLoading"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <span class="rule-name">{{ record.name }}</span>
            </template>
            <template v-if="column.key === 'users'">
              <span>{{ record.userList?.length || 0 }} 人</span>
            </template>
            <template v-if="column.key === 'permissions'">
              <span>{{ record.permissionList?.length || 0 }} 个</span>
            </template>
            <template v-if="column.key === 'tables'">
              <a-tag v-for="table in getTableNames(record)" :key="table" color="blue">
                {{ table }}
              </a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space size="small">
                <a-button type="link" size="small" @click="handleEditRule(record)">
                  <EditOutlined />
                  编辑
                </a-button>
                <a-popconfirm title="确定删除该规则组吗？" @confirm="deleteHandler(record)">
                  <a-button type="link" danger size="small">
                    <DeleteOutlined />
                    删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>
    </div>

    <a-drawer
      v-model:open="ruleConfigvVisible"
      :closable="false"
      :width="900"
      :extra="closeButton"
    >
      <template #title>
        <span>{{ drawerTitle }}</span>
      </template>

      <div class="steps-wrapper">
        <a-steps :current="activeStep" :size="'small'" class="drawer-steps">
          <a-step title="设置权限规则" />
          <a-step title="选择受限用户" />
        </a-steps>
      </div>

      <div v-show="activeStep === 0" class="drawer-content">
        <a-form
          ref="termFormRef"
          :model="currentPermission"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 20 }"
        >
          <a-form-item label="规则组名称">
            <a-input
              v-model:value="currentPermission.name"
              placeholder="请输入规则组名称"
              size="large"
            />
          </a-form-item>
        </a-form>

        <a-divider />

        <div class="permission-section">
          <div class="section-header">
            <span class="section-title">权限规则</span>
            <a-space>
              <a-button type="primary" @click="handleAddPermission(1)">
                <template #icon><LockOutlined /></template>
                添加行权限
              </a-button>
              <a-button type="primary" @click="handleAddPermission(0)">
                <template #icon><UnlockOutlined /></template>
                添加列权限
              </a-button>
            </a-space>
          </div>

          <div v-if="currentPermission.permissions.length === 0" class="empty-permission">
            <a-empty description="暂无权限规则，请点击上方按钮添加" />
          </div>

          <a-table
            v-else
            :columns="permissionColumns"
            :data-source="currentPermission.permissions"
            :pagination="false"
            row-key="id"
            :size="'middle'"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'type'">
                <a-tag :color="record.type === 'row' ? 'blue' : 'green'">
                  {{ record.type === 'row' ? '行权限' : '列权限' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space size="small">
                  <a-button type="link" size="small" @click="editForm(record)">
                    <EditOutlined />
                    编辑
                  </a-button>
                  <a-button type="link" danger size="small" @click="deleteRuleHandler(record)">
                    <DeleteOutlined />
                    删除
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </div>

      <div v-show="activeStep !== 0" class="select-permission_content">
        <SelectPermission ref="selectPermissionRef"></SelectPermission>
      </div>

      <template #footer>
        <a-button @click="beforeClose">取消</a-button>
        <a-button v-if="activeStep === 1" @click="preview">上一步</a-button>
        <a-button v-if="activeStep === 0" type="primary" @click="next">下一步</a-button>
        <a-button v-if="activeStep === 1" type="primary" @click="savePermission">
          保存
        </a-button>
      </template>
    </a-drawer>

    <a-modal
      v-model:open="dialogFormVisible"
      :title="dialogTitle"
      :width="850"
      :footer="null"
      :body-style="{ padding: '24px' }"
    >
      <a-card>
        <a-form
          ref="columnFormRef"
          :model="columnForm"
          :label-col="{ span: 5 }"
          :wrapper-col="{ span: 19 }"
        >
          <a-form-item label="规则名称">
            <a-input
              v-model:value="columnForm.name"
              placeholder="请输入规则名称"
              size="large"
            />
          </a-form-item>
          <a-form-item label="数据集合">
            <a-select
              v-model:value="columnForm.dsId"
              placeholder="请选择数据集"
              style="width: 100%"
              size="large"
              @change="handleInitDsIdChange"
            >
              <a-select-option
                v-for="item in dsListOptions"
                :key="item.id"
                :value="item.id"
              >
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>

        <a-divider orientation="left">
          <span class="section-title">
            {{ columnForm.type === 'row' ? '设置行权限规则' : '设置列权限规则' }}
          </span>
        </a-divider>

        <div v-if="ruleType === 1" class="auth-tree_wrapper">
          <a-alert
            type="info"
            show-icon
            message="行权限规则说明"
            description="通过表达式树配置数据行级过滤条件，支持 AND/OR 逻辑组合和多种比较运算符"
            style="margin-bottom: 16px"
          />
          <div class="auth-tree_content">
            <AuthTree ref="authTreeRef" @save="saveAuthTree"></AuthTree>
          </div>
        </div>

        <div v-else class="column-permission_wrapper">
          <a-alert
            type="info"
            show-icon
            message="列权限规则说明"
            description="通过开关控制字段的可见性，关闭的字段将对受限用户隐藏"
            style="margin-bottom: 16px"
          />
          <div class="column-search-bar">
            <a-input
              v-model:value="searchColumn"
              placeholder="搜索字段"
              allow-clear
              style="width: 260px"
            >
              <template #prefix>
                <SearchOutlined />
              </template>
            </a-input>
            <span class="column-count">
              共 {{ tableColumnData.length }} 个字段
            </span>
          </div>
          <div class="column-table-content">
            <a-table
              :columns="columnColumns"
              :data-source="tableColumnData"
              :pagination="false"
              row-key="fieldName"
              :size="'middle'"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'enable'">
                  <a-switch :checked="record.enable" @change="record.enable = !record.enable" />
                </template>
              </template>
            </a-table>
          </div>
        </div>
      </a-card>

      <div class="dialog-footer">
        <a-button @click="closeForm">取消</a-button>
        <a-button type="primary" @click="saveHandler">保存</a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, provide, nextTick, h } from 'vue'
import {
  SearchOutlined,
  PlusOutlined,
  CloseOutlined,
  EditOutlined,
  DeleteOutlined,
  LockOutlined,
  UnlockOutlined,
} from '@ant-design/icons-vue'
import LeftSidebar from '@/components/LeftSidebar.vue'
import SelectPermission from './SelectPermission.vue'
import AuthTree from './auth-tree/AuthTree.vue'
import { getDatasetList, getDatasetDetail } from '@/api/dataset'
import {
  createRule,
  updateRule,
  deleteRule,
  getAllRules,
  batchCreatePermissions,
  updatePermission,
} from '@/api/permission'

const keywords = ref('')
const activeStep = ref(0)
const dialogFormVisible = ref(false)
const ruleConfigvVisible = ref(false)
const termFormRef = ref()
const columnFormRef = ref()
const drawerTitle = ref('')
const dialogTitle = ref('')
const activeDs = ref(null)
const ruleList = ref<any[]>([])

const defaultPermission = {
  id: '',
  name: '',
  permissions: [],
  users: [],
}
const currentPermission = reactive<any>(JSON.parse(JSON.stringify(defaultPermission)))

const searchColumn = ref('')
const isCreate = ref(false)
const defaultForm = {
  name: '',
  id: '',
  dsId: '',
  type: 'row',
  dsName: '',
  tableName: '',
  permissions: [] as any[],
  expression_tree: {},
}
const columnForm = reactive(JSON.parse(JSON.stringify(defaultForm)))
const selectPermissionRef = ref()
const fieldListOptions = ref<any[]>([])
const dsListOptions = ref<any[]>([])

const columns = [
  { title: '规则名称', dataIndex: 'name', key: 'name' },
  { title: '涉及用户', key: 'users' },
  { title: '权限数', key: 'permissions' },
  { title: '操作', key: 'action', width: 220 },
]

const getTableNames = (record: any) => {
  const tables = new Set<string>()
  record.permissions?.forEach((perm: any) => {
    if (perm.tableName) {
      tables.add(perm.tableName)
    }
  })
  return Array.from(tables)
}

provide('filedList', fieldListOptions)

const permissionColumns = [
  { title: '规则名称', dataIndex: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '数据集', dataIndex: 'dsName' },
  { title: '操作', key: 'action', width: 120 },
]

const columnColumns = [
  { title: '字段名称', dataIndex: 'field_name' },
  { title: '字段说明', dataIndex: 'field_comment' },
  { title: '操作', dataIndex: 'enable', key: 'enable', width: 100 },
]

const ruleListWithSearch = computed(() => {
  if (!keywords.value) {return ruleList.value}
  return ruleList.value.filter((ele) =>
    ele.name.toLowerCase().includes(keywords.value.toLowerCase())
  )
})

const tableColumnData = computed<any[]>(() => {
  if (!searchColumn.value) {return columnForm.permissions}
  return columnForm.permissions.filter((ele: any) =>
    ele.field_name.toLowerCase().includes(searchColumn.value.toLowerCase())
  )
})

const setDrawerTitle = () => {
  if (isCreate.value) {
    drawerTitle.value = '添加规则组'
  } else {
    drawerTitle.value = '编辑规则组'
  }
}

const ruleType = ref(0)

const handleAddPermission = (val: any) => {
  ruleType.value = val
  Object.assign(columnForm, JSON.parse(JSON.stringify(defaultForm)))
  if (val === 1) {
    handleRowPermission(null)
  } else {
    handleColumnPermission(null)
  }
}

const saveAuthTree = (val: any) => {
  columnForm.expression_tree = val ? JSON.parse(JSON.stringify(val)) : {}
  const { expression_tree, dsId, type, name, dsName } = columnForm
  if (columnForm.id) {
    for (const key in currentPermission.permissions) {
      if (currentPermission.permissions[key].id === columnForm.id) {
        Object.assign(
          currentPermission.permissions[key],
          JSON.parse(JSON.stringify({
            expression_tree,
            tree: expression_tree,
            dsId,
            type,
            name,
            dsName,
          }))
        )
      }
    }
  } else {
    currentPermission.permissions.push(
      JSON.parse(JSON.stringify({
        expression_tree,
        tree: expression_tree,
        dsId,
        type,
        name,
        dsName,
        id: +new Date(),
      }))
    )
  }
  dialogFormVisible.value = false
}

const getDsList = async (row: any) => {
  try {
    const res = await getDatasetList()
    dsListOptions.value = res || []
    if (row?.dsId) {
      columnForm.dsId = row.dsId
    } else {
      columnForm.dsId = null
    }
  } finally {
    if (!row && columnForm.type === 'row') {
      authTreeRef.value?.init(columnForm.expression_tree)
    }
  }

  if (row) {
    handleDsIdChange({ id: row.dsId, name: row.dsName })
    handleEditeTable(row.dsId)
  }
}

const handleRowPermission = (row: any) => {
  columnForm.type = 'row'
  getDsList(row)
  if (row) {
    const { name, dsId, id, tree, dsName, expression_tree, expressionTree } = row
    const expressionData = tree || expression_tree || expressionTree
    Object.assign(columnForm, {
      id,
      name,
      dsId,
      dsName,
      expression_tree: typeof expressionData === 'object' ? expressionData : (typeof expressionData === 'string' && expressionData !== 'undefined' && expressionData !== 'null' && expressionData.trim() ? JSON.parse(expressionData) : {}),
    })
  }
  dialogFormVisible.value = true
  dialogTitle.value = row?.id ? '编辑行权限' : '添加行权限'
}

const handleColumnPermission = (row: any) => {
  columnForm.type = 'column'
  getDsList(row)
  if (row) {
    const { name, dsId, id, permissions, dsName } = row
    Object.assign(columnForm, {
      id,
      name,
      dsId,
      dsName,
      permissions: permissions || [],
    })
  }
  dialogFormVisible.value = true
  dialogTitle.value = row?.id ? '编辑列权限' : '添加列权限'
}

const handleInitDsIdChange = async (val: any) => {
  const ds = dsListOptions.value.find((d: any) => d.id === val)
  if (ds) {
    columnForm.dsId = ds.id
    columnForm.dsName = ds.name
    try {
      const detail = await getDatasetDetail(val)
      if (detail && detail.fields) {
        fieldListOptions.value = (detail.fields as any[]).map((ele: any) => ({
          id: ele.fieldName,
          field_name: ele.fieldName,
          fieldName: ele.fieldName,
          description: ele.description || '',
        }))
        if (columnForm.type === 'column') {
          columnForm.permissions = fieldListOptions.value.map((ele: any) => ({
            fieldName: ele.fieldName,
            field_name: ele.fieldName,
            field_comment: ele.description || '',
            enable: true,
          }))
        }
      } else {
        fieldListOptions.value = []
        columnForm.permissions = []
      }
    } catch {
      fieldListOptions.value = []
      columnForm.permissions = []
    }
    if (authTreeRef.value) {
      authTreeRef.value.init({})
    }
  }
}

const handleDsIdChange = (val: any) => {
  columnForm.dsId = val.id
  columnForm.dsName = val.name
}

const handleEditeTable = async (val: any) => {
  try {
    const detail = await getDatasetDetail(val)
    if (detail && detail.fields) {
      fieldListOptions.value = (detail.fields as any[]).map((ele: any) => ({
        id: ele.fieldName,
        field_name: ele.fieldName,
        fieldName: ele.fieldName,
        description: ele.description || '',
      }))
      if (columnForm.type !== 'row') {
        const enableMap = columnForm.permissions.reduce((pre: any, next: any) => {
          pre[next.fieldName] = next.enable
          return pre
        }, {})
        columnForm.permissions = fieldListOptions.value.map((ele: any) => ({
          fieldName: ele.fieldName,
          field_name: ele.fieldName,
          field_comment: ele.description || '',
          enable: enableMap[ele.fieldName] ?? false,
        }))
      }
    }
  } catch {
    fieldListOptions.value = []
  }
  if (columnForm.type === 'row') {
    authTreeRef.value?.init(columnForm.expression_tree)
  }
}

const beforeClose = () => {
  ruleConfigvVisible.value = false
  activeStep.value = 0
  isCreate.value = false
}

const searchLoading = ref(false)
const handleSearch = () => {
  searchLoading.value = true
  getAllRules()
    .then((res: any) => {
      ruleList.value = res || []
    })
    .finally(() => {
      searchLoading.value = false
    })
}

handleSearch()

const addHandler = () => {
  setDrawerTitle()
  isCreate.value = true
  activeStep.value = 0
  Object.assign(currentPermission, JSON.parse(JSON.stringify(defaultPermission)))
  ruleConfigvVisible.value = true
}

const editForm = (row: any) => {
  if (row.type === 'row') {
    ruleType.value = 1
    handleRowPermission(row)
  } else {
    ruleType.value = 0
    handleColumnPermission(row)
  }
}

const handleEditRule = (row: any) => {
  isCreate.value = false
  activeStep.value = 0
  setDrawerTitle()
  Object.assign(currentPermission, JSON.parse(JSON.stringify(row)))
  ruleConfigvVisible.value = true
  nextTick(() => {
    selectPermissionRef.value?.open(row.userList || [])
  })
}

const deleteRuleHandler = (row: any) => {
  currentPermission.permissions = currentPermission.permissions.filter(
    (ele: any) => ele.id !== row.id
  )
}

const deleteHandler = (row: any) => {
  deleteRule(row.id).then(() => {
    handleSearch()
  })
}

const closeForm = () => {
  dialogFormVisible.value = false
}

const authTreeRef = ref()

const saveHandler = () => {
  if (!columnForm.name || !columnForm.dsId) {
    return
  }
  if (columnForm.type === 'row') {
    const val = authTreeRef.value?.submit()
    if (val) {
      saveAuthTree(val)
    }
  } else {
    const { permissions, dsId, type, name, dsName } = columnForm
    if (columnForm.id) {
      for (const key in currentPermission.permissions) {
        if (currentPermission.permissions[key].id === columnForm.id) {
          Object.assign(
            currentPermission.permissions[key],
            JSON.parse(JSON.stringify({
              permissions,
              dsId,
              type,
              name,
              dsName,
            }))
          )
        }
      }
    } else {
      currentPermission.permissions.push(
        JSON.parse(JSON.stringify({
          permissions,
          dsId,
          type,
          name,
          dsName,
          id: +new Date(),
        }))
      )
    }
    dialogFormVisible.value = false
  }
}

const preview = () => {
  activeStep.value = 0
}

const next = () => {
  if (!currentPermission.name) {
    return
  }
  activeStep.value = 1
  nextTick(() => {
    selectPermissionRef.value?.open(currentPermission.userList || [])
  })
}

const saveLoading = ref(false)

const transformExpressionTree = (tree: any): any => {
  if (!tree) {return null}
  return {
    logic: tree.logic,
    items: tree.items?.map((item: any) => transformExpressionItem(item)) || []
  }
}

const transformExpressionItem = (item: any): any => {
  if (!item) {return null}
  const { fieldName, filter_type, filterType, sub_tree, subTree, name } = item
  return {
    type: item.type,
    fieldName: fieldName || name || '',
    filterType: filterType || filter_type || '',
    term: item.term || '',
    value: item.value,
    subTree: subTree || (sub_tree ? transformExpressionTree(sub_tree) : null)
  }
}

const transformColumnPermissions = (permissions: any[]): any[] => {
  return permissions?.map((p: any) => ({
    fieldId: p.field_id ? parseInt(p.field_id) : (p.fieldId || null),
    fieldName: p.fieldName || p.field_name || '',
    fieldComment: p.fieldComment || p.field_comment || '',
    enable: p.enable !== undefined ? p.enable : true
  })) || []
}

const save = async () => {
  if (saveLoading.value) {return}
  saveLoading.value = true

  try {
    const { id, name, permissions, users } = JSON.parse(JSON.stringify(currentPermission))
    const selectedUsers = selectPermissionRef.value?.checkTableList.map((ele: any) => ele.userId) || []

    const permissionList: number[] = []

    for (const perm of permissions) {
      const permData: any = {
        name: perm.name,
        type: perm.type,
        dsId: perm.dsId ? parseInt(perm.dsId) : null,
        enable: 1,
      }

      if (perm.type === 'row') {
        const expressionTree = typeof perm.expression_tree === 'object' 
          ? perm.expression_tree 
          : (typeof perm.expression_tree === 'string' && perm.expression_tree !== 'undefined' && perm.expression_tree !== 'null' && perm.expression_tree.trim()) 
            ? JSON.parse(perm.expression_tree)
            : null
        permData.expressionTree = expressionTree ? transformExpressionTree(expressionTree) : null
      } else {
        const columnPermissions = typeof perm.permissions === 'object' 
          ? perm.permissions 
          : (typeof perm.permissions === 'string' && perm.permissions !== 'undefined' && perm.permissions !== 'null' && perm.permissions.trim())
            ? JSON.parse(perm.permissions)
            : []
        permData.permissions = transformColumnPermissions(columnPermissions)
      }

      if (perm.id && perm.id.toString().length < 10) {
        await updatePermission({ ...permData, id: perm.id })
        permissionList.push(perm.id)
      } else {
        const saved = await batchCreatePermissions([permData])
        permissionList.push(saved[0].id)
      }
    }

    const obj: any = {
      name,
      permissionList,
      userList: selectedUsers,
      enable: 1,
    }

    if (id) {
      obj.id = id
      await updateRule(obj)
    } else {
      await createRule(obj)
    }

    handleSearch()
    beforeClose()
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saveLoading.value = false
  }
}

const savePermission = () => {
  save()
}
</script>

<style lang="scss" scoped>
.main-container {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.permission-container {
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

.page-content {
  .rule-name {
    font-weight: 500;
  }
}

.drawer-content {
  padding: 24px 0;
}

.steps-wrapper {
  padding: 8px 0 20px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 24px;

  .drawer-steps {
    max-width: 400px;
    margin: 0 auto;
  }
}

.permission-section {
  margin-top: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .section-title {
    font-size: 16px;
    font-weight: 500;
    color: #1f2329;
  }
}

.empty-permission {
  padding: 40px 0;
}

.select-permission_content {
  padding: 24px 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}

.steps-container {
  flex: 1;
  display: flex;
  justify-content: center;
}

.steps {
  max-width: 500px;
  width: 100%;
}

.auth-tree_content {
  padding: 16px;
  border-radius: 6px;
  border: 1px solid #dee0e3;
  min-height: 64px;
  margin-top: 8px;
}

.auth-tree_wrapper {
  margin-top: 16px;
}

.column-permission_wrapper {
  margin-top: 16px;
}

.column-search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .column-count {
    color: #8f959e;
    font-size: 14px;
  }
}

.column-table-content {
  margin-top: 8px;
  border: 1px solid #dee0e3;
  border-radius: 6px;
  overflow: hidden;
}
</style>
