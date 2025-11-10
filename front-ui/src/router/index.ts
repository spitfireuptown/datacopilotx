import { createRouter, createWebHistory } from 'vue-router';
import homeRoutes from './modules/home';

const router = createRouter({
  history: createWebHistory(),
  routes: [...homeRoutes]
});

export default router;
