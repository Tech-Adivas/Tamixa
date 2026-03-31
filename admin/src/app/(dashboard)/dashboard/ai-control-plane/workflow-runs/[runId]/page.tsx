"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useAuth } from "@/contexts/auth-context";
import { api } from "@/lib/api";
import { canManageAiControlPlane, canViewAiControlPlane } from "@/lib/admin-roles";
import type { WorkflowRunDetail } from "@/types/api";
import { PageHeader } from "@/components/layout/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";

export default function WorkflowRunDetailPage() {
  const params = useParams();
  const runId = typeof params.runId === "string" ? params.runId : "";
  const { user } = useAuth();
  const canView = canViewAiControlPlane(user);
  const canManage = canManageAiControlPlane(user);

  const [detail, setDetail] = useState<WorkflowRunDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelBusy, setCancelBusy] = useState(false);

  const load = useCallback(() => {
    if (!runId || !canView) return;
    setLoading(true);
    setError(null);
    api.admin
      .getAiControlPlaneWorkflowRun(runId)
      .then(setDetail)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))
      .finally(() => setLoading(false));
  }, [runId, canView]);

  useEffect(() => {
    load();
  }, [load]);

  const cancel = async () => {
    if (!runId) return;
    setCancelBusy(true);
    setError(null);
    try {
      await api.admin.cancelAiControlPlaneWorkflowRun(runId);
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setCancelBusy(false);
    }
  };

  if (!canView) {
    return (
      <div className="space-y-6">
        <PageHeader title="Workflow run" breadcrumbs={[{ label: "AI control plane", href: "/dashboard/ai-control-plane" }, { label: "Run" }]} />
        <Card>
          <CardContent className="pt-6">
            <p className="text-muted-foreground">You don&apos;t have permission to view this page.</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Workflow run"
        description={runId ? `Run ${runId}` : ""}
        breadcrumbs={[
          { label: "AI control plane", href: "/dashboard/ai-control-plane" },
          { label: "Run detail" },
        ]}
      />

      <div className="flex flex-wrap gap-2">
        <Button variant="outline" size="sm" asChild>
          <Link href="/dashboard/ai-control-plane">Back to control plane</Link>
        </Button>
        {canManage && detail?.status === "IN_PROGRESS" && (
          <Button variant="destructive" size="sm" onClick={cancel} disabled={cancelBusy}>
            {cancelBusy ? "Cancelling…" : "Cancel run"}
          </Button>
        )}
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      {loading && <p className="text-sm text-muted-foreground">Loading…</p>}

      {!loading && detail && (
        <Card>
          <CardHeader>
            <CardTitle className="flex flex-wrap items-center gap-2 text-lg">
              Status
              <Badge variant={detail.status === "IN_PROGRESS" ? "default" : "secondary"}>{detail.status}</Badge>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4 text-sm">
            <div>
              <span className="text-muted-foreground">Current step: </span>
              {detail.currentStepKey ?? "—"}
            </div>
            <div>
              <span className="text-muted-foreground">Started: </span>
              {detail.startedAtIso ?? "—"}
            </div>
            <div>
              <span className="text-muted-foreground">Completed: </span>
              {detail.completedAtIso ?? "—"}
            </div>
            <div>
              <p className="mb-2 text-muted-foreground">Output JSON</p>
              <pre className="max-h-[min(60vh,480px)] overflow-auto rounded-lg border bg-muted/40 p-3 text-xs">
                {detail.output ? JSON.stringify(detail.output, null, 2) : "—"}
              </pre>
            </div>
          </CardContent>
        </Card>
      )}

      {!loading && !detail && !error && runId && (
        <p className="text-sm text-muted-foreground">Run not found.</p>
      )}
    </div>
  );
}
