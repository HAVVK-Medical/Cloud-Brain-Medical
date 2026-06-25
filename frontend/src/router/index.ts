import { createRouter, createWebHistory } from 'vue-router';

import { pinia } from '@/stores/pinia';
import { useAuthStore } from '@/stores/auth';
import AuthView from '@/views/auth/AuthView.vue';
import HealthView from '@/views/HealthView.vue';
import HomeView from '@/views/HomeView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/health',
      name: 'health',
      component: HealthView,
    },
    {
      path: '/login',
      name: 'login',
      component: AuthView,
    },
    {
      path: '/patient',
      name: 'patient',
      component: () => import('@/views/patient/PatientHomeView.vue'),
      meta: { requiresAuth: true, role: 'patient' },
    },
    {
      path: '/doctor',
      name: 'doctor',
      component: () => import('@/views/doctor/DoctorHomeView.vue'),
      meta: { requiresAuth: true, role: 'doctor' },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('@/views/admin/AdminHomeView.vue'),
      meta: { requiresAuth: true, role: 'admin' },
    },
  ],
});

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia);

  if (!authStore.hydrated) {
    authStore.hydrateFromStorage();
  }

  if (authStore.isAuthenticated && authStore.isExpired) {
    if (!authStore.refreshToken) {
      return {
        path: '/login',
        query: { redirect: to.fullPath, role: to.meta.role },
      };
    }

    try {
      await authStore.refreshSession();
    } catch {
      return {
        path: '/login',
        query: { redirect: to.fullPath, role: to.meta.role },
      };
    }
  }

  if (!to.meta.requiresAuth) {
    return true;
  }

  if (!authStore.isAuthenticated || authStore.isExpired) {
    return {
      path: '/login',
      query: { redirect: to.fullPath, role: to.meta.role },
    };
  }

  if (to.meta.role && authStore.role !== to.meta.role) {
    return {
      path: '/login',
      query: { redirect: to.fullPath, role: to.meta.role },
    };
  }

  return true;
});

export default router;
