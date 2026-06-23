import { createRouter, createWebHistory } from 'vue-router';
import homeRoutes from './modules/home';
import { useAuthStore } from '@/stores/modules/auth';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/user/Login.vue'),
      meta: { requiresAuth: false },
    },
    ...homeRoutes,
  ],
});

// 路由守卫
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore();

  // 不需要认证的页面直接放行
  if (to.meta.requiresAuth === false) {
    next();
    return;
  }

  // 需要认证但没有 token
  if (!authStore.isLoggedIn) {
    next({ path: '/login', query: { redirect: to.fullPath } });
    return;
  }

  next();
});

export default router;