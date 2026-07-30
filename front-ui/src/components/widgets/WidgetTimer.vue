<template>
  <div class="widget-timer">
    <div class="timer-display" :style="displayStyle">
      <div class="timer-time">{{ timeStr }}</div>
      <div v-if="config.showDate !== false" class="timer-date">{{ dateStr }}</div>
    </div>
    <!-- 配置面板 -->
    <div v-if="!readonly && showConfig" class="timer-config">
      <div class="config-row">
        <label class="config-label">时间制</label>
        <select class="config-select" :value="config.format || '24h'" @change="updateFormat(($event.target as HTMLSelectElement).value as any)">
          <option value="24h">24 小时</option>
          <option value="12h">12 小时</option>
        </select>
      </div>
      <div class="config-row">
        <label class="config-label">字号</label>
        <select class="config-select" :value="config.fontSize || 32" @change="updateFontSize(Number(($event.target as HTMLSelectElement).value))">
          <option :value="20">20px</option>
          <option :value="24">24px</option>
          <option :value="28">28px</option>
          <option :value="32">32px</option>
          <option :value="40">40px</option>
          <option :value="48">48px</option>
          <option :value="56">56px</option>
        </select>
      </div>
      <div class="config-row">
        <label class="config-label">颜色</label>
        <input type="color" :value="config.fontColor || '#333333'" @input="updateFontColor(($event.target as HTMLInputElement).value)" />
      </div>
      <div class="config-row">
        <label class="config-label">显示日期</label>
        <input type="checkbox" :checked="config.showDate !== false" @change="updateShowDate(($event.target as HTMLInputElement).checked)" />
      </div>
    </div>
    <div v-if="!readonly" class="widget-inner-edit">
      <a-button type="link" size="small" @click="showConfig = !showConfig">
        {{ showConfig ? '收起配置' : '时间设置' }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import type { TimerConfig } from '@/types/dashboardWidget';

const props = defineProps<{
  config: TimerConfig;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  configChange: [config: TimerConfig];
}>();

const showConfig = ref(false);
const now = ref(new Date());
let timer: ReturnType<typeof setInterval> | null = null;

const timeStr = computed(() => {
  const d = now.value;
  let h = d.getHours();
  const m = d.getMinutes().toString().padStart(2, '0');
  const s = d.getSeconds().toString().padStart(2, '0');

  if (props.config.format === '12h') {
    const period = h >= 12 ? 'PM' : 'AM';
    h = h % 12 || 12;
    return `${h.toString().padStart(2, '0')}:${m}:${s} ${period}`;
  }
  return `${h.toString().padStart(2, '0')}:${m}:${s}`;
});

const dateStr = computed(() => {
  const d = now.value;
  const year = d.getFullYear();
  const month = (d.getMonth() + 1).toString().padStart(2, '0');
  const day = d.getDate().toString().padStart(2, '0');
  const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
  const weekDay = weekDays[d.getDay()];
  return `${year}-${month}-${day} 星期${weekDay}`;
});

const displayStyle = computed(() => ({
  fontSize: `${props.config.fontSize || 32}px`,
  color: props.config.fontColor || '#333333',
}));

const updateFormat = (val: '12h' | '24h') => {
  emit('configChange', { ...props.config, format: val });
};

const updateFontSize = (val: number) => {
  emit('configChange', { ...props.config, fontSize: val });
};

const updateFontColor = (val: string) => {
  emit('configChange', { ...props.config, fontColor: val });
};

const updateShowDate = (val: boolean) => {
  emit('configChange', { ...props.config, showDate: val });
};

onMounted(() => {
  timer = setInterval(() => {
    now.value = new Date();
  }, 1000);
});

onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
});
</script>

<style lang="scss" scoped>
.widget-timer {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.timer-display {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-family: 'Courier New', 'Consolas', monospace;
  font-weight: 600;
  letter-spacing: 2px;

  .timer-time {
    line-height: 1.2;
  }

  .timer-date {
    font-size: 14px;
    opacity: 0.7;
    margin-top: 8px;
    letter-spacing: 1px;
    font-weight: 400;
  }
}

.timer-config {
  border-top: 1px solid #f0f0f0;
  padding: 8px;
  width: 100%;

  .config-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }

  .config-label {
    font-size: 12px;
    color: #666;
    min-width: 50px;
  }

  .config-select {
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    padding: 2px 6px;
    font-size: 12px;
  }

  input[type="color"] {
    width: 28px;
    height: 28px;
    border: none;
    cursor: pointer;
    padding: 0;
    border-radius: 4px;
  }
}

.config-toggle {
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
