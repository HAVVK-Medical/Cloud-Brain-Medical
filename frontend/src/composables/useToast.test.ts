import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useToast } from './useToast';

describe('useToast', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
  });

  it('adds success, error, and warning toasts and removes them after timeout', () => {
    const toast = useToast();

    toast.success('saved');
    toast.error('failed');
    toast.warning('check');

    expect(toast.toasts.value.map((item) => item.tone)).toEqual(['success', 'error', 'warning']);
    expect(toast.toasts.value.map((item) => item.text)).toEqual(['saved', 'failed', 'check']);

    vi.advanceTimersByTime(2999);
    expect(toast.toasts.value).toHaveLength(3);

    vi.advanceTimersByTime(1);
    expect(toast.toasts.value).toEqual([]);
  });
});
