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
  }
];

export default homeRoutes;
