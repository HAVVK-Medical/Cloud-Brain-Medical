import { computed, reactive, ref, watch, type ComputedRef } from 'vue';

export function usePagination<T>(source: ComputedRef<T[]>, pageSize = 8) {
  const page = ref(1);
  const total = computed(() => source.value.length);
  const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));
  const pagedItems = computed(() => {
    const start = (page.value - 1) * pageSize;
    return source.value.slice(start, start + pageSize);
  });

  watch(pageCount, (count) => {
    if (page.value > count) {
      page.value = count;
    }
    if (page.value < 1) {
      page.value = 1;
    }
  }, { immediate: true });

  function setPage(nextPage: number) {
    page.value = Math.min(Math.max(1, Math.floor(nextPage)), pageCount.value);
  }

  function resetPage() {
    page.value = 1;
  }

  return reactive({
    page,
    total,
    pageCount,
    pagedItems,
    pageSize,
    setPage,
    resetPage,
  });
}
