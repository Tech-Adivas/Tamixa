"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { ChildSummary, PagedResponse } from "@/types/api";
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
import { EmptyState } from "@/components/empty-state";
import { UserCircle } from "lucide-react";

const PAGE_SIZE = 20;

export default function ChildrenPage() {
  const [data, setData] = useState<PagedResponse<ChildSummary> | null>(null);
  const [page, setPage] = useState(0);
  const [parentId, setParentId] = useState<string>("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    const pid = parentId.trim() ? Number(parentId) : undefined;
    api.admin
      .getChildren(page, PAGE_SIZE, pid)
      .then(setData)
      .catch((e) => setError(e instanceof Error ? e.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, [page, parentId]);

  const handleFilter = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Child profiles</h1>
        <p className="text-muted-foreground">
          View child profiles. Filter by parent ID.
        </p>
      </div>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle>Children</CardTitle>
          <form onSubmit={handleFilter} className="flex gap-2">
            <Input
              placeholder="Parent ID (optional)"
              type="number"
              value={parentId}
              onChange={(e) => setParentId(e.target.value)}
              className="w-36"
            />
            <Button type="submit" size="sm">
              Filter
            </Button>
          </form>
        </CardHeader>
        <CardContent>
          {error && (
            <p className="mb-4 text-sm text-destructive">{error}</p>
          )}
          {loading ? (
            <p className="text-muted-foreground">Loading…</p>
          ) : data ? (
            data.content.length === 0 ? (
              <EmptyState
                icon={UserCircle}
                title="No children found"
                description="Try adjusting the parent ID filter or add child profiles."
              />
            ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>ID</TableHead>
                    <TableHead>Name</TableHead>
                    <TableHead>Parent</TableHead>
                    <TableHead>Age</TableHead>
                    <TableHead>Language</TableHead>
                    <TableHead>Created</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.map((row) => (
                    <TableRow key={row.id}>
                      <TableCell>{row.id}</TableCell>
                      <TableCell>{row.name}</TableCell>
                      <TableCell>
                        {row.parentEmail ?? row.parentId}
                      </TableCell>
                      <TableCell>{row.age}</TableCell>
                      <TableCell>{row.languagePreference ?? "—"}</TableCell>
                      <TableCell>
                        {new Date(row.createdAt).toLocaleString()}
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
            )
          ) : null}
        </CardContent>
      </Card>
    </div>
  );
}
