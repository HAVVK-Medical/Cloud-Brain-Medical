<script setup lang="ts">
import SectionCard from '@/components/shared/SectionCard.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="p-4 space-y-4">
    <SectionCard title="个人资料">
      <div class="space-y-3">
        <label class="label-text">姓名 <input v-model="workspace.profileForm.realName" class="input-field phone-input mt-1" placeholder="请输入姓名" /></label>
        <label class="label-text">性别
          <select v-model="workspace.profileForm.gender" class="input-field phone-input mt-1">
            <option value="">请选择</option><option value="男">男</option><option value="女">女</option>
          </select>
        </label>
        <label class="label-text">年龄 <input v-model="workspace.profileForm.age" class="input-field phone-input mt-1" type="number" placeholder="请输入年龄" /></label>
        <label class="label-text">手机号 <input v-model="workspace.profileForm.phone" class="input-field phone-input mt-1" placeholder="请输入手机号" /></label>
        <label class="label-text">身份证号 <input v-model="workspace.profileForm.idCardNumber" class="input-field phone-input mt-1" placeholder="请输入身份证号" /></label>
      </div>
    </SectionCard>

    <SectionCard title="既往史">
      <textarea v-model="workspace.profileForm.medicalHistory" class="input-field phone-input h-24 resize-none" placeholder="请输入既往病史..." />
    </SectionCard>

    <SectionCard title="备注">
      <textarea v-model="workspace.profileForm.remark" class="input-field phone-input h-24 resize-none" placeholder="过敏史等其他信息..." />
    </SectionCard>

    <button class="btn-primary w-full" type="button" @click="workspace.saveProfile()" :disabled="workspace.savingProfile">
      {{ workspace.savingProfile ? '保存中...' : '保存资料' }}
    </button>
  </div>
</template>
