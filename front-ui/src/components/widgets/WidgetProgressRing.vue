<template>
  <div class="widget-progress-ring">
    <div class="ring-container" :style="containerStyle">
      <svg :width="size" :height="size" class="ring-svg">
        <!-- 轨道 -->
        <circle
          :cx="size / 2"
          :cy="size / 2"
          :r="radius"
          fill="none"
          :stroke="trackColor"
          :stroke-width="strokeWidth"
        />
        <!-- 进度 -->
        <circle
          :cx="size / 2"
          :cy="size / 2"
          :r="radius"
          fill="none"
          :stroke="color"
          :stroke-width="strokeWidth"
          :stroke-dasharray="circumference"
          :stroke-dashoffset="dashOffset"
          stroke-linecap="round"
          class="progress-arc"
          :style="{ transition: `stroke-dashoffset ${duration}s ease-out` }"
        />
      </svg>
      <div v-if="showLabel" class="ring-label" :style="labelStyle">
        {{ Math.round(value) }}%
      </div>
    </div>
    <!-- 配置面板 -->
    <div v-if="!readonly && showConfig" class="ring-config">
      <div class="config-row">
        <label class="config-label">进度</label>
        <input class="config-input" type="range" min="0" max="100" :value="value" @input="updateValue(Number(($event.target as HTMLInputElement).value))" />
        <span class="config-value">{{ value }}%</span>
      </div>
      <div class="config-row">
        <label class="config-label">大小</label>
        <select class="config-select" :value="size" @change="updateSize(Number(($event.target as HTMLSelectElement).value))">
          <option :value="80">小</option>
          <option :value="120">中</option>
          <option :value="160">大</option>
        </select>
      </div>
      <div class="config-row">
        <label class="config-label">颜色</label>
        <input type="color" :value="color" @input="updateColor(($event.target as HTMLInputElement).value)" />
      </div>
      <div class="config-row">
        <label class="config-label">显示%</label>
        <input type="checkbox" :checked="showLabel" @change="updateShowLabel(($event.target as HTMLInputElement).checked)" />
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
import { computed, onMounted, ref } from 'vue';
import type { ProgressRingConfig } from '@/types/dashboardWidget';

const props = defineProps<{
  config: ProgressRingConfig;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  configChange: [config: ProgressRingConfig];
}>();

const showConfig = ref(false);
const animatedValue = ref(0);
const duration = 1.2;

const value = computed(() => Math.min(100, Math.max(0, props.config.value ?? 0)));
const size = computed(() => props.config.size ?? 120);
const strokeWidth = computed(() => props.config.strokeWidth ?? 10);
const color = computed(() => props.config.color ?? '#1890ff');
const trackColor = computed(() => props.config.trackColor ?? '#f0f0f0');
const showLabel = computed(() => props.config.showLabel !== false);
const fontSize = computed(() => props.config.fontSize ?? 20);

const radius = computed(() => (size.value - strokeWidth.value) / 2);
const circumference = computed(() => 2 * Math.PI * radius.value);
const dashOffset = computed(() => circumference.value * (1 - animatedValue.value / 100));

const containerStyle = computed(() => ({
  width: `${size.value}px`,
  height: `${size.value}px`,
}));

const labelStyle = computed(() => ({
  fontSize: `${fontSize.value}px`,
  color: color.value,
}));

onMounted(() => {
  // 延迟启动动画
  setTimeout(() => {
    animatedValue.value = value.value;
  }, 100);
});

const updateValue = (v: number) => {
  animatedValue.value = v;
  emit('configChange', { ...props.config, value: v });
};
const updateSize = (v: number) => emit('configChange', { ...props.config, size: v });
const updateColor = (v: string) => emit('configChange', { ...props.config, color: v });
const updateShowLabel = (v: boolean) => emit('configChange', { ...props.config, showLabel: v });
</script>

<style scoped lang="scss">
.widget-progress-ring {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 8px;
}

.ring-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.ring-svg {
  transform: rotate(-90deg);
}

.progress-arc {
  transition: stroke-dashoffset 1.2s ease-out;
}

.ring-label {
  position: absolute;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.ring-config {
  width: 100%;
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

  .config-input[type="range"] {
    flex: 1;
  }

  .config-value {
    font-size: 12px;
    color: #999;
    min-width: 30px;
    text-align: right;
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
