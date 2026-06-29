<script setup lang="ts">
import { ref } from 'vue';
import { Search, Plus } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
import PaginationBar from '@/components/shared/PaginationBar.vue';

const tabs = [
  { id: 'departments' as const, label: '科室' },
  { id: 'doctors' as const, label: '医生' },
  { id: 'drugs' as const, label: '药品' },
];
const activeTab = ref<typeof tabs[number]['id']>('departments');

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="space-y-6">
    <div class="flex gap-1 bg-gray-100 rounded-lg p-1 w-fit">
      <button v-for="tab in tabs" :key="tab.id" type="button" class="px-4 py-1.5 rounded-md text-sm font-medium transition" :class="activeTab === tab.id ? 'bg-white text-brand shadow-sm' : 'text-text-secondary hover:text-text-main'" @click="activeTab = tab.id">
        {{ tab.label }}
      </button>
    </div>

    <div class="flex gap-3">
      <input v-if="activeTab === 'drugs'" v-model="workspace.drugKeyword" class="input-field flex-1" placeholder="搜索药品..." />
      <button class="btn-primary" type="button" @click="workspace.loadAll()" :disabled="workspace.loading">
        <Search :size="16" /><span>搜索</span>
      </button>
      <button class="btn-secondary" type="button" @click="workspace.createNew(activeTab === 'departments' ? 'department' : activeTab === 'doctors' ? 'doctor' : 'drug')">
        <Plus :size="16" /><span>新增</span>
      </button>
    </div>

    <!-- Departments -->
    <SectionCard v-if="activeTab === 'departments'" title="科室列表">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead><tr class="border-b border-border text-left text-text-secondary">
            <th class="pb-2 font-medium">名称</th><th class="pb-2 font-medium">编码</th><th class="pb-2 font-medium">类型</th><th class="pb-2 font-medium">状态</th><th class="pb-2 font-medium">操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="item in workspace.paginatedDepartments.pagedItems" :key="item.id" class="border-b border-border">
              <td class="py-2.5 font-medium">{{ item.name }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.code }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.type ?? '-' }}</td>
              <td class="py-2.5"><StatusChip :tone="item.status === 'ACTIVE' ? 'success' : 'neutral'">{{ item.status === 'ACTIVE' ? '启用' : '停用' }}</StatusChip></td>
              <td class="py-2.5"><div class="flex gap-1">
                <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.selectDepartment(item)">编辑</button>
                <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.currentKind = 'department'; workspace.currentId = item.id; workspace.requestToggleCurrent()" :disabled="workspace.saving">{{ item.status === 'ACTIVE' ? '停用' : '启用' }}</button>
              </div></td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar
        :page="workspace.paginatedDepartments.page"
        :page-count="workspace.paginatedDepartments.pageCount"
        :total="workspace.paginatedDepartments.total"
        :page-size="workspace.paginatedDepartments.pageSize"
        @update:page="workspace.paginatedDepartments.setPage"
      />
      <EmptyState v-if="!workspace.departments.length && !workspace.loading" icon="search" title="暂无科室数据" />
    </SectionCard>

    <!-- Doctors -->
    <SectionCard v-if="activeTab === 'doctors'" title="医生列表">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead><tr class="border-b border-border text-left text-text-secondary">
            <th class="pb-2 font-medium">姓名</th><th class="pb-2 font-medium">科室</th><th class="pb-2 font-medium">职称</th><th class="pb-2 font-medium">状态</th><th class="pb-2 font-medium">操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="item in workspace.paginatedDoctors.pagedItems" :key="item.id" class="border-b border-border">
              <td class="py-2.5 font-medium">{{ item.name }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.departmentName ?? '-' }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.title ?? '-' }}</td>
              <td class="py-2.5"><StatusChip :tone="item.status === 'ACTIVE' ? 'success' : 'neutral'">{{ item.status === 'ACTIVE' ? '启用' : '停用' }}</StatusChip></td>
              <td class="py-2.5"><div class="flex gap-1">
                <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.selectDoctor(item)">编辑</button>
                <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.currentKind = 'doctor'; workspace.currentId = item.id; workspace.requestToggleCurrent()" :disabled="workspace.saving">{{ item.status === 'ACTIVE' ? '停用' : '启用' }}</button>
              </div></td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar
        :page="workspace.paginatedDoctors.page"
        :page-count="workspace.paginatedDoctors.pageCount"
        :total="workspace.paginatedDoctors.total"
        :page-size="workspace.paginatedDoctors.pageSize"
        @update:page="workspace.paginatedDoctors.setPage"
      />
      <EmptyState v-if="!workspace.doctors.length && !workspace.loading" icon="search" title="暂无医生数据" />
    </SectionCard>

    <!-- Drugs -->
    <SectionCard v-if="activeTab === 'drugs'" title="药品列表">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead><tr class="border-b border-border text-left text-text-secondary">
            <th class="pb-2 font-medium">名称</th><th class="pb-2 font-medium">规格</th><th class="pb-2 font-medium">厂家</th><th class="pb-2 font-medium">状态</th><th class="pb-2 font-medium">操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="item in workspace.paginatedDrugs.pagedItems" :key="item.id" class="border-b border-border">
              <td class="py-2.5 font-medium">{{ item.name }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.specification ?? '-' }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.manufacturer ?? '-' }}</td>
              <td class="py-2.5"><StatusChip :tone="item.status === 'ACTIVE' ? 'success' : 'neutral'">{{ item.status === 'ACTIVE' ? '启用' : '停用' }}</StatusChip></td>
              <td class="py-2.5"><div class="flex gap-1">
                <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.selectDrug(item)">编辑</button>
                <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.currentKind = 'drug'; workspace.currentId = item.id; workspace.requestToggleCurrent()" :disabled="workspace.saving">{{ item.status === 'ACTIVE' ? '停用' : '启用' }}</button>
              </div></td>
            </tr>
          </tbody>
        </table>
      </div>
      <PaginationBar
        :page="workspace.paginatedDrugs.page"
        :page-count="workspace.paginatedDrugs.pageCount"
        :total="workspace.paginatedDrugs.total"
        :page-size="workspace.paginatedDrugs.pageSize"
        @update:page="workspace.paginatedDrugs.setPage"
      />
      <EmptyState v-if="!workspace.drugs.length && !workspace.loading" icon="search" title="暂无药品数据" />
    </SectionCard>
  </div>
</template>
