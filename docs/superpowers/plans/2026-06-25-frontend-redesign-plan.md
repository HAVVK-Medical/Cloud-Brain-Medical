# Frontend Redesign Implementation Plan

> **For agentic workers:** SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild all three role workspaces (patient/doctor/admin) with Tailwind CSS, unified design tokens, phone-frame simulation for patient, and sidebar layouts for doctor/admin — without touching any backend code.

**Architecture:** Vue 3 + TypeScript + Vite + Tailwind CSS v4 + Pinia + Lucide Icons. Backend API, DTOs, auth store, and Axios layer (http.ts, workflow.ts, auth.ts) remain completely unchanged. New shared component library + role-scoped workspace shells + refactored panels under new layouts.

**Tech Stack:** Vue 3.5, Vite 8, TypeScript 6, Tailwind CSS v4, Pinia 3, Lucide Vue Next, ECharts 6

---

## File Structure Map

```
Create:
  frontend/src/styles/tailwind.css              ← replaces base.css
  frontend/src/components/shared/StatusChip.vue
  frontend/src/components/shared/EmptyState.vue
  frontend/src/components/shared/ConfirmDialog.vue
  frontend/src/components/shared/LoadingSkeleton.vue
  frontend/src/components/shared/SectionCard.vue
  frontend/src/components/shared/ConnectionBadge.vue
  frontend/src/components/shared/ToastProvider.vue
  frontend/src/composables/useToast.ts
  frontend/src/components/layout/GlobalTopbar.vue
  frontend/src/components/layout/SideNav.vue
  frontend/src/components/layout/PhoneFrame.vue
  frontend/src/stores/notification.ts
  frontend/src/stores/ai-stream.ts

Modify:
  frontend/package.json                          ← add tailwind + plugin deps
  frontend/vite.config.ts                        ← add tailwind plugin
  frontend/src/main.ts                           ← replace base.css import
  frontend/src/App.vue                           ← new template with GlobalTopbar + RouterView
  frontend/src/router/index.ts                   ← add sub-routes
  frontend/src/views/HomeView.vue                ← redesign entry page
  frontend/src/views/auth/AuthView.vue           ← restyle login form
  frontend/src/views/patient/PatientHomeView.vue ← become PatientWorkspace shell
  frontend/src/views/doctor/DoctorHomeView.vue   ← become DoctorWorkspace shell
  frontend/src/views/admin/AdminHomeView.vue     ← become AdminWorkspace shell
  frontend/src/views/patient/panels/*.vue        ← restyle all 6 panels
  frontend/src/views/doctor/panels/*.vue         ← restyle all 4 panels
  frontend/src/views/admin/panels/*.vue          ← restyle all 5 panels
  frontend/src/components/DashboardCharts.vue    ← restyle

Remove:
  frontend/src/styles/base.css (replaced by tailwind.css)
```

---

## Phase 1: Infrastructure

### Task 1.1: Install Tailwind CSS v4 and Vite plugin

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/vite.config.ts`

- [ ] **Step 1: Add Tailwind CSS v4 dependencies**

Run:
```bash
cd frontend && npm install tailwindcss @tailwindcss/vite
```

- [ ] **Step 2: Configure Vite plugin**

Read [vite.config.ts](frontend/vite.config.ts). Then edit to add the tailwind plugin:

```typescript
import { fileURLToPath, URL } from 'node:url';

import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8088',
        ws: true,
        changeOrigin: true,
      },
    },
  },
});
```

- [ ] **Step 3: Verify build**

Run:
```bash
cd frontend && npm run dev
```

Expected: Vite starts without errors. Open browser to verify no blank screen (tailwind is loaded but no styles applied yet).

- [ ] **Step 4: Commit**

```bash
git add frontend/package.json frontend/vite.config.ts frontend/package-lock.json
git commit -m "chore: add Tailwind CSS v4 with Vite plugin

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 1.2: Create Tailwind CSS file and replace base.css

**Files:**
- Create: `frontend/src/styles/tailwind.css`
- Modify: `frontend/src/main.ts`
- Remove: `frontend/src/styles/base.css` (later, after migration complete)

- [ ] **Step 1: Create tailwind.css with design tokens**

Create `frontend/src/styles/tailwind.css`:

```css
@import 'tailwindcss';

@theme {
  --color-brand: #0D7C73;
  --color-brand-soft: #E6F4F2;
  --color-brand-deep: #095952;
  --color-surface: #F7FAF8;
  --color-card: #FFFFFF;
  --color-text-main: #1B2B2A;
  --color-text-secondary: #6B7D7B;
  --color-border: #DEE7E5;
  --color-danger: #DC4E3E;
  --color-warning: #E8952C;
  --color-success: #3D9142;
  --color-info: #3B82C5;

  --font-sans: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans CJK SC', sans-serif;
  --font-mono: 'SF Mono', 'Consolas', 'Menlo', monospace;
}

@layer base {
  body {
    @apply bg-surface text-text-main font-sans antialiased;
    font-size: 14px;
  }

  /* Phone frame prevents iOS zoom */
  .phone-input {
    font-size: 16px;
  }
}

@layer components {
  .btn-primary {
    @apply inline-flex items-center justify-center gap-1.5 rounded-md bg-brand px-4 py-2 text-sm font-medium text-white transition hover:bg-brand-deep disabled:opacity-50 disabled:cursor-not-allowed;
  }

  .btn-secondary {
    @apply inline-flex items-center justify-center gap-1.5 rounded-md border border-border bg-white px-4 py-2 text-sm font-medium text-text-main transition hover:bg-brand-soft disabled:opacity-50 disabled:cursor-not-allowed;
  }

  .btn-danger {
    @apply inline-flex items-center justify-center gap-1.5 rounded-md bg-danger px-4 py-2 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed;
  }

  .btn-ghost {
    @apply inline-flex items-center justify-center gap-1.5 rounded-md px-3 py-2 text-sm text-text-secondary transition hover:bg-brand-soft hover:text-text-main disabled:opacity-50 disabled:cursor-not-allowed;
  }

  .card {
    @apply rounded-lg border border-border bg-card p-5;
  }

  .input-field {
    @apply w-full rounded-md border border-border bg-white px-3 py-2 text-sm placeholder:text-text-secondary focus:border-brand focus:outline-none focus:ring-1 focus:ring-brand;
  }

  .label-text {
    @apply block text-sm font-medium text-text-main mb-1;
  }
}
```

- [ ] **Step 2: Update main.ts to import tailwind.css**

Read [main.ts](frontend/src/main.ts). Edit the import:

```typescript
import { createApp } from 'vue';

import App from './App.vue';
import router from './router';
import { useAuthStore } from './stores/auth';
import { pinia } from './stores/pinia';
import './styles/tailwind.css';

const app = createApp(App);
app.use(pinia);
app.use(router);
useAuthStore(pinia).hydrateFromStorage();
app.mount('#app');
```

- [ ] **Step 3: Verify app loads**

Run:
```bash
cd frontend && npm run dev
```

Expected: App loads with basic Tailwind reset (fonts, colors applied). Some existing components will look broken because they still use old CSS classes — this is expected.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/styles/tailwind.css frontend/src/main.ts
git commit -m "feat: add Tailwind CSS design tokens and global styles

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 1.3: Create StatusChip shared component

**Files:**
- Create: `frontend/src/components/shared/StatusChip.vue`

- [ ] **Step 1: Write StatusChip component**

Create `frontend/src/components/shared/StatusChip.vue`:

```vue
<script setup lang="ts">
export type StatusTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

const props = withDefaults(defineProps<{
  tone?: StatusTone;
  dot?: boolean;
}>(), {
  tone: 'neutral',
  dot: true,
});

const toneClasses: Record<StatusTone, string> = {
  success: 'bg-green-50 text-success',
  warning: 'bg-amber-50 text-warning',
  danger: 'bg-red-50 text-danger',
  info: 'bg-blue-50 text-info',
  neutral: 'bg-gray-100 text-text-secondary',
};

const dotColor: Record<StatusTone, string> = {
  success: 'bg-success',
  warning: 'bg-warning',
  danger: 'bg-danger',
  info: 'bg-info',
  neutral: 'bg-gray-400',
};
</script>

<template>
  <span class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium" :class="toneClasses[tone]">
    <span v-if="dot" class="inline-block h-1.5 w-1.5 rounded-full" :class="dotColor[tone]" />
    <slot />
  </span>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/shared/StatusChip.vue
git commit -m "feat: add StatusChip shared component

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 1.4: Create EmptyState shared component

**Files:**
- Create: `frontend/src/components/shared/EmptyState.vue`

- [ ] **Step 1: Write EmptyState component**

Create `frontend/src/components/shared/EmptyState.vue`:

```vue
<script setup lang="ts">
import { computed } from 'vue';
import { CalendarDays, FileText, Inbox, SearchX } from 'lucide-vue-next';

const props = withDefaults(defineProps<{
  icon?: 'inbox' | 'calendar' | 'file' | 'search';
  title: string;
  description?: string;
  actionLabel?: string;
}>(), {
  icon: 'inbox',
});

const emit = defineEmits<{
  action: [];
}>();

const iconComponent = computed(() => {
  switch (props.icon) {
    case 'calendar': return CalendarDays;
    case 'file': return FileText;
    case 'search': return SearchX;
    default: return Inbox;
  }
});
</script>

<template>
  <div class="flex flex-col items-center justify-center py-16 text-center">
    <component :is="iconComponent" :size="40" class="text-text-secondary/40 mb-4" />
    <h3 class="text-base font-semibold text-text-main mb-1">{{ title }}</h3>
    <p v-if="description" class="text-sm text-text-secondary max-w-sm mb-4">{{ description }}</p>
    <button v-if="actionLabel" class="btn-primary" type="button" @click="emit('action')">
      {{ actionLabel }}
    </button>
  </div>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/shared/EmptyState.vue
git commit -m "feat: add EmptyState shared component

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 1.5: Create ConfirmDialog shared component

**Files:**
- Create: `frontend/src/components/shared/ConfirmDialog.vue`

- [ ] **Step 1: Write ConfirmDialog component**

Create `frontend/src/components/shared/ConfirmDialog.vue`:

```vue
<script setup lang="ts">
import { AlertTriangle, Info, X } from 'lucide-vue-next';

export type DialogLevel = 'info' | 'warning' | 'danger';

const props = withDefaults(defineProps<{
  open: boolean;
  title: string;
  message: string;
  level?: DialogLevel;
  confirmLabel?: string;
  cancelLabel?: string;
  loading?: boolean;
}>(), {
  level: 'info',
  confirmLabel: '确认',
  cancelLabel: '取消',
  loading: false,
});

const emit = defineEmits<{
  confirm: [];
  cancel: [];
}>();

const iconColor: Record<DialogLevel, string> = {
  info: 'text-info',
  warning: 'text-warning',
  danger: 'text-danger',
};

const confirmBtnClass: Record<DialogLevel, string> = {
  info: 'btn-primary',
  warning: 'bg-warning text-white rounded-md px-4 py-2 text-sm font-medium hover:opacity-90 disabled:opacity-50',
  danger: 'btn-danger',
};
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center">
      <div class="absolute inset-0 bg-black/40" @click="!loading && emit('cancel')" />
      <div class="relative bg-white rounded-xl shadow-xl max-w-sm w-full mx-4 p-6">
        <div class="flex items-start gap-3 mb-4">
          <AlertTriangle v-if="level === 'danger' || level === 'warning'" :size="20" :class="iconColor[level]" class="shrink-0 mt-0.5" />
          <Info v-else :size="20" :class="iconColor[level]" class="shrink-0 mt-0.5" />
          <div class="flex-1">
            <h3 class="text-base font-semibold text-text-main">{{ title }}</h3>
            <p class="text-sm text-text-secondary mt-1">{{ message }}</p>
          </div>
          <button type="button" class="btn-ghost !p-1" @click="!loading && emit('cancel')" :disabled="loading">
            <X :size="16" />
          </button>
        </div>
        <div class="flex justify-end gap-2">
          <button type="button" class="btn-secondary" @click="emit('cancel')" :disabled="loading">
            {{ cancelLabel }}
          </button>
          <button type="button" :class="confirmBtnClass[level]" @click="emit('confirm')" :disabled="loading">
            {{ loading ? '处理中...' : confirmLabel }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/shared/ConfirmDialog.vue
git commit -m "feat: add ConfirmDialog shared component

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 1.6: Create LoadingSkeleton shared component

**Files:**
- Create: `frontend/src/components/shared/LoadingSkeleton.vue`

- [ ] **Step 1: Write LoadingSkeleton component**

Create `frontend/src/components/shared/LoadingSkeleton.vue`:

```vue
<script setup lang="ts">
withDefaults(defineProps<{
  rows?: number;
  type?: 'card' | 'table' | 'text';
}>(), {
  rows: 4,
  type: 'card',
});
</script>

<template>
  <div class="animate-pulse">
    <template v-if="type === 'card'">
      <div v-for="i in rows" :key="i" class="card mb-3">
        <div class="h-4 bg-gray-200 rounded w-2/3 mb-2" />
        <div class="h-3 bg-gray-100 rounded w-full mb-1" />
        <div class="h-3 bg-gray-100 rounded w-5/6" />
      </div>
    </template>
    <template v-else-if="type === 'table'">
      <div class="h-8 bg-gray-200 rounded w-full mb-2" />
      <div v-for="i in rows" :key="i" class="h-10 bg-gray-100 rounded w-full mb-1" />
    </template>
    <template v-else>
      <div v-for="i in rows" :key="i" class="flex gap-3 mb-3">
        <div class="h-3 bg-gray-200 rounded w-1/4" />
        <div class="h-3 bg-gray-100 rounded w-3/4" />
      </div>
    </template>
  </div>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/shared/LoadingSkeleton.vue
git commit -m "feat: add LoadingSkeleton shared component

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 1.7: Create SectionCard shared component

**Files:**
- Create: `frontend/src/components/shared/SectionCard.vue`

- [ ] **Step 1: Write SectionCard component**

Create `frontend/src/components/shared/SectionCard.vue`:

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { ChevronDown } from 'lucide-vue-next';

withDefaults(defineProps<{
  title?: string;
  collapsible?: boolean;
}>(), {
  collapsible: false,
});

const collapsed = ref(false);
</script>

<template>
  <div class="card">
    <div v-if="title" class="flex items-center justify-between mb-4">
      <h3 class="text-base font-semibold text-text-main">{{ title }}</h3>
      <button v-if="collapsible" type="button" class="btn-ghost !p-1" @click="collapsed = !collapsed">
        <ChevronDown :size="16" class="transition-transform" :class="{ 'rotate-180': !collapsed }" />
      </button>
    </div>
    <div v-show="!collapsed || !collapsible">
      <slot />
    </div>
  </div>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/shared/SectionCard.vue
git commit -m "feat: add SectionCard shared component

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 1.8: Create Toast system (composable + provider)

**Files:**
- Create: `frontend/src/composables/useToast.ts`
- Create: `frontend/src/components/shared/ToastProvider.vue`

- [ ] **Step 1: Write useToast composable**

Create `frontend/src/composables/useToast.ts`:

```typescript
import { ref } from 'vue';

export interface ToastMessage {
  id: number;
  text: string;
  tone: 'success' | 'error' | 'warning';
}

const toasts = ref<ToastMessage[]>([]);
let nextId = 1;

export function useToast() {
  function addToast(text: string, tone: ToastMessage['tone'] = 'success') {
    const id = nextId++;
    toasts.value.push({ id, text, tone });
    setTimeout(() => {
      toasts.value = toasts.value.filter((t) => t.id !== id);
    }, 3000);
  }

  return {
    toasts,
    success: (text: string) => addToast(text, 'success'),
    error: (text: string) => addToast(text, 'error'),
    warning: (text: string) => addToast(text, 'warning'),
  };
}
```

- [ ] **Step 2: Write ToastProvider component**

Create `frontend/src/components/shared/ToastProvider.vue`:

```vue
<script setup lang="ts">
import { CheckCircle2, XCircle, AlertTriangle, X } from 'lucide-vue-next';
import { useToast } from '@/composables/useToast';

const { toasts } = useToast();

const iconMap = {
  success: CheckCircle2,
  error: XCircle,
  warning: AlertTriangle,
};

const bgMap = {
  success: 'bg-success text-white',
  error: 'bg-danger text-white',
  warning: 'bg-warning text-white',
};
</script>

<template>
  <div class="fixed top-4 right-4 z-[100] flex flex-col gap-2">
    <TransitionGroup name="toast">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        :class="bgMap[toast.tone]"
        class="flex items-center gap-2 rounded-lg px-4 py-2.5 shadow-lg text-sm font-medium"
      >
        <component :is="iconMap[toast.tone]" :size="16" />
        <span>{{ toast.text }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-enter-active { transition: all 0.25s ease-out; }
.toast-leave-active { transition: all 0.2s ease-in; }
.toast-enter-from { opacity: 0; transform: translateX(50px); }
.toast-leave-to { opacity: 0; transform: translateX(50px); }
</style>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/composables/useToast.ts frontend/src/components/shared/ToastProvider.vue
git commit -m "feat: add Toast notification system

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 1.9: Create ConnectionBadge shared component

**Files:**
- Create: `frontend/src/components/shared/ConnectionBadge.vue`

- [ ] **Step 1: Write ConnectionBadge component**

Create `frontend/src/components/shared/ConnectionBadge.vue`:

```vue
<script setup lang="ts">
import { Wifi, WifiOff } from 'lucide-vue-next';

withDefaults(defineProps<{
  connected: boolean;
  label?: string;
}>(), {
  label: '实时连接',
});
</script>

<template>
  <span class="inline-flex items-center gap-1 text-xs" :class="connected ? 'text-success' : 'text-text-secondary'">
    <Wifi v-if="connected" :size="14" />
    <WifiOff v-else :size="14" />
    <span v-if="label">{{ connected ? label : `${label}已断开` }}</span>
  </span>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/shared/ConnectionBadge.vue
git commit -m "feat: add ConnectionBadge shared component

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Phase 2: Shell Layer

### Task 2.1: Create GlobalTopbar

**Files:**
- Create: `frontend/src/components/layout/GlobalTopbar.vue`

- [ ] **Step 1: Write GlobalTopbar component**

Create `frontend/src/components/layout/GlobalTopbar.vue`:

```vue
<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import { Activity, Bell, LogOut, RefreshCw, Wifi, WifiOff } from 'lucide-vue-next';

import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';
import { getRoleLabel } from '@/utils/zh';
import ConnectionBadge from '@/components/shared/ConnectionBadge.vue';

const appStore = useAppStore();
const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

const roleLabel = computed(() => authStore.isAuthenticated ? getRoleLabel(authStore.role) : '访客');
const healthOk = computed(() => appStore.health?.status === 'UP');

defineEmits<{
  refresh: [];
}>();
</script>

<template>
  <header class="h-12 bg-brand text-white flex items-center px-4 gap-4 shrink-0 z-50">
    <RouterLink to="/" class="flex items-center gap-2 font-semibold text-sm hover:opacity-80 transition">
      <Activity :size="18" />
      <span class="hidden sm:inline">智慧云脑诊疗平台</span>
    </RouterLink>

    <div class="flex-1" />

    <span class="text-xs bg-white/15 rounded-full px-2.5 py-0.5 hidden sm:block">
      {{ roleLabel }} · {{ authStore.sessionLabel }}
    </span>

    <span class="inline-flex items-center gap-1 text-xs" :class="healthOk ? 'text-green-200' : 'text-amber-200'" :title="healthOk ? '后端正常' : '后端异常'">
      <span class="h-1.5 w-1.5 rounded-full inline-block" :class="healthOk ? 'bg-green-300' : 'bg-amber-300'" />
      后端{{ healthOk ? '正常' : '异常' }}
    </span>

    <button type="button" class="p-1 rounded hover:bg-white/10 transition" @click="$emit('refresh')" title="刷新健康状态">
      <RefreshCw :size="16" :class="{ 'animate-spin': appStore.loading }" />
    </button>

    <button v-if="authStore.isAuthenticated" type="button" class="p-1 rounded hover:bg-white/10 transition" title="退出登录" @click="async () => { await authStore.logout(); await router.push('/login'); }">
      <LogOut :size="16" />
    </button>
  </header>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/layout/GlobalTopbar.vue
git commit -m "feat: add GlobalTopbar layout component

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2.2: Create SideNav component

**Files:**
- Create: `frontend/src/components/layout/SideNav.vue`

- [ ] **Step 1: Write SideNav component**

Create `frontend/src/components/layout/SideNav.vue`:

```vue
<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { Component } from 'vue';
import type { LucideIcon } from 'lucide-vue-next';

export interface NavItem {
  id: string;
  label: string;
  path: string;
  icon: Component;
}

const props = defineProps<{
  items: NavItem[];
  title?: string;
  subtitle?: string;
}>();

const route = useRoute();
const router = useRouter();

const activeId = computed(() => {
  const current = props.items.find((item) => route.path.startsWith(item.path));
  return current?.id ?? props.items[0]?.id;
});

function navigate(item: NavItem) {
  router.push(item.path);
}
</script>

<template>
  <nav class="w-52 shrink-0 bg-white border-r border-border flex flex-col min-h-0">
    <div v-if="title" class="px-5 py-4 border-b border-border">
      <p class="text-sm font-semibold text-text-main">{{ title }}</p>
      <p v-if="subtitle" class="text-xs text-text-secondary mt-0.5">{{ subtitle }}</p>
    </div>
    <div class="flex-1 py-2 overflow-y-auto">
      <button
        v-for="item in items"
        :key="item.id"
        type="button"
        class="w-full flex items-center gap-2.5 px-5 py-2.5 text-sm transition text-left border-l-2 -ml-px"
        :class="activeId === item.id
          ? 'text-brand bg-brand-soft border-brand font-medium'
          : 'text-text-secondary border-transparent hover:bg-gray-50'"
        @click="navigate(item)"
      >
        <component :is="item.icon" :size="16" />
        <span>{{ item.label }}</span>
      </button>
    </div>
  </nav>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/layout/SideNav.vue
git commit -m "feat: add SideNav layout component

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2.3: Create PhoneFrame component

**Files:**
- Create: `frontend/src/components/layout/PhoneFrame.vue`

- [ ] **Step 1: Write PhoneFrame component**

Create `frontend/src/components/layout/PhoneFrame.vue`:

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { Battery, Signal, Wifi } from 'lucide-vue-next';

const time = ref(new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }));
setInterval(() => {
  time.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false });
}, 60000);
</script>

<template>
  <div class="flex items-center justify-center py-8">
    <div class="w-[375px] rounded-2xl border-[6px] border-gray-800 bg-white shadow-2xl overflow-hidden">
      <!-- Status bar -->
      <div class="flex items-center justify-between px-5 py-2 bg-white text-xs font-medium text-gray-900">
        <span>{{ time }}</span>
        <span class="flex items-center gap-1">
          <Signal :size="12" />
          <Wifi :size="12" />
          <Battery :size="12" />
        </span>
      </div>
      <!-- Content area -->
      <div class="h-[600px] overflow-y-auto bg-surface">
        <slot />
      </div>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/components/layout/PhoneFrame.vue
git commit -m "feat: add PhoneFrame layout component

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2.4: Add notification store

**Files:**
- Create: `frontend/src/stores/notification.ts`

- [ ] **Step 1: Write notification store**

Create `frontend/src/stores/notification.ts`:

```typescript
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { listUnreadNotifications, markNotificationRead } from '@/api/workflow';
import type { NotificationRecordSummary } from '@/api/workflow';
import { useAuthStore } from '@/stores/auth';
import { useToast } from '@/composables/useToast';

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref<NotificationRecordSummary[]>([]);
  const loading = ref(false);
  const socketState = ref<'idle' | 'connecting' | 'connected' | 'closed'>('idle');
  let socket: WebSocket | null = null;

  const unreadCount = computed(() => notifications.value.filter((n) => n.read !== true).length);
  const connected = computed(() => socketState.value === 'connected');

  function buildWsUrl(path: string, token: string) {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}${path}?token=${encodeURIComponent(token)}`;
  }

  async function load() {
    loading.value = true;
    try {
      notifications.value = await listUnreadNotifications();
    } finally {
      loading.value = false;
    }
  }

  async function ack(id: number) {
    await markNotificationRead(id);
    notifications.value = notifications.value.filter((n) => n.id !== id);
  }

  function connect(token: string) {
    if (socket) return;
    socketState.value = 'connecting';
    socket = new WebSocket(buildWsUrl('/ws/notifications', token));
    socket.onopen = () => { socketState.value = 'connected'; };
    socket.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data) as { type?: string; payload?: NotificationRecordSummary };
        if (msg.type === 'notification' && msg.payload) {
          notifications.value = [msg.payload, ...notifications.value.filter((n) => n.id !== msg.payload!.id)].slice(0, 20);
        }
      } catch { /* ignore */ }
    };
    socket.onclose = () => { socket = null; socketState.value = 'closed'; };
    socket.onerror = () => { socketState.value = 'closed'; };
  }

  function disconnect() {
    socket?.close();
    socket = null;
    socketState.value = 'idle';
  }

  return { notifications, loading, socketState, unreadCount, connected, load, ack, connect, disconnect };
});
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/stores/notification.ts
git commit -m "feat: add notification Pinia store

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2.5: Add AI stream store

**Files:**
- Create: `frontend/src/stores/ai-stream.ts`

- [ ] **Step 1: Write AI stream store**

Create `frontend/src/stores/ai-stream.ts`:

```typescript
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { createAiStreamSession, cancelAiStreamSession } from '@/api/workflow';
import type { MedicalRecordSummary, DiagnosisSuggestionResponse } from '@/api/workflow';

export const useAiStreamStore = defineStore('ai-stream', () => {
  const sessionId = ref<string | null>(null);
  const streamText = ref('');
  const streaming = ref(false);
  const connected = computed(() => sessionId.value !== null && streaming.value);

  function parseSsePayload<T>(value: string): T | null {
    try { return JSON.parse(value) as T; } catch { return null; }
  }

  async function start(
    taskType: 'MEDICAL_RECORD' | 'DIAGNOSIS',
    registrationId: number,
    conversationText: string,
    diagnosisDirection: string | null,
    onResult: (data: MedicalRecordSummary | DiagnosisSuggestionResponse) => void,
  ) {
    streamText.value = '';
    streaming.value = true;
    const session = await createAiStreamSession({ taskType, registrationId, conversationText, diagnosisDirection });
    sessionId.value = session.sessionId;

    return new Promise<void>((resolve, reject) => {
      let completed = false;
      const source = new EventSource(`/api/ai-stream-sessions/${session.sessionId}/events?token=${encodeURIComponent(session.streamToken)}`);

      const finish = () => {
        completed = true;
        sessionId.value = null;
        streaming.value = false;
        source.close();
        resolve();
      };

      source.addEventListener('chunk', (event) => {
        const payload = parseSsePayload<{ text?: string }>(event.data);
        streamText.value += payload?.text ?? event.data;
      });
      source.addEventListener('result', (event) => {
        const payload = parseSsePayload<any>(event.data);
        if (payload) onResult(payload);
      });
      source.addEventListener('done', finish);
      source.addEventListener('cancelled', finish);
      source.onerror = () => {
        if (!completed) {
          sessionId.value = null;
          streaming.value = false;
          source.close();
          reject(new Error('stream failed'));
        }
      };
    });
  }

  async function cancel() {
    if (sessionId.value) {
      try { await cancelAiStreamSession(sessionId.value); } catch { /* already ended */ }
    }
    sessionId.value = null;
    streaming.value = false;
    streamText.value = '';
  }

  return { sessionId, streamText, streaming, connected, start, cancel };
});
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/stores/ai-stream.ts
git commit -m "feat: add AI stream Pinia store

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2.6: Update App.vue with new layout

**Files:**
- Modify: `frontend/src/App.vue`

- [ ] **Step 1: Rewrite App.vue**

Read [App.vue](frontend/src/App.vue). Replace the entire file content with:

```vue
<script setup lang="ts">
import { onMounted } from 'vue';
import { RouterView } from 'vue-router';
import { useAppStore } from '@/stores/app';
import GlobalTopbar from '@/components/layout/GlobalTopbar.vue';
import ToastProvider from '@/components/shared/ToastProvider.vue';

const appStore = useAppStore();

onMounted(() => {
  void appStore.refreshHealth();
});
</script>

<template>
  <div class="flex flex-col h-screen">
    <GlobalTopbar @refresh="appStore.refreshHealth()" />
    <div class="flex-1 min-h-0">
      <RouterView />
    </div>
    <ToastProvider />
  </div>
</template>
```

- [ ] **Step 2: Verify app renders**

Run:
```bash
cd frontend && npm run dev
```

Expected: App loads showing GlobalTopbar and RouterView content. Toast container is in place (invisible until toasts fire). Some old CSS styling will be broken — this is expected and will be fixed panel by panel.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/App.vue
git commit -m "refactor: replace App.vue with GlobalTopbar + RouterView layout

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2.7: Update router with sub-routes

**Files:**
- Modify: `frontend/src/router/index.ts`

- [ ] **Step 1: Add sub-routes**

Read [router/index.ts](frontend/src/router/index.ts). Replace the route definitions with sub-routes:

```typescript
import { createRouter, createWebHistory } from 'vue-router';

import { pinia } from '@/stores/pinia';
import { useAuthStore } from '@/stores/auth';
import AuthView from '@/views/auth/AuthView.vue';
import HealthView from '@/views/HealthView.vue';
import HomeView from '@/views/HomeView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/health',
      name: 'health',
      component: HealthView,
    },
    {
      path: '/login',
      name: 'login',
      component: AuthView,
    },
    {
      path: '/patient',
      component: () => import('@/views/patient/PatientHomeView.vue'),
      meta: { requiresAuth: true, role: 'patient' },
      redirect: '/patient/overview',
      children: [
        { path: 'overview', name: 'patient-overview', component: () => import('@/views/patient/panels/PatientOverviewPanel.vue') },
        { path: 'triage', name: 'patient-triage', component: () => import('@/views/patient/panels/PatientTriagePanel.vue') },
        { path: 'registration', name: 'patient-registration', component: () => import('@/views/patient/panels/PatientRegistrationPanel.vue') },
        { path: 'records', name: 'patient-records', component: () => import('@/views/patient/panels/PatientRecordsPanel.vue') },
        { path: 'profile', name: 'patient-profile', component: () => import('@/views/patient/panels/PatientProfilePanel.vue') },
        { path: 'history', name: 'patient-history', component: () => import('@/views/patient/panels/PatientHistoryPanel.vue') },
      ],
    },
    {
      path: '/doctor',
      component: () => import('@/views/doctor/DoctorHomeView.vue'),
      meta: { requiresAuth: true, role: 'doctor' },
      redirect: '/doctor/overview',
      children: [
        { path: 'overview', name: 'doctor-overview', component: () => import('@/views/doctor/panels/DoctorOverviewPanel.vue') },
        { path: 'consultation', name: 'doctor-consultation', component: () => import('@/views/doctor/panels/DoctorConsultationPanel.vue') },
        { path: 'consultation/:id', name: 'doctor-consultation-patient', component: () => import('@/views/doctor/panels/DoctorConsultationPanel.vue') },
        { path: 'history', name: 'doctor-history', component: () => import('@/views/doctor/panels/DoctorHistoryPanel.vue') },
        { path: 'schedule', name: 'doctor-schedule', component: () => import('@/views/doctor/panels/DoctorSchedulePanel.vue') },
      ],
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminHomeView.vue'),
      meta: { requiresAuth: true, role: 'admin' },
      redirect: '/admin/overview',
      children: [
        { path: 'overview', name: 'admin-overview', component: () => import('@/views/admin/panels/AdminOverviewPanel.vue') },
        { path: 'master-data', name: 'admin-master-data', component: () => import('@/views/admin/panels/AdminMasterPanel.vue') },
        { path: 'resources', name: 'admin-resources', component: () => import('@/views/admin/panels/AdminResourcesPanel.vue') },
        { path: 'config', name: 'admin-config', component: () => import('@/views/admin/panels/AdminConfigPanel.vue') },
        { path: 'audit', name: 'admin-audit', component: () => import('@/views/admin/panels/AdminAuditPanel.vue') },
      ],
    },
  ],
});

// Navigation guard — keep EXACTLY as-is (no changes)
router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia);

  if (!authStore.hydrated) {
    authStore.hydrateFromStorage();
  }

  if (authStore.isAuthenticated && authStore.isExpired) {
    if (!authStore.refreshToken) {
      return {
        path: '/login',
        query: { redirect: to.fullPath, role: to.meta.role },
      };
    }

    try {
      await authStore.refreshSession();
    } catch {
      return {
        path: '/login',
        query: { redirect: to.fullPath, role: to.meta.role },
      };
    }
  }

  if (!to.meta.requiresAuth) {
    return true;
  }

  if (!authStore.isAuthenticated || authStore.isExpired) {
    return {
      path: '/login',
      query: { redirect: to.fullPath, role: to.meta.role },
    };
  }

  if (to.meta.role && authStore.role !== to.meta.role) {
    return {
      path: '/login',
      query: { redirect: to.fullPath, role: to.meta.role },
    };
  }

  return true;
});

export default router;
```

- [ ] **Step 2: Verify routes work**

Run:
```bash
cd frontend && npm run dev
```

Navigate to `/patient` — should redirect to `/patient/overview`. Same for `/doctor` → `/doctor/overview` and `/admin` → `/admin/overview`.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/router/index.ts
git commit -m "refactor: add sub-routes for patient/doctor/admin workspaces

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2.8: Rewrite PatientHomeView as workspace shell with PhoneFrame

**Files:**
- Modify: `frontend/src/views/patient/PatientHomeView.vue`

- [ ] **Step 1: Rewrite PatientHomeView**

Read [PatientHomeView.vue](frontend/src/views/patient/PatientHomeView.vue). Replace the file entirely:

```vue
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { CalendarDays, FileText, ScanSearch, Ticket, UserRound } from 'lucide-vue-next';

import {
  cancelRegistration,
  createFeedback,
  createRegistration,
  getPatientInfo,
  listDepartments,
  listDoctors,
  listPatientFeedback,
  listPatientMedicalRecords,
  listPatientPrescriptions,
  listRegistrations,
  listSchedules,
  listTriageHistory,
  triageConsult,
  updatePatientInfo,
} from '@/api/workflow';
import type {
  DepartmentOption,
  DoctorOption,
  FeedbackResponse,
  MedicalRecordSummary,
  PatientProfile,
  PrescriptionSummary,
  RegistrationSummary,
  ScheduleOption,
  TriageResponse,
} from '@/api/workflow';
import { useAuthStore } from '@/stores/auth';
import { resolveUiErrorMessage } from '@/utils/zh';
import PhoneFrame from '@/components/layout/PhoneFrame.vue';
import LoadingSkeleton from '@/components/shared/LoadingSkeleton.vue';

const authStore = useAuthStore();
const route = useRoute();
const router = useRouter();

// ... same state declarations as original PatientHomeView (lines 41-105):
const loading = ref(false);
const triaging = ref(false);
const registering = ref(false);
const savingProfile = ref(false);
const submittingFeedback = ref(false);
const canceling = ref(false);
const error = ref('');

const patient = ref<PatientProfile | null>(null);
const departments = ref<DepartmentOption[]>([]);
const doctors = ref<DoctorOption[]>([]);
const schedules = ref<ScheduleOption[]>([]);
const triageHistory = ref<TriageResponse[]>([]);
const registrations = ref<RegistrationSummary[]>([]);
const medicalRecords = ref<MedicalRecordSummary[]>([]);
const prescriptions = ref<PrescriptionSummary[]>([]);
const feedbacks = ref<FeedbackResponse[]>([]);
const triageResult = ref<TriageResponse | null>(null);

const selectedDepartmentId = ref<number | null>(null);
const selectedDoctorId = ref<number | null>(null);
const selectedScheduleId = ref<number | null>(null);

const triageForm = reactive({ chiefComplaint: '' });
const profileForm = reactive({
  realName: '',
  gender: '',
  age: '',
  phone: '',
  idCardNumber: '',
  medicalHistory: '',
  remark: '',
});
const feedbackForm = reactive({
  registrationId: null as number | null,
  rating: 5,
  triageAccurate: true,
  comment: '',
});
const cancelReasons = reactive<Record<number, string>>({});

// ... same computed and methods as original (lines 84-414):
const selectedDepartment = computed(() => departments.value.find((item) => item.id === selectedDepartmentId.value) ?? null);
const selectedDoctor = computed(() => doctors.value.find((item) => item.id === selectedDoctorId.value) ?? null);
const visibleSchedules = computed(() =>
  selectedDoctorId.value ? schedules.value.filter((item) => item.doctorId === selectedDoctorId.value) : schedules.value,
);
const selectedSchedule = computed(() => visibleSchedules.value.find((item) => item.id === selectedScheduleId.value) ?? null);
const waitingRegistrations = computed(() => registrations.value.filter((item) => item.status === 'WAITING'));
const completedRegistrations = computed(() => registrations.value.filter((item) => item.status === 'COMPLETED'));
const latestRegistration = computed(() => registrations.value[0] ?? null);
const latestTriage = computed(() => triageHistory.value[0] ?? triageResult.value);
const displayName = computed(() => patient.value?.realName || patient.value?.username || authStore.sessionLabel);

// Tab bar items
const tabs = [
  { id: 'overview', label: '概览', icon: CalendarDays, path: '/patient/overview' },
  { id: 'triage', label: '分诊', icon: ScanSearch, path: '/patient/triage' },
  { id: 'registration', label: '挂号', icon: Ticket, path: '/patient/registration' },
  { id: 'records', label: '病历', icon: FileText, path: '/patient/records' },
  { id: 'profile', label: '我的', icon: UserRound, path: '/patient/profile' },
] as const;

const activeTab = computed(() => {
  const matching = tabs.find((t) => route.path.startsWith(t.path));
  return matching?.id ?? 'overview';
});

function goTab(tab: typeof tabs[number]) {
  router.push(tab.path);
}

// ... all the same async methods (loadCatalog, loadPatientData, refreshAll, chooseDepartment, etc.)
// Copy them EXACTLY from the original PatientHomeView.vue lines 167-414

// [INSERT ALL METHODS FROM ORIGINAL lines 167-414 HERE — they remain unchanged]

const workspace = reactive({
  loading, triaging, registering, savingProfile, submittingFeedback, canceling, error,
  patient, departments, doctors, schedules, triageHistory, registrations,
  medicalRecords, prescriptions, feedbacks, triageResult,
  selectedDepartmentId, selectedDoctorId, selectedScheduleId,
  triageForm, profileForm, feedbackForm, cancelReasons,
  selectedDepartment, selectedDoctor, visibleSchedules, selectedSchedule,
  waitingRegistrations, completedRegistrations, latestRegistration, latestTriage,
  displayName,
  chooseDepartment, chooseDoctor, chooseSchedule,
  runTriage, submitRegistration, saveProfile, cancelWaitingRegistration, submitFeedback,
  formatDateTime, formatDate, truncate,
});

onMounted(() => {
  void refreshAll();
});
</script>

<template>
  <PhoneFrame>
    <!-- Phone Tab bar at bottom -->
    <div class="flex flex-col h-full">
      <!-- Main content area -->
      <div class="flex-1 overflow-y-auto">
        <p v-if="error" class="mx-4 mt-3 p-2.5 rounded-md bg-red-50 text-danger text-xs">{{ error }}</p>
        <RouterView v-slot="{ Component: PanelComp }">
          <component :is="PanelComp" :workspace="workspace" v-if="PanelComp" />
        </RouterView>
        <!-- Fallback content when no panel loaded -->
        <LoadingSkeleton v-if="loading" :rows="3" class="p-4" />
      </div>

      <!-- Bottom tab bar -->
      <div class="shrink-0 bg-white border-t border-border flex">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="flex-1 flex flex-col items-center gap-0.5 py-2 text-xs transition"
          :class="activeTab === tab.id ? 'text-brand' : 'text-text-secondary'"
          @click="goTab(tab)"
        >
          <component :is="tab.icon" :size="18" />
          <span>{{ tab.label }}</span>
        </button>
      </div>
    </div>
  </PhoneFrame>
</template>
```

Note: Copy ALL the business logic methods from the original `PatientHomeView.vue` (lines 124-414 — `formatDateTime`, `formatDate`, `truncate`, `syncScheduleSelection`, `syncDoctorSelection`, `applyTriageSelection`, `loadCatalog`, `loadPatientData`, `refreshAll`, `chooseDepartment`, `chooseDoctor`, `chooseSchedule`, `runTriage`, `submitRegistration`, `saveProfile`, `cancelWaitingRegistration`, `submitFeedback`) into the `<script setup>` block. These methods are the business logic layer and must remain exactly the same.

- [ ] **Step 2: Verify patient workspace shows phone frame**

Run:
```bash
cd frontend && npm run dev
```

Navigate to `/patient/overview`. Expected: Phone frame is visible, tab bar at bottom works to switch between sub-routes.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/patient/PatientHomeView.vue
git commit -m "refactor: convert PatientHomeView to PhoneFrame workspace shell

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2.9: Rewrite DoctorHomeView with SideNav

**Files:**
- Modify: `frontend/src/views/doctor/DoctorHomeView.vue`

- [ ] **Step 1: Rewrite DoctorHomeView as SideNav + RouterView layout**

Read [DoctorHomeView.vue](frontend/src/views/doctor/DoctorHomeView.vue). Replace the template with SideNav layout while keeping all business logic (`<script setup>`) entirely intact:

Replace only the `<template>` section of DoctorHomeView.vue with:

```vue
<template>
  <div class="flex flex-1 min-h-0">
    <SideNav
      title="医生工作台"
      :subtitle="doctor?.name || authStore.sessionLabel"
      :items="[
        { id: 'overview', label: '总览', path: '/doctor/overview', icon: LayoutDashboard },
        { id: 'consultation', label: '接诊', path: '/doctor/consultation', icon: Stethoscope },
        { id: 'history', label: '历史', path: '/doctor/history', icon: Clock },
        { id: 'schedule', label: '排班', path: '/doctor/schedule', icon: CalendarDays },
      ]"
    />

    <div class="flex-1 overflow-y-auto p-6">
      <p v-if="error" class="mb-4 p-3 rounded-md bg-red-50 text-danger text-sm">{{ error }}</p>

      <!-- Topline stats -->
      <div class="flex items-center gap-3 mb-6 flex-wrap">
        <StatusChip :tone="selectedRegistration?.status === 'WAITING' ? 'info' : 'success'">
          {{ selectedRegistration?.patientName || '未选中患者' }}
        </StatusChip>
        <StatusChip :tone="notificationSocketState === 'connected' ? 'success' : 'neutral'" :dot="true">
          通知 {{ notificationSocketState === 'connected' ? '已连接' : '未连接' }}
        </StatusChip>
        <span class="flex-1" />
        <button class="btn-ghost" type="button" @click="refreshAll" :disabled="loading">
          <RefreshCw :size="16" :class="{ 'animate-spin': loading }" />
          <span>{{ loading ? '加载中' : '刷新' }}</span>
        </button>
      </div>

      <RouterView v-slot="{ Component: PanelComp }">
        <component :is="PanelComp" :workspace="doctorWorkspace" v-if="PanelComp" />
      </RouterView>
    </div>
  </div>
</template>
```

Also update the imports in `<script setup>`:
```typescript
import { LayoutDashboard, Stethoscope, Clock, CalendarDays, RefreshCw } from 'lucide-vue-next';
import SideNav from '@/components/layout/SideNav.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
```

Business logic (`<script setup>`) stays EXACTLY the same. Only the template and imports change.

- [ ] **Step 2: Verify doctor workspace shows sidebar**

Run:
```bash
cd frontend && npm run dev
```

Navigate to `/doctor/overview`. Expected: Left sidebar with navigation, right content area with panel.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/doctor/DoctorHomeView.vue
git commit -m "refactor: convert DoctorHomeView to SideNav workspace shell

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2.10: Rewrite AdminHomeView with SideNav

**Files:**
- Modify: `frontend/src/views/admin/AdminHomeView.vue`

- [ ] **Step 1: Rewrite AdminHomeView as SideNav + RouterView layout**

Same pattern as Task 2.9. Read [AdminHomeView.vue](frontend/src/views/admin/AdminHomeView.vue). Keep all business logic (`<script setup>`) EXACTLY unchanged. Replace the `<template>`:

```vue
<template>
  <div class="flex flex-1 min-h-0">
    <SideNav
      title="管理工作台"
      :subtitle="authStore.sessionLabel"
      :items="[
        { id: 'overview', label: '总览', path: '/admin/overview', icon: LayoutDashboard },
        { id: 'master', label: '基础数据', path: '/admin/master-data', icon: Building2 },
        { id: 'resources', label: '排班与资源', path: '/admin/resources', icon: Layers },
        { id: 'config', label: '配置', path: '/admin/config', icon: Settings },
        { id: 'audit', label: '审计', path: '/admin/audit', icon: ShieldCheck },
      ]"
    />

    <div class="flex-1 overflow-y-auto p-6">
      <p v-if="error" class="mb-4 p-3 rounded-md bg-red-50 text-danger text-sm">{{ error }}</p>

      <div class="flex items-center gap-3 mb-6 flex-wrap">
        <StatusChip :tone="notificationSocketState === 'connected' ? 'success' : 'neutral'" :dot="true">
          通知 {{ notificationSocketState === 'connected' ? '已连接' : '未连接' }}
        </StatusChip>
        <span class="flex-1" />
        <button class="btn-ghost" type="button" @click="loadAll" :disabled="loading">
          <RefreshCw :size="16" :class="{ 'animate-spin': loading }" />
          <span>{{ loading ? '加载中' : '刷新' }}</span>
        </button>
      </div>

      <RouterView v-slot="{ Component: PanelComp }">
        <component :is="PanelComp" :workspace="adminWorkspace" v-if="PanelComp" />
      </RouterView>
    </div>
  </div>
</template>
```

Add imports:
```typescript
import { LayoutDashboard, Building2, Layers, Settings, ShieldCheck, RefreshCw } from 'lucide-vue-next';
import SideNav from '@/components/layout/SideNav.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
```

Business logic (`<script setup>`) stays EXACTLY the same.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/admin/AdminHomeView.vue
git commit -m "refactor: convert AdminHomeView to SideNav workspace shell

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Phase 3: Patient Panels

Each panel in this phase should:
1. Keep ALL business logic from the original panel file completely unchanged
2. Replace the `<template>` with Tailwind CSS classes using `SectionCard`, `StatusChip`, `EmptyState`, `LoadingSkeleton`
3. Add `ConfirmDialog` for delete/cancel operations
4. Wire up `useToast` for success/error feedback

### Task 3.1: Restyle PatientOverviewPanel

**Files:**
- Modify: `frontend/src/views/patient/panels/PatientOverviewPanel.vue`

- [ ] **Step 1: Rewrite template with Tailwind**

Read the original [PatientOverviewPanel.vue](frontend/src/views/patient/panels/PatientOverviewPanel.vue). Keep `<script setup>` entirely unchanged. Replace `<template>`:

```vue
<template>
  <div class="p-4 space-y-4">
    <p class="text-sm font-semibold text-text-main">你好，{{ workspace.displayName }}</p>

    <!-- Current registration card -->
    <SectionCard v-if="workspace.latestRegistration" title="当前挂号">
      <div class="space-y-2 text-sm">
        <div class="flex justify-between">
          <span class="text-text-secondary">科室</span>
          <span class="font-medium">{{ workspace.latestRegistration.departmentName }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-text-secondary">医生</span>
          <span class="font-medium">{{ workspace.latestRegistration.doctorName }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-text-secondary">日期</span>
          <span class="font-medium">{{ workspace.formatDate(workspace.latestRegistration.workDate) }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-text-secondary">时段</span>
          <span class="font-medium">{{ workspace.latestRegistration.period ?? '未指定' }}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-text-secondary">状态</span>
          <StatusChip :tone="workspace.latestRegistration.status === 'WAITING' ? 'info' : workspace.latestRegistration.status === 'COMPLETED' ? 'success' : 'neutral'">
            {{ workspace.latestRegistration.status }}
          </StatusChip>
        </div>
      </div>
    </SectionCard>

    <EmptyState
      v-else
      icon="calendar"
      title="暂无挂号记录"
      description="完成分诊后可在这里查看和操作挂号"
      action-label="去分诊"
      @action="$router.push('/patient/triage')"
    />

    <!-- Latest triage result -->
    <SectionCard v-if="workspace.latestTriage" title="最近分诊结果">
      <div class="space-y-2 text-sm">
        <div>
          <span class="text-text-secondary">主诉</span>
          <p class="mt-0.5 text-text-main">{{ workspace.truncate(workspace.latestTriage.chiefComplaint, 100) }}</p>
        </div>
        <div class="flex justify-between">
          <span class="text-text-secondary">推荐科室</span>
          <span class="font-medium text-brand">{{ workspace.latestTriage.recommendedDept }}</span>
        </div>
        <div v-if="workspace.latestTriage.recommendedDoctors.length" class="flex justify-between">
          <span class="text-text-secondary">推荐医生</span>
          <span class="font-medium">{{ workspace.latestTriage.recommendedDoctors.map(d => d.name).join('、') }}</span>
        </div>
      </div>
    </SectionCard>

    <!-- Quick stats -->
    <div class="grid grid-cols-2 gap-3">
      <div class="card text-center">
        <div class="text-2xl font-bold text-brand">{{ workspace.waitingRegistrations.length }}</div>
        <div class="text-xs text-text-secondary mt-1">待就诊</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-success">{{ workspace.completedRegistrations.length }}</div>
        <div class="text-xs text-text-secondary mt-1">已完成</div>
      </div>
    </div>
  </div>
</template>
```

Add imports at top of `<script setup>`:
```typescript
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/patient/panels/PatientOverviewPanel.vue
git commit -m "refactor: restyle PatientOverviewPanel with Tailwind

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 3.2: Restyle PatientTriagePanel

**Files:**
- Modify: `frontend/src/views/patient/panels/PatientTriagePanel.vue`

- [ ] **Step 1: Rewrite template with Tailwind**

Keep `<script setup>` entirely unchanged. Replace `<template>`:

```vue
<template>
  <div class="p-4 space-y-4">
    <!-- Input area -->
    <div class="space-y-3">
      <textarea
        v-model="workspace.triageForm.chiefComplaint"
        class="input-field phone-input h-28 resize-none"
        placeholder="请描述您的不适症状，例如：头痛、发热、咳嗽等..."
      />
      <button
        class="btn-primary w-full"
        type="button"
        @click="workspace.runTriage()"
        :disabled="workspace.triaging || !workspace.triageForm.chiefComplaint.trim()"
      >
        <Sparkles :size="16" />
        <span>{{ workspace.triaging ? '分析中...' : '智能分诊' }}</span>
      </button>
    </div>

    <!-- Result area -->
    <div v-if="workspace.triageResult" class="space-y-4">
      <SectionCard title="分诊结果">
        <div class="space-y-3 text-sm">
          <div>
            <span class="text-text-secondary">主诉</span>
            <p class="mt-0.5 font-medium">{{ workspace.triageResult.chiefComplaint }}</p>
          </div>
          <div class="flex items-center gap-2">
            <StatusChip tone="success">{{ workspace.triageResult.recommendedDept }}</StatusChip>
            <StatusChip tone="info">{{ workspace.triageResult.recommendationSource }}</StatusChip>
          </div>
          <p class="text-text-secondary text-xs">{{ workspace.triageResult.reason }}</p>
        </div>
      </SectionCard>

      <!-- Recommended doctors -->
      <SectionCard v-if="workspace.triageResult.recommendedDoctors.length" title="推荐医生">
        <div class="space-y-2">
          <div
            v-for="doc in workspace.triageResult.recommendedDoctors"
            :key="doc.id"
            class="flex items-center justify-between py-2 border-b border-border last:border-b-0"
          >
            <div>
              <p class="text-sm font-medium">{{ doc.name }}</p>
              <p class="text-xs text-text-secondary">{{ doc.title }} · {{ doc.departmentName }}</p>
            </div>
            <button class="btn-secondary !py-1 !px-3 !text-xs" type="button" @click="workspace.chooseDoctor(doc.id)">
              选择
            </button>
          </div>
        </div>
      </SectionCard>

      <!-- Available schedules -->
      <SectionCard v-if="workspace.visibleSchedules.length" title="可用号源">
        <div class="space-y-2">
          <div
            v-for="slot in workspace.visibleSchedules.slice(0, 5)"
            :key="slot.id"
            class="flex items-center justify-between py-2 border-b border-border last:border-b-0 text-sm"
          >
            <div>
              <p class="font-medium">{{ slot.workDate }} · {{ slot.period }}</p>
              <p class="text-xs text-text-secondary">{{ slot.visitLevel }} · 剩余 {{ slot.remainingSlots }} 号</p>
            </div>
            <StatusChip tone="info">{{ slot.remainingSlots }}号</StatusChip>
          </div>
        </div>
      </SectionCard>

      <button class="btn-primary w-full" type="button" @click="$router.push('/patient/registration')">
        去挂号
        <ArrowRight :size="16" />
      </button>
    </div>

    <EmptyState
      v-else
      icon="search"
      title="输入症状开始分诊"
      description="AI 会根据您的症状推荐合适的科室和医生"
    />
  </div>
</template>
```

Add imports:
```typescript
import { Sparkles, ArrowRight } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/patient/panels/PatientTriagePanel.vue
git commit -m "refactor: restyle PatientTriagePanel with Tailwind

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 3.3: Restyle PatientRegistrationPanel

**Files:**
- Modify: `frontend/src/views/patient/panels/PatientRegistrationPanel.vue`

- [ ] **Step 1: Rewrite template with Tailwind**

Keep `<script setup>` entirely unchanged. Replace `<template>` with a step-based card layout. Include `ConfirmDialog` for the registration confirmation step.

```vue
<template>
  <div class="p-4 space-y-4">
    <!-- Step indicators -->
    <div class="flex items-center gap-1 text-xs font-medium">
      <span class="flex items-center gap-1" :class="workspace.selectedDepartmentId ? 'text-success' : 'text-brand'">
        <span class="w-5 h-5 rounded-full flex items-center justify-center text-white" :class="workspace.selectedDepartmentId ? 'bg-success' : 'bg-brand'">1</span>
        选科室
      </span>
      <span class="text-text-secondary mx-1">→</span>
      <span class="flex items-center gap-1" :class="workspace.selectedDoctorId ? 'text-success' : 'text-text-secondary'">
        <span class="w-5 h-5 rounded-full flex items-center justify-center text-white" :class="workspace.selectedDoctorId ? 'bg-success' : 'bg-gray-300'">2</span>
        选医生
      </span>
      <span class="text-text-secondary mx-1">→</span>
      <span class="flex items-center gap-1" :class="workspace.selectedScheduleId ? 'text-success' : 'text-text-secondary'">
        <span class="w-5 h-5 rounded-full flex items-center justify-center text-white" :class="workspace.selectedScheduleId ? 'bg-success' : 'bg-gray-300'">3</span>
        选时段
      </span>
    </div>

    <!-- Department selection -->
    <SectionCard title="选择科室">
      <div class="grid grid-cols-2 gap-2">
        <button
          v-for="dept in workspace.departments"
          :key="dept.id"
          type="button"
          class="rounded-lg border px-3 py-2.5 text-sm text-left transition"
          :class="workspace.selectedDepartmentId === dept.id ? 'border-brand bg-brand-soft text-brand font-medium' : 'border-border hover:border-brand'"
          @click="workspace.chooseDepartment(dept.id)"
        >
          {{ dept.name }}
        </button>
      </div>
    </SectionCard>

    <!-- Doctor selection -->
    <SectionCard title="选择医生">
      <div v-if="!workspace.doctors.length" class="text-center text-sm text-text-secondary py-4">该科室暂无可用医生</div>
      <div class="space-y-2">
        <div
          v-for="doc in workspace.doctors"
          :key="doc.id"
          class="flex items-center justify-between py-3 px-3 rounded-lg border cursor-pointer transition"
          :class="workspace.selectedDoctorId === doc.id ? 'border-brand bg-brand-soft' : 'border-border hover:border-brand'"
          @click="workspace.chooseDoctor(doc.id)"
        >
          <div>
            <p class="text-sm font-medium">{{ doc.name }}</p>
            <p class="text-xs text-text-secondary">{{ doc.title }} · {{ doc.specialty }}</p>
          </div>
          <span v-if="workspace.selectedDoctorId === doc.id" class="text-brand font-bold text-lg">✓</span>
        </div>
      </div>
    </SectionCard>

    <!-- Schedule selection -->
    <SectionCard title="选择时段" v-if="workspace.visibleSchedules.length">
      <div class="space-y-2">
        <div
          v-for="slot in workspace.visibleSchedules"
          :key="slot.id"
          class="flex items-center justify-between py-2 px-3 rounded-lg border cursor-pointer transition"
          :class="workspace.selectedScheduleId === slot.id ? 'border-brand bg-brand-soft' : 'border-border hover:border-brand'"
          @click="workspace.chooseSchedule(slot.id)"
        >
          <div>
            <p class="text-sm font-medium">{{ slot.workDate }} · {{ slot.period }}</p>
            <p class="text-xs text-text-secondary">{{ slot.doctorName }} · {{ slot.visitLevel }}</p>
          </div>
          <StatusChip :tone="slot.remainingSlots && slot.remainingSlots > 0 ? 'success' : 'danger'">
            {{ slot.remainingSlots ? `余${slot.remainingSlots}号` : '约满' }}
          </StatusChip>
        </div>
      </div>
    </SectionCard>

    <!-- Selected summary -->
    <SectionCard v-if="workspace.selectedSchedule" title="挂号确认">
      <div class="space-y-2 text-sm mb-4">
        <div class="flex justify-between"><span class="text-text-secondary">科室</span><span>{{ workspace.selectedDepartment?.name }}</span></div>
        <div class="flex justify-between"><span class="text-text-secondary">医生</span><span>{{ workspace.selectedDoctor?.name }}</span></div>
        <div class="flex justify-between"><span class="text-text-secondary">时段</span><span>{{ workspace.selectedSchedule.workDate }} {{ workspace.selectedSchedule.period }}</span></div>
      </div>
      <button
        class="btn-primary w-full"
        type="button"
        @click="workspace.submitRegistration()"
        :disabled="workspace.registering"
      >
        <Ticket :size="16" />
        <span>{{ workspace.registering ? '挂号中...' : '确认挂号' }}</span>
      </button>
    </SectionCard>

    <EmptyState
      v-if="!workspace.departments.length && !workspace.loading"
      icon="calendar"
      title="暂无可用科室"
      description="当前没有可挂号的科室，请稍后再试"
    />
  </div>
</template>
```

Add imports:
```typescript
import { Ticket } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/patient/panels/PatientRegistrationPanel.vue
git commit -m "refactor: restyle PatientRegistrationPanel with Tailwind

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 3.4: Restyle PatientRecordsPanel (records + prescriptions + feedback)

**Files:**
- Modify: `frontend/src/views/patient/panels/PatientRecordsPanel.vue`
- Modify: `frontend/src/views/patient/panels/PatientProfilePanel.vue`
- Modify: `frontend/src/views/patient/panels/PatientHistoryPanel.vue`

- [ ] **Step 1: Restyle PatientRecordsPanel**

Keep `<script setup>` entirely unchanged. Replace `<template>` with Tailwind card-based record list. Add `ConfirmDialog` for feedback submission and `EmptyState` for empty lists.

Template structure:
```vue
<template>
  <div class="p-4 space-y-4">
    <!-- Medical Records Section -->
    <SectionCard title="就诊记录">
      <div v-if="workspace.medicalRecords.length" class="space-y-3">
        <div v-for="record in workspace.medicalRecords" :key="record.id" class="py-3 border-b border-border last:border-b-0">
          <p class="text-sm font-medium">{{ workspace.formatDateTime(record.createdAt) }}</p>
          <p class="text-sm text-text-secondary mt-0.5">{{ record.departmentName }} · {{ record.doctorName }}</p>
          <p v-if="record.preliminaryDiagnosis" class="text-sm mt-1">{{ record.preliminaryDiagnosis }}</p>
          <StatusChip :tone="record.aiGenerated ? 'info' : 'neutral'" class="mt-1.5">{{ record.aiGenerated ? 'AI辅助' : '手动' }}</StatusChip>
        </div>
      </div>
      <EmptyState v-else icon="file" title="暂无就诊记录" description="完成就诊后可在这里查看病历" />
    </SectionCard>

    <!-- Prescriptions Section -->
    <SectionCard title="处方记录">
      <div v-if="workspace.prescriptions.length" class="space-y-3">
        <div v-for="presc in workspace.prescriptions" :key="presc.id" class="py-3 border-b border-border last:border-b-0">
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium">{{ workspace.formatDateTime(presc.createdAt) }}</span>
            <StatusChip :tone="presc.riskLevel === 'HIGH' ? 'danger' : presc.riskLevel === 'MEDIUM' ? 'warning' : 'success'">
              {{ presc.riskLevel === 'HIGH' ? '高风险' : presc.riskLevel === 'MEDIUM' ? '中风险' : '低风险' }}
            </StatusChip>
          </div>
          <p class="text-sm text-text-secondary mt-0.5">{{ presc.doctorName }} · {{ presc.departmentName }}</p>
        </div>
      </div>
      <EmptyState v-else icon="file" title="暂无处方记录" />
    </SectionCard>

    <!-- Feedback Section -->
    <SectionCard title="提交反馈">
      <div class="space-y-3">
        <label class="label-text">选择就诊记录</label>
        <select v-model="workspace.feedbackForm.registrationId" class="input-field">
          <option :value="null" disabled>请选择已完成的就诊</option>
          <option v-for="reg in workspace.completedRegistrations" :key="reg.id" :value="reg.id">
            {{ workspace.formatDate(reg.workDate) }} · {{ reg.doctorName }}
          </option>
        </select>

        <label class="label-text">评分</label>
        <div class="flex gap-1">
          <button v-for="n in 5" :key="n" type="button" class="text-2xl transition" :class="n <= workspace.feedbackForm.rating ? 'text-warning' : 'text-text-secondary/30'" @click="workspace.feedbackForm.rating = n">★</button>
        </div>

        <label class="label-text">评价</label>
        <textarea v-model="workspace.feedbackForm.comment" class="input-field h-20 resize-none" placeholder="写下您的就诊体验..." />

        <button class="btn-primary w-full" type="button" @click="workspace.submitFeedback()" :disabled="workspace.submittingFeedback || !workspace.feedbackForm.registrationId">
          {{ workspace.submittingFeedback ? '提交中...' : '提交反馈' }}
        </button>
      </div>
    </SectionCard>
  </div>
</template>
```

Add imports:
```typescript
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
```

- [ ] **Step 2: Restyle PatientProfilePanel**

Keep `<script setup>` entirely unchanged. Replace `<template>` with form layout:

```vue
<template>
  <div class="p-4 space-y-4">
    <SectionCard title="个人资料">
      <div class="space-y-3">
        <label class="label-text">姓名</label>
        <input v-model="workspace.profileForm.realName" class="input-field phone-input" placeholder="请输入姓名" />

        <label class="label-text">性别</label>
        <select v-model="workspace.profileForm.gender" class="input-field phone-input">
          <option value="">请选择</option>
          <option value="男">男</option>
          <option value="女">女</option>
        </select>

        <label class="label-text">年龄</label>
        <input v-model="workspace.profileForm.age" class="input-field phone-input" type="number" placeholder="请输入年龄" />

        <label class="label-text">手机号</label>
        <input v-model="workspace.profileForm.phone" class="input-field phone-input" placeholder="请输入手机号" />
      </div>
    </SectionCard>

    <SectionCard title="既往史">
      <textarea v-model="workspace.profileForm.medicalHistory" class="input-field phone-input h-24 resize-none" placeholder="请输入既往病史..." />
    </SectionCard>

    <SectionCard title="过敏史">
      <textarea v-model="workspace.profileForm.remark" class="input-field phone-input h-24 resize-none" placeholder="请输入过敏史..." />
    </SectionCard>

    <button class="btn-primary w-full" type="button" @click="workspace.saveProfile()" :disabled="workspace.savingProfile">
      {{ workspace.savingProfile ? '保存中...' : '保存资料' }}
    </button>
  </div>
</template>
```

- [ ] **Step 3: Restyle PatientHistoryPanel**

Keep `<script setup>` entirely unchanged. Replace `<template>`:

```vue
<template>
  <div class="p-4 space-y-4">
    <SectionCard title="分诊历史">
      <div v-if="workspace.triageHistory.length" class="space-y-3">
        <div v-for="item in workspace.triageHistory" :key="item.triageRecordId" class="py-2 border-b border-border last:border-b-0">
          <p class="text-sm font-medium">{{ workspace.truncate(item.chiefComplaint, 60) }}</p>
          <div class="flex items-center gap-2 mt-1">
            <StatusChip tone="success">{{ item.recommendedDept }}</StatusChip>
            <StatusChip tone="info">{{ item.recommendationSource }}</StatusChip>
          </div>
        </div>
      </div>
      <EmptyState v-else icon="search" title="暂无分诊历史" description="完成分诊后可在这里查看记录" />
    </SectionCard>

    <SectionCard title="挂号历史">
      <div v-if="workspace.registrations.length" class="space-y-2">
        <div v-for="reg in workspace.registrations" :key="reg.id" class="py-2 border-b border-border last:border-b-0 text-sm flex justify-between">
          <div>
            <span class="font-medium">{{ workspace.formatDate(reg.workDate) }}</span>
            <span class="text-text-secondary"> · {{ reg.doctorName }}</span>
          </div>
          <StatusChip :tone="reg.status === 'CANCELLED' ? 'danger' : reg.status === 'COMPLETED' ? 'success' : 'info'">
            {{ reg.status === 'WAITING' ? '待就诊' : reg.status === 'COMPLETED' ? '已完成' : reg.status === 'CANCELLED' ? '已取消' : reg.status }}
          </StatusChip>
        </div>
      </div>
      <EmptyState v-else icon="calendar" title="暂无挂号历史" />
    </SectionCard>

    <SectionCard title="反馈记录">
      <div v-if="workspace.feedbacks.length" class="space-y-2">
        <div v-for="fb in workspace.feedbacks" :key="fb.id" class="py-2 border-b border-border last:border-b-0 text-sm">
          <div class="flex items-center gap-1 text-warning">
            <span v-for="n in 5" :key="n">{{ n <= fb.rating ? '★' : '☆' }}</span>
          </div>
          <p v-if="fb.comment" class="text-text-secondary mt-0.5">{{ fb.comment }}</p>
        </div>
      </div>
      <EmptyState v-else icon="file" title="暂无反馈记录" />
    </SectionCard>
  </div>
</template>
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/patient/panels/PatientRecordsPanel.vue frontend/src/views/patient/panels/PatientProfilePanel.vue frontend/src/views/patient/panels/PatientHistoryPanel.vue
git commit -m "refactor: restyle patient Records, Profile, History panels with Tailwind

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Phase 4: Doctor Panels

### Task 4.1: Restyle DoctorOverviewPanel (keep ECharts)

**Files:**
- Modify: `frontend/src/views/doctor/panels/DoctorOverviewPanel.vue`

- [ ] **Step 1: Rewrite template with Tailwind metric cards + DashboardCharts**

Keep `<script setup>` entirely unchanged. Replace `<template>`:

```vue
<template>
  <div class="space-y-6">
    <!-- Metric cards row -->
    <div class="grid grid-cols-4 gap-4">
      <div class="card text-center">
        <div class="text-2xl font-bold text-brand">{{ workspace.dashboard?.todayVisits ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">今日接诊</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-warning">{{ workspace.dashboard?.waitingRegistrations ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">待处理</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-danger">{{ workspace.dashboard?.highRiskReviews ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">高风险</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-info">{{ workspace.dashboard?.todayAiCallRecords ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">AI调用</div>
      </div>
    </div>

    <!-- Queue preview -->
    <SectionCard title="待接诊队列">
      <div v-if="workspace.queue.length" class="space-y-2">
        <div
          v-for="reg in workspace.queue.slice(0, 10)"
          :key="reg.id"
          class="flex items-center justify-between py-2 border-b border-border last:border-b-0"
        >
          <div>
            <p class="text-sm font-medium">{{ reg.patientName }}</p>
            <p class="text-xs text-text-secondary">{{ workspace.formatDate(reg.registrationTime) }}</p>
          </div>
          <StatusChip :tone="reg.status === 'WAITING' ? 'warning' : 'success'">
            {{ reg.status === 'WAITING' ? '等待接诊' : '就诊中' }}
          </StatusChip>
        </div>
      </div>
      <EmptyState v-else icon="calendar" title="暂无待接诊患者" />
    </SectionCard>

    <!-- Dashboard charts -->
    <SectionCard title="趋势">
      <DashboardCharts :workspace="workspace" />
    </SectionCard>
  </div>
</template>
```

Add imports:
```typescript
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
import DashboardCharts from '@/components/DashboardCharts.vue';
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/doctor/panels/DoctorOverviewPanel.vue
git commit -m "refactor: restyle DoctorOverviewPanel with Tailwind

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 4.2: Restyle DoctorConsultationPanel (largest panel)

**Files:**
- Modify: `frontend/src/views/doctor/panels/DoctorConsultationPanel.vue`

- [ ] **Step 1: Rewrite template with Tailwind**

This is the largest and most complex panel. Keep ALL `<script setup>` exactly unchanged. Replace `<template>` with a split layout:

```vue
<template>
  <div class="space-y-6">
    <!-- Queue + workspace split -->
    <div class="grid grid-cols-[240px_1fr] gap-6">
      <!-- Left: Patient queue -->
      <SectionCard title="患者队列" class="h-fit">
        <div v-if="workspace.queue.length" class="space-y-1">
          <button
            v-for="reg in workspace.queue"
            :key="reg.id"
            type="button"
            class="w-full text-left px-3 py-2 rounded-md text-sm transition"
            :class="workspace.selectedRegistrationId === reg.id ? 'bg-brand-soft text-brand font-medium' : 'hover:bg-gray-50'"
            @click="workspace.selectRegistration(reg.id)"
          >
            <p>{{ reg.patientName }}</p>
            <p class="text-xs text-text-secondary">{{ workspace.truncate(reg.chiefComplaint, 20) }}</p>
          </button>
        </div>
        <EmptyState v-else icon="calendar" title="暂无患者" />
      </SectionCard>

      <!-- Right: Workspace -->
      <div v-if="workspace.workspace" class="space-y-4">
        <LoadingSkeleton v-if="workspace.workspaceLoading" :rows="3" />

        <div v-else class="space-y-4">
          <!-- Quick actions -->
          <div class="flex items-center gap-2">
            <button class="btn-primary" type="button" @click="workspace.beginSelectedConsultation()" :disabled="workspace.startingConsultation">
              {{ workspace.startingConsultation ? '接诊中' : '开始接诊' }}
            </button>
            <button class="btn-danger" type="button" @click="workspace.completeSelectedConsultation()" :disabled="workspace.completingConsultation">
              {{ workspace.completingConsultation ? '处理中' : '结束就诊' }}
            </button>
          </div>

          <!-- Conversation input -->
          <SectionCard title="问诊记录">
            <textarea
              v-model="workspace.consultationForm.conversationText"
              class="input-field h-32 resize-none"
              placeholder="输入问诊对话内容..."
            />
            <div class="flex gap-2 mt-3">
              <button class="btn-primary" type="button" @click="workspace.generateDraftMedicalRecord()" :disabled="workspace.generatingRecord">
                <Sparkles :size="16" />
                <span>{{ workspace.generatingRecord ? '生成中' : 'AI生成病历' }}</span>
              </button>
              <button class="btn-secondary" type="button" @click="workspace.diagnoseCurrentCase()" :disabled="workspace.diagnosingRecord">
                <Sparkles :size="16" />
                <span>{{ workspace.diagnosingRecord ? '诊断中' : 'AI诊断建议' }}</span>
              </button>
            </div>
          </SectionCard>

          <!-- Medical record form -->
          <SectionCard title="病历草稿">
            <div class="grid grid-cols-2 gap-3">
              <label class="label-text">主诉 <textarea v-model="workspace.recordForm.chiefComplaint" class="input-field h-16 resize-none mt-1" /></label>
              <label class="label-text">现病史 <textarea v-model="workspace.recordForm.presentIllness" class="input-field h-16 resize-none mt-1" /></label>
              <label class="label-text">既往史 <textarea v-model="workspace.recordForm.pastHistory" class="input-field h-16 resize-none mt-1" /></label>
              <label class="label-text">体格检查 <textarea v-model="workspace.recordForm.physicalExam" class="input-field h-16 resize-none mt-1" /></label>
              <label class="label-text col-span-2">初步诊断 <textarea v-model="workspace.recordForm.preliminaryDiagnosis" class="input-field h-16 resize-none mt-1" /></label>
              <label class="label-text col-span-2">治疗方案 <textarea v-model="workspace.recordForm.treatmentPlan" class="input-field h-16 resize-none mt-1" /></label>
            </div>
            <button class="btn-primary mt-3" type="button" @click="workspace.saveCurrentMedicalRecord()" :disabled="workspace.savingRecord">
              {{ workspace.savingRecord ? '保存中' : '保存病历' }}
            </button>
          </SectionCard>

          <!-- Prescription editor -->
          <SectionCard title="处方编辑">
            <div class="space-y-3">
              <div v-for="(item, idx) in workspace.prescriptionItems" :key="item.key" class="flex items-center gap-2">
                <select v-model="item.drugId" class="input-field flex-1" @change="workspace.applyDrugDefaults(item)">
                  <option :value="null" disabled>选择药品</option>
                  <option v-for="drug in workspace.availableDrugs" :key="drug.id" :value="drug.id">{{ drug.name }}</option>
                </select>
                <input v-model="item.dosage" class="input-field w-16" placeholder="用量" />
                <input v-model="item.frequency" class="input-field w-20" placeholder="频次" />
                <input v-model="item.quantity" class="input-field w-16" placeholder="数量" />
                <button v-if="workspace.prescriptionItems.length > 1" class="btn-ghost !p-1 !text-danger" type="button" @click="workspace.removePrescriptionItem(item.key)">✕</button>
              </div>
              <div class="flex gap-2">
                <button class="btn-secondary" type="button" @click="workspace.addPrescriptionItem()">+ 添加药品</button>
                <button class="btn-primary" type="button" @click="workspace.reviewCurrentPrescription()" :disabled="workspace.reviewingPrescription">
                  {{ workspace.reviewingPrescription ? '审方中' : '提交审方' }}
                </button>
              </div>
            </div>
          </SectionCard>

          <!-- Review result -->
          <SectionCard v-if="workspace.reviewResult" title="审方结果">
            <div class="space-y-2 text-sm">
              <div class="flex items-center gap-2">
                <span class="text-text-secondary">风险等级：</span>
                <StatusChip :tone="workspace.reviewResult.riskLevel === 'HIGH' ? 'danger' : workspace.reviewResult.riskLevel === 'MEDIUM' ? 'warning' : 'success'">
                  {{ workspace.reviewResult.riskLevel === 'HIGH' ? '高风险' : workspace.reviewResult.riskLevel === 'MEDIUM' ? '中风险' : '低风险' }}
                </StatusChip>
              </div>
              <div v-if="workspace.reviewResult.localRuleHits" class="p-3 rounded-md bg-red-50 text-danger text-xs whitespace-pre-wrap">
                {{ workspace.reviewResult.localRuleHits }}
              </div>
              <div v-if="workspace.reviewResult.llmSuggestion" class="p-3 rounded-md bg-blue-50 text-info text-xs whitespace-pre-wrap">
                {{ workspace.reviewResult.llmSuggestion }}
              </div>
            </div>
            <button
              class="btn-primary mt-3"
              type="button"
              @click="workspace.submitCurrentPrescription()"
              :disabled="workspace.submittingPrescription"
            >
              {{ workspace.submittingPrescription ? '提交中' : '确认提交处方' }}
            </button>
          </SectionCard>
        </div>
      </div>

      <!-- No patient selected -->
      <EmptyState v-else icon="stethoscope" title="请选择患者开始接诊" description="从左侧队列选择一个患者" />
    </div>
  </div>
</template>
```

Add imports at top of `<script setup>`:
```typescript
import { Sparkles } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';
import LoadingSkeleton from '@/components/shared/LoadingSkeleton.vue';
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/doctor/panels/DoctorConsultationPanel.vue
git commit -m "refactor: restyle DoctorConsultationPanel with Tailwind

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 4.3: Restyle DoctorHistoryPanel and DoctorSchedulePanel

**Files:**
- Modify: `frontend/src/views/doctor/panels/DoctorHistoryPanel.vue`
- Modify: `frontend/src/views/doctor/panels/DoctorSchedulePanel.vue`

- [ ] **Step 1: Restyle DoctorHistoryPanel**

Keep `<script setup>` entirely unchanged. Replace `<template>`:

```vue
<template>
  <div class="space-y-6">
    <!-- Search -->
    <div class="flex gap-3">
      <input v-model="workspace.recordSearch" class="input-field flex-1" placeholder="搜索病历..." />
      <button class="btn-primary" type="button" @click="workspace.loadHistory()">搜索</button>
    </div>

    <!-- Medical records -->
    <SectionCard title="病历历史">
      <div v-if="workspace.medicalRecords.length" class="space-y-3">
        <div v-for="record in workspace.medicalRecords" :key="record.id" class="py-3 border-b border-border last:border-b-0 text-sm">
          <div class="flex justify-between items-start">
            <div>
              <p class="font-medium">{{ record.patientName }}</p>
              <p class="text-text-secondary text-xs">{{ workspace.formatDateTime(record.createdAt) }} · {{ record.departmentName }}</p>
            </div>
            <StatusChip :tone="record.aiGenerated ? 'info' : 'neutral'">{{ record.aiGenerated ? 'AI' : '手动' }}</StatusChip>
          </div>
          <p class="mt-1">{{ workspace.truncate(record.preliminaryDiagnosis, 80) }}</p>
        </div>
      </div>
      <EmptyState v-else icon="file" title="暂无病历记录" />
    </SectionCard>

    <!-- Prescription history -->
    <SectionCard title="处方历史">
      <div v-if="workspace.prescriptions.length" class="space-y-3">
        <div v-for="presc in workspace.prescriptions" :key="presc.id" class="py-3 border-b border-border last:border-b-0 text-sm">
          <div class="flex justify-between">
            <span class="font-medium">{{ presc.patientName }}</span>
            <StatusChip :tone="presc.riskLevel === 'HIGH' ? 'danger' : presc.riskLevel === 'MEDIUM' ? 'warning' : 'success'">
              {{ presc.riskLevel === 'HIGH' ? '高风险' : presc.riskLevel === 'MEDIUM' ? '中风险' : '低风险' }}
            </StatusChip>
          </div>
          <p class="text-xs text-text-secondary">医生：{{ presc.doctorName }} · {{ workspace.formatDateTime(presc.createdAt) }}</p>
        </div>
      </div>
      <EmptyState v-else icon="file" title="暂无处方记录" />
    </SectionCard>
  </div>
</template>
```

- [ ] **Step 2: Restyle DoctorSchedulePanel**

Keep `<script setup>` entirely unchanged. Replace `<template>`:

```vue
<template>
  <SectionCard title="我的排班">
    <div v-if="workspace.schedules.length" class="space-y-2">
      <div
        v-for="slot in workspace.schedules"
        :key="slot.id"
        class="flex items-center justify-between py-3 px-3 rounded-lg border border-border"
      >
        <div>
          <p class="text-sm font-medium">{{ slot.workDate }} · {{ slot.period }}</p>
          <p class="text-xs text-text-secondary">{{ slot.departmentName }} · {{ slot.visitLevel }}</p>
        </div>
        <div class="text-right">
          <p class="text-sm font-medium">{{ slot.remainingSlots }}/{{ slot.totalSlots }}</p>
          <StatusChip :tone="slot.remainingSlots && slot.remainingSlots > 0 ? 'success' : 'danger'">
            {{ slot.remainingSlots && slot.remainingSlots > 0 ? '有余号' : '已约满' }}
          </StatusChip>
        </div>
      </div>
    </div>
    <EmptyState v-else icon="calendar" title="暂无排班" description="暂无当前排班数据" />
  </SectionCard>
</template>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/doctor/panels/DoctorHistoryPanel.vue frontend/src/views/doctor/panels/DoctorSchedulePanel.vue
git commit -m "refactor: restyle DoctorHistory and DoctorSchedule panels with Tailwind

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Phase 5: Admin Panels

### Task 5.1: Restyle AdminOverviewPanel

**Files:**
- Modify: `frontend/src/views/admin/panels/AdminOverviewPanel.vue`

- [ ] **Step 1: Rewrite template with metric cards + charts**

Keep `<script setup>` entirely unchanged. Replace `<template>` with the same pattern as DoctorOverviewPanel:

```vue
<template>
  <div class="space-y-6">
    <div class="grid grid-cols-5 gap-4">
      <div class="card text-center">
        <div class="text-2xl font-bold text-brand">{{ workspace.dashboard?.todayRegistrations ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">今日挂号</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-info">{{ workspace.dashboard?.todayVisits ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">今日接诊</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-warning">{{ workspace.dashboard?.todayPrescriptions ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">处方数</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-info">{{ workspace.aiUsage?.totalCalls ?? 0 }}</div>
        <div class="text-xs text-text-secondary mt-1">AI调用</div>
      </div>
      <div class="card text-center">
        <div class="text-2xl font-bold text-danger">{{ workspace.triageAccuracy ? Math.round(workspace.triageAccuracy.accuracyRate * 100) + '%' : '-' }}</div>
        <div class="text-xs text-text-secondary mt-1">分诊准确率</div>
      </div>
    </div>

    <SectionCard title="趋势">
      <DashboardCharts :workspace="workspace" />
    </SectionCard>
  </div>
</template>
```

Add imports:
```typescript
import SectionCard from '@/components/shared/SectionCard.vue';
import DashboardCharts from '@/components/DashboardCharts.vue';
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/admin/panels/AdminOverviewPanel.vue
git commit -m "refactor: restyle AdminOverviewPanel with Tailwind

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 5.2: Restyle AdminMasterPanel (departments + doctors + drugs tabs)

**Files:**
- Modify: `frontend/src/views/admin/panels/AdminMasterPanel.vue`
- Modify: `frontend/src/views/admin/panels/AdminResourcesPanel.vue`
- Modify: `frontend/src/views/admin/panels/AdminConfigPanel.vue`
- Modify: `frontend/src/views/admin/panels/AdminAuditPanel.vue`

- [ ] **Step 1: Restyle AdminMasterPanel**

Keep `<script setup>` entirely unchanged. Replace `<template>`:

```vue
<template>
  <div class="space-y-6">
    <!-- Tab switcher -->
    <div class="flex gap-1 bg-gray-100 rounded-lg p-1 w-fit">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        class="px-4 py-1.5 rounded-md text-sm font-medium transition"
        :class="activeTab === tab.id ? 'bg-white text-brand shadow-sm' : 'text-text-secondary hover:text-text-main'"
        @click="activeTab = tab.id"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- Search bar -->
    <div class="flex gap-3">
      <input v-if="activeTab === 'drugs'" v-model="workspace.drugKeyword" class="input-field flex-1" placeholder="搜索药品..." />
      <button class="btn-primary" type="button" @click="workspace.loadAll()" :disabled="workspace.loading">
        <Search :size="16" />
        <span>搜索</span>
      </button>
      <button class="btn-secondary" type="button" @click="workspace.createNew(activeTab === 'departments' ? 'department' : activeTab === 'doctors' ? 'doctor' : 'drug')">
        <Plus :size="16" />
        <span>新增</span>
      </button>
    </div>

    <!-- Departments tab -->
    <SectionCard v-if="activeTab === 'departments'" title="科室列表">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border text-left text-text-secondary">
              <th class="pb-2 font-medium">名称</th>
              <th class="pb-2 font-medium">编码</th>
              <th class="pb-2 font-medium">类型</th>
              <th class="pb-2 font-medium">状态</th>
              <th class="pb-2 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in workspace.departments" :key="item.id" class="border-b border-border">
              <td class="py-2.5 font-medium">{{ item.name }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.code }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.type ?? '-' }}</td>
              <td class="py-2.5">
                <StatusChip :tone="item.status === 'ACTIVE' ? 'success' : 'neutral'">{{ item.status === 'ACTIVE' ? '启用' : '停用' }}</StatusChip>
              </td>
              <td class="py-2.5">
                <div class="flex gap-1">
                  <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.selectDepartment(item); activeTab = 'departments'">编辑</button>
                  <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.toggleCurrent()" :disabled="workspace.saving">
                    {{ item.status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState v-if="!workspace.departments.length && !workspace.loading" icon="search" title="暂无科室数据" />
    </SectionCard>

    <!-- Doctors tab -->
    <SectionCard v-if="activeTab === 'doctors'" title="医生列表">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border text-left text-text-secondary">
              <th class="pb-2 font-medium">姓名</th>
              <th class="pb-2 font-medium">科室</th>
              <th class="pb-2 font-medium">职称</th>
              <th class="pb-2 font-medium">状态</th>
              <th class="pb-2 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in workspace.doctors" :key="item.id" class="border-b border-border">
              <td class="py-2.5 font-medium">{{ item.name }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.departmentName ?? '-' }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.title ?? '-' }}</td>
              <td class="py-2.5">
                <StatusChip :tone="item.status === 'ACTIVE' ? 'success' : 'neutral'">{{ item.status === 'ACTIVE' ? '启用' : '停用' }}</StatusChip>
              </td>
              <td class="py-2.5">
                <div class="flex gap-1">
                  <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.selectDoctor(item)">编辑</button>
                  <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.toggleCurrent()" :disabled="workspace.saving">切换</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </SectionCard>

    <!-- Drugs tab -->
    <SectionCard v-if="activeTab === 'drugs'" title="药品列表">
      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b border-border text-left text-text-secondary">
              <th class="pb-2 font-medium">名称</th>
              <th class="pb-2 font-medium">规格</th>
              <th class="pb-2 font-medium">厂家</th>
              <th class="pb-2 font-medium">状态</th>
              <th class="pb-2 font-medium">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in workspace.drugs" :key="item.id" class="border-b border-border">
              <td class="py-2.5 font-medium">{{ item.name }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.specification ?? '-' }}</td>
              <td class="py-2.5 text-text-secondary">{{ item.manufacturer ?? '-' }}</td>
              <td class="py-2.5">
                <StatusChip :tone="item.status === 'ACTIVE' ? 'success' : 'neutral'">{{ item.status === 'ACTIVE' ? '启用' : '停用' }}</StatusChip>
              </td>
              <td class="py-2.5">
                <div class="flex gap-1">
                  <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.selectDrug(item)">编辑</button>
                  <button class="btn-ghost !p-1 !text-xs" type="button" @click="workspace.toggleCurrent()" :disabled="workspace.saving">切换</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </SectionCard>
  </div>
</template>
```

Add imports:
```typescript
import { ref } from 'vue';
import { Search, Plus } from 'lucide-vue-next';
import SectionCard from '@/components/shared/SectionCard.vue';
import StatusChip from '@/components/shared/StatusChip.vue';
import EmptyState from '@/components/shared/EmptyState.vue';

const tabs = [
  { id: 'departments', label: '科室' },
  { id: 'doctors', label: '医生' },
  { id: 'drugs', label: '药品' },
] as const;
const activeTab = ref<(typeof tabs)[number]['id']>('departments');
```

- [ ] **Step 2: Restyle AdminResourcesPanel**

Same pattern — keep `<script setup>`, replace `<template>` with schedule table + batch create button. Use `SectionCard`.

- [ ] **Step 3: Restyle AdminConfigPanel**

Three tabs (rules / AI config / prompts — use existing `<script setup>` logic). Add tab switcher, tables with toggle + edit actions.

- [ ] **Step 4: Restyle AdminAuditPanel**

Two tabs (AI records / audit logs). Tables with timestamps and status chips. Keep `<script setup>` unchanged.

- [ ] **Step 5: Integrate AdminEditorPanel**

The existing `AdminEditorPanel.vue` should be restyled to use Tailwind form classes and placed in a modal/drawer or appear as a section below the table when an item is selected. Keep the CRUD logic intact.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/admin/panels/
git commit -m "refactor: restyle all admin panels with Tailwind

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Phase 6: Polish

### Task 6.1: Redesign HomeView entry page

**Files:**
- Modify: `frontend/src/views/HomeView.vue`

- [ ] **Step 1: Rewrite HomeView as role card grid**

Read [HomeView.vue](frontend/src/views/HomeView.vue). Keep `<script setup>` unchanged. Replace `<template>`:

```vue
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

    <!-- Health status footer -->
    <div class="mt-10 text-center">
      <StatusChip :tone="appStore.health?.status === 'UP' ? 'success' : 'danger'">
        {{ healthText }}
      </StatusChip>
    </div>
  </div>
</template>
```

Add imports:
```typescript
import { UserRound, Stethoscope, ShieldCheck } from 'lucide-vue-next';
import StatusChip from '@/components/shared/StatusChip.vue';
```

Remove the old `metric-grid` section in the template — it's info noise for a production entry page.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/HomeView.vue
git commit -m "refactor: redesign HomeView as role card grid entry

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 6.2: Restyle AuthView

**Files:**
- Modify: `frontend/src/views/auth/AuthView.vue`

- [ ] **Step 1: Rewrite AuthView template with Tailwind**

Keep `<script setup>` entirely unchanged. Replace `<template>`:

```vue
<template>
  <div class="max-w-sm mx-auto py-16 px-4">
    <h1 class="text-xl font-bold text-text-main text-center mb-8">{{ isLogin ? '登录' : '患者注册' }}</h1>

    <div class="card space-y-3">
      <label class="label-text">用户名 <input v-model="loginForm.username" class="input-field phone-input mt-1" placeholder="请输入用户名" /></label>

      <label class="label-text">密码 <input v-model="loginForm.password" class="input-field phone-input mt-1" type="password" placeholder="请输入密码" /></label>

      <template v-if="!isLogin">
        <label class="label-text">真实姓名 <input v-model="registerForm.realName" class="input-field phone-input mt-1" placeholder="请输入真实姓名" /></label>
        <label class="label-text">手机号 <input v-model="registerForm.phone" class="input-field phone-input mt-1" placeholder="请输入手机号" /></label>
      </template>

      <!-- Role selector (only for login) -->
      <div v-if="isLogin" class="flex gap-1 bg-gray-100 rounded-lg p-1">
        <button
          v-for="r in roles"
          :key="r.id"
          type="button"
          class="flex-1 py-1.5 text-sm font-medium rounded-md transition"
          :class="activeRole === r.id ? 'bg-white text-brand shadow-sm' : 'text-text-secondary'"
          @click="activeRole = r.id"
        >{{ r.label }}</button>
      </div>

      <p v-if="authStore.error" class="text-danger text-xs">{{ authStore.error }}</p>

      <button class="btn-primary w-full" type="button" @click="submitForm" :disabled="authStore.loading">
        {{ authStore.loading ? '处理中...' : (isLogin ? '登录' : '注册') }}
      </button>

      <button class="btn-ghost w-full" type="button" @click="isLogin = !isLogin">
        {{ isLogin ? '没有账号？去注册' : '已有账号？去登录' }}
      </button>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/views/auth/AuthView.vue
git commit -m "refactor: restyle AuthView with Tailwind

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 6.3: Clean up — remove base.css

**Files:**
- Remove: `frontend/src/styles/base.css`

- [ ] **Step 1: Delete base.css**

```bash
rm frontend/src/styles/base.css
```

- [ ] **Step 2: Verify app still loads without errors**

Run:
```bash
cd frontend && npm run dev
```

Navigate through all three workspaces. Check:
- `/` home page loads
- `/login` auth page loads
- `/patient/overview` phone frame shows
- `/doctor/overview` sidebar + panels load
- `/admin/overview` sidebar + panels load

- [ ] **Step 3: Build check**

```bash
cd frontend && npm run build
```

Expected: Build succeeds with no errors.

- [ ] **Step 4: Commit**

```bash
git rm frontend/src/styles/base.css
git commit -m "refactor: remove legacy base.css — fully migrated to Tailwind

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 6.4: Wire up Toast + ConfirmDialog across all panels

**Files:**
- Modify: All panels that perform destructive or important actions

- [ ] **Step 1: Add ConfirmDialog to patient registration cancellation**

In PatientHomeView.vue: Add a `confirmCancel` state and wrap the `cancelWaitingRegistration` call. Use `ConfirmDialog` in the phone frame.

- [ ] **Step 2: Add ConfirmDialog to doctor prescription submission**

In DoctorConsultationPanel.vue: Wrap `submitCurrentPrescription` call with `ConfirmDialog` (level="warning").

- [ ] **Step 3: Add ConfirmDialog to doctor consultation completion**

In DoctorConsultationPanel.vue: Wrap `completeSelectedConsultation`.

- [ ] **Step 4: Add success Toasts in all panels**

Add `import { useToast } from '@/composables/useToast'` and call `toast.success('...')` after each successful async operation across all panels.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/
git commit -m "feat: wire up ConfirmDialog and Toast across all panels

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 6.5: Final verification

- [ ] **Step 1: Full type check**

```bash
cd frontend && npm run typecheck
```

Expected: Zero type errors.

- [ ] **Step 2: Full build**

```bash
cd frontend && npm run build
```

Expected: Successful production build.

- [ ] **Step 3: Manual smoke test (with backend running)**

Start the backend, then run the frontend. Smoke test the following flows:

1. Open `/` → see three role cards
2. Login as patient → `/patient/overview` → phone frame shows with bottom tabs
3. Go to triage → enter symptom → get AI recommendation
4. Go to registration → select department/doctor/schedule → confirm
5. Go to records → see medical records and prescriptions
6. Login as doctor → `/doctor/overview` → sidebar + dashboard
7. Go to consultation → select patient from queue → edit record/prescription → review → submit
8. Login as admin → `/admin/overview` → sidebar + dashboard
9. Go to master data → view/edit departments, doctors, drugs
10. Verify WebSocket notification connection (green indicator)
11. Verify SSE streaming (skeleton → filled text)

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "chore: final verification tweaks and cleanup

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Summary

| Phase | Tasks | Files Created | Files Modified | Files Removed |
|---|---|---|---|---|
| 1. Infrastructure | 9 | 10 | 3 | 0 |
| 2. Shell Layer | 10 | 3 | 5 | 0 |
| 3. Patient Panels | 4 | 0 | 6 | 0 |
| 4. Doctor Panels | 3 | 0 | 4 | 0 |
| 5. Admin Panels | 2 | 0 | 6 (batch) | 0 |
| 6. Polish | 5 | 0 | 5 | 1 |
| **Total** | **33** | **13** | **29** | **1** |

**Key invariant:** Zero changes to `frontend/src/api/*`, `frontend/src/stores/auth.ts`, `frontend/src/types/*`, `frontend/src/utils/*`, or any backend code.
