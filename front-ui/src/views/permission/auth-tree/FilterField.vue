<template>
  <div class="white-nowrap">
    <div
      class="filed"
      @mouseover="showDel = true"
      @mouseleave="showDel = false"
    >
      <a-select
        v-model:value="fieldId"
        placeholder="选择字段"
        style="width: 200px"
        @change="onFieldChange"
      >
        <a-select-option v-for="ele in dimensions" :key="ele.id" :value="ele.id">
          {{ ele.field_name }}
        </a-select-option>
      </a-select>
      <a-select
        v-model:value="term"
        style="width: 120px; margin-left: 8px"
        placeholder="操作符"
        :disabled="!fieldId"
        @change="onTermChange"
      >
        <a-select-option
          v-for="ele in operators"
          :key="ele.value"
          :value="ele.value"
        >
          {{ ele.label }}
        </a-select-option>
      </a-select>
      <a-input
        v-if="!['null', 'not_null'].includes(term)"
        v-model:value="value"
        style="max-width: 280px; margin-left: 8px"
        placeholder="请输入值"
        :disabled="!fieldId || !term"
        allow-clear
        @change="onValueChange"
      />
      <span class="del-wrapper">
        <DeleteOutlined v-if="showDel" class="font16" @click="emits('del')" />
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, inject, computed, watch, onMounted } from 'vue'
import { DeleteOutlined } from '@ant-design/icons-vue'

export interface Item {
  term: string
  field_id: string
  filter_type: string
  enum_value: string
  name: string
  value: any
}

type Props = {
  index: number
  item: Item
}

const props = withDefaults(defineProps<Props>(), {
  index: 0,
  item: () => ({
    term: '',
    field_id: '',
    filter_type: '',
    enum_value: '',
    name: '',
    value: null,
  }),
})

const emits = defineEmits(['update:item', 'del'])
const showDel = ref(false)

const fieldId = ref(props.item.field_id)
const term = ref(props.item.term)
const value = ref(props.item.value)

const filedList = inject('filedList') as any

const operators = computed(() => [
  { value: 'eq', label: '等于' },
  { value: 'not_eq', label: '不等于' },
  { value: 'gt', label: '大于' },
  { value: 'lt', label: '小于' },
  { value: 'in', label: '属于' },
  { value: 'not_in', label: '不属于' },
  { value: 'like', label: '包含' },
  { value: 'not_like', label: '不包含' },
  { value: 'null', label: '为空' },
  { value: 'not_null', label: '不为空' },
])

const computedFiledList = computed<any[]>(() => {
  return filedList.value || []
})

const dimensions = computed(() => {
  return computedFiledList.value
})

const emitUpdate = () => {
  const field = dimensions.value.find((f: any) => f.id === fieldId.value)
  const fieldName = field ? field.field_name : ''
  emits('update:item', {
    ...props.item,
    field_id: fieldId.value,
    term: term.value,
    value: value.value,
    filter_type: 'logic',
    name: fieldName,
    enum_value: '',
  })
}

const onFieldChange = (newFieldId: string) => {
  fieldId.value = newFieldId
  term.value = ''
  value.value = ''
  emitUpdate()
}

const onTermChange = (newTerm: string) => {
  term.value = newTerm
  if (['null', 'not_null'].includes(newTerm)) {
    value.value = ''
  }
  emitUpdate()
}

const onValueChange = () => {
  emitUpdate()
}

watch(() => props.item.field_id, (newFieldId) => {
  if (fieldId.value !== newFieldId) {
    fieldId.value = newFieldId
  }
})

watch(() => props.item.name, (newName) => {
  if (newName && !fieldId.value) {
    const field = dimensions.value.find((f: any) => f.field_name === newName)
    if (field) {
      fieldId.value = field.id
    }
  }
})

watch(() => props.item.term, (newTerm) => {
  if (term.value !== newTerm) {
    term.value = newTerm
  }
})

watch(() => props.item.value, (newValue) => {
  if (value.value !== newValue) {
    value.value = newValue
  }
})

onMounted(() => {
  if (props.item.name && !fieldId.value) {
    const field = dimensions.value.find((f: any) => f.field_name === props.item.name)
    if (field) {
      fieldId.value = field.id
    }
  }
})
</script>

<style lang="scss" scoped>
.white-nowrap {
  white-space: nowrap;
}

.filed {
  height: 41.4px;
  padding: 1px 3px 1px 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  margin-left: 20px;
  min-width: 200px;
  justify-content: left;
  position: relative;
  white-space: nowrap;

  .font16 {
    font-size: 16px;
    cursor: pointer;
  }

  .del-wrapper {
    width: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
