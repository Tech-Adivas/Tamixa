"use client";

import { useEffect, useState, useCallback } from "react";
import { api } from "@/lib/api";
import { mockApi } from "@/lib/mock-api";
import type { ParentSummary, ParentDetail, PagedResponse, CreateParentRequest, UpdateParentRequest } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { Users, Ban, CheckCircle, Plus, Pencil, Trash2 } from "lucide-react";
import { useActionResult } from "@/contexts/action-result-context";

const PAGE_SIZE = 10;
const STATUS_OPTIONS = [
  { value: "ALL", label: "All statuses" },
  { value: "ACTIVE", label: "Active" },
  { value: "SUSPENDED", label: "Suspended" },
  { value: "PENDING", label: "Pending" },
];

const ROLE_OPTIONS = [
  { value: "PARENT", label: "Parent" },
  { value: "SUPER_ADMIN", label: "Super Admin" },
  { value: "REVENUE_ANALYST", label: "Revenue Analyst" },
  { value: "CONTENT_MANAGER", label: "Content Manager" },
  { value: "SUPPORT", label: "Support" },
];

export default function ParentsPage() {
  const { showSuccess, showError } = useActionResult();
  const [data, setData] = useState<PagedResponse<ParentSummary> | null>(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [suspendingId, setSuspendingId] = useState<number | null>(null);
  const [unsuspendingId, setUnsuspendingId] = useState<number | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createSubmitting, setCreateSubmitting] = useState(false);
  const [createForm, setCreateForm] = useState<CreateParentRequest>({ email: "", password: "", phone: "", role: "PARENT" });
  const [editId, setEditId] = useState<number | null>(null);
  const [editDetail, setEditDetail] = useState<ParentDetail | null>(null);
  const [editForm, setEditForm] = useState<UpdateParentRequest>({});
  const [editSubmitting, setEditSubmitting] = useState(false);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);
  const [suspendConfirmId, setSuspendConfirmId] = useState<number | null>(null);
  const [suspendSubmitting, setSuspendSubmitting] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    if (mockApi.useMock()) {
      setData(
        mockApi.getMockParents(page, PAGE_SIZE, search || undefined, statusFilter !== "ALL" ? statusFilter : undefined)
      );
      setLoading(false);
      return;
    }
    api.admin
      .getParents(page, PAGE_SIZE, search || undefined, statusFilter !== "ALL" ? statusFilter : undefined)
      .then((res) => {
        setData({
          ...res,
          content: res.content.map((row) => ({
            ...row,
            name: row.name ?? row.email.split("@")[0],
            plan: row.plan ?? "—",
            status: row.status ?? "ACTIVE",
          })),
        });
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [page, search, statusFilter]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setSearch(searchInput);
    setPage(0);
  };

  const handleSuspend = async (parentId: number) => {
    setSuspendingId(parentId);
    try {
      await api.admin.suspendParent(parentId);
      showSuccess("Account suspended", "The account has been suspended.");
      setSuspendConfirmId(null);
      load();
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Unable to suspend the account.";
      showError("Suspend failed", msg);
      throw e;
    } finally {
      setSuspendingId(null);
    }
  };

  const handleSuspendConfirm = async () => {
    if (suspendConfirmId == null) return;
    setSuspendSubmitting(true);
    try {
      await handleSuspend(suspendConfirmId);
    } finally {
      setSuspendSubmitting(false);
    }
  };

  const handleUnsuspend = async (parentId: number) => {
    setUnsuspendingId(parentId);
    try {
      await api.admin.unsuspendParent(parentId);
      showSuccess("Account reactivated", "The account has been unsuspended.");
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Unable to reactivate the account.";
      showError("Unsuspend failed", msg);
    } finally {
      setUnsuspendingId(null);
      load();
    }
  };

  const handleCreateOpen = () => {
    setCreateForm({ email: "", password: "", phone: "", role: "PARENT" });
    setCreateOpen(true);
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createForm.email?.trim() || !createForm.password?.trim()) {
      showError("Validation", "Email and password are required.");
      return;
    }
    if (createForm.password.length < 8) {
      showError("Validation", "Password must be at least 8 characters.");
      return;
    }
    setCreateSubmitting(true);
    try {
      await api.admin.createParent({
        email: createForm.email.trim(),
        password: createForm.password,
        phone: createForm.phone?.trim() || undefined,
        role: createForm.role || "PARENT",
      });
      showSuccess("Parent created", "The parent account has been created.");
      setCreateOpen(false);
      load();
    } catch (e) {
      showError("Create failed", e instanceof Error ? e.message : "Failed to create parent.");
    } finally {
      setCreateSubmitting(false);
    }
  };

  const handleEditOpen = async (row: ParentSummary) => {
    if (mockApi.useMock()) {
      setEditDetail({
        id: row.id,
        email: row.email,
        role: row.role,
        status: row.status ?? "ACTIVE",
        plan: row.plan ?? "FREE",
        phone: null,
        createdAt: row.createdAt,
        suspendedAt: null,
      });
      setEditForm({ email: row.email, phone: undefined, role: row.role });
    } else {
      try {
        const detail = await api.admin.getParent(row.id);
        setEditDetail(detail);
        setEditForm({ email: detail.email, phone: detail.phone ?? undefined, role: detail.role });
      } catch {
        showError("Load failed", "Could not load parent details.");
        return;
      }
    }
    setEditId(row.id);
  };

  const handleEditClose = () => {
    setEditId(null);
    setEditDetail(null);
    setEditForm({});
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (editId == null) return;
    setEditSubmitting(true);
    try {
      await api.admin.updateParent(editId, {
        email: editForm.email?.trim() || undefined,
        phone: editForm.phone !== undefined ? editForm.phone.trim() : undefined,
        role: editForm.role || undefined,
      });
      showSuccess("Parent updated", "The parent account has been updated.");
      handleEditClose();
      load();
    } catch (e) {
      showError("Update failed", e instanceof Error ? e.message : "Failed to update parent.");
    } finally {
      setEditSubmitting(false);
    }
  };

  const handleDeleteClick = (row: ParentSummary) => setDeleteId(row.id);

  const handleDeleteConfirm = async () => {
    if (deleteId == null) return;
    setDeleteSubmitting(true);
    try {
      await api.admin.deleteParent(deleteId);
      showSuccess("Parent deleted", "The parent account has been removed.");
      setDeleteId(null);
      load();
    } catch (e) {
      showError("Delete failed", e instanceof Error ? e.message : "Cannot delete parent (e.g. has linked children).");
    } finally {
      setDeleteSubmitting(false);
    }
  };

  const deleteRow = data?.content.find((r) => r.id === deleteId);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Parent management"
        description="View, search, and manage parent accounts. Suspend access when needed."
      />
      <Card>
        <CardHeader className="card-header-responsive space-y-0 pb-2">
          <CardTitle>Parents</CardTitle>
          <div className="filters-row">
            <Button type="button" size="sm" onClick={handleCreateOpen}>
              <Plus className="mr-1 h-3 w-3" />
              Add parent
            </Button>
            <form onSubmit={handleSearch} className="flex min-w-0 flex-1 flex-wrap items-center gap-2 sm:flex-initial">
              <Input
                placeholder="Search by email…"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="min-w-0 flex-1 sm:w-48"
              />
              <Button type="submit" size="sm">
                Search
              </Button>
            </form>
            <Select
              value={statusFilter}
              onValueChange={(v) => {
                setStatusFilter(v);
                setPage(0);
              }}
            >
              <SelectTrigger className="w-full min-w-[120px] sm:w-[140px]">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                {STATUS_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent>
          {error && (
            <p className="mb-4 text-sm text-destructive">{error}</p>
          )}
          {loading ? (
            <div className="space-y-2">
              <Skeleton className="h-10 w-full" />
              {[1, 2, 3, 4, 5].map((i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : data ? (
            <>
              {data.content.length === 0 ? (
                <EmptyState
                  icon={Users}
                  title="No parents found"
                  description="Try adjusting search or filters."
                />
              ) : (
                <>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Name</TableHead>
                        <TableHead>Email</TableHead>
                        <TableHead>Plan</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead>Created</TableHead>
                        <TableHead className="w-[180px]">Actions</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {data.content.map((row) => (
                        <TableRow key={row.id}>
                          <TableCell className="font-medium">
                            {row.name ?? row.email.split("@")[0]}
                          </TableCell>
                          <TableCell>{row.email}</TableCell>
                          <TableCell>{row.plan ?? "—"}</TableCell>
                          <TableCell>
                            <Badge
                              variant={
                                row.status === "SUSPENDED"
                                  ? "destructive"
                                  : row.status === "PENDING"
                                    ? "secondary"
                                    : "default"
                              }
                            >
                              {row.status ?? "ACTIVE"}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            {new Date(row.createdAt).toLocaleString()}
                          </TableCell>
                          <TableCell>
                            <div className="flex flex-wrap gap-1">
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => handleEditOpen(row)}
                                title="Edit"
                              >
                                <Pencil className="h-3 w-3" />
                              </Button>
                              <Button
                                variant="outline"
                                size="sm"
                                className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                                onClick={() => handleDeleteClick(row)}
                                title="Delete"
                              >
                                <Trash2 className="h-3 w-3" />
                              </Button>
                              {row.status === "SUSPENDED" ? (
                                <Button
                                  variant="outline"
                                  size="sm"
                                  className="text-green-600 hover:bg-green-50 hover:text-green-700"
                                  disabled={unsuspendingId === row.id}
                                  onClick={() => handleUnsuspend(row.id)}
                                  title={unsuspendingId === row.id ? "Reactivating…" : "Unsuspend"}
                                >
                                  <CheckCircle className="h-3 w-3" />
                                </Button>
                              ) : (
                                <Button
                                  variant="outline"
                                  size="sm"
                                  className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                                  disabled={suspendingId === row.id}
                                  onClick={() => setSuspendConfirmId(row.id)}
                                  title={suspendingId === row.id ? "Suspending…" : "Suspend"}
                                >
                                  <Ban className="h-3 w-3" />
                                </Button>
                              )}
                            </div>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <div className="pagination-row mt-4">
                    <p className="text-sm text-muted-foreground">
                      {data.totalElements} total · page {data.page + 1} of{" "}
                      {data.totalPages || 1}
                    </p>
                    <div className="flex gap-2">
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={data.first}
                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                      >
                        Previous
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={data.last}
                        onClick={() => setPage((p) => p + 1)}
                      >
                        Next
                      </Button>
                    </div>
                  </div>
                </>
              )}
            </>
          ) : null}
        </CardContent>
      </Card>

      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add parent</DialogTitle>
            <DialogDescription>Create a new parent account. Email and password are required.</DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreateSubmit} className="space-y-4">
            <div>
              <label className="text-sm font-medium">Email</label>
              <Input
                type="email"
                value={createForm.email}
                onChange={(e) => setCreateForm((f) => ({ ...f, email: e.target.value }))}
                placeholder="parent@example.com"
                className="mt-1"
                required
              />
            </div>
            <div>
              <label className="text-sm font-medium">Password</label>
              <Input
                type="password"
                value={createForm.password}
                onChange={(e) => setCreateForm((f) => ({ ...f, password: e.target.value }))}
                placeholder="Min 8 characters"
                className="mt-1"
                minLength={8}
                required
              />
            </div>
            <div>
              <label className="text-sm font-medium">Phone (optional)</label>
              <Input
                type="text"
                value={createForm.phone ?? ""}
                onChange={(e) => setCreateForm((f) => ({ ...f, phone: e.target.value }))}
                placeholder="+1234567890"
                className="mt-1"
              />
            </div>
            <div>
              <label className="text-sm font-medium">Role</label>
              <Select
                value={createForm.role ?? "PARENT"}
                onValueChange={(v) => setCreateForm((f) => ({ ...f, role: v }))}
              >
                <SelectTrigger className="mt-1">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {ROLE_OPTIONS.map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setCreateOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={createSubmitting}>
                {createSubmitting ? "Creating…" : "Create"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={editId != null} onOpenChange={(open) => !open && handleEditClose()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit parent</DialogTitle>
            <DialogDescription>Update email, phone, or role. Leave fields blank to keep current values.</DialogDescription>
          </DialogHeader>
          {editDetail && (
            <form onSubmit={handleEditSubmit} className="space-y-4">
              <div>
                <label className="text-sm font-medium">Email</label>
                <Input
                  type="email"
                  value={editForm.email ?? editDetail.email}
                  onChange={(e) => setEditForm((f) => ({ ...f, email: e.target.value }))}
                  placeholder="parent@example.com"
                  className="mt-1"
                />
              </div>
              <div>
                <label className="text-sm font-medium">Phone (optional)</label>
                <Input
                  type="text"
                  value={editForm.phone ?? editDetail.phone ?? ""}
                  onChange={(e) => setEditForm((f) => ({ ...f, phone: e.target.value }))}
                  placeholder="+1234567890"
                  className="mt-1"
                />
              </div>
              <div>
                <label className="text-sm font-medium">Role</label>
                <Select
                  value={editForm.role ?? editDetail.role}
                  onValueChange={(v) => setEditForm((f) => ({ ...f, role: v }))}
                >
                  <SelectTrigger className="mt-1">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {ROLE_OPTIONS.map((opt) => (
                      <SelectItem key={opt.value} value={opt.value}>
                        {opt.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <DialogFooter>
                <Button type="button" variant="outline" onClick={handleEditClose}>
                  Cancel
                </Button>
                <Button type="submit" disabled={editSubmitting}>
                  {editSubmitting ? "Saving…" : "Save"}
                </Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={suspendConfirmId != null}
        onOpenChange={(open) => !open && setSuspendConfirmId(null)}
        title="Suspend account"
        description={
          suspendConfirmId != null
            ? `Suspend access for this parent? They will not be able to sign in until unsuspended.`
            : "Suspend this account?"
        }
        confirmLabel="Suspend"
        variant="destructive"
        loading={suspendSubmitting}
        onConfirm={handleSuspendConfirm}
      />

      <Dialog open={deleteId != null} onOpenChange={(open) => !open && setDeleteId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete parent</DialogTitle>
            <DialogDescription>
              {deleteRow
                ? `Delete the account for ${deleteRow.email}? This cannot be undone. Delete is only allowed when the parent has no linked children.`
                : "Confirm delete."}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setDeleteId(null)}>
              Cancel
            </Button>
            <Button
              type="button"
              variant="destructive"
              disabled={deleteSubmitting}
              onClick={handleDeleteConfirm}
            >
              {deleteSubmitting ? "Deleting…" : "Delete"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
