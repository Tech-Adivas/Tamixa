"use client";

import { useEffect, useState, useCallback } from "react";
import { api } from "@/lib/api";
import { mockApi } from "@/lib/mock-api";
import type { ParentSummary, PagedResponse } from "@/types/api";
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
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/empty-state";
import { Users, Ban, CheckCircle } from "lucide-react";
import { useActionResult } from "@/contexts/action-result-context";

const PAGE_SIZE = 10;
const STATUS_OPTIONS = [
  { value: "ALL", label: "All statuses" },
  { value: "ACTIVE", label: "Active" },
  { value: "SUSPENDED", label: "Suspended" },
  { value: "PENDING", label: "Pending" },
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
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Unable to suspend the account.";
      showError("Suspend failed", msg);
    } finally {
      setSuspendingId(null);
      load();
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

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-foreground">
          Parent management
        </h1>
        <p className="mt-1 text-muted-foreground">
          View, search, and manage parent accounts. Suspend access when needed.
        </p>
      </div>
      <Card>
        <CardHeader className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between space-y-0 pb-2">
          <CardTitle>Parents</CardTitle>
          <div className="flex flex-wrap items-center gap-2">
            <form onSubmit={handleSearch} className="flex gap-2">
              <Input
                placeholder="Search by email…"
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="w-48"
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
              <SelectTrigger className="w-[140px]">
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
                        <TableHead className="w-[100px]">Actions</TableHead>
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
                            {row.status === "SUSPENDED" ? (
                              <Button
                                variant="outline"
                                size="sm"
                                className="text-green-600 hover:bg-green-50 hover:text-green-700"
                                disabled={unsuspendingId === row.id}
                                onClick={() => handleUnsuspend(row.id)}
                              >
                                <CheckCircle className="mr-1 h-3 w-3" />
                                {unsuspendingId === row.id ? "Reactivating…" : "Unsuspend"}
                              </Button>
                            ) : (
                              <Button
                                variant="outline"
                                size="sm"
                                className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                                disabled={suspendingId === row.id}
                                onClick={() => handleSuspend(row.id)}
                              >
                                <Ban className="mr-1 h-3 w-3" />
                                {suspendingId === row.id ? "Suspending…" : "Suspend"}
                              </Button>
                            )}
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
    </div>
  );
}
