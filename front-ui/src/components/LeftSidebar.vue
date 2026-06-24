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
          <a-avatar :size="36" :style="{ backgroundColor: '#1890ff' }">
            {{ (authStore.userInfo?.nickname || authStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}
          </a-avatar>
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
import { MessageOutlined, DatabaseOutlined, RadarChartOutlined, SettingOutlined, UserOutlined, LockOutlined } from '@ant-design/icons-vue';
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
  position: relative;
}

.menu-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.sidebar-bottom {
  width: 100%;
  position: absolute;
  bottom: 0;
  left: 0;
  padding-bottom: 16px;
  box-sizing: border-box;
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

.admin-menu-list {
  width: 100%;
  border-top: 1px solid #f0f0f0;
  padding-top: 8px;
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