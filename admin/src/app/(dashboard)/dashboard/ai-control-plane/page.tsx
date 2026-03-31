"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/contexts/auth-context";
import { api } from "@/lib/api";
import { canManageAiControlPlane, canViewAiControlPlane } from "@/lib/admin-roles";
import type {
  AiProjectSummary,
  AiWorkflowRunSummary,
  AiWorkflowSummary,
  PagedResponse,
  PromptVersion,
} from "@/types/api";
import { PageHeader } from "@/components/layout/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { cn } from "@/lib/utils";

export default function AiControlPlanePage() {
  const { user } = useAuth();
  const canView = canViewAiControlPlane(user);
  const canManage = canManageAiControlPlane(user);

  const [projects, setProjects] = useState<AiProjectSummary[]>([]);
  const [projectCode, setProjectCode] = useState("tamixa");
  const [workflows, setWorkflows] = useState<AiWorkflowSummary[]>([]);
  const [runsPage, setRunsPage] = useState<PagedResponse<AiWorkflowRunSummary> | null>(null);
  const [runsPageIndex, setRunsPageIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const [executeOpen, setExecuteOpen] = useState(false);
  const [executeWorkflowKey, setExecuteWorkflowKey] = useState("");
  const [executeEntityType, setExecuteEntityType] = useState("story_request");
  const [executeEntityId, setExecuteEntityId] = useState("");
  const [executeInputJson, setExecuteInputJson] = useState("{}");
  const [executeBusy, setExecuteBusy] = useState(false);
  const [executeMessage, setExecuteMessage] = useState<string | null>(null);

  const [resolveAssetKey, setResolveAssetKey] = useState("");
  const [resolveVersion, setResolveVersion] = useState("");
  const [resolveResult, setResolveResult] = useState<PromptVersion | null>(null);
  const [resolveBusy, setResolveBusy] = useState(false);

  const [publishAssetKey, setPublishAssetKey] = useState("");
  const [publishContent, setPublishContent] = useState("");
  const [publishBusy, setPublishBusy] = useState(false);

  const [approveAssetId, setApproveAssetId] = useState("");
  const [approveVersion, setApproveVersion] = useState("");
  const [approveBusy, setApproveBusy] = useState(false);

  const loadProjects = useCallback(() => {
    if (!canView) return Promise.resolve();
    return api.admin.getAiControlPlaneProjects().then((list) => {
      setProjects(list);
      if (list.length > 0) {
        setProjectCode((prev) => (list.some((p) => p.code === prev) ? prev : list[0].code));
      }
    });
  }, [canView]);

  const loadWorkflows = useCallback(() => {
    if (!canView || !projectCode.trim()) return;
    api.admin
      .getAiControlPlaneWorkflows(projectCode.trim())
      .then(setWorkflows)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)));
  }, [canView, projectCode]);

  const loadRuns = useCallback(() => {
    if (!canView || !projectCode.trim()) return;
    api.admin
      .getAiControlPlaneWorkflowRuns(projectCode.trim(), runsPageIndex, 15)
      .then(setRunsPage)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)));
  }, [canView, projectCode, runsPageIndex]);

  useEffect(() => {
    if (!canView) return;
    setError(null);
    loadProjects().catch((e) => setError(e instanceof Error ? e.message : String(e)));
  }, [canView, loadProjects]);

  useEffect(() => {
    if (!canView) return;
    loadWorkflows();
  }, [canView, loadWorkflows]);

  useEffect(() => {
    if (!canView) return;
    loadRuns();
  }, [canView, loadRuns]);

  const openExecute = () => {
    setExecuteMessage(null);
    const first = workflows[0]?.workflowKey ?? "";
    setExecuteWorkflowKey(first);
    setExecuteOpen(true);
  };

  const submitExecute = async () => {
    if (!projectCode.trim() || !executeWorkflowKey.trim()) return;
    let input: Record<string, unknown> = {};
    try {
      input = JSON.parse(executeInputJson || "{}") as Record<string, unknown>;
    } catch {
      setExecuteMessage("Input must be valid JSON.");
      return;
    }
    setExecuteBusy(true);
    setExecuteMessage(null);
    try {
      const r = await api.admin.executeAiControlPlaneWorkflow({
        projectCode: projectCode.trim(),
        workflowKey: executeWorkflowKey.trim(),
        entityType: executeEntityType.trim() || "story_request",
        entityId: executeEntityId.trim() || null,
        input,
      });
      setExecuteMessage(`Started run ${r.workflowRunId} (${r.status}).`);
      setExecuteOpen(false);
      loadRuns();
    } catch (e) {
      setExecuteMessage(e instanceof Error ? e.message : String(e));
    } finally {
      setExecuteBusy(false);
    }
  };

  const doResolve = async () => {
    if (!resolveAssetKey.trim()) return;
    setResolveBusy(true);
    setResolveResult(null);
    try {
      const v =
        resolveVersion.trim() === "" ? undefined : Number.parseInt(resolveVersion, 10);
      const r = await api.admin.resolveAiControlPlanePrompt(
        projectCode.trim(),
        resolveAssetKey.trim(),
        Number.isNaN(v) ? undefined : v
      );
      setResolveResult(r);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setResolveBusy(false);
    }
  };

  const doPublish = async () => {
    if (!publishAssetKey.trim() || !publishContent.trim()) return;
    setPublishBusy(true);
    try {
      await api.admin.publishAiControlPlanePrompt({
        projectCode: projectCode.trim(),
        assetKey: publishAssetKey.trim(),
        content: publishContent,
      });
      setPublishAssetKey("");
      setPublishContent("");
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setPublishBusy(false);
    }
  };

  const doApprove = async () => {
    if (!approveAssetId.trim() || !approveVersion.trim()) return;
    setApproveBusy(true);
    try {
      await api.admin.approveAiControlPlanePromptVersion(
        approveAssetId.trim(),
        Number.parseInt(approveVersion, 10)
      );
      setApproveAssetId("");
      setApproveVersion("");
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setApproveBusy(false);
    }
  };

  if (!canView) {
    return (
      <div className="space-y-6">
        <PageHeader
          title="AI control plane"
          description="Governance registry and workflow run tracking."
          breadcrumbs
        />
        <Card>
          <CardContent className="pt-6">
            <p className="text-muted-foreground">
              You don&apos;t have permission to view the AI control plane. Ask a super admin to grant{" "}
              <code className="text-xs">VIEW_AI_CONTROL_PLANE</code>.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="AI control plane"
        description="Projects, workflows, and run history. Content managers can view; admins can execute and mutate."
        breadcrumbs
      />

      {error && (
        <p className="text-sm text-destructive" role="alert">
          {error}
        </p>
      )}

      <div className="flex flex-wrap items-end gap-3">
        <div className="space-y-2">
          <Label htmlFor="cp-project">Project</Label>
          <select
            id="cp-project"
            className={cn(
              "flex h-11 min-h-[44px] w-[min(100%,220px)] rounded-xl border-2 border-input bg-transparent px-3 text-sm font-medium"
            )}
            value={projectCode}
            onChange={(e) => {
              setProjectCode(e.target.value);
              setRunsPageIndex(0);
            }}
          >
            {projects.map((p) => (
              <option key={p.id} value={p.code}>
                {p.name} ({p.code})
              </option>
            ))}
            {projects.length === 0 && <option value={projectCode}>{projectCode}</option>}
          </select>
        </div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => {
            setError(null);
            void loadProjects().catch((e) => setError(e instanceof Error ? e.message : String(e)));
            loadWorkflows();
            loadRuns();
          }}
        >
          Refresh
        </Button>
        {canManage && (
          <Button type="button" size="sm" onClick={openExecute} disabled={workflows.length === 0}>
            Execute workflow
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Workflows</CardTitle>
        </CardHeader>
        <CardContent>
          {workflows.length === 0 ? (
            <p className="text-sm text-muted-foreground">No workflows for this project.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Key</TableHead>
                  <TableHead>Name</TableHead>
                  <TableHead>Category</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Version</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {workflows.map((w) => (
                  <TableRow key={w.id}>
                    <TableCell className="font-mono text-xs">{w.workflowKey}</TableCell>
                    <TableCell>{w.name}</TableCell>
                    <TableCell>{w.category}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{w.status}</Badge>
                    </TableCell>
                    <TableCell>{w.version}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0">
          <CardTitle className="text-lg">Workflow runs</CardTitle>
          {runsPage && runsPage.totalPages > 1 && (
            <div className="flex gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={runsPage.first}
                onClick={() => setRunsPageIndex((p) => Math.max(0, p - 1))}
              >
                Previous
              </Button>
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={runsPage.last}
                onClick={() => setRunsPageIndex((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </CardHeader>
        <CardContent>
          {!runsPage ? (
            <p className="text-sm text-muted-foreground">Loading…</p>
          ) : runsPage.content.length === 0 ? (
            <p className="text-sm text-muted-foreground">No runs yet.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Workflow</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Step</TableHead>
                  <TableHead>Started</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {runsPage.content.map((r) => (
                  <TableRow key={r.id}>
                    <TableCell className="font-mono text-xs">{r.workflowKey}</TableCell>
                    <TableCell>
                      <Badge variant={r.status === "IN_PROGRESS" ? "default" : "secondary"}>
                        {r.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground text-xs">
                      {r.currentStepKey ?? "—"}
                    </TableCell>
                    <TableCell className="text-xs text-muted-foreground">{r.startedAtIso}</TableCell>
                    <TableCell>
                      <Button variant="link" className="h-auto p-0" asChild>
                        <Link href={`/dashboard/ai-control-plane/workflow-runs/${r.id}`}>View</Link>
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg">Prompt registry</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="space-y-3">
            <p className="text-sm text-muted-foreground">Resolve a prompt asset version (read-only).</p>
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              <div className="space-y-2">
                <Label htmlFor="resolve-key">Asset key</Label>
                <Input
                  id="resolve-key"
                  value={resolveAssetKey}
                  onChange={(e) => setResolveAssetKey(e.target.value)}
                  placeholder="e.g. story.system"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="resolve-ver">Version (optional)</Label>
                <Input
                  id="resolve-ver"
                  value={resolveVersion}
                  onChange={(e) => setResolveVersion(e.target.value)}
                  placeholder="omit = current"
                />
              </div>
              <div className="flex items-end">
                <Button type="button" variant="secondary" onClick={doResolve} disabled={resolveBusy}>
                  {resolveBusy ? "Resolving…" : "Resolve"}
                </Button>
              </div>
            </div>
            {resolveResult && (
              <pre className="mt-2 overflow-x-auto rounded-lg border bg-muted/40 p-3 text-xs">
                {JSON.stringify(resolveResult, null, 2)}
              </pre>
            )}
          </div>

          {canManage && (
            <>
              <div className="space-y-3 border-t pt-6">
                <p className="text-sm text-muted-foreground">Publish a new draft version.</p>
                <div className="grid gap-3 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="pub-key">Asset key</Label>
                    <Input
                      id="pub-key"
                      value={publishAssetKey}
                      onChange={(e) => setPublishAssetKey(e.target.value)}
                    />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="pub-content">Content</Label>
                  <textarea
                    id="pub-content"
                    className="flex min-h-[120px] w-full rounded-xl border-2 border-input bg-transparent px-4 py-2 text-sm"
                    value={publishContent}
                    onChange={(e) => setPublishContent(e.target.value)}
                  />
                </div>
                <Button type="button" onClick={doPublish} disabled={publishBusy}>
                  {publishBusy ? "Publishing…" : "Publish version"}
                </Button>
              </div>

              <div className="space-y-3 border-t pt-6">
                <p className="text-sm text-muted-foreground">Approve a draft version.</p>
                <div className="grid gap-3 sm:grid-cols-3">
                  <div className="space-y-2">
                    <Label htmlFor="ap-asset">Asset ID (UUID)</Label>
                    <Input
                      id="ap-asset"
                      value={approveAssetId}
                      onChange={(e) => setApproveAssetId(e.target.value)}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="ap-ver">Version</Label>
                    <Input
                      id="ap-ver"
                      value={approveVersion}
                      onChange={(e) => setApproveVersion(e.target.value)}
                    />
                  </div>
                  <div className="flex items-end">
                    <Button type="button" variant="secondary" onClick={doApprove} disabled={approveBusy}>
                      {approveBusy ? "Saving…" : "Approve"}
                    </Button>
                  </div>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      <Dialog open={executeOpen} onOpenChange={setExecuteOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Execute workflow</DialogTitle>
          </DialogHeader>
          <div className="grid gap-3 py-2">
            <div className="space-y-2">
              <Label>Workflow</Label>
              <select
                className="flex h-11 w-full rounded-xl border-2 border-input bg-transparent px-3 text-sm"
                value={executeWorkflowKey}
                onChange={(e) => setExecuteWorkflowKey(e.target.value)}
              >
                {workflows.map((w) => (
                  <option key={w.id} value={w.workflowKey}>
                    {w.workflowKey}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="ex-entity-type">Entity type</Label>
              <Input
                id="ex-entity-type"
                value={executeEntityType}
                onChange={(e) => setExecuteEntityType(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="ex-entity-id">Entity ID (optional)</Label>
              <Input
                id="ex-entity-id"
                value={executeEntityId}
                onChange={(e) => setExecuteEntityId(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="ex-json">Input JSON</Label>
              <textarea
                id="ex-json"
                className="flex min-h-[88px] w-full rounded-xl border-2 border-input bg-transparent px-4 py-2 font-mono text-xs"
                value={executeInputJson}
                onChange={(e) => setExecuteInputJson(e.target.value)}
              />
            </div>
            {executeMessage && <p className="text-sm text-destructive">{executeMessage}</p>}
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setExecuteOpen(false)}>
              Cancel
            </Button>
            <Button type="button" onClick={submitExecute} disabled={executeBusy}>
              {executeBusy ? "Starting…" : "Start run"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
