"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { PagedResponse } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { ClipboardList } from "lucide-react";

const PAGE_SIZE = 20;

interface AuditEntry {
  id: number;
  adminEmail: string;
  action: string;
  resourceType: string;
  resourceId: string;
  details: string;
  createdAt: string;
}

export default function AuditPage() {
  const [data, setData] = useState<PagedResponse<AuditEntry> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    api.admin
      .getAuditTrail(page, PAGE_SIZE)
      .then((res) => setData(res as unknown as PagedResponse<AuditEntry>))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Audit trail"
        description="Admin actions (flag, approve, reject, suspend, refund, delete) are logged here and to the backend AUDIT log."
        breadcrumbs
      />
      <Card>
        <CardHeader>
          <CardTitle>Admin actions</CardTitle>
        </CardHeader>
        <CardContent>
          {error && (
            <p className="mb-4 text-sm text-destructive">{error}</p>
          )}
          {loading ? (
            <div className="space-y-2">
              <Skeleton className="h-10 w-full" />
              {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : data && data.content.length > 0 ? (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Time</TableHead>
                    <TableHead>Admin</TableHead>
                    <TableHead>Action</TableHead>
                    <TableHead>Resource</TableHead>
                    <TableHead>Details</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.map((row) => (
                    <TableRow key={row.id}>
                      <TableCell className="whitespace-nowrap">
                        {new Date(row.createdAt).toLocaleString()}
                      </TableCell>
                      <TableCell>{row.adminEmail}</TableCell>
                      <TableCell>{row.action}</TableCell>
                      <TableCell>
                        {row.resourceType}
                        {row.resourceId ? ` #${row.resourceId}` : ""}
                      </TableCell>
                      <TableCell className="max-w-[300px] truncate" title={row.details}>
                        {row.details || "—"}
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
          ) : (
            <EmptyState
              icon={ClipboardList}
              title="No audit entries yet"
              description="Admin actions (flag story, approve, reject, suspend parent, refund invoice, delete story) will appear here."
            />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
