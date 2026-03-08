"use client";

import { useEffect, useState, useCallback } from "react";
import { api } from "@/lib/api";
import type { AdminUser, PagedResponse, ParentSummary } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/empty-state";
import { UserCog, Plus } from "lucide-react";
import { useActionResult } from "@/contexts/action-result-context";

const PAGE_SIZE = 20;
const ADMIN_ROLES = [
  { value: "SUPER_ADMIN", label: "Super Admin" },
  { value: "REVENUE_ANALYST", label: "Revenue Analyst" },
  { value: "CONTENT_MANAGER", label: "Content Manager" },
  { value: "SUPPORT", label: "Support" },
  { value: "PARENT", label: "Revoke (Parent)" },
];

export default function UsersPage() {
  const { showSuccess, showError } = useActionResult();
  const [data, setData] = useState<PagedResponse<AdminUser> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [addParentId, setAddParentId] = useState("");
  const [addRole, setAddRole] = useState("REVENUE_ANALYST");
  const [addSubmitting, setAddSubmitting] = useState(false);
  const [parentsSearch, setParentsSearch] = useState("");
  const [parentsResult, setParentsResult] = useState<ParentSummary[]>([]);
  const [updatingId, setUpdatingId] = useState<number | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    api.admin
      .getAdminUsers(page, PAGE_SIZE)
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [page]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!addOpen || parentsSearch.length < 2) {
      setParentsResult([]);
      return;
    }
    api.admin
      .getParents(0, 20, parentsSearch)
      .then((r) => setParentsResult(r.content.filter((p) => p.role === "PARENT")))
      .catch(() => setParentsResult([]));
  }, [addOpen, parentsSearch]);

  const handleAdd = async () => {
    const parentId = parseInt(addParentId, 10);
    if (isNaN(parentId) || parentId < 1) {
      showError("Invalid parent", "Enter a valid parent ID.");
      return;
    }
    setAddSubmitting(true);
    try {
      await api.admin.addAdminUser(parentId, addRole);
      showSuccess("Admin added", "User has been assigned admin role.");
      setAddOpen(false);
      setAddParentId("");
      setAddRole("REVENUE_ANALYST");
      load();
    } catch (e) {
      showError("Add failed", e instanceof Error ? e.message : "Unable to add admin user.");
    } finally {
      setAddSubmitting(false);
    }
  };

  const handleAddByParent = async (parent: ParentSummary) => {
    setAddSubmitting(true);
    try {
      await api.admin.addAdminUser(parent.id, addRole);
      showSuccess("Admin added", `${parent.email} has been assigned admin role.`);
      setAddOpen(false);
      setAddParentId("");
      setParentsSearch("");
      load();
    } catch (e) {
      showError("Add failed", e instanceof Error ? e.message : "Unable to add admin user.");
    } finally {
      setAddSubmitting(false);
    }
  };

  const handleUpdateRole = async (userId: number, role: string) => {
    setUpdatingId(userId);
    try {
      await api.admin.updateAdminRole(userId, role);
      showSuccess("Role updated", "User role has been updated.");
      load();
    } catch (e) {
      showError("Update failed", e instanceof Error ? e.message : "Unable to update role.");
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-foreground">
          User management
        </h1>
        <p className="mt-1 text-muted-foreground">
          Add and manage admin users. Assign roles to control dashboard access. Revenue Analytics is visible only to Super Admin and Revenue Analyst.
        </p>
      </div>
      <Card>
        <CardHeader className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between space-y-0 pb-2">
          <CardTitle>Admin users</CardTitle>
          <Button size="sm" onClick={() => setAddOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Add admin
          </Button>
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
                  icon={UserCog}
                  title="No admin users"
                  description="Add users from the Parents list to grant admin access."
                />
              ) : (
                <>
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Email</TableHead>
                        <TableHead>Role</TableHead>
                        <TableHead>Created</TableHead>
                        <TableHead className="w-[200px]">Change role</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {data.content.map((row) => (
                        <TableRow key={row.id}>
                          <TableCell className="font-medium">{row.email}</TableCell>
                          <TableCell>
                            <Badge variant="secondary">{row.role}</Badge>
                          </TableCell>
                          <TableCell>
                            {new Date(row.createdAt).toLocaleString()}
                          </TableCell>
                          <TableCell>
                            <Select
                              value={row.role}
                              onValueChange={(v) => handleUpdateRole(row.id, v)}
                              disabled={updatingId === row.id}
                            >
                              <SelectTrigger className="w-[180px]">
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                {ADMIN_ROLES.map((opt) => (
                                  <SelectItem key={opt.value} value={opt.value}>
                                    {opt.label}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <div className="mt-4 flex items-center justify-between">
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

      <Dialog open={addOpen} onOpenChange={setAddOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add admin user</DialogTitle>
            <DialogDescription>
              Promote an existing parent to admin. Find the parent in Parent management, then enter their ID or search by email below.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Parent ID</label>
              <Input
                type="number"
                placeholder="e.g. 123"
                value={addParentId}
                onChange={(e) => setAddParentId(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Or search by email</label>
              <Input
                placeholder="Search parents…"
                value={parentsSearch}
                onChange={(e) => setParentsSearch(e.target.value)}
              />
              {parentsResult.length > 0 && (
                <div className="mt-2 max-h-40 overflow-y-auto rounded-md border p-2">
                  {parentsResult.slice(0, 10).map((p) => (
                    <button
                      key={p.id}
                      type="button"
                      className="flex w-full items-center justify-between rounded px-2 py-1.5 text-left text-sm hover:bg-muted"
                      onClick={() => {
                        setAddParentId(String(p.id));
                        handleAddByParent(p);
                      }}
                    >
                      <span>{p.email}</span>
                      <span className="text-muted-foreground">ID: {p.id}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">Role</label>
              <Select value={addRole} onValueChange={setAddRole}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {ADMIN_ROLES.filter((r) => r.value !== "PARENT").map((opt) => (
                    <SelectItem key={opt.value} value={opt.value}>
                      {opt.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAddOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleAdd} disabled={addSubmitting}>
              {addSubmitting ? "Adding…" : "Add"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
