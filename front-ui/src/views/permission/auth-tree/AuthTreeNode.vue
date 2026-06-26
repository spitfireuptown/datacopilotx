<template>
  <template v-for="(item, index) in items" :key="index">
    <div v-if="item.child" class="group-item">
      <div class="group-header">
        <a-select v-model:value="item.logic" style="width: 100px">
          <a-select-option value="and">AND</a-select-option>
          <a-select-option value="or">OR</a-select-option>
        </a-select>
        <DeleteOutlined class="delete-icon" @click="$emit('del', [...path, index])" />
      </div>
      <div class="group-content">
        <AuthTreeNode
          :items="item.child"
          :path="[...path, index]"
          @del="$emit('del', $event)"
          @update:item="$emit('update:item', $event)"
          @add-child="(childPath, type) => $emit('addChild', childPath, type)"
        />
      </div>
      <div class="group-footer">
        <a-button size="small" @click="$emit('addChild', [...path, index], 'condition')">
          + 添加条件
        </a-button>
        <a-button size="small" @click="$emit('addChild', [...path, index], 'group')">
          + 添加分组
        </a-button>
      </div>
    </div>
    <AuthTreeItem
      v-else
      :item="item"
      :index="index"
      @del="$emit('del', [...path, index])"
      @update:item="$emit('update:item', [...path, index], $event)"
    />
  </template>
</template>

<script setup lang="ts">
import { DeleteOutlined } from '@ant-design/icons-vue'
import AuthTreeItem from './FilterField.vue'

defineProps<{
  items: any[]
  path?: number[]
}>()

defineEmits(['del', 'update:item', 'addChild'])
</script>

<style lang="scss" scoped>
.group-item {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;

  .group-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .delete-icon {
      cursor: pointer;
      color: #ff4d4f;
      font-size: 16px;
    }
  }

  .group-content {
    padding-left: 16px;
    border-left: 2px solid #d9d9d9;
    margin-bottom: 12px;
  }

  .group-footer {
    display: flex;
    gap: 8px;
  }
}
</style>
