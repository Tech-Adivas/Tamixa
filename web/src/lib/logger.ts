/**
 * Centralized logging for Araro web app.
 * Provides structured logging for API errors and runtime errors.
 * Can be extended to ship logs to Sentry or similar.
 *
 * Security: never log PII (emails, phones, child names). Use IDs for traceability.
 */

export type LogLevel = "debug" | "info" | "warn" | "error";

const LOG_LEVEL_PRIORITY: Record<LogLevel, number> = {
  debug: 0,
  info: 1,
  warn: 2,
  error: 3,
};

let minimumLevel: LogLevel = "debug";

export function setLogLevel(level: LogLevel) {
  minimumLevel = level;
}

function shouldLog(level: LogLevel): boolean {
  return LOG_LEVEL_PRIORITY[level] >= LOG_LEVEL_PRIORITY[minimumLevel];
}

function formatMessage(scope: string, message: string, meta?: Record<string, unknown>): string {
  const metaStr = meta ? ` ${JSON.stringify(meta)}` : "";
  return `[Araro/${scope}] ${message}${metaStr}`;
}

export const logger = {
  debug(scope: string, message: string, meta?: Record<string, unknown>) {
    if (shouldLog("debug")) {
      console.debug(formatMessage(scope, message, meta));
    }
  },
  info(scope: string, message: string, meta?: Record<string, unknown>) {
    if (shouldLog("info")) {
      console.info(formatMessage(scope, message, meta));
    }
  },
  warn(scope: string, message: string, meta?: Record<string, unknown>) {
    if (shouldLog("warn")) {
      console.warn(formatMessage(scope, message, meta));
    }
  },
  error(scope: string, message: string, error?: unknown, meta?: Record<string, unknown>) {
    if (shouldLog("error")) {
      const errMsg = error instanceof Error ? error.message : String(error);
      const stack = error instanceof Error ? error.stack : undefined;
      console.error(formatMessage(scope, `${message}: ${errMsg}`, { ...meta, stack }));
    }
  },
};
