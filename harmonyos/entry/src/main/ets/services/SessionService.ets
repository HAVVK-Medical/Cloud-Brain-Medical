import preferences from '@ohos.data.preferences';
import type common from '@ohos.app.ability.common';
import { SESSION_STORAGE_KEY } from '../common/Config';
import type { LoginResponse } from '../models/ApiModels';

const PREFERENCES_NAME = 'havvk_medical';

export class SessionService {
  private context: common.Context;

  constructor(context: common.Context) {
    this.context = context;
  }

  async save(session: LoginResponse): Promise<void> {
    const store = await preferences.getPreferences(this.context, PREFERENCES_NAME);
    await store.put(SESSION_STORAGE_KEY, JSON.stringify(session));
    await store.flush();
  }

  async load(): Promise<LoginResponse | null> {
    const store = await preferences.getPreferences(this.context, PREFERENCES_NAME);
    const raw = await store.get(SESSION_STORAGE_KEY, '');
    if (!raw || typeof raw !== 'string') {
      return null;
    }

    try {
      return JSON.parse(raw) as LoginResponse;
    } catch (error) {
      await this.clear();
      return null;
    }
  }

  async clear(): Promise<void> {
    const store = await preferences.getPreferences(this.context, PREFERENCES_NAME);
    await store.delete(SESSION_STORAGE_KEY);
    await store.flush();
  }
}
