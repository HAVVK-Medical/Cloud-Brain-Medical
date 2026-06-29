<script setup lang="ts">
import { computed } from 'vue';
import { CalendarPlus, Plus } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
import PaginationBar from '@/components/shared/PaginationBar.vue';

const { workspace } = defineProps<{ workspace: any }>();

const weekdayOptions = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 0, label: '周日' },
];

const periodOptions = ['上午', '下午', '夜诊'];
const batchDoctors = computed(() =>
  workspace.doctors.filter((doctor: { departmentId: number | null }) =>
    !workspace.batchScheduleForm.departmentId || doctor.departmentId === workspace.batchScheduleForm.departmentId,
  ),
);
</script>

<template>
  <div class="space-y-6">
    <SectionCard title="批量生成排班">
      <div class="grid gap-3 lg:grid-cols-4">
        <label class="label-text">
          科室
          <select v-model.number="workspace.batchScheduleForm.departmentId" class="input-field mt-1">
            <option v-for="department in workspace.departments" :key="department.id" :value="department.id">
              {{ department.name }}
            </option>
          </select>
        </label>
        <label class="label-text">
          医生
          <select v-model.number="workspace.batchScheduleForm.doctorId" class="input-field mt-1">
            <option v-for="doctor in batchDoctors" :key="doctor.id" :value="doctor.id">
              {{ doctor.name }}
            </option>
          </select>
        </label>
        <label class="label-text">
          开始日期
          <input v-model="workspace.batchScheduleForm.startDate" type="date" class="input-field mt-1" />
        </label>
        <label class="label-text">
          结束日期
          <input v-model="workspace.batchScheduleForm.endDate" type="date" class="input-field mt-1" />
        </label>
      </div>

      <div class="mt-4 grid gap-4 lg:grid-cols-[1.2fr_1fr_1fr]">
        <div>
          <p class="label-text mb-2">星期</p>
          <div class="flex flex-wrap gap-2">
            <label
              v-for="option in weekdayOptions"
              :key="option.value"
              class="inline-flex items-center gap-2 rounded-md border border-border px-3 py-1.5 text-sm"
            >
              <input v-model="workspace.batchScheduleForm.weekdays" type="checkbox" :value="option.value" />
              <span>{{ option.label }}</span>
            </label>
          </div>
        </div>
        <div>
          <p class="label-text mb-2">时段</p>
          <div class="flex flex-wrap gap-2">
            <label
              v-for="period in periodOptions"
              :key="period"
              class="inline-flex items-center gap-2 rounded-md border border-border px-3 py-1.5 text-sm"
            >
              <input v-model="workspace.batchScheduleForm.periods" type="checkbox" :value="period" />
              <span>{{ period }}</span>
            </label>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-2">
          <label class="label-text">
            总号源
            <input v-model.number="workspace.batchScheduleForm.totalSlots" type="number" min="1" class="input-field mt-1" />
          </label>
          <label class="label-text">
            剩余号源
            <input v-model.number="workspace.batchScheduleForm.remainingSlots" type="number" min="0" class="input-field mt-1" />
          </label>
          <label class="label-text">
            门诊级别
            <input v-model="workspace.batchScheduleForm.visitLevel" class="input-field mt-1" />
          </label>
          <label class="label-text">
            状态
            <select v-model="workspace.batchScheduleForm.status" class="input-field mt-1">
              <option value="ACTIVE">启用</option>
              <option value="INACTIVE">停用</option>
            </select>
          </label>
        </div>
      </div>

      <div class="mt-4 flex flex-wrap items-center gap-3">
        <button class="btn-primary" type="button" @click="workspace.createBatchSchedules()" :disabled="workspace.batchCreatingSchedules">
          <CalendarPlus :size="16" />
          <span>{{ workspace.batchCreatingSchedules ? '生成中' : '生成排班' }}</span>
        </button>
        <span class="text-sm text-text-secondary">
          预计生成 {{ workspace.batchSchedulePreviewCount }} 条，覆盖 {{ workspace.batchSchedulePreviewDates.length }} 天
        </span>
        <span v-if="workspace.batchSchedulePreviewDates.length" class="text-xs text-text-secondary">
          {{ workspace.batchSchedulePreviewDates.slice(0, 5).join('、') }}{{ workspace.batchSchedulePreviewDates.length > 5 ? ' ...' : '' }}
        </span>
        <span v-if="workspace.batchScheduleMessage" class="text-sm text-success">{{ workspace.batchScheduleMessage }}</span>
      </div>
    </SectionCard>

    <SectionCard title="排班列表">
      <div class="mb-4 flex flex-wrap items-end gap-3">
        <label class="label-text">
          开始日期
          <input v-model="workspace.scheduleFromDate" type="date" class="input-field mt-1 w-40" />
        </label>
        <label class="label-text">
          结束日期
          <input v-model="workspace.scheduleToDate" type="date" class="input-field mt-1 w-40" />
        </label>
        <button class="btn-secondary" type="button" @click="workspace.loadAll()" :disabled="workspace.loading">应用筛选</button>
        <button class="btn-ghost" type="button" @click="workspace.clearFilters()">清空筛选</button>
        <span class="flex-1" />
        <button class="btn-secondary" type="button" @click="workspace.createNew('schedule')"><Plus :size="16" /><span>新增排班</span></button>
      </div>
      <div v-if="workspace.visibleSchedules.length" class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead><tr class="border-b border-border text-left text-text-secondary">
            <th class="pb-2 font-medium">日期</th><th class="pb-2 font-medium">时段</th><th class="pb-2 font-medium">医生</th><th class="pb-2 font-medium">科室</th><th class="pb-2 font-medium">号源</th><th class="pb-2 font-medium">状态</th><th class="pb-2 font-medium">操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="item in workspace.paginatedSchedules.pagedItems" :key="item.id" class="border-b border-border">
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
      <PaginationBar
        :page="workspace.paginatedSchedules.page"
        :page-count="workspace.paginatedSchedules.pageCount"
        :total="workspace.paginatedSchedules.total"
        :page-size="workspace.paginatedSchedules.pageSize"
        @update:page="workspace.paginatedSchedules.setPage"
      />
      <EmptyState v-if="!workspace.visibleSchedules.length" icon="calendar" title="暂无排班" />
    </SectionCard>
  </div>
</template>
