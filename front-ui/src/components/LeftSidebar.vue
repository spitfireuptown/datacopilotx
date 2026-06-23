<template>
  <div class="sidebar2">
    <div class="menu-list">
      <!-- 使用:class绑定动态添加active类 -->
      <div class="menu-item" :class="{ active: activeMenu === 'chat' }" @click="goToChat">
        <MessageOutlined class="menu-icon" />
        <span class="menu-text">问数</span>
      </div>
      <div class="menu-item" :class="{ active: activeMenu === 'dataset' }" @click="goToDatasetConfig">
        <RadarChartOutlined class="menu-icon" />
        <span class="menu-text">数据集</span>
      </div>
      <div class="menu-item" :class="{ active: activeMenu === 'knowledge' }" @click="goToKnowledge">
        <DatabaseOutlined class="menu-icon" />
        <span class="menu-text">知识库</span>
      </div>
      <div class="menu-item" :class="{ active: activeMenu === 'settings' }" @click="goToSettings">
        <SettingOutlined class="menu-icon" />
        <span class="menu-text">设置</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 导入必要的依赖
import { ref, onMounted, watch } from 'vue';
import { MessageOutlined, DatabaseOutlined, RadarChartOutlined, SettingOutlined } from '@ant-design/icons-vue';
import { useRouter, useRoute } from 'vue-router';
import { useDialogueStore } from '@/stores/modules/dialogues';

const router = useRouter();
const route = useRoute();
const dialogueStore = useDialogueStore();

// 当前激活的菜单
const activeMenu = ref('chat'); // 默认选中"问数"

// 初始化时根据当前路由设置激活菜单
onMounted(() => {
  updateActiveMenu();
});

// 监听路由变化，更新激活菜单
watch(() => route.path, () => {
  updateActiveMenu();
});

// 根据当前路由更新激活菜单
const updateActiveMenu = () => {
  const path = route.path;
  if (path === '/') {
    activeMenu.value = 'chat';
  } else if (path === '/dataset-config' || path === '/database-connection-form' || 
             path === '/data-source-type-select' || path === '/excel-upload-form') {
    activeMenu.value = 'dataset';
  } else if (path === '/knowledge' || path.startsWith('/knowledge/')) {
    activeMenu.value = 'knowledge';
  } else if (path === '/model-config') {
    activeMenu.value = 'settings';
  }
};

// 跳转到问数页面并创建新对话
const goToChat = () => {
  activeMenu.value = 'chat';
  
  // 如果当前已经在问数页面，直接重置对话
  if (route.path === '/') {
    // 重置对话状态
    dialogueStore.resetHistory();
    
    // 通知其他组件创建新对话
    window.dispatchEvent(new CustomEvent('createNewChat'));
  } else {
    // 如果不在问数页面，导航到问数页面
    router.push('/');
    
    // 导航完成后创建新对话
    router.afterEach(() => {
      dialogueStore.resetHistory();
      window.dispatchEvent(new CustomEvent('createNewChat'));
    });
  }
};

// 跳转到数据集页面
const goToDatasetConfig = () => {
  activeMenu.value = 'dataset';
  router.push('/dataset-config');
};

// 跳转到知识库页面
const goToKnowledge = () => {
  activeMenu.value = 'knowledge';
  router.push('/knowledge');
};

// 跳转到设置页面
const goToSettings = () => {
  activeMenu.value = 'settings';
  router.push('/model-config');
};
</script>

<style lang="scss" scoped>
/* 样式部分保持不变 */
.sidebar2 {
  width: 80px;
  height: 100%;
  background-color: #fff;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  padding: 16px 0;
  box-sizing: border-box;
  align-items: center;
}

.menu-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0;
  cursor: pointer;
  transition: all 0.3s;
  width: 100%;
  color: #666;
}

.menu-item:hover {
  background-color: #f5f5f5;
}

.menu-item.active {
  color: #1890ff;
  background-color: #e6f7ff;
}

.menu-icon {
  font-size: 18px;
  margin-bottom: 4px;
}

.menu-text {
  font-size: 12px;
}
</style>