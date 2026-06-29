<script setup lang="ts">
import { ref } from 'vue';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
import PaginationBar from '@/components/shared/PaginationBar.vue';

const auditTabs = [
  { id: 'ai' as const, label: 'AI调用记录' },
  { id: 'audit' as const, label: '审计日志' },
];
const activeAuditTab = ref<typeof auditTabs[number]['id']>('ai');

const aiRecordTabs = [
  { id: 'ALL', label: '全部' },
  { id: 'TRIAGE', label: '分诊' },
  { id: 'MEDICAL_RECORD', label: '病历' },
  { id: 'DIAGNOSIS', label: '诊断' },
  { id: 'PRESCRIPTION_REVIEW', label: '审方' },
];

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="space-y-6">
    <div class="flex gap-1 bg-gray-100 rounded-lg p-1 w-fit">
      <button v-for="tab in auditTabs" :key="tab.id" type="button" class="px-4 py-1.5 rounded-md text-sm font-medium transition" :class="activeAuditTab === tab.id ? 'bg-white text-brand shadow-sm' : 'text-text-secondary hover:text-text-main'" @click="activeAuditTab = tab.id">
        {{ tab.label }}
      </button>
    </div>

    <SectionCard v-if="activeAuditTab === 'ai'" title="AI调用记录">
      <div class="mb-4 flex flex-wrap gap-1 rounded-lg bg-gray-100 p-1 w-fit">
        <button
          v-for="tab in aiRecordTabs"
          :key="tab.id"
          type="button"
          class="px-3 py-1.5 rounded-md text-xs font-medium transition"
          :class="workspace.aiRecordTaskType === tab.id ? 'bg-white text-brand shadow-sm' : 'text-text-secondary hover:text-text-main'"
          @click="workspace.changeAiRecordTaskType(tab.id)"
        >
          {{ tab.label }}
        </button>
      </div>
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead><tr class="border-b border-border text-left text-text-secondary">
            <th class="pb-2 font-medium">时间</th><th class="pb-2 font-medium">类型</th><th class="pb-2 font-medium">提供方</th><th class="pb-2 font-medium">耗时</th><th class="pb-2 font-medium">状态</th>
          </tr></thead>
          <tbody>
            <tr v-for="item in workspace.paginatedAiRecords.pagedItems" :key="item.id" class="border-b border-border">
              <td class="py-2.5 text-text-secondary">{{ workspace.formatDateTime(item.createdAt) }}</td>
              <td class="py-2.5 font-medium">{{ item.taskType }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.provider ?? '-' }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.durationMs ? item.durationMs + 'ms' : '-' }}</td>
              <td class="py-2.5"><StatusChip :tone="item.callStatus === 'SUCCESS' ? 'success' : 'danger'">{{ item.callStatus }}</StatusChip></td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar
        :page="workspace.paginatedAiRecords.page"
        :page-count="workspace.paginatedAiRecords.pageCount"
        :total="workspace.paginatedAiRecords.total"
        :page-size="workspace.paginatedAiRecords.pageSize"
        @update:page="workspace.paginatedAiRecords.setPage"
      />
      <EmptyState v-if="!workspace.aiRecords.length" icon="search" title="暂无AI调用记录" />
    </SectionCard>

    <SectionCard v-if="activeAuditTab === 'audit'" title="审计日志">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead><tr class="border-b border-border text-left text-text-secondary">
            <th class="pb-2 font-medium">时间</th><th class="pb-2 font-medium">操作人</th><th class="pb-2 font-medium">操作</th><th class="pb-2 font-medium">资源</th><th class="pb-2 font-medium">结果</th>
          </tr></thead>
          <tbody>
            <tr v-for="item in workspace.paginatedAuditLogs.pagedItems" :key="item.id" class="border-b border-border">
              <td class="py-2.5 text-text-secondary">{{ workspace.formatDateTime(item.occurredAt) }}</td>
              <td class="py-2.5 font-medium">{{ item.actorRole }}/{{ item.actorId }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.action }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.resourceType }}{{ item.resourceId ? '/' + item.resourceId : '' }}</td>
              <td class="py-2.5"><StatusChip :tone="item.success ? 'success' : 'danger'">{{ item.success ? '成功' : '失败' }}</StatusChip></td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar
        :page="workspace.paginatedAuditLogs.page"
        :page-count="workspace.paginatedAuditLogs.pageCount"
        :total="workspace.paginatedAuditLogs.total"
        :page-size="workspace.paginatedAuditLogs.pageSize"
        @update:page="workspace.paginatedAuditLogs.setPage"
      />
    </SectionCard>
  </div>
</template>
