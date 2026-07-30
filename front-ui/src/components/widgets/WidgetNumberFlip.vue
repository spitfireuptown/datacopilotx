<template>
  <div class="widget-number-flip">
    <div class="number-display" :style="displayStyle">
      <span v-if="prefix" class="number-prefix">{{ prefix }}</span>
      <span class="number-value">{{ displayValue }}</span>
      <span v-if="suffix" class="number-suffix">{{ suffix }}</span>
    </div>
    <!-- 配置面板 -->
    <div v-if="!readonly && showConfig" class="flip-config">
      <div class="config-row">
        <label class="config-label">目标值</label>
        <input class="config-input" type="number" :value="value" @input="updateValue(Number(($event.target as HTMLInputElement).value))" />
      </div>
      <div class="config-row">
        <label class="config-label">前缀</label>
        <input class="config-input" :value="prefix" placeholder="如 ¥" @input="updatePrefix(($event.target as HTMLInputElement).value)" />
      </div>
      <div class="config-row">
        <label class="config-label">后缀</label>
        <input class="config-input" :value="suffix" placeholder="如 %" @input="updateSuffix(($event.target as HTMLInputElement).value)" />
      </div>
      <div class="config-row">
        <label class="config-label">小数</label>
        <select class="config-select" :value="decimals" @change="updateDecimals(Number(($event.target as HTMLSelectElement).value))">
          <option :value="0">0位</option>
          <option :value="1">1位</option>
          <option :value="2">2位</option>
        </select>
      </div>
      <div class="config-row">
        <label class="config-label">动画</label>
        <select class="config-select" :value="duration" @change="updateDuration(Number(($event.target as HTMLSelectElement).value))">
          <option :value="500">0.5秒</option>
          <option :value="1000">1秒</option>
          <option :value="2000">2秒</option>
          <option :value="3000">3秒</option>
        </select>
      </div>
      <div class="config-row">
        <label class="config-label">颜色</label>
        <input type="color" :value="config.fontColor || '#1890ff'" @input="updateFontColor(($event.target as HTMLInputElement).value)" />
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
import { computed, onMounted, ref, watch } from 'vue';
import type { NumberFlipConfig } from '@/types/dashboardWidget';

const props = defineProps<{
  config: NumberFlipConfig;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  configChange: [config: NumberFlipConfig];
}>();

const showConfig = ref(false);
const currentValue = ref(0);
let animFrame: number | null = null;

const value = computed(() => props.config.value ?? 0);
const prefix = computed(() => props.config.prefix || '');
const suffix = computed(() => props.config.suffix || '');
const decimals = computed(() => props.config.decimals ?? 0);
const duration = computed(() => props.config.duration ?? 1000);

const displayValue = computed(() => {
  return currentValue.value.toFixed(decimals.value);
});

const displayStyle = computed(() => ({
  fontSize: `${props.config.fontSize || 36}px`,
  color: props.config.fontColor || '#1890ff',
}));

const animateTo = (target: number) => {
  if (animFrame) {cancelAnimationFrame(animFrame);}
  const start = currentValue.value;
  const diff = target - start;
  const startTime = performance.now();
  const dur = duration.value;

  const step = (now: number) => {
    const elapsed = now - startTime;
    const progress = Math.min(elapsed / dur, 1);
    // easeOutCubic
    const eased = 1 - Math.pow(1 - progress, 3);
    currentValue.value = start + diff * eased;
    if (progress < 1) {
      animFrame = requestAnimationFrame(step);
    }
  };
  animFrame = requestAnimationFrame(step);
};

onMounted(() => {
  currentValue.value = 0;
  animateTo(value.value);
});

watch(value, (v) => animateTo(v));

const updateValue = (v: number) => emit('configChange', { ...props.config, value: v });
const updatePrefix = (v: string) => emit('configChange', { ...props.config, prefix: v });
const updateSuffix = (v: string) => emit('configChange', { ...props.config, suffix: v });
const updateDecimals = (v: number) => emit('configChange', { ...props.config, decimals: v });
const updateDuration = (v: number) => emit('configChange', { ...props.config, duration: v });
const updateFontColor = (v: string) => emit('configChange', { ...props.config, fontColor: v });
</script>

<style scoped lang="scss">
.widget-number-flip {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.number-display {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;

  .number-prefix,
  .number-suffix {
    font-size: 0.5em;
    font-weight: 500;
    opacity: 0.8;
  }

  .number-prefix {
    margin-right: 4px;
  }

  .number-suffix {
    margin-left: 4px;
  }
}

.flip-config {
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

  .config-input,
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
