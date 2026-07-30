<template>
  <div class="widget-text">
    <div
      class="text-content"
      :class="{ draggable: !readonly && !showEditor }"
      :style="contentStyle"
      @mousedown="onContentMouseDown"
      v-html="renderedContent"
    />
    <!-- 编辑面板 -->
    <div v-if="!readonly && showEditor" class="text-editor">
      <div class="editor-section">
        <label class="editor-label">内容</label>
        <textarea
          class="editor-textarea"
          :value="config.content"
          placeholder="输入文本内容..."
          @input="updateContent(($event.target as HTMLTextAreaElement).value)"
        />
      </div>
      <div class="editor-row">
        <label class="editor-label">字号</label>
        <select class="editor-select" :value="config.fontSize" @change="updateFontSize(Number(($event.target as HTMLSelectElement).value))">
          <option :value="12">12px</option>
          <option :value="14">14px</option>
          <option :value="16">16px</option>
          <option :value="18">18px</option>
          <option :value="20">20px</option>
          <option :value="24">24px</option>
          <option :value="28">28px</option>
          <option :value="32">32px</option>
        </select>
      </div>
      <div class="editor-row">
        <label class="editor-label">颜色</label>
        <input type="color" :value="config.fontColor || '#333333'" @input="updateFontColor(($event.target as HTMLInputElement).value)" />
      </div>
      <div class="editor-row">
        <label class="editor-label">对齐</label>
        <div class="align-btns">
          <button :class="{ active: config.textAlign === 'left' }" @click="updateAlign('left')">左</button>
          <button :class="{ active: config.textAlign === 'center' }" @click="updateAlign('center')">中</button>
          <button :class="{ active: config.textAlign === 'right' }" @click="updateAlign('right')">右</button>
        </div>
      </div>
    </div>
    <div v-if="!readonly" class="widget-inner-edit">
      <a-button type="link" size="small" @click="showEditor = !showEditor">
        {{ showEditor ? '收起编辑' : '编辑内容' }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue';
import type { TextConfig } from '@/types/dashboardWidget';

const props = defineProps<{
  config: TextConfig;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  configChange: [config: TextConfig];
}>();

const showEditor = ref(false);

// 注入父组件的拖拽函数
const widgetStartDrag = inject<((e: MouseEvent) => void) | null>('widgetStartDrag', null);

const onContentMouseDown = (e: MouseEvent) => {
  // 编辑模式下不拖拽，让文本可选中
  if (showEditor.value || props.readonly) {return;}
  // 调用父组件的拖拽函数
  if (widgetStartDrag) {
    widgetStartDrag(e);
  }
};

const contentStyle = computed(() => ({
  fontSize: `${props.config.fontSize || 14}px`,
  color: props.config.fontColor || '#333333',
  textAlign: props.config.textAlign || 'left',
}));

const renderedContent = computed(() => {
  const text = props.config.content || '';
  // 简单换行渲染，支持 **粗体**
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br/>');
});

const updateContent = (val: string) => {
  emit('configChange', { ...props.config, content: val });
};

const updateFontSize = (val: number) => {
  emit('configChange', { ...props.config, fontSize: val });
};

const updateFontColor = (val: string) => {
  emit('configChange', { ...props.config, fontColor: val });
};

const updateAlign = (val: 'left' | 'center' | 'right') => {
  emit('configChange', { ...props.config, textAlign: val });
};
</script>

<style lang="scss" scoped>
.widget-text {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.text-content {
  flex: 1;
  padding: 12px;
  overflow: auto;
  line-height: 1.6;
  word-break: break-word;

  &.draggable {
    cursor: move;
    user-select: none;
  }
}

.text-editor {
  border-top: 1px solid #f0f0f0;
  padding: 8px;

  .editor-section {
    margin-bottom: 8px;
  }

  .editor-label {
    font-size: 12px;
    color: #666;
    display: block;
    margin-bottom: 4px;
  }

  .editor-textarea {
    width: 100%;
    min-height: 60px;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    padding: 4px 8px;
    font-size: 12px;
    resize: vertical;
    box-sizing: border-box;
    font-family: inherit;
  }

  .editor-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;

    input[type="color"] {
      width: 28px;
      height: 28px;
      border: none;
      cursor: pointer;
      padding: 0;
      border-radius: 4px;
    }
  }

  .editor-select {
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    padding: 2px 6px;
    font-size: 12px;
  }

  .align-btns {
    display: flex;
    gap: 4px;

    button {
      width: 28px;
      height: 24px;
      border: 1px solid #d9d9d9;
      background: #fff;
      border-radius: 4px;
      cursor: pointer;
      font-size: 12px;

      &.active {
        background: #1890ff;
        color: #fff;
        border-color: #1890ff;
      }

      &:hover:not(.active) {
        border-color: #1890ff;
        color: #1890ff;
      }
    }
  }
}

.editor-toggle {
  text-align: center;
  flex-shrink: 0;
}

.widget-inner-edit {
  text-align: center;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;

  .widget-wrapper:hover & {
    opacity: 1;
  }
}
</style>
