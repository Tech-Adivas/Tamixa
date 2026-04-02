/// <reference types="vite/client" />

/** Non-standard browser APIs used only for capability hints (e.g. Chrome). */
declare global {
  interface Navigator {
    readonly deviceMemory?: number;
    readonly connection?: {
      readonly effectiveType?: string;
    };
  }

  interface Performance {
    readonly memory?: {
      readonly usedJSHeapSize: number;
      readonly jsHeapSizeLimit: number;
    };
  }
}

export {};
