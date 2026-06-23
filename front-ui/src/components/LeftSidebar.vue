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

    <!-- 左下角用户信息 -->
    <a-popover v-if="authStore.isLoggedIn" trigger="click" placement="rightBottom">
      <template #content>
        <div class="user-popover">
          <div class="user-popover-name">{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</div>
          <div class="user-popover-role">{{ authStore.roleDesc }}</div>
          <a-divider style="margin: 8px 0" />
          <a-button type="text" danger block @click="handleLogout">退出登录</a-button>
        </div>
      </template>
      <div class="user-info">
        <a-avatar :size="36" :style="{ backgroundColor: '#1890ff' }">
          {{ (authStore.userInfo?.nickname || authStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}
        </a-avatar>
        <span class="user-name">{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</span>
      </div>
    </a-popover>
  </div>
</template>

<script setup lang="ts">
// 导入必要的依赖
import { ref, onMounted, watch } from 'vue';
import { MessageOutlined, DatabaseOutlined, RadarChartOutlined, SettingOutlined } from '@ant-design/icons-vue';
import { useRouter, useRoute } from 'vue-router';
import { useDialogueStore } from '@/stores/modules/dialogues';
import { useAuthStore } from '@/stores/modules/auth';

const router = useRouter();
const route = useRoute();
const dialogueStore = useDialogueStore();
const authStore = useAuthStore();

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

// 退出登录
const handleLogout = () => {
  authStore.logout();
  router.push('/login');
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

/* 用户信息区域 */
.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 4px;
  cursor: pointer;
  transition: all 0.3s;
  border-top: 1px solid #f0f0f0;
}

.user-info:hover {
  background-color: #f5f5f5;
}

.user-name {
  font-size: 11px;
  color: #333;
  margin-top: 6px;
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
}

.user-popover {
  min-width: 140px;
}

.user-popover-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.user-popover-role {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>