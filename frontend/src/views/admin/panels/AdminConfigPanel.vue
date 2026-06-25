<script setup lang="ts">
import { ref } from 'vue';
import { Plus } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

const configTabs = [
  { id: 'rules' as const, label: '审方规则' },
  { id: 'ai' as const, label: 'AI配置' },
  { id: 'prompt' as const, label: 'Prompt模板' },
];
const activeConfigTab = ref<typeof configTabs[number]['id']>('rules');

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <div class="space-y-6">
    <div class="flex gap-1 bg-gray-100 rounded-lg p-1 w-fit">
      <button v-for="tab in configTabs" :key="tab.id" type="button" class="px-4 py-1.5 rounded-md text-sm font-medium transition" :class="activeConfigTab === tab.id ? 'bg-white text-brand shadow-sm' : 'text-text-secondary hover:text-text-main'" @click="activeConfigTab = tab.id">
        {{ tab.label }}
      </button>
    </div>

    <div class="flex gap-2 mb-4">
      <button class="btn-secondary" type="button" @click="workspace.createNew(activeConfigTab === 'rules' ? 'rule' : activeConfigTab === 'ai' ? 'ai' : 'prompt')">
        <Plus :size="16" /><span>新增</span>
      </button>
    </div>

    <SectionCard v-if="activeConfigTab === 'rules'" title="审方规则">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead><tr class="border-b border-border text-left text-text-secondary">
            <th class="pb-2 font-medium">编码</th><th class="pb-2 font-medium">类型</th><th class="pb-2 font-medium">风险</th><th class="pb-2 font-medium">状态</th><th class="pb-2 font-medium">操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="item in workspace.rules" :key="item.id" class="border-b border-border">
              <td class="py-2.5 font-medium">{{ item.ruleCode }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.ruleType }}</td>
              <td class="py-2.5"><StatusChip :tone="item.riskLevel === 'HIGH' ? 'danger' : item.riskLevel === 'MEDIUM' ? 'warning' : 'success'">{{ item.riskLevel }}</StatusChip></td>
              <td class="py-2.5"><StatusChip :tone="item.status === 'ACTIVE' ? 'success' : 'neutral'">{{ item.status === 'ACTIVE' ? '启用' : '停用' }}</StatusChip></td>
              <td class="py-2.5"><div class="flex gap-1">
                <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.selectRule(item)">编辑</button>
                <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.currentKind = 'rule'; workspace.currentId = item.id; workspace.toggleCurrent()" :disabled="workspace.saving">切换</button>
              </div></td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState v-if="!workspace.rules.length" icon="search" title="暂无规则" />
    </SectionCard>

    <SectionCard v-if="activeConfigTab === 'ai'" title="AI配置">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead><tr class="border-b border-border text-left text-text-secondary">
            <th class="pb-2 font-medium">提供方</th><th class="pb-2 font-medium">模型</th><th class="pb-2 font-medium">范围</th><th class="pb-2 font-medium">状态</th><th class="pb-2 font-medium">操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="item in workspace.aiConfigs" :key="item.id" class="border-b border-border">
              <td class="py-2.5 font-medium">{{ item.provider }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.modelName }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.taskScope }}</td>
              <td class="py-2.5"><StatusChip :tone="item.status === 'ACTIVE' ? 'success' : 'neutral'">{{ item.status === 'ACTIVE' ? '启用' : '停用' }}</StatusChip></td>
              <td class="py-2.5"><div class="flex gap-1">
                <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.selectAi(item)">编辑</button>
                <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.currentKind = 'ai'; workspace.currentId = item.id; workspace.toggleCurrent()" :disabled="workspace.saving">切换</button>
              </div></td>
            </tr>
          </tbody>
        </table>
      </div>
    </SectionCard>

    <SectionCard v-if="activeConfigTab === 'prompt'" title="Prompt模板">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead><tr class="border-b border-border text-left text-text-secondary">
            <th class="pb-2 font-medium">编码</th><th class="pb-2 font-medium">类型</th><th class="pb-2 font-medium">科室</th><th class="pb-2 font-medium">版本</th><th class="pb-2 font-medium">状态</th><th class="pb-2 font-medium">操作</th>
          </tr></thead>
          <tbody>
            <tr v-for="item in workspace.promptTemplates" :key="item.id" class="border-b border-border">
              <td class="py-2.5 font-medium">{{ item.templateCode }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.taskType }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.deptCode ?? '-' }}</td>
              <td class="py-2.5 text-text-secondary">v{{ item.version }}</td>
              <td class="py-2.5"><StatusChip :tone="item.status === 'ACTIVE' ? 'success' : 'neutral'">{{ item.status === 'ACTIVE' ? '启用' : '停用' }}</StatusChip></td>
              <td class="py-2.5"><div class="flex gap-1">
                <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.selectPrompt(item)">编辑</button>
                <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.currentKind = 'prompt'; workspace.currentId = item.id; workspace.toggleCurrent()" :disabled="workspace.saving">切换</button>
              </div></td>
            </tr>
          </tbody>
        </table>
      </div>
    </SectionCard>
  </div>
</template>
