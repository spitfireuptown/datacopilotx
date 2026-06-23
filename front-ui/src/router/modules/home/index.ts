import { RouteRecordRaw } from 'vue-router';

const homeRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'ChatDemo',
    component: () => import('@/views/AIChat.vue'),
    meta: {
      title: 'Hello World',
      keepAlive: true
    }
  },
  {
    path: '/dataset-config',
    name: 'DatasetConfig',
    component: () => import('@/views/DatasetConfig.vue'),
    meta: {
      title: '数据源配置',
      keepAlive: true
    }
  },
  {
    path: '/data-source-type-select',
    name: 'DataSourceTypeSelect',
    component: () => import('@/views/DataSourceTypeSelect.vue'),
    meta: {
      title: '选择数据源类型',
      keepAlive: true
    }
  },
  {
    path: '/excel-upload-form',
    name: 'ExcelUploadForm',
    component: () => import('@/views/ExcelUploadForm.vue'),
    meta: {
      title: 'Excel文件上传',
      keepAlive: true
    }
  },
  {
    path: '/model-config',
    name: 'ModelConfig',
    component: () => import('@/views/ModelConfig.vue'),
    meta: {
      title: '大模型配置',
      keepAlive: true
    }
  },
  {
    path: '/database-connection-form',
    name: 'DatabaseConnectionForm',
    component: () => import('@/views/DatabaseConnectionForm.vue'),
    meta: {
      title: '数据库连接配置',
      keepAlive: true
    }
  },
  {
    path: '/knowledge',
    name: 'KnowledgeBase',
    component: () => import('@/views/KnowledgeBase.vue'),
    meta: {
      title: '知识库管理',
      keepAlive: true
    }
  },
  {
    path: '/knowledge/:id',
    name: 'KnowledgeDetail',
    component: () => import('@/views/KnowledgeDetail.vue'),
    meta: {
      title: '知识库详情',
      keepAlive: false
    }
  },
  {
    path: '/user-management',
    name: 'UserManagement',
    component: () => import('@/views/UserManagement.vue'),
    meta: {
      title: '用户管理',
      keepAlive: true
    }
  },
  {
    path: '/auth-config',
    name: 'AuthConfig',
    component: () => import('@/views/AuthConfig.vue'),
    meta: {
      title: '权限配置',
      keepAlive: true
    }
  }
];

export default homeRoutes;
