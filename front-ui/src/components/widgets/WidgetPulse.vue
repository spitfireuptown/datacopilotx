<template>
  <div class="widget-pulse">
    <div class="pulse-display">
      <div class="pulse-dot-wrapper" :style="dotWrapperStyle">
        <span class="pulse-dot" :style="dotStyle" />
        <span class="pulse-ring" :style="ringStyle" />
      </div>
      <span v-if="text" class="pulse-text" :style="textStyle">{{ text }}</span>
    </div>
    <!-- 配置面板 -->
    <div v-if="!readonly && showConfig" class="pulse-config">
      <div class="config-row">
        <label class="config-label">文本</label>
        <input class="config-input" :value="text" placeholder="状态文本..." @input="updateText(($event.target as HTMLInputElement).value)" />
      </div>
      <div class="config-row">
        <label class="config-label">颜色</label>
        <div class="color-presets">
          <span
            v-for="c in presetColors"
            :key="c"
            class="color-dot"
            :class="{ active: color === c }"
            :style="{ background: c }"
            @click="updateColor(c)"
          />
        </div>
      </div>
      <div class="config-row">
        <label class="config-label">大小</label>
        <select class="config-select" :value="pulseSize" @change="updatePulseSize(($event.target as HTMLSelectElement).value as any)">
          <option value="small">小</option>
          <option value="medium">中</option>
          <option value="large">大</option>
        </select>
      </div>
      <div class="config-row">
        <label class="config-label">字号</label>
        <select class="config-select" :value="config.fontSize" @change="updateFontSize(Number(($event.target as HTMLSelectElement).value))">
          <option :value="12">12px</option>
          <option :value="14">14px</option>
          <option :value="16">16px</option>
          <option :value="18">18px</option>
          <option :value="20">20px</option>
        </select>
      </div>
    </div>
    <div v-if="!readonly" class="widget-inner-edit">
      <a-button type="link" size="small" @click="showConfig = !showConfig">
        {{ showConfig ? '收起配置' : '设置' }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { PulseConfig } from '@/types/dashboardWidget';

const props = defineProps<{
  config: PulseConfig;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  configChange: [config: PulseConfig];
}>();

const showConfig = ref(false);

const presetColors = ['#52c41a', '#1890ff', '#faad14', '#ff4d4f', '#722ed1', '#13c2c2'];

const text = computed(() => props.config.text || '');
const color = computed(() => props.config.color || '#52c41a');
const pulseSize = computed(() => props.config.pulseSize || 'medium');

const sizeMap = { small: 10, medium: 14, large: 20 };
const dotSize = computed(() => sizeMap[pulseSize.value]);

const dotWrapperStyle = computed(() => ({
  width: `${dotSize.value}px`,
  height: `${dotSize.value}px`,
}));

const dotStyle = computed(() => ({
  width: `${dotSize.value}px`,
  height: `${dotSize.value}px`,
  background: color.value,
  borderRadius: '50%',
}));

const ringStyle = computed(() => ({
  width: `${dotSize.value}px`,
  height: `${dotSize.value}px`,
  borderRadius: '50%',
  background: color.value,
}));

const textStyle = computed(() => ({
  fontSize: `${props.config.fontSize || 14}px`,
  color: '#333',
}));

const updateText = (v: string) => emit('configChange', { ...props.config, text: v });
const updateColor = (v: string) => emit('configChange', { ...props.config, color: v });
const updatePulseSize = (v: 'small' | 'medium' | 'large') => emit('configChange', { ...props.config, pulseSize: v });
const updateFontSize = (v: number) => emit('configChange', { ...props.config, fontSize: v });
</script>

<style scoped lang="scss">
.widget-pulse {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.pulse-display {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.pulse-dot-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pulse-dot {
  position: relative;
  z-index: 1;
}

.pulse-ring {
  position: absolute;
  top: 0;
  left: 0;
  opacity: 0.4;
  animation: pulse-ring 2s ease-out infinite;
}

@keyframes pulse-ring {
  0% {
    transform: scale(1);
    opacity: 0.4;
  }
  100% {
    transform: scale(2.5);
    opacity: 0;
  }
}

.pulse-text {
  font-weight: 500;
}

.pulse-config {
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;

  .config-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;

    &:last-child { margin-bottom: 0; }
  }

  .config-label {
    font-size: 12px;
    color: #666;
    min-width: 40px;
  }

  .config-input {
    flex: 1;
    padding: 4px 8px;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    font-size: 12px;
  }

  .color-presets {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
  }

  .color-dot {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    cursor: pointer;
    border: 2px solid transparent;
    transition: all 0.15s;

    &:hover {
      transform: scale(1.2);
    }

    &.active {
      border-color: #333;
    }
  }

  .config-select {
    flex: 1;
    padding: 4px 8px;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    font-size: 12px;
  }
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
