/// <reference types="vite/client" />

/* eslint-disable @typescript-eslint/no-unused-vars -- ImportMetaEnv merges with Vite client types for import.meta.env */
interface ImportMetaEnv {
  /** Pipe-separated lines shown under Support & safety (optional; no HTML). */
  readonly VITE_COMPLIANCE_RESOURCE_LINES?: string;
}

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
