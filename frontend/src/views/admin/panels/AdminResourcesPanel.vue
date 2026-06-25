<script setup lang="ts">
import { Plus } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <SectionCard title="排班列表">
    <div class="flex gap-2 mb-4">
      <button class="btn-secondary" type="button" @click="workspace.createNew('schedule')"><Plus :size="16" /><span>新增排班</span></button>
    </div>
    <div v-if="workspace.visibleSchedules.length" class="overflow-x-auto">
      <table class="w-full text-sm">
        <thead><tr class="border-b border-border text-left text-text-secondary">
          <th class="pb-2 font-medium">日期</th><th class="pb-2 font-medium">时段</th><th class="pb-2 font-medium">医生</th><th class="pb-2 font-medium">科室</th><th class="pb-2 font-medium">号源</th><th class="pb-2 font-medium">状态</th><th class="pb-2 font-medium">操作</th>
        </tr></thead>
        <tbody>
          <tr v-for="item in workspace.visibleSchedules" :key="item.id" class="border-b border-border">
            <td class="py-2.5 font-medium">{{ item.workDate }}</td>
            <td class="py-2.5 text-text-secondary">{{ item.period }}</td>
            <td class="py-2.5">{{ item.doctorName }}</td>
            <td class="py-2.5 text-text-secondary">{{ item.departmentName }}</td>
            <td class="py-2.5">{{ item.remainingSlots }}/{{ item.totalSlots }}</td>
            <td class="py-2.5"><StatusChip :tone="item.status === 'ACTIVE' ? 'success' : 'neutral'">{{ item.status === 'ACTIVE' ? '启用' : '停用' }}</StatusChip></td>
            <td class="py-2.5"><div class="flex gap-1">
              <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.selectSchedule(item)">编辑</button>
              <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.currentKind = 'schedule'; workspace.currentId = item.id; workspace.toggleCurrent()" :disabled="workspace.saving">切换</button>
            </div></td>
          </tr>
        </tbody>
      </table>
    </div>
    <EmptyState v-else icon="calendar" title="暂无排班" />
  </SectionCard>
</template>
