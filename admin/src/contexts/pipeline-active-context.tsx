"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import { api } from "@/lib/api";
import { adminStoryLanguageLabel } from "@/lib/library-story-workflow";

const POLL_INTERVAL_MS = 4000;
const OPTIMISTIC_BANNER_MS = 120000;

export type ActivePipelineStory = { storyId: number; language: string; ageMinutes?: number };

type PipelineActiveContextValue = {
  isPipelineActive: boolean;
  activeStories: ActivePipelineStory[];
  refresh: () => Promise<void>;
  /** Call when regenerate/generate is triggered so banner stays visible until backend reports active. */
  registerTriggered: (storyId: number) => void;
};

const PipelineActiveContext = createContext<PipelineActiveContextValue>({
  isPipelineActive: false,
  activeStories: [],
  refresh: async () => {},
  registerTriggered: () => {},
});

export function getLanguageLabel(code: string): string {
  return adminStoryLanguageLabel(code);
}

export function PipelineActiveProvider({
  children,
  userRole,
}: {
  children: React.ReactNode;
  userRole: string | null;
}) {
  const [isPipelineActive, setIsPipelineActive] = useState(false);
  const [activeStories, setActiveStories] = useState<ActivePipelineStory[]>([]);
  const [optimisticStoryIds, setOptimisticStoryIds] = useState<Map<number, number>>(new Map());

  const registerTriggered = useCallback((storyId: number) => {
    setOptimisticStoryIds((prev) => {
      const next = new Map(prev);
      next.set(storyId, Date.now());
      return next;
    });
  }, []);

  const refresh = useCallback(async () => {
    if (!userRole) return;
    try {
      const res = await api.admin.getPipelineActiveNow();
      setIsPipelineActive(res?.active ?? false);
      setActiveStories(
        res?.activeStories?.map((s) => ({ storyId: s.storyId, language: s.language, ageMinutes: s.ageMinutes ?? 0 })) ?? []
      );
    } catch {
      setIsPipelineActive(false);
      setActiveStories([]);
    }
  }, [userRole]);

  useEffect(() => {
    const now = Date.now();
    const backendIds = new Set(activeStories.map((s) => s.storyId));
    setOptimisticStoryIds((prev) => {
      if (prev.size === 0) return prev;
      const next = new Map(prev);
      for (const [sid, at] of prev) {
        if (backendIds.has(sid) || now - at >= OPTIMISTIC_BANNER_MS) next.delete(sid);
      }
      return next.size === prev.size ? prev : next;
    });
  }, [activeStories]);

  useEffect(() => {
    if (!userRole) return;
    refresh();
    // Burst poll on mount to catch pipeline that just started
    const t1 = setTimeout(refresh, 1000);
    const t2 = setTimeout(refresh, 2500);
    const id = setInterval(() => {
      if (document.visibilityState === "visible") refresh();
    }, POLL_INTERVAL_MS);
    const onVisibility = () => {
      if (document.visibilityState === "visible") refresh();
    };
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      clearTimeout(t1);
      clearTimeout(t2);
      clearInterval(id);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [userRole, refresh]);

  const showBanner = isPipelineActive || optimisticStoryIds.size > 0;
  const effectiveStories =
    activeStories.length > 0
      ? activeStories
      : Array.from(optimisticStoryIds.keys()).map((storyId) => ({
          storyId,
          language: "",
          ageMinutes: 0,
        }));

  return (
    <PipelineActiveContext.Provider
      value={{
        isPipelineActive: showBanner,
        activeStories: effectiveStories,
        refresh,
        registerTriggered,
      }}
    >
      {children}
    </PipelineActiveContext.Provider>
  );
}

export function usePipelineActive() {
  return useContext(PipelineActiveContext);
}
