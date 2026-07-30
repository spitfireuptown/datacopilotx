<template>
  <div class="widget-carousel">
    <template v-if="images.length > 0">
      <div class="carousel-viewport">
        <transition :name="transitionName" mode="out-in">
          <img
            :key="currentIndex"
            :src="images[currentIndex]"
            class="carousel-image"
            @error="onImageError"
          />
        </transition>
      </div>
      <!-- 指示器 -->
      <div v-if="images.length > 1" class="carousel-indicators">
        <span
          v-for="(_, idx) in images"
          :key="idx"
          class="indicator-dot"
          :class="{ active: idx === currentIndex }"
          @click="goTo(idx)"
        />
      </div>
      <!-- 左右箭头 -->
      <div v-if="images.length > 1 && !readonly" class="carousel-arrows">
        <span class="arrow-btn" @click="prev">&lt;</span>
        <span class="arrow-btn" @click="next">&gt;</span>
      </div>
    </template>
    <template v-else>
      <div class="carousel-empty">
        <p>暂无图片</p>
        <p v-if="!readonly" class="empty-tip">请在配置中添加图片 URL</p>
      </div>
    </template>
    <!-- 配置面板（编辑模式） -->
    <div v-if="!readonly && showConfig" class="carousel-config">
      <div class="config-section">
        <label class="config-label">图片 URL（每行一个）</label>
        <textarea
          class="config-textarea"
          :value="images.join('\n')"
          placeholder="https://example.com/image1.jpg&#10;https://example.com/image2.jpg"
          @input="updateImages(($event.target as HTMLTextAreaElement).value)"
        />
      </div>
      <div class="config-row">
        <label class="config-label">切换间隔</label>
        <select class="config-select" :value="interval" @change="updateInterval(Number(($event.target as HTMLSelectElement).value))">
          <option :value="3000">3 秒</option>
          <option :value="5000">5 秒</option>
          <option :value="10000">10 秒</option>
        </select>
      </div>
      <div class="config-row">
        <label class="config-label">切换动画</label>
        <select class="config-select" :value="transition" @change="updateTransition(($event.target as HTMLSelectElement).value as any)">
          <option value="fade">淡入淡出</option>
          <option value="slide">左右滑动</option>
        </select>
      </div>
    </div>
    <div v-if="!readonly" class="widget-inner-edit">
      <a-button type="link" size="small" @click="showConfig = !showConfig">
        {{ showConfig ? '收起配置' : '配置图片' }}
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { CarouselConfig } from '@/types/dashboardWidget';

const props = defineProps<{
  config: CarouselConfig;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  configChange: [config: CarouselConfig];
}>();

const images = computed(() => props.config.images || []);
const interval = computed(() => props.config.interval || 5000);
const transition = computed(() => props.config.transition || 'fade');
const transitionName = computed(() => transition.value === 'slide' ? 'carousel-slide' : 'carousel-fade');

const currentIndex = ref(0);
const showConfig = ref(false);
let timer: ReturnType<typeof setInterval> | null = null;

const startAutoPlay = () => {
  stopAutoPlay();
  if (images.value.length > 1) {
    timer = setInterval(() => {
      currentIndex.value = (currentIndex.value + 1) % images.value.length;
    }, interval.value);
  }
};

const stopAutoPlay = () => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
};

const goTo = (idx: number) => {
  currentIndex.value = idx;
  startAutoPlay();
};

const next = () => {
  currentIndex.value = (currentIndex.value + 1) % images.value.length;
  startAutoPlay();
};

const prev = () => {
  currentIndex.value = (currentIndex.value - 1 + images.value.length) % images.value.length;
  startAutoPlay();
};

const updateImages = (val: string) => {
  const list = val.split('\n').map(s => s.trim()).filter(Boolean);
  emit('configChange', { ...props.config, images: list });
  currentIndex.value = 0;
};

const updateInterval = (val: number) => {
  emit('configChange', { ...props.config, interval: val });
};

const updateTransition = (val: 'fade' | 'slide') => {
  emit('configChange', { ...props.config, transition: val });
};

const onImageError = (e: Event) => {
  (e.target as HTMLImageElement).style.display = 'none';
};

watch(() => [images.value.length, interval.value], () => {
  startAutoPlay();
});

onMounted(() => {
  startAutoPlay();
});

onBeforeUnmount(() => {
  stopAutoPlay();
});
</script>

<style lang="scss" scoped>
.widget-carousel {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
}

.carousel-viewport {
  flex: 1;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.carousel-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.carousel-indicators {
  display: flex;
  justify-content: center;
  gap: 6px;
  padding: 6px 0;

  .indicator-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #d9d9d9;
    cursor: pointer;
    transition: background 0.2s;

    &.active {
      background: #1890ff;
    }
  }
}

.carousel-arrows {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  transform: translateY(-50%);
  display: flex;
  justify-content: space-between;
  padding: 0 8px;
  pointer-events: none;

  .arrow-btn {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: rgba(0, 0, 0, 0.3);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    pointer-events: auto;
    font-size: 14px;
    transition: background 0.2s;

    &:hover {
      background: rgba(0, 0, 0, 0.5);
    }
  }
}

.carousel-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;

  p {
    margin: 0;
    font-size: 14px;
  }

  .empty-tip {
    font-size: 12px;
    margin-top: 4px;
    opacity: 0.7;
  }
}

.carousel-config {
  padding: 8px;
  border-top: 1px solid #f0f0f0;

  .config-section {
    margin-bottom: 8px;
  }

  .config-label {
    font-size: 12px;
    color: #666;
    display: block;
    margin-bottom: 4px;
  }

  .config-textarea {
    width: 100%;
    min-height: 60px;
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    padding: 4px 8px;
    font-size: 12px;
    resize: vertical;
    box-sizing: border-box;
  }

  .config-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }

  .config-select {
    border: 1px solid #d9d9d9;
    border-radius: 4px;
    padding: 2px 6px;
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

// 淡入淡出动画
.carousel-fade-enter-active,
.carousel-fade-leave-active {
  transition: opacity 0.4s ease;
}

.carousel-fade-enter-from,
.carousel-fade-leave-to {
  opacity: 0;
}

// 左右滑动动画
.carousel-slide-enter-active,
.carousel-slide-leave-active {
  transition: all 0.4s ease;
}

.carousel-slide-enter-from {
  transform: translateX(100%);
  opacity: 0;
}

.carousel-slide-leave-to {
  transform: translateX(-100%);
  opacity: 0;
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
