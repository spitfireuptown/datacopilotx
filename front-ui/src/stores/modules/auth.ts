import { defineStore } from 'pinia';
import { login, getUserInfo, type LoginParams, type UserInfo } from '@/api/auth';

const TOKEN_KEY = 'access_token';
const USER_INFO_KEY = 'user_info';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: (localStorage.getItem(TOKEN_KEY) || '') as string,
    userInfo: (() => {
      const stored = localStorage.getItem(USER_INFO_KEY);
      return stored ? JSON.parse(stored) as UserInfo : null;
    })(),
    roles: [] as string[],
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    role: (state) => state.userInfo?.role ?? -1,
    roleDesc: (state) => state.userInfo?.roleDesc ?? '',
  },

  actions: {
    async login(params: LoginParams) {
      const result = await login(params);
      this.token = result.token;
      localStorage.setItem(TOKEN_KEY, result.token);
      await this.fetchUserInfo();
    },

    async fetchUserInfo() {
      const userInfo = await getUserInfo();
      this.userInfo = userInfo;
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo));
      const roleMap: Record<number, string> = { 0: 'super-admin', 1: 'admin', 2: 'user' };
      this.roles = [roleMap[userInfo.role] || 'user'];
    },

    logout() {
      this.token = '';
      this.userInfo = null;
      this.roles = [];
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_INFO_KEY);
    },

    clearAuth() {
      this.logout();
    },
  },
});