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
      <div class="menu-item" :class="{ active: activeMenu === 'dashboard' }" @click="goToDashboard">
        <DashboardOutlined class="menu-icon" />
        <span class="menu-text">仪表盘</span>
      </div>
    </div>

    <div class="sidebar-bottom">
      <!-- 超级管理员专属菜单 -->
      <div v-if="authStore.role === 0" class="admin-menu-list">
        <div class="menu-item" :class="{ active: activeMenu === 'user-management' }" @click="goToUserManagement">
          <UserOutlined class="menu-icon" />
          <span class="menu-text">用户管理</span>
        </div>
      </div>

      <!-- 管理员和超级管理员专属菜单 -->
      <div v-if="authStore.role === 0 || authStore.role === 1" class="admin-menu-list">
        <div class="menu-item" :class="{ active: activeMenu === 'settings' }" @click="goToSettings">
          <SettingOutlined class="menu-icon" />
          <span class="menu-text">设置</span>
        </div>
        <div class="menu-item" :class="{ active: activeMenu === 'auth-config' }" @click="goToAuthConfig">
          <LockOutlined class="menu-icon" />
          <span class="menu-text">权限配置</span>
        </div>
      </div>

      <!-- 左下角用户信息 -->
      <a-popover v-if="authStore.isLoggedIn" trigger="click" placement="rightBottom">
        <template #content>
          <div class="user-popover">
            <div class="user-popover-name">{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</div>
            <div class="user-popover-role">{{ authStore.roleDesc }}</div>
            <a-divider style="margin: 8px 0" />
            <a-button type="text" block @click="showPasswordModal = true">修改密码</a-button>
            <a-button type="text" danger block @click="handleLogout">退出登录</a-button>
          </div>
        </template>
        <div class="user-info">
          <div class="user-avatar-ring">
            <a-avatar :size="36" :style="{ backgroundColor: '#6366f1', fontWeight: 600 }">
              {{ (authStore.userInfo?.nickname || authStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}
            </a-avatar>
          </div>
          <span class="user-name">{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</span>
        </div>
      </a-popover>
    </div>

    <!-- 修改密码弹窗 -->
    <a-modal
      v-model:open="showPasswordModal"
      title="修改密码"
      :confirm-loading="passwordLoading"
      @ok="handleChangePassword"
      @cancel="resetPasswordForm"
    >
      <a-form :model="passwordForm" layout="vertical">
        <a-form-item label="原密码">
          <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入原密码" />
        </a-form-item>
        <a-form-item label="新密码">
          <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码" />
        </a-form-item>
        <a-form-item label="确认新密码">
          <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
// 导入必要的依赖
import { ref, reactive, onMounted, watch } from 'vue';
import { message } from 'ant-design-vue';
import { MessageOutlined, DatabaseOutlined, RadarChartOutlined, SettingOutlined, UserOutlined, LockOutlined, DashboardOutlined } from '@ant-design/icons-vue';
import { useRouter, useRoute } from 'vue-router';
import { useDialogueStore } from '@/stores/modules/dialogues';
import { useAuthStore } from '@/stores/modules/auth';
import { changePassword } from '@/api/auth';

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
  } else if (path === '/dashboard') {
    activeMenu.value = 'dashboard';
  } else if (path === '/model-config') {
    activeMenu.value = 'settings';
  } else if (path === '/user-management') {
    activeMenu.value = 'user-management';
  } else if (path === '/auth-config') {
    activeMenu.value = 'auth-config';
  }
};

// 跳转到问数页面并创建新对话
const goToChat = () => {
  activeMenu.value = 'chat';
  
  if (route.path === '/') {
    dialogueStore.resetHistory();
    window.dispatchEvent(new CustomEvent('createNewChat'));
  } else {
    router.push('/');
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

// 跳转到仪表盘页面
const goToDashboard = () => {
  activeMenu.value = 'dashboard';
  router.push('/dashboard');
};

// 跳转到设置页面
const goToSettings = () => {
  activeMenu.value = 'settings';
  router.push('/model-config');
};

// 跳转到用户管理页面
const goToUserManagement = () => {
  activeMenu.value = 'user-management';
  router.push('/user-management');
};

// 跳转到权限配置页面
const goToAuthConfig = () => {
  activeMenu.value = 'auth-config';
  router.push('/auth-config');
};

// 修改密码
const showPasswordModal = ref(false);
const passwordLoading = ref(false);
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const resetPasswordForm = () => {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
};

const handleChangePassword = async () => {
  if (!passwordForm.oldPassword) {
    message.warning('请输入原密码');
    return;
  }
  if (!passwordForm.newPassword) {
    message.warning('请输入新密码');
    return;
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    message.warning('两次输入的新密码不一致');
    return;
  }
  passwordLoading.value = true;
  try {
    await changePassword(passwordForm.oldPassword, passwordForm.newPassword);
    message.success('密码修改成功');
    showPasswordModal.value = false;
    resetPasswordForm();
  } catch (error: any) {
    message.error(error?.message || '密码修改失败');
  } finally {
    passwordLoading.value = false;
  }
};

// 退出登录
const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<style lang="scss" scoped>
.sidebar2 {
  position: relative;
  z-index: 20;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 80px;
  height: 100%;
  padding: 16px 0;
  background-color: var(--bg-sidebar);
  border-right: 1px solid var(--border-color-light);
}

.menu-list {
  box-sizing: border-box;
  display: flex;
  flex: 1;
  flex-direction: column;
  width: 100%;
  padding: 0 8px;
}

.sidebar-bottom {
  position: absolute;
  bottom: 0;
  left: 0;
  box-sizing: border-box;
  width: 100%;
  padding-bottom: 16px;
}

.menu-icon {
  margin-bottom: 4px;
  font-size: 18px;
}

.menu-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  padding: 12px 4px;
  margin-bottom: 4px;
  color: var(--text-2);
  cursor: pointer;
  border-radius: 12px;
  transition:
    background-color 0.25s ease,
    color 0.25s ease,
    transform 0.25s ease;

  .menu-icon {
    transition: transform 0.25s ease;
  }

  &:hover {
    color: var(--text-1);
    background-color: var(--bg-hover);

    .menu-icon {
      transform: translateY(-1px) scale(1.08);
    }
  }

  &.active {
    font-weight: 500;
    color: var(--brand-primary);
    background-color: var(--bg-hover);

    /* 左侧品牌渐变指示条 */
    &::before {
      position: absolute;
      top: 50%;
      left: -8px;
      width: 3px;
      height: 24px;
      content: '';
      background: var(--brand-gradient);
      border-radius: 2px;
      box-shadow: var(--brand-glow);
      transform: translateY(-50%);
    }

    /* 图标为 SVG（fill: currentColor），不能用 background-clip 渐变，直接用品牌色 */
    .menu-icon {
      color: var(--brand-primary);
    }
  }
}

.menu-text {
  font-size: 12px;
}

.admin-menu-list {
  width: 100%;
  padding-top: 8px;
  border-top: 1px solid var(--border-color-light);
}

/* 用户信息区域 */
.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 4px;
  cursor: pointer;
  border-top: 1px solid var(--border-color-light);
  transition: background-color 0.25s ease;
}

.user-info:hover {
  background-color: var(--bg-hover);
}

/* 头像品牌渐变描边 */
.user-avatar-ring {
  padding: 2px;
  background: var(--brand-gradient);
  border-radius: 50%;
  box-shadow: var(--brand-glow);

  :deep(.ant-avatar) {
    box-sizing: content-box;
    border: 2px solid var(--bg-sidebar);
  }
}

.user-name {
  max-width: 72px;
  margin-top: 6px;
  overflow: hidden;
  font-size: 11px;
  color: var(--text-2);
  text-align: center;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-popover {
  min-width: 140px;
}

.user-popover-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-1);
}

.user-popover-role {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-3);
}
</style>