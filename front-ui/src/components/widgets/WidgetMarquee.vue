<template>
  <div class="widget-marquee">
    <div class="marquee-track" :style="trackStyle">
      <span class="marquee-text" :style="textStyle">{{ text }}</span>
    </div>
    <!-- 配置面板 -->
    <div v-if="!readonly && showConfig" class="marquee-config">
      <div class="config-row">
        <label class="config-label">内容</label>
        <input class="config-input" :value="text" placeholder="输入滚动文字..." @input="updateText(($event.target as HTMLInputElement).value)" />
      </div>
      <div class="config-row">
        <label class="config-label">速度</label>
        <select class="config-select" :value="speed" @change="updateSpeed(Number(($event.target as HTMLSelectElement).value))">
          <option :value="30">慢速</option>
          <option :value="60">中速</option>
          <option :value="120">快速</option>
        </select>
      </div>
      <div class="config-row">
        <label class="config-label">方向</label>
        <select class="config-select" :value="direction" @change="updateDirection(($event.target as HTMLSelectElement).value as any)">
          <option value="left">从右向左</option>
          <option value="right">从左向右</option>
        </select>
      </div>
      <div class="config-row">
        <label class="config-label">字号</label>
        <select class="config-select" :value="config.fontSize" @change="updateFontSize(Number(($event.target as HTMLSelectElement).value))">
          <option :value="14">14px</option>
          <option :value="16">16px</option>
          <option :value="18">18px</option>
          <option :value="20">20px</option>
          <option :value="24">24px</option>
        </select>
      </div>
      <div class="config-row">
        <label class="config-label">颜色</label>
        <input type="color" :value="config.fontColor || '#333333'" @input="updateFontColor(($event.target as HTMLInputElement).value)" />
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
import type { MarqueeConfig } from '@/types/dashboardWidget';

const props = defineProps<{
  config: MarqueeConfig;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  configChange: [config: MarqueeConfig];
}>();

const showConfig = ref(false);

const text = computed(() => props.config.text || '请输入滚动文字内容');
const speed = computed(() => props.config.speed || 60);
const direction = computed(() => props.config.direction || 'left');

const trackStyle = computed(() => {
  const duration = 10; // base duration
  const animDirection = direction.value === 'left' ? 'normal' : 'reverse';
  return {
    animation: `marquee-scroll ${duration}s linear infinite`,
    animationDirection: animDirection,
  };
});

const textStyle = computed(() => ({
  fontSize: `${props.config.fontSize || 16}px`,
  color: props.config.fontColor || '#333333',
}));

const updateText = (v: string) => emit('configChange', { ...props.config, text: v });
const updateSpeed = (v: number) => emit('configChange', { ...props.config, speed: v });
const updateDirection = (v: 'left' | 'right') => emit('configChange', { ...props.config, direction: v });
const updateFontSize = (v: number) => emit('configChange', { ...props.config, fontSize: v });
const updateFontColor = (v: string) => emit('configChange', { ...props.config, fontColor: v });
</script>

<style scoped lang="scss">
.widget-marquee {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.marquee-track {
  display: flex;
  align-items: center;
  white-space: nowrap;
  padding: 0 20px;
  flex: 1;
  animation: marquee-scroll 10s linear infinite;
}

.marquee-text {
  font-weight: 500;
}

@keyframes marquee-scroll {
  0% { transform: translateX(100%); }
  100% { transform: translateX(-100%); }
}

.marquee-config {
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
