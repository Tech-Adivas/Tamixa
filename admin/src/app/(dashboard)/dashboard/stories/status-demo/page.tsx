"use client";

/**
 * Story Status System Demo Page
 * 
 * Demonstrates the unified story status and pipeline progress components
 * for the Tamixa Premium UX Overhaul (Task 11).
 * 
 * This page is for development/testing purposes and can be removed in production.
 */

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import {
  StoryStatusBadge,
  StoryStatusIcon,
  PipelineStatusBadges,
  PipelineProgressIndicator,
  type StoryStatus,
} from "@/components/design-system";
import type { PipelineStatusResponse } from "@/types/api";

const STORY_STATUSES: StoryStatus[] = [
  "DRAFT",
  "PUBLISHED",
  "PROCESSING",
  "READY",
  "CHANGES_REQUESTED",
  "REJECTED",
];

const MOCK_PIPELINE_STATUS_COMPLETE: PipelineStatusResponse = {
  ta: "COMPLETED",
  en: "COMPLETED",
  hi: "COMPLETED",
  te: "COMPLETED",
  kn: "COMPLETED",
  ml: "COMPLETED",
  overallStatus: "COMPLETED",
  progress: "100",
};

const MOCK_PIPELINE_STATUS_IN_PROGRESS: PipelineStatusResponse = {
  ta: "COMPLETED",
  en: "COMPLETED",
  hi: "TRANSLATING",
  te: "PENDING",
  kn: "PENDING",
  ml: "PENDING",
  processing: "hi",
  overallStatus: "TRANSLATING_LANGUAGES",
  progress: "33",
};

const MOCK_PIPELINE_STATUS_WITH_ERRORS: PipelineStatusResponse = {
  ta: "COMPLETED",
  en: "COMPLETED",
  hi: "FAILED — Translation service timeout",
  te: "COMPLETED",
  kn: "FAILED — TTS quota exceeded",
  ml: "PENDING",
  overallStatus: "FAILED",
  progress: "50",
  failedLanguagesCount: "2",
};

export default function StatusDemoPage() {
  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Story Status System Demo</h1>
        <p className="text-muted-foreground mt-2">
          Unified status badges and pipeline progress indicators for the admin dashboard
        </p>
      </div>

      {/* Story Status Badges */}
      <Card>
        <CardHeader>
          <CardTitle>Story Status Badges</CardTitle>
          <CardDescription>
            Displays the current lifecycle status of a story with consistent color coding and icons
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* All statuses - Medium size */}
          <div>
            <h3 className="text-sm font-medium mb-3">All Status Types (Medium)</h3>
            <div className="flex flex-wrap gap-3">
              {STORY_STATUSES.map((status) => (
                <StoryStatusBadge key={status} status={status} size="md" />
              ))}
              <StoryStatusBadge status="PUBLISHED" narrationApproved size="md" />
            </div>
          </div>

          {/* Size variations */}
          <div>
            <h3 className="text-sm font-medium mb-3">Size Variations (READY status)</h3>
            <div className="flex flex-wrap items-center gap-3">
              <StoryStatusBadge status="READY" size="sm" />
              <StoryStatusBadge status="READY" size="md" />
              <StoryStatusBadge status="READY" size="lg" />
            </div>
          </div>

          {/* Without icons */}
          <div>
            <h3 className="text-sm font-medium mb-3">Without Icons</h3>
            <div className="flex flex-wrap gap-3">
              {STORY_STATUSES.map((status) => (
                <StoryStatusBadge key={status} status={status} showIcon={false} />
              ))}
            </div>
          </div>

          {/* Compact icons only */}
          <div>
            <h3 className="text-sm font-medium mb-3">Compact Icons (for dense tables)</h3>
            <div className="flex flex-wrap items-center gap-3">
              {STORY_STATUSES.map((status) => (
                <div key={status} className="flex items-center gap-2">
                  <StoryStatusIcon status={status} />
                  <span className="text-sm text-muted-foreground">{status}</span>
                </div>
              ))}
              <div className="flex items-center gap-2">
                <StoryStatusIcon status="PUBLISHED" narrationApproved />
                <span className="text-sm text-muted-foreground">LIVE</span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Pipeline Status Badges */}
      <Card>
        <CardHeader>
          <CardTitle>Pipeline Status Badges</CardTitle>
          <CardDescription>
            Shows per-language translation, rewriting, and TTS processing status
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Complete pipeline */}
          <div>
            <h3 className="text-sm font-medium mb-3">Complete Pipeline</h3>
            <PipelineStatusBadges status={MOCK_PIPELINE_STATUS_COMPLETE} />
          </div>

          {/* In progress */}
          <div>
            <h3 className="text-sm font-medium mb-3">In Progress (Translating Hindi)</h3>
            <PipelineStatusBadges status={MOCK_PIPELINE_STATUS_IN_PROGRESS} />
          </div>

          {/* With errors */}
          <div>
            <h3 className="text-sm font-medium mb-3">With Errors</h3>
            <PipelineStatusBadges status={MOCK_PIPELINE_STATUS_WITH_ERRORS} />
          </div>

          {/* Compact mode */}
          <div>
            <h3 className="text-sm font-medium mb-3">Compact Mode (short language codes)</h3>
            <PipelineStatusBadges status={MOCK_PIPELINE_STATUS_IN_PROGRESS} compact />
          </div>

          {/* Without progress label */}
          <div>
            <h3 className="text-sm font-medium mb-3">Without Progress Label</h3>
            <PipelineStatusBadges status={MOCK_PIPELINE_STATUS_IN_PROGRESS} showProgress={false} />
          </div>
        </CardContent>
      </Card>

      {/* Pipeline Progress Indicator */}
      <Card>
        <CardHeader>
          <CardTitle>Pipeline Progress Indicator</CardTitle>
          <CardDescription>
            Compact completion count for table views
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="flex flex-wrap items-center gap-4">
            <div className="flex flex-col gap-2">
              <span className="text-xs text-muted-foreground">Complete</span>
              <PipelineProgressIndicator status={MOCK_PIPELINE_STATUS_COMPLETE} />
            </div>
            <div className="flex flex-col gap-2">
              <span className="text-xs text-muted-foreground">In Progress</span>
              <PipelineProgressIndicator status={MOCK_PIPELINE_STATUS_IN_PROGRESS} />
            </div>
            <div className="flex flex-col gap-2">
              <span className="text-xs text-muted-foreground">With Errors</span>
              <PipelineProgressIndicator status={MOCK_PIPELINE_STATUS_WITH_ERRORS} />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Usage in Table Context */}
      <Card>
        <CardHeader>
          <CardTitle>Table Context Example</CardTitle>
          <CardDescription>
            How status badges appear in a typical story list table
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b">
                  <th className="px-4 py-3 text-left">ID</th>
                  <th className="px-4 py-3 text-left">Title</th>
                  <th className="px-4 py-3 text-center">Status</th>
                  <th className="px-4 py-3 text-left">Pipeline Progress</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                <tr>
                  <td className="px-4 py-3 font-mono">1234</td>
                  <td className="px-4 py-3">The Brave Fox</td>
                  <td className="px-4 py-3 text-center">
                    <StoryStatusIcon status="PUBLISHED" narrationApproved />
                  </td>
                  <td className="px-4 py-3">
                    <PipelineStatusBadges status={MOCK_PIPELINE_STATUS_COMPLETE} compact />
                  </td>
                </tr>
                <tr>
                  <td className="px-4 py-3 font-mono">1235</td>
                  <td className="px-4 py-3">Ocean Mystery</td>
                  <td className="px-4 py-3 text-center">
                    <StoryStatusIcon status="PROCESSING" />
                  </td>
                  <td className="px-4 py-3">
                    <PipelineStatusBadges status={MOCK_PIPELINE_STATUS_IN_PROGRESS} compact />
                  </td>
                </tr>
                <tr>
                  <td className="px-4 py-3 font-mono">1236</td>
                  <td className="px-4 py-3">Space Journey</td>
                  <td className="px-4 py-3 text-center">
                    <StoryStatusIcon status="DRAFT" />
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-muted-foreground text-xs">—</span>
                  </td>
                </tr>
                <tr>
                  <td className="px-4 py-3 font-mono">1237</td>
                  <td className="px-4 py-3">Mountain Adventure</td>
                  <td className="px-4 py-3 text-center">
                    <StoryStatusIcon status="READY" />
                  </td>
                  <td className="px-4 py-3">
                    <PipelineProgressIndicator status={MOCK_PIPELINE_STATUS_COMPLETE} />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Design Tokens Reference */}
      <Card>
        <CardHeader>
          <CardTitle>Design Tokens Reference</CardTitle>
          <CardDescription>
            Color coding aligned with Tamixa design system
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <h4 className="text-sm font-medium">Story Status Colors</h4>
              <div className="space-y-1 text-xs">
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-muted-foreground/40 bg-muted/30" />
                  <span>DRAFT - Gray (muted)</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-amber-500/60 bg-amber-500/15" />
                  <span>PUBLISHED - Amber (in review)</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-blue-500/60 bg-blue-500/15" />
                  <span>PROCESSING - Blue (active)</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-emerald-500/60 bg-emerald-500/15" />
                  <span>READY - Green (ready)</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-orange-500/60 bg-orange-500/15" />
                  <span>CHANGES_REQUESTED - Orange</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-red-500/60 bg-red-500/15" />
                  <span>REJECTED - Red (error)</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-green-500/60 bg-green-500/15" />
                  <span>LIVE - Green (approved)</span>
                </div>
              </div>
            </div>
            <div className="space-y-2">
              <h4 className="text-sm font-medium">Pipeline Stage Colors</h4>
              <div className="space-y-1 text-xs">
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-muted-foreground/40 bg-muted/30" />
                  <span>PENDING - Gray (not started)</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-orange-500/60 bg-orange-500/15" />
                  <span>TRANSLATING - Orange (active)</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-orange-500/60 bg-orange-500/15" />
                  <span>REWRITING - Orange (active)</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-orange-500/60 bg-orange-500/15" />
                  <span>TTS_PROCESSING - Orange (active)</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-emerald-500/60 bg-emerald-500/15" />
                  <span>COMPLETED - Green (success)</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-4 h-4 rounded border-2 border-red-500/60 bg-red-500/15" />
                  <span>FAILED - Red (error)</span>
                </div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
