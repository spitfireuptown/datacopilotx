<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '@/stores/modules/auth';

const authStore = useAuthStore();

const handleAuthExpired = () => {
  authStore.clearAuth();
};

onMounted(() => {
  window.addEventListener('authExpired', handleAuthExpired);
});

onUnmounted(() => {
  window.removeEventListener('authExpired', handleAuthExpired);
});
</script>

<template>
  <router-view></router-view>
</template>
