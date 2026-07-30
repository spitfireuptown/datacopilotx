<template>
  <div
    class="widget-wrapper"
    :class="{ 'content-only': contentOnly }"
    :style="wrapperStyle"
    @mousedown.stop
  >
    <!-- 标题栏（拖拽手柄） -->
    <div v-if="!contentOnly" class="widget-header" :class="{ readonly }" @mousedown="startDrag">
      <a-input
        v-if="isRenaming"
        ref="renameInputRef"
        v-model:value="renameValue"
        size="small"
        class="rename-input"
        @mousedown.stop
        @blur="confirmRename"
        @keydown.enter="confirmRename"
        @keydown.esc="cancelRename"
      />
      <div v-else class="widget-title" @dblclick.stop="startRename">
        <component :is="typeIcon" class="title-icon" />
        {{ widget.title }}
      </div>
      <div v-if="!readonly" class="widget-actions">
        <a-tooltip title="组件设置">
          <a-popover trigger="click" placement="bottomRight" :get-popup-container="(n:any) => n.parentNode">
            <template #content>
              <div class="card-settings-panel">
                <div class="settings-section">
                  <div class="settings-label">背景色</div>
                  <div class="preset-colors">
                    <span
                      v-for="c in presetColors"
                      :key="c"
                      class="color-swatch"
                      :class="{ active: (widget.bgColor || '#ffffff') === c }"
                      :style="{ background: c }"
                      @click="changeColor(c)"
                    />
                  </div>
                  <div class="custom-color-row">
                    <input type="color" :value="widget.bgColor || '#ffffff'" @input="changeColor(($event.target as HTMLInputElement).value)" />
                    <span class="custom-label">自定义</span>
                  </div>
                </div>
                <div class="settings-divider" />
                <div class="settings-row">
                  <label class="settings-checkbox">
                    <input type="checkbox" :checked="contentOnly" @change="toggleContentOnly" />
                    <span>仅显示内容（隐藏标题和背景）</span>
                  </label>
                </div>
              </div>
            </template>
            <SettingOutlined class="action-icon" @mousedown.stop @click.stop />
          </a-popover>
        </a-tooltip>
        <a-tooltip title="重命名">
          <EditOutlined class="action-icon" @click.stop="startRename" @mousedown.stop />
        </a-tooltip>
        <a-tooltip title="删除">
          <DeleteOutlined class="action-icon delete-icon" @click.stop="handleDelete" @mousedown.stop />
        </a-tooltip>
      </div>
    </div>

    <!-- 仅显示内容模式下的悬浮浮层 -->
    <div v-if="contentOnly" class="content-only-overlay">
      <div class="overlay-name">{{ widget.title }}</div>
      <div v-if="!readonly" class="overlay-actions">
        <a-tooltip title="组件设置">
          <a-popover trigger="click" placement="bottomRight" :get-popup-container="(n:any) => n.parentNode">
            <template #content>
              <div class="card-settings-panel">
                <div class="settings-section">
                  <div class="settings-label">背景色</div>
                  <div class="preset-colors">
                    <span
                      v-for="c in presetColors"
                      :key="c"
                      class="color-swatch"
                      :class="{ active: (widget.bgColor || '#ffffff') === c }"
                      :style="{ background: c }"
                      @click="changeColor(c)"
                    />
                  </div>
                  <div class="custom-color-row">
                    <input type="color" :value="widget.bgColor || '#ffffff'" @input="changeColor(($event.target as HTMLInputElement).value)" />
                    <span class="custom-label">自定义</span>
                  </div>
                </div>
                <div class="settings-divider" />
                <div class="settings-row">
                  <label class="settings-checkbox">
                    <input type="checkbox" :checked="contentOnly" @change="toggleContentOnly" />
                    <span>仅显示内容（隐藏标题和背景）</span>
                  </label>
                </div>
              </div>
            </template>
            <SettingOutlined class="overlay-icon" @mousedown.stop @click.stop />
          </a-popover>
        </a-tooltip>
        <a-tooltip title="删除">
          <DeleteOutlined class="overlay-icon delete-icon" @mousedown.stop @click.stop="handleDelete" />
        </a-tooltip>
      </div>
    </div>

    <!-- 内容插槽 -->
    <div class="widget-body">
      <slot />
    </div>

    <!-- 右下角缩放手柄 -->
    <div v-if="!readonly" class="resize-handle" @mousedown="startResize">
      <svg width="12" height="12" viewBox="0 0 12 12">
        <path d="M0 12 L12 0 M4 12 L12 4 M8 12 L12 8" stroke="#bbb" stroke-width="1.2" fill="none" />
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h, nextTick, provide, ref } from 'vue';
import { DeleteOutlined, EditOutlined, SettingOutlined, PictureOutlined, FontSizeOutlined, FieldTimeOutlined, SoundOutlined, NumberOutlined, PieChartOutlined, AimOutlined } from '@ant-design/icons-vue';
import type { DashboardWidget } from '@/types/dashboardWidget';
import { isDarkColor, adjustColor } from '@/types/dashboardWidget';

const props = defineProps<{
  widget: DashboardWidget;
  containerWidth: number;
  containerHeight: number;
  readonly?: boolean;
  contentOnly?: boolean;
}>();

const emit = defineEmits<{
  delete: [id: string];
  layoutChange: [id: string, x: number, y: number, w: number, h: number];
  rename: [id: string, name: string];
  colorChange: [id: string, color: string];
  contentOnlyChange: [id: string, value: boolean];
}>();

const GRID = 20;

const presetColors = [
  '#ffffff', '#f0f5ff', '#f6ffed', '#fff7e6', '#fff1f0',
  '#1a1a2e', '#16213e', '#0f3460', '#2d3436',
];

const typeIcon = computed(() => {
  const map: Record<string, any> = {
    carousel: PictureOutlined,
    text: FontSizeOutlined,
    timer: FieldTimeOutlined,
    marquee: SoundOutlined,
    numberFlip: NumberOutlined,
    progressRing: PieChartOutlined,
    pulse: AimOutlined,
  };
  return map[props.widget.type] || FontSizeOutlined;
});

const isDark = computed(() => props.widget.bgColor ? isDarkColor(props.widget.bgColor) : false);

// 提供拖拽函数给子组件使用
provide('widgetStartDrag', (e: MouseEvent) => {
  startDrag(e);
});

const wrapperStyle = computed(() => {
  const bg = props.widget.bgColor || '#ffffff';
  const headerBg = props.widget.bgColor ? adjustColor(bg, isDark.value ? -5 : -3) : '#fafafa';
  const textColor = isDark.value ? '#ffffff' : '#333333';
  return {
    left: `${props.widget.layoutX}px`,
    top: `${props.widget.layoutY}px`,
    width: `${props.widget.layoutW}px`,
    height: `${props.widget.layoutH}px`,
    background: bg,
    color: textColor,
    '--header-bg': headerBg,
    '--text-color': textColor,
    '--border-color': isDark.value ? 'rgba(255,255,255,0.1)' : '#f0f0f0',
  };
});

// ---- Drag ----
const isDragging = ref(false);
const dragStartX = ref(0);
const dragStartY = ref(0);
const dragOrigX = ref(0);
const dragOrigY = ref(0);

const startDrag = (e: MouseEvent) => {
  if (props.readonly) {return;}
  isDragging.value = true;
  dragStartX.value = e.clientX;
  dragStartY.value = e.clientY;
  dragOrigX.value = props.widget.layoutX;
  dragOrigY.value = props.widget.layoutY;
  document.addEventListener('mousemove', onDrag);
  document.addEventListener('mouseup', stopDrag);
};

const onDrag = (e: MouseEvent) => {
  if (!isDragging.value) {return;}
  const dx = e.clientX - dragStartX.value;
  const dy = e.clientY - dragStartY.value;
  let nx = Math.round((dragOrigX.value + dx) / GRID) * GRID;
  let ny = Math.round((dragOrigY.value + dy) / GRID) * GRID;
  nx = Math.max(0, Math.min(nx, Math.max(0, props.containerWidth - props.widget.layoutW)));
  ny = Math.max(0, Math.min(ny, Math.max(0, props.containerHeight - props.widget.layoutH)));
  emit('layoutChange', props.widget.id, nx, ny, props.widget.layoutW, props.widget.layoutH);
};

const stopDrag = () => {
  isDragging.value = false;
  document.removeEventListener('mousemove', onDrag);
  document.removeEventListener('mouseup', stopDrag);
};

// ---- Resize ----
const isResizing = ref(false);
const resizeStartX = ref(0);
const resizeStartY = ref(0);
const resizeOrigW = ref(0);
const resizeOrigH = ref(0);

const startResize = (e: MouseEvent) => {
  e.stopPropagation();
  e.preventDefault();
  isResizing.value = true;
  resizeStartX.value = e.clientX;
  resizeStartY.value = e.clientY;
  resizeOrigW.value = props.widget.layoutW;
  resizeOrigH.value = props.widget.layoutH;
  document.addEventListener('mousemove', onResize);
  document.addEventListener('mouseup', stopResize);
};

const onResize = (e: MouseEvent) => {
  if (!isResizing.value) {return;}
  const dx = e.clientX - resizeStartX.value;
  const dy = e.clientY - resizeStartY.value;
  let nw = Math.round((resizeOrigW.value + dx) / GRID) * GRID;
  let nh = Math.round((resizeOrigH.value + dy) / GRID) * GRID;
  nw = Math.max(200, nw);
  nh = Math.max(120, nh);
  emit('layoutChange', props.widget.id, props.widget.layoutX, props.widget.layoutY, nw, nh);
};

const stopResize = () => {
  isResizing.value = false;
  document.removeEventListener('mousemove', onResize);
  document.removeEventListener('mouseup', stopResize);
};

// ---- Rename ----
const isRenaming = ref(false);
const renameValue = ref('');
const renameInputRef = ref<any>(null);

const startRename = () => {
  if (props.readonly) {return;}
  isRenaming.value = true;
  renameValue.value = props.widget.title || '';
  nextTick(() => {
    renameInputRef.value?.focus?.();
    renameInputRef.value?.select?.();
  });
};

const confirmRename = () => {
  if (!isRenaming.value) {return;}
  isRenaming.value = false;
  const name = renameValue.value.trim();
  if (name && name !== props.widget.title) {
    emit('rename', props.widget.id, name);
  }
};

const cancelRename = () => {
  isRenaming.value = false;
};

// ---- Color ----
const changeColor = (color: string) => {
  emit('colorChange', props.widget.id, color);
};

// ---- Content Only ----
const toggleContentOnly = () => {
  emit('contentOnlyChange', props.widget.id, !props.contentOnly);
};

// ---- Delete ----
const handleDelete = () => {
  emit('delete', props.widget.id);
};
</script>

<style lang="scss" scoped>
.widget-wrapper {
  position: absolute;
  border-radius: 8px;
  box-shadow: none;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  }

  &.content-only {
    background: transparent !important;
    box-shadow: none;

    &:hover {
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
    }
  }
}

.content-only-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(4px);
  opacity: 0;
  transition: opacity 0.2s;
  z-index: 20;
  border-bottom: 1px solid #f0f0f0;

  .widget-wrapper:hover & {
    opacity: 1;
  }

  .overlay-name {
    font-size: 13px;
    font-weight: 500;
    color: #333;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    margin-right: 8px;
  }

  .overlay-actions {
    display: flex;
    gap: 4px;
    flex-shrink: 0;
  }

  .overlay-icon {
    font-size: 14px;
    color: #999;
    cursor: pointer;
    padding: 2px;

    &:hover {
      color: #1890ff;
    }

    &.delete-icon:hover {
      color: #ff4d4f;
    }
  }
}

.widget-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 38px;
  padding: 0 12px;
  background: var(--header-bg, #fafafa);
  border-bottom: 1px solid transparent;
  cursor: move;
  user-select: none;
  flex-shrink: 0;
  transition: border-color 0.2s;

  &.readonly {
    cursor: default;
  }

  .widget-wrapper:hover & {
    border-bottom-color: var(--border-color, #f0f0f0);
  }

  .widget-title {
    font-size: 13px;
    font-weight: 500;
    color: var(--text-color, #333);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    margin-right: 8px;
    display: flex;
    align-items: center;
    gap: 6px;

    .title-icon {
      font-size: 14px;
      opacity: 0.7;
    }
  }

  .rename-input {
    flex: 1;
    margin-right: 8px;
  }

  .widget-actions {
    display: flex;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.2s;
  }

  .widget-wrapper:hover & .widget-actions {
    opacity: 1;
  }

  .action-icon {
    font-size: 14px;
    color: var(--text-color, #999);
    opacity: 0.6;
    cursor: pointer;
    padding: 2px;

    &:hover {
      opacity: 1;
      color: #1890ff;
    }

    &.delete-icon:hover {
      color: #ff4d4f;
    }
  }
}

.widget-body {
  flex: 1;
  overflow: auto;
  min-height: 0;
}

.resize-handle {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 20px;
  height: 20px;
  cursor: nwse-resize;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  padding: 2px;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.2s;

  .widget-wrapper:hover & {
    opacity: 1;
  }

  &:hover svg path {
    stroke: #1890ff;
  }
}

.card-settings-panel {
  width: 220px;

  .settings-section {
    margin-bottom: 4px;
  }

  .settings-label {
    font-size: 12px;
    color: #666;
    margin-bottom: 8px;
  }

  .preset-colors {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-bottom: 10px;
  }

  .color-swatch {
    width: 24px;
    height: 24px;
    border-radius: 4px;
    cursor: pointer;
    border: 2px solid #d9d9d9;
    transition: all 0.15s;

    &:hover {
      transform: scale(1.15);
    }

    &.active {
      border-color: #1890ff;
    }
  }

  .custom-color-row {
    display: flex;
    align-items: center;
    gap: 8px;

    input[type="color"] {
      width: 28px;
      height: 28px;
      border: none;
      cursor: pointer;
      padding: 0;
      border-radius: 4px;
    }

    .custom-label {
      font-size: 12px;
      color: #999;
    }
  }

  .settings-divider {
    height: 1px;
    background: #f0f0f0;
    margin: 10px 0;
  }

  .settings-row {
    padding: 0 2px;
  }

  .settings-checkbox {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    font-size: 13px;
    color: #333;

    input[type="checkbox"] {
      cursor: pointer;
    }

    span {
      user-select: none;
    }
  }
}
</style>
