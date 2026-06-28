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
