import { computed, nextTick, ref } from 'vue';
import { describe, expect, it } from 'vitest';

import { usePagination } from './usePagination';

describe('usePagination', () => {
  it('calculates page counts and current page items', () => {
    const source = computed(() => [1, 2, 3, 4, 5]);
    const pagination = usePagination(source, 2);

    expect(pagination.total).toBe(5);
    expect(pagination.pageCount).toBe(3);
    expect(pagination.page).toBe(1);
    expect(pagination.pagedItems).toEqual([1, 2]);

    pagination.setPage(3);

    expect(pagination.page).toBe(3);
    expect(pagination.pagedItems).toEqual([5]);
  });

  it('clamps invalid page requests to valid boundaries', () => {
    const source = computed(() => ['a', 'b', 'c']);
    const pagination = usePagination(source, 2);

    pagination.setPage(99);
    expect(pagination.page).toBe(2);

    pagination.setPage(0);
    expect(pagination.page).toBe(1);

    pagination.setPage(1.9);
    expect(pagination.page).toBe(1);
  });

  it('resets and shrinks page when source length decreases', async () => {
    const items = ref([1, 2, 3, 4, 5]);
    const pagination = usePagination(computed(() => items.value), 2);

    pagination.setPage(3);
    expect(pagination.page).toBe(3);

    items.value = [1];
    await nextTick();

    expect(pagination.pageCount).toBe(1);
    expect(pagination.page).toBe(1);

    pagination.setPage(2);
    pagination.resetPage();
    expect(pagination.page).toBe(1);
  });
});
