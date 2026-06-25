<script setup lang="ts">
import { Plus, Trash2, MessageSquare } from 'lucide-vue-next';
import type { ChatSession } from '@/types/chat';

defineProps<{ sessions: ChatSession[]; currentId: number | null }>();

const emit = defineEmits<{
  select: [id: number];
  delete: [id: number];
  new: [];
}>();
</script>

<template>
  <div class="session-list">
    <div class="session-list__header">
      <span class="session-list__title">对话历史</span>
      <button class="session-list__new-btn" @click="emit('new')" title="新建对话">
        <Plus :size="16" />
      </button>
    </div>
    <div class="session-list__items">
      <button v-for="s in sessions" :key="s.id" class="session-item"
              :class="{ active: s.id === currentId }" @click="emit('select', s.id)">
        <MessageSquare :size="14" class="session-item__icon" />
        <span class="session-item__title">{{ s.title || '新对话' }}</span>
        <button class="session-item__delete" @click.stop="emit('delete', s.id)" title="删除">
          <Trash2 :size="12" />
        </button>
      </button>
      <p v-if="!sessions.length" class="session-list__empty">暂无对话</p>
    </div>
  </div>
</template>

<style scoped>
.session-list { width: 220px; border-right: 1px solid var(--border); background: var(--surface-soft); display: flex; flex-direction: column; flex-shrink: 0; }
.session-list__header { display: flex; align-items: center; justify-content: space-between; padding: 12px; border-bottom: 1px solid var(--border); }
.session-list__title { font-size: 13px; font-weight: 600; color: var(--muted); }
.session-list__new-btn { width: 28px; height: 28px; border-radius: 6px; border: none; background: var(--primary); color: #fff; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.session-list__items { flex: 1; overflow-y: auto; padding: 8px; }
.session-item { display: flex; align-items: center; gap: 8px; width: 100%; padding: 8px 10px; border: none; border-radius: 6px; background: transparent; cursor: pointer; font-size: 13px; color: var(--text); text-align: left; }
.session-item:hover { background: var(--surface); }
.session-item.active { background: var(--primary-soft); }
.session-item__icon { flex-shrink: 0; color: var(--muted); }
.session-item__title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-item__delete { opacity: 0; border: none; background: none; cursor: pointer; color: var(--muted); padding: 2px; }
.session-item:hover .session-item__delete { opacity: 1; }
.session-item__delete:hover { color: var(--danger); }
.session-list__empty { text-align: center; color: var(--muted); font-size: 13px; padding: 20px 0; }
</style>
