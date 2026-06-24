<script setup lang="ts">
import { BadgeCheck, CalendarDays, Sparkles } from 'lucide-vue-next';

const { workspace } = defineProps<{ workspace: any }>();
</script>

<template>
  <section class="section workspace-panel">
    <div class="section-head">
      <div>
        <h3 class="section-title">规则 / 配置 / 模板</h3>
        <p class="section-copy">这里先保留本地规则与外部 AI Provider 的配置落点，后续接豆包时直接扩展适配器即可。</p>
      </div>
      <div class="action-row">
        <button class="button-secondary" type="button" @click="workspace.createNew('rule')"><BadgeCheck :size="16" /><span>新规则</span></button>
        <button class="button-secondary" type="button" @click="workspace.createNew('ai')"><Sparkles :size="16" /><span>新配置</span></button>
        <button class="button-secondary" type="button" @click="workspace.createNew('prompt')"><CalendarDays :size="16" /><span>新模板</span></button>
      </div>
    </div>

    <div class="detail-grid three">
      <ul class="mini-list overflow-list">
        <li v-for="rule in workspace.rules" :key="rule.id" class="mini-item" @click="workspace.selectRule(rule)">
          <div class="mini-item-head">
            <div class="mini-item-title">{{ rule.ruleCode }}</div>
            <span class="pill">{{ rule.riskLevel || 'UNKNOWN' }}</span>
          </div>
          <div class="mini-item-meta">
            <span>{{ rule.ruleType }}</span>
            <span>{{ workspace.formatStatus(rule.status) }}</span>
          </div>
        </li>
      </ul>

      <ul class="mini-list overflow-list">
        <li v-for="config in workspace.aiConfigs" :key="config.id" class="mini-item" @click="workspace.selectAi(config)">
          <div class="mini-item-head">
            <div class="mini-item-title">{{ config.provider }} / {{ config.modelName }}</div>
            <span class="pill" :data-tone="config.enabled ? 'healthy' : 'danger'">{{ config.enabled ? '启用' : '停用' }}</span>
          </div>
          <div class="mini-item-meta">
            <span>{{ config.taskScope }}</span>
            <span>{{ config.configVersion }}</span>
          </div>
        </li>
      </ul>

      <ul class="mini-list overflow-list">
        <li v-for="template in workspace.promptTemplates" :key="template.id" class="mini-item" @click="workspace.selectPrompt(template)">
          <div class="mini-item-head">
            <div class="mini-item-title">{{ template.templateCode }}</div>
            <span class="pill">{{ template.taskType }}</span>
          </div>
          <div class="mini-item-meta">
            <span>{{ template.deptCode || '通用' }}</span>
            <span>{{ workspace.formatStatus(template.status) }}</span>
          </div>
        </li>
      </ul>
    </div>
  </section>
</template>
