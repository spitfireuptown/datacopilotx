<template>
  <div class="welcome-wrap">
    <!-- 品牌渐变光晕装饰 -->
    <div class="welcome-glow welcome-glow-left" aria-hidden="true"></div>
    <div class="welcome-glow welcome-glow-right" aria-hidden="true"></div>

    <div class="welcome-hero">
      <h1 class="welcome-hero-title">Hello, I'm <span class="welcome-hero-brand">DataCopilotX</span></h1>
      <p class="welcome-hero-desc">连接你的数据，用自然语言获得洞察、归因与预测</p>
    </div>

  </div>
</template>

<script setup lang="ts">
import { Prompts, type PromptsProps } from 'ant-design-x-vue';
import { BulbOutlined, InfoCircleOutlined, StarOutlined } from '@ant-design/icons-vue';
import { h } from 'vue';

const promptItems: PromptsProps['items'] = [
  {
    key: 'analysis',
    icon: h(BulbOutlined, { style: { color: '#8b5cf6' } }),
    label: 'Ignite Your Creativity',
    description: 'Got any sparks for a new project?'
  },
  {
    key: 'Modeling',
    icon: h(InfoCircleOutlined, {
      style: { color: '#6366f1' }
    }),
    label: 'Uncover Background Info',
    description: 'Help me understand this topic.'
  }
];
const promptStyle = {
  item: {
    width: 'calc(50% - 6px)'
  }
};
</script>

<style scoped lang="scss">
.welcome-wrap {
  position: relative;
  box-sizing: border-box;
  width: 100%;
  max-width: 860px;
  padding: 72px 30px 40px;
  margin: 0 auto;
}

/* 品牌渐变光晕 */
.welcome-glow {
  position: absolute;
  z-index: 0;
  width: 420px;
  height: 420px;
  pointer-events: none;
  filter: blur(90px);
  border-radius: 50%;
}

.welcome-glow-left {
  top: -120px;
  left: -80px;
  background: radial-gradient(circle, rgb(99 102 241 / 16%) 0%, transparent 70%);
}

.welcome-glow-right {
  top: 60px;
  right: -120px;
  background: radial-gradient(circle, rgb(139 92 246 / 12%) 0%, transparent 70%);
}

/* 主视觉区 */
.welcome-hero {
  position: relative;
  z-index: 1;
  padding: 24px 0 8px;
  text-align: center;

  .welcome-hero-badge {
    display: inline-flex;
    gap: 6px;
    align-items: center;
    padding: 5px 14px;
    font-size: 12px;
    color: var(--text-2);
    letter-spacing: 0.04em;
    background: var(--bg-glass);
    backdrop-filter: blur(10px);
    border: 1px solid var(--border-color);
    border-radius: 999px;

    .welcome-badge-icon {
      font-size: 12px;
      color: var(--brand-primary);
    }
  }

  .welcome-hero-title {
    margin: 18px 0 10px;
    font-size: 34px;
    font-weight: 700;
    color: var(--text-1);
    letter-spacing: -0.01em;
  }

  .welcome-hero-brand {
    color: transparent;
    background: var(--brand-gradient);
    background-clip: text;
  }

  .welcome-hero-desc {
    margin: 0;
    font-size: 15px;
    color: var(--text-3);
    letter-spacing: 0.02em;
  }
}

/* 提示卡片：玻璃化 + 悬浮上移 */
:deep(.ant-prompts) {
  position: relative;
  z-index: 1;
}

:deep(.ant-prompts-item) {
  background: var(--bg-glass) !important;
  backdrop-filter: blur(10px);
  border: 1px solid var(--border-color) !important;
  border-radius: 14px !important;
  box-shadow: var(--shadow-sm);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease !important;

  &:hover {
    border-color: var(--brand-primary) !important;
    box-shadow: var(--shadow-md);
    transform: translateY(-3px);
  }
}

:deep(.ant-prompts-item-title) {
  color: var(--text-1) !important;
}

:deep(.ant-prompts-item-description) {
  color: var(--text-3) !important;
}
</style>
