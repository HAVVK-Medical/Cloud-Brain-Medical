<script setup lang="ts">
import { Stethoscope, ShieldCheck, UserRound } from 'lucide-vue-next';
import { RouterLink } from 'vue-router';
</script>

<template>
  <div class="max-w-3xl mx-auto py-16 px-6">
    <div class="text-center mb-10">
      <h1 class="text-2xl font-bold text-text-main">智慧云脑诊疗平台</h1>
      <p class="text-text-secondary mt-2 text-sm">选择您的角色进入工作区</p>
    </div>

    <div class="grid grid-cols-3 gap-6">
      <RouterLink to="/patient" class="card text-center hover:border-brand hover:shadow-md transition cursor-pointer no-underline">
        <div class="w-12 h-12 rounded-xl bg-brand-soft text-brand flex items-center justify-center mx-auto mb-3">
          <UserRound :size="24" />
        </div>
        <h2 class="text-base font-semibold text-text-main">患者端</h2>
        <p class="text-xs text-text-secondary mt-1">分诊、挂号、看病历</p>
      </RouterLink>

      <RouterLink to="/doctor" class="card text-center hover:border-brand hover:shadow-md transition cursor-pointer no-underline">
        <div class="w-12 h-12 rounded-xl bg-brand-soft text-brand flex items-center justify-center mx-auto mb-3">
          <Stethoscope :size="24" />
        </div>
        <h2 class="text-base font-semibold text-text-main">医生端</h2>
        <p class="text-xs text-text-secondary mt-1">接诊、病历、审方</p>
      </RouterLink>

      <RouterLink to="/admin" class="card text-center hover:border-brand hover:shadow-md transition cursor-pointer no-underline">
        <div class="w-12 h-12 rounded-xl bg-brand-soft text-brand flex items-center justify-center mx-auto mb-3">
          <ShieldCheck :size="24" />
        </div>
        <h2 class="text-base font-semibold text-text-main">管理端</h2>
        <p class="text-xs text-text-secondary mt-1">数据维护、配置、审计</p>
      </RouterLink>
    </div>

  </div>
</template>
