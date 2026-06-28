<template>
  <div class="auth-tree">
    <div class="logic-select">
      <a-select v-model:value="logic" style="width: 100px">
        <a-select-option value="and">AND</a-select-option>
        <a-select-option value="or">OR</a-select-option>
      </a-select>
    </div>
    <div class="add-condition">
      <a-button size="small" @click="addCondition('condition')">
        + 添加条件
      </a-button>
      <a-button size="small" @click="addCondition('group')">
        + 添加分组
      </a-button>
    </div>
    <div class="relation-list">
      <AuthTreeNode
        :items="relationList"
        :path="[]"
        @del="del"
        @update:item="updateItem"
        @add-child="addChild"
      />
      <div v-if="!relationList.length" class="empty-tip">
        请添加条件或分组
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, defineExpose } from 'vue'
import AuthTreeNode from './AuthTreeNode.vue'

interface ExpressionItem {
  term: string
  fieldName: string
  filter_type: string
  enum_value: string
  name: string
  value: any
  child?: ExpressionItem[]
  logic?: string
}

const props = defineProps<{
  modelValue?: any
}>()

const logic = ref<'or' | 'and'>('or')
const relationList = ref<ExpressionItem[]>([])

const getItem = (path: number[]): { list: ExpressionItem[]; index: number } | null => {
  let list: ExpressionItem[] = relationList.value
  for (let i = 0; i < path.length - 1; i++) {
    const item = list[path[i]]
    if (!item || !item.child) {return null}
    list = item.child
  }
  return { list, index: path[path.length - 1] }
}

const init = (expressionTree: any) => {
  const { logic: lg = 'or', items = [] } = expressionTree || {}
  logic.value = lg
  relationList.value = dfsInit(items)
}

const dfsInit = (arr: any[]): ExpressionItem[] => {
  const elementList: ExpressionItem[] = []
  arr.forEach((ele: any) => {
    const { sub_tree, subTree } = ele
    if (sub_tree || subTree) {
      const treeData = sub_tree || subTree
      const { items, logic: childLogic } = treeData
      const child = dfsInit(items)
      elementList.push({ logic: childLogic, child })
    } else {
      const { enum_value, enumValue, fieldName, filter_type, filterType, term, value } = ele
      elementList.push({
        enum_value: (enum_value || enumValue)?.join(',') || '',
        fieldName: fieldName || '',
        filter_type: filter_type || filterType || '',
        term: term || '',
        value: value || '',
        name: fieldName || '',
      })
    }
  })
  return elementList
}

const submit = (): any => {
  return {
    logic: logic.value,
    items: dfsSubmit(relationList.value),
  }
}

const dfsSubmit = (arr: ExpressionItem[]): any[] => {
  const items: any[] = []
  arr.forEach((ele) => {
    const { child = [], logic: groupLogic } = ele
    if (child.length) {
      const sub_tree = dfsSubmit(child)
      items.push({
        enum_value: [],
        fieldName: '',
        filter_type: '',
        term: '',
        type: 'tree',
        value: '',
        sub_tree: { logic: groupLogic, items: sub_tree },
      })
    } else {
      const { enum_value, fieldName, filter_type, term, value } = ele
      if (fieldName) {
        items.push({
          enum_value: enum_value ? enum_value.split(',') : [],
          fieldName,
          filter_type,
          term,
          value,
          type: 'item',
          sub_tree: null,
        })
      }
    }
  })
  return items
}

const addCondition = (type: string) => {
  relationList.value.push(
    type === 'condition'
      ? {
          fieldName: '',
          value: '',
          enum_value: '',
          term: '',
          filter_type: 'logic',
          name: '',
        }
      : { child: [], logic: 'or' }
  )
}

const del = (path: number[]) => {
  const result = getItem(path)
  if (result) {
    result.list.splice(result.index, 1)
  }
}

const updateItem = (path: number[], value: any) => {
  const result = getItem(path)
  if (result) {
    result.list.splice(result.index, 1, { ...value })
  }
}

const addChild = (path: number[], type: string) => {
  const result = getItem(path)
  if (!result) {return}
  const parent = result.list[result.index]
  if (!parent) {return}
  if (!parent.child) {
    parent.child = []
  }
  parent.child.push(
    type === 'condition'
      ? {
          fieldName: '',
          value: '',
          enum_value: '',
          term: '',
          filter_type: 'logic',
          name: '',
        }
      : { child: [], logic: 'or' }
  )
}

defineExpose({
  init,
  submit,
})
</script>

<style lang="scss" scoped>
.auth-tree {
  padding: 16px;

  .logic-select {
    margin-bottom: 16px;
  }

  .add-condition {
    margin-bottom: 16px;
    display: flex;
    gap: 8px;
  }

  .relation-list {
    background: #fafafa;
    border-radius: 8px;
    padding: 16px;
  }

  .empty-tip {
    text-align: center;
    color: #999;
    padding: 40px;
  }
}
</style>
