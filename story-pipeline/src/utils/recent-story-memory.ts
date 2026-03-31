import type { StoryOutput } from "../schemas/story-output.schema.js";
import type { RecentStoryPatterns } from "../types/pipeline-input.js";

export interface RecentStoryMemoryOptions {
  maxOpenings?: number;
  maxMorals?: number;
  maxNames?: number;
  maxPlots?: number;
}

const DEFAULT_OPTS: Required<RecentStoryMemoryOptions> = {
  maxOpenings: 30,
  maxMorals: 40,
  maxNames: 60,
  maxPlots: 25,
};

/**
 * Sliding-window memory for bulk generation: feed patterns into planner input.
 */
export class RecentStoryMemory {
  private openings: string[] = [];
  private morals: string[] = [];
  private names: Set<string> = new Set();
  private plots: string[] = [];
  private readonly opts: Required<RecentStoryMemoryOptions>;

  constructor(opts: RecentStoryMemoryOptions = {}) {
    this.opts = { ...DEFAULT_OPTS, ...opts };
  }

  /** First ~240 chars of story_text after stripping markers roughly = opening */
  record(story: StoryOutput, plotSummary?: string): void {
    const opening = story.story_text.replace(/\[[^\]]+\]/g, "").slice(0, 240).trim();
    if (opening) {
      this.openings.push(opening);
      if (this.openings.length > this.opts.maxOpenings) this.openings.shift();
    }
    if (story.moral) {
      this.morals.push(story.moral.trim());
      if (this.morals.length > this.opts.maxMorals) this.morals.shift();
    }
    const titleWords = story.title.split(/\s+/).filter((w) => w.length > 2);
    titleWords.forEach((w) => this.names.add(w.slice(0, 48)));
    if (this.names.size > this.opts.maxNames) {
      const arr = [...this.names];
      this.names = new Set(arr.slice(-this.opts.maxNames));
    }
    if (plotSummary?.trim()) {
      this.plots.push(plotSummary.trim().slice(0, 200));
      if (this.plots.length > this.opts.maxPlots) this.plots.shift();
    }
  }

  getPatterns(): RecentStoryPatterns {
    return {
      openings: [...this.openings],
      morals: [...this.morals],
      characterNames: [...this.names],
      plotSummaries: [...this.plots],
    };
  }

  clear(): void {
    this.openings = [];
    this.morals = [];
    this.names.clear();
    this.plots = [];
  }
}
