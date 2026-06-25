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
      component: () => import('@/views/patient/PatientHomeView.vue'),
      meta: { requiresAuth: true, role: 'patient' },
      redirect: '/patient/overview',
      children: [
        { path: 'overview', name: 'patient-overview', component: () => import('@/views/patient/panels/PatientOverviewPanel.vue') },
        { path: 'triage', name: 'patient-triage', component: () => import('@/views/patient/panels/PatientTriagePanel.vue') },
        { path: 'registration', name: 'patient-registration', component: () => import('@/views/patient/panels/PatientRegistrationPanel.vue') },
        { path: 'records', name: 'patient-records', component: () => import('@/views/patient/panels/PatientRecordsPanel.vue') },
        { path: 'profile', name: 'patient-profile', component: () => import('@/views/patient/panels/PatientProfilePanel.vue') },
        { path: 'history', name: 'patient-history', component: () => import('@/views/patient/panels/PatientHistoryPanel.vue') },
      ],
    },
    {
      path: '/doctor',
      component: () => import('@/views/doctor/DoctorHomeView.vue'),
      meta: { requiresAuth: true, role: 'doctor' },
      redirect: '/doctor/overview',
      children: [
        { path: 'overview', name: 'doctor-overview', component: () => import('@/views/doctor/panels/DoctorOverviewPanel.vue') },
        { path: 'consultation', name: 'doctor-consultation', component: () => import('@/views/doctor/panels/DoctorConsultationPanel.vue') },
        { path: 'consultation/:id', name: 'doctor-consultation-patient', component: () => import('@/views/doctor/panels/DoctorConsultationPanel.vue') },
        { path: 'history', name: 'doctor-history', component: () => import('@/views/doctor/panels/DoctorHistoryPanel.vue') },
        { path: 'schedule', name: 'doctor-schedule', component: () => import('@/views/doctor/panels/DoctorSchedulePanel.vue') },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminHomeView.vue'),
      meta: { requiresAuth: true, role: 'admin' },
      redirect: '/admin/overview',
      children: [
        { path: 'overview', name: 'admin-overview', component: () => import('@/views/admin/panels/AdminOverviewPanel.vue') },
        { path: 'master-data', name: 'admin-master-data', component: () => import('@/views/admin/panels/AdminMasterPanel.vue') },
        { path: 'resources', name: 'admin-resources', component: () => import('@/views/admin/panels/AdminResourcesPanel.vue') },
        { path: 'config', name: 'admin-config', component: () => import('@/views/admin/panels/AdminConfigPanel.vue') },
        { path: 'audit', name: 'admin-audit', component: () => import('@/views/admin/panels/AdminAuditPanel.vue') },
      ],
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
