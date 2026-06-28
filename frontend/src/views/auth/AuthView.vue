<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { LogIn, ShieldCheck, Stethoscope, UserPlus, UserRound } from 'lucide-vue-next';

import { useAuthStore } from '@/stores/auth';
import type { WorkspaceRole } from '@/types/enums';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const role = ref<WorkspaceRole>('patient');
const mode = ref<'login' | 'register'>('login');
const submitted = ref(false);
const expiredNotice = ref('');

const redirectTarget = computed(() => {
  const redirect = route.query.redirect;
  if (typeof redirect === 'string' && redirect.startsWith('/')) return redirect;
  return role.value === 'patient' ? '/patient' : role.value === 'doctor' ? '/doctor' : '/admin';
});

const form = reactive({
  username: '',
  password: '',
  realName: '',
  phone: '',
  gender: '',
  age: '',
});

const roleOptions: Array<{ value: WorkspaceRole; label: string; icon: typeof UserRound }> = [
  { value: 'patient', label: '患者', icon: UserRound },
  { value: 'doctor', label: '医生', icon: Stethoscope },
  { value: 'admin', label: '管理员', icon: ShieldCheck },
];

const canRegister = computed(() => role.value === 'patient');

watch(
  () => route.query.role,
  (nextRole) => {
    if (nextRole === 'patient' || nextRole === 'doctor' || nextRole === 'admin') {
      role.value = nextRole;
      if (nextRole !== 'patient') mode.value = 'login';
    }
  },
  { immediate: true },
);

watch(
  () => route.query.reason,
  (reason) => {
    if (reason === 'expired') {
      expiredNotice.value = '登录已过期，请重新登录';
    }
  },
  { immediate: true },
);

async function submit() {
  submitted.value = true;
  try {
    if (role.value === 'patient' && mode.value === 'register') {
      await authStore.register({
        username: form.username.trim(),
        password: form.password,
        realName: form.realName.trim(),
        phone: form.phone.trim(),
        gender: form.gender.trim(),
        age: form.age ? Number(form.age) : null,
      });
      mode.value = 'login';
      return;
    }
    await authStore.login(role.value, {
      username: form.username.trim(),
      password: form.password,
    });
    await router.push(redirectTarget.value);
  } finally {
    submitted.value = false;
  }
}
</script>

<template>
  <div class="max-w-sm mx-auto py-16 px-4">
    <h1 class="text-xl font-bold text-text-main text-center mb-8">
      {{ mode === 'register' && role === 'patient' ? '患者注册' : '登录' }}
    </h1>

    <div class="card space-y-4">
      <!-- Role selector -->
      <div class="flex gap-1 bg-gray-100 rounded-lg p-1">
        <button
          v-for="option in roleOptions"
          :key="option.value"
          type="button"
          class="flex-1 py-1.5 text-sm font-medium rounded-md transition flex items-center justify-center gap-1"
          :class="role === option.value ? 'bg-white text-brand shadow-sm' : 'text-text-secondary'"
          @click="role = option.value"
        >
          <component :is="option.icon" :size="14" />
          <span>{{ option.label }}</span>
        </button>
      </div>

      <!-- Mode toggle -->
      <div class="flex gap-1 bg-gray-100 rounded-lg p-1">
        <button
          class="flex-1 py-1.5 text-sm font-medium rounded-md transition flex items-center justify-center gap-1"
          :class="mode === 'login' ? 'bg-white text-brand shadow-sm' : 'text-text-secondary'"
          type="button"
          @click="mode = 'login'"
        >
          <LogIn :size="14" /><span>登录</span>
        </button>
        <button
          v-if="canRegister"
          class="flex-1 py-1.5 text-sm font-medium rounded-md transition flex items-center justify-center gap-1"
          :class="mode === 'register' ? 'bg-white text-brand shadow-sm' : 'text-text-secondary'"
          type="button"
          @click="mode = 'register'"
        >
          <UserPlus :size="14" /><span>注册</span>
        </button>
      </div>

      <!-- Form fields -->
      <label class="label-text">用户名
        <input v-model="form.username" class="input-field phone-input mt-1" autocomplete="username" placeholder="请输入用户名" />
      </label>

      <label class="label-text">密码
        <input v-model="form.password" class="input-field phone-input mt-1" type="password" autocomplete="current-password" placeholder="••••••••" />
      </label>

      <template v-if="role === 'patient' && mode === 'register'">
        <label class="label-text">真实姓名
          <input v-model="form.realName" class="input-field phone-input mt-1" autocomplete="name" placeholder="请输入真实姓名" />
        </label>
        <label class="label-text">手机号
          <input v-model="form.phone" class="input-field phone-input mt-1" autocomplete="tel" placeholder="请输入手机号" />
        </label>
        <div class="grid grid-cols-2 gap-3">
          <label class="label-text">性别
            <input v-model="form.gender" class="input-field phone-input mt-1" placeholder="男/女" />
          </label>
          <label class="label-text">年龄
            <input v-model="form.age" class="input-field phone-input mt-1" type="number" min="0" placeholder="0" />
          </label>
        </div>
      </template>

      <p v-if="expiredNotice" class="text-warning text-xs p-2 bg-yellow-50 rounded-md">{{ expiredNotice }}</p>
      <p v-if="authStore.error" class="text-danger text-xs p-2 bg-red-50 rounded-md">{{ authStore.error }}</p>

      <button class="btn-primary w-full" type="submit" @click="submit" :disabled="authStore.loading || submitted">
        <LogIn :size="16" />
        <span>{{ authStore.loading ? '处理中...' : (mode === 'register' && role === 'patient' ? '注册' : '登录') }}</span>
      </button>
    </div>
  </div>
</template>
