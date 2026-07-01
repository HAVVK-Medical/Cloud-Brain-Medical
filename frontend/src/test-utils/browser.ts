type StorageMap = Map<string, string>;

export function createMemoryStorage(initial: Record<string, string> = {}) {
  const data: StorageMap = new Map(Object.entries(initial));
  return {
    get length() {
      return data.size;
    },
    clear() {
      data.clear();
    },
    getItem(key: string) {
      return data.has(key) ? data.get(key)! : null;
    },
    key(index: number) {
      return Array.from(data.keys())[index] ?? null;
    },
    removeItem(key: string) {
      data.delete(key);
    },
    setItem(key: string, value: string) {
      data.set(key, String(value));
    },
  } satisfies Storage;
}

export function installBrowserMocks(options: {
  storage?: Storage;
  protocol?: string;
  host?: string;
  pathname?: string;
  assign?: (url: string) => void;
} = {}) {
  const storage = options.storage ?? createMemoryStorage();
  const location = {
    protocol: options.protocol ?? 'http:',
    host: options.host ?? 'localhost:5173',
    pathname: options.pathname ?? '/',
    assign: options.assign ?? (() => undefined),
  };
  const windowMock = {
    localStorage: storage,
    location,
  };

  Object.defineProperty(globalThis, 'window', {
    configurable: true,
    writable: true,
    value: windowMock,
  });
  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true,
    writable: true,
    value: storage,
  });

  return { storage, location, windowMock };
}

export function removeBrowserMocks() {
  Reflect.deleteProperty(globalThis, 'window');
  Reflect.deleteProperty(globalThis, 'localStorage');
}
